package org.movsim.simulator.trafficlights;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.movsim.autogen.ControllerGroup;
import org.movsim.autogen.Phase;
import org.movsim.autogen.TrafficLightCondition;
import org.movsim.autogen.TrafficLightState;
import org.movsim.autogen.TrafficLightStatus;
import org.movsim.simulator.SimulationTimeStep;
import org.movsim.simulator.roadnetwork.LaneSegment;
import org.movsim.simulator.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class TrafficLightControlGroup implements SimulationTimeStep, TriggerCallback {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(TrafficLightControlGroup.class);

    private final List<Phase> phases;

    private final String groupId;

    private final String firstSignalId; // for logging

    private int currentPhaseIndex = 0;

    private int nextPhaseIndex = -1;

    private double currentPhaseDuration;

    private double currentIntergreenDuration;

    private double currentAllRedDuration;

    private double currentGapTime;
    
    private double currentMaximumDuration;

    private final double conditionRange;

    private final double conditionGapTime;

    private boolean intergreen;

    private boolean allRed;

    /** mapping from the signal's name to the trafficlight */
    private final Map<String, TrafficLight> trafficLights = new HashMap<>();

    TrafficLightControlGroup(ControllerGroup controllerGroup, String firstSignalId) {
        Preconditions.checkNotNull(controllerGroup);
        this.groupId = controllerGroup.getId();
        this.firstSignalId = firstSignalId;
        this.conditionRange = controllerGroup.getRange();
        this.conditionGapTime = controllerGroup.getGap();
        this.phases = ImmutableList.copyOf(controllerGroup.getPhase()); // deep copy
        createTrafficlights();
        updateTrafficLights(phases.get(currentPhaseIndex));
    }

    private void createTrafficlights() {
        for (Phase phase : phases) {
            TrafficLight trafficLight = null;
            for (TrafficLightState trafficlightState : phase.getTrafficLightState()) {
                String name = Preconditions.checkNotNull(trafficlightState.getName());
                trafficLight = trafficLights.get(name);
                if (trafficLight == null) {
                    trafficLight = new TrafficLight(name, groupId, this);
                    trafficLights.put(name, trafficLight);

                    // need amber state during intergreen period, created once only
                    if (phase.getIntergreen() > 0) {
                        trafficLight.addPossibleState(TrafficLightStatus.AMBER);
                    }
                }
                trafficLight.addPossibleState(trafficlightState.getStatus());
            }
        }
    }

    @Override
    public void timeStep(double dt, double simulationTime, long iterationCount) {
        currentPhaseDuration += dt;
        currentGapTime += dt;
        if (intergreen) { currentIntergreenDuration += dt; }
        if (allRed) { currentAllRedDuration += dt; }
        if (nextPhaseIndex != -1) { currentMaximumDuration += dt; }

        Phase phase = phases.get(currentPhaseIndex);
        updateGapTimer(phase);
        determinePhase(phase);
        if (recordDataCallback != null) {
            recordDataCallback.recordData(simulationTime, iterationCount, trafficLights.values());
        }
    }

    private void determinePhase(Phase phase) {
        // if the current phase has extended beyond its minimum time
        // and a phase change is possible, check if any other phases
        // are demanded and assign the next target phase

        // if the next phase has been set, check to see if the change
        // should be made based on gap timer or max green conditions
        if (nextPhaseIndex == -1) {
            setNextPhaseIndex();
        } else if (phaseMinimumConditionFulfilled(phase)
                && (gapTimeoutConditionFulfilled() || phaseMaximumConditionFulfilled(phase))) {
            updatePhase(phase);
        }
    }

    public void updatePhase(Phase phase) {
        if (nextPhaseIndex == -1) { return; } //no update required
        
        // check if we need to initiate a phase change
        if (!intergreen && currentIntergreenDuration == 0) {
            LOG.info("setting current phase to intergreen");
            setIntergreen(phase);
        }

        // check if a current intergreen period has finished, start all red period
        if (!allRed && currentIntergreenDuration >= phase.getIntergreen()) {
            LOG.info("setting current phase to allred");
            setAllRed(phase);
        }

        // check if a current all red period has finished, go to the next phase
        if (currentAllRedDuration >= phase.getAllRed()) {
            LOG.info("setting next phase");
            nextPhase();
        }
    }

    @Override
    public void nextPhase() {
        currentPhaseIndex = nextPhaseIndex;
        nextPhaseIndex = -1;
        intergreen = false;
        allRed = false;
        currentGapTime = 0;
        currentPhaseDuration = 0;
        currentIntergreenDuration = 0;
        currentAllRedDuration = 0;
        currentMaximumDuration = 0;
        updateTrafficLights(phases.get(currentPhaseIndex));
    }


    private boolean isTriggerConditionFullfilled(Phase phase) {
        for (TrafficLightState state : phase.getTrafficLightState()) {
            TrafficLight light = trafficLights.get(state.getName());
            if (state.getCondition() == TrafficLightCondition.REQUEST && vehicleIsInFrontOfLight(light)) {
                LOG.debug("trigger fulfilled for trafficLight " + state.getName());
                return true;
            }
        }
        return false;
    }

    private boolean vehicleIsInFrontOfLight(TrafficLight trafficLight) {
        for (LaneSegment laneSegment : trafficLight.roadSegment().laneSegments()) {
            Vehicle vehicle = laneSegment.rearVehicle(trafficLight.position());
            if (vehicle != null && (trafficLight.position() - vehicle.getFrontPosition() < conditionRange)) {
                LOG.debug("condition check: vehicle is in front of trafficlight: vehPos={}, trafficlightPos={}",
                        vehicle.getFrontPosition(), trafficLight.position());
                return true;
            }
        }
        return false;
    }

    private boolean vehicleIsInFrontOfLightAndDriving(TrafficLight trafficLight) {
        for (LaneSegment laneSegment : trafficLight.roadSegment().laneSegments()) {
            Vehicle vehicle = laneSegment.rearVehicle(trafficLight.position());
            if (vehicle != null && (trafficLight.position() - vehicle.getFrontPosition() < conditionRange)
                    && vehicle.getSpeed() > 0) {
                LOG.debug("condition check: vehicle is in front of trafficlight: vehPos={}, trafficlightPos={}",
                        vehicle.getFrontPosition(), trafficLight.position());
                return true;
            }
        }
        return false;
    }

    private void updateTrafficLights(Phase actualPhase) {
        for (TrafficLightState trafficLightState : actualPhase.getTrafficLightState()) {
            trafficLights.get(trafficLightState.getName()).setState(trafficLightState.getStatus());
        }
    }
    
    private void updateGapTimer(Phase phase) {
        for (TrafficLightState state : phase.getTrafficLightState()) {
            if (state.getStatus() == TrafficLightStatus.GREEN) {
                if (vehicleIsInFrontOfLight(trafficLights.get(state.getName()))) {
                    currentGapTime = 0;
                    return;
                }
            }
        }
    }

    private void setIntergreen(Phase phase) {
        // set any green lights to yellow for the duration of the intergreen period
        // red lights should not change
        for (TrafficLightState state : phase.getTrafficLightState()) {
            if (state.getStatus() == TrafficLightStatus.GREEN) {
                trafficLights.get(state.getName()).setState(TrafficLightStatus.AMBER);
            }
        }
        intergreen = true;
    }

    private void setAllRed(Phase phase) {
        // set all lights red for the duration of the all_red period
        for (TrafficLightState state : phase.getTrafficLightState()) {
            trafficLights.get(state.getName()).setState(TrafficLightStatus.RED);
        }
        allRed = true;
    }

    public boolean gapTimeoutConditionFulfilled() {
        return currentGapTime > conditionGapTime;
    }

    public boolean phaseDurationConditionFulfilled(Phase phase) {
        return currentPhaseDuration + phase.getIntergreen() + phase.getAllRed() >= phase.getDuration();
    }

    public boolean phaseMaximumConditionFulfilled(Phase phase) {
        return currentMaximumDuration + phase.getIntergreen() + phase.getAllRed() > phase.getMax();
    }

    public boolean phaseMinimumConditionFulfilled(Phase phase) {
        return currentPhaseDuration > phase.getMin();
    }

    Iterable<TrafficLight> trafficLights() {
        return ImmutableList.copyOf(trafficLights.values().iterator());
    }

    TrafficLight getTrafficLight(String signalName) {
        return Preconditions.checkNotNull(trafficLights.get(signalName), "signalName=\"" + signalName
                + "\" not defined in controllerGroup=" + groupId);
    }

    private void setNextPhaseIndex() {
        // attempt to move to the "next" available phase based on
        // ordering of phase index. Continue until a phase with
        // demand is chosen
        int targetPhaseIndex = currentPhaseIndex;
        while (true) {
            if (targetPhaseIndex == phases.size() - 1) {
                targetPhaseIndex = 0;
            } else {
                targetPhaseIndex++;
            }

            if (targetPhaseIndex == currentPhaseIndex) {
                break;
            }

            Phase targetPhase = phases.get(targetPhaseIndex);
            if (isTriggerConditionFullfilled(targetPhase)) {
                break;
            }
        }

        if (targetPhaseIndex != currentPhaseIndex) {
            nextPhaseIndex = targetPhaseIndex;
        }
    }

    public String groupId() {
        return groupId;
    }

    String firstSignalId() {
        return firstSignalId;
    }

    public interface RecordDataCallback {
        /**
         * Callback to allow the application to process or record the traffic light data.
         * 
         * @param simulationTime
         *            the current logical time in the simulation
         * @param iterationCount
         * @param trafficLights
         */
        public void recordData(double simulationTime, long iterationCount, Iterable<TrafficLight> trafficLights);
    }

    private RecordDataCallback recordDataCallback;

    public void setRecorder(RecordDataCallback recordDataCallback) {
        this.recordDataCallback = recordDataCallback;
    }

    public double getPhaseTime() {
        return currentPhaseDuration;
    }

    public double getGapTime() {
        return currentGapTime;
    }

}
