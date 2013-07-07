package org.movsim.simulator.trafficlights;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.movsim.autogen.ControllerGroup;
import org.movsim.autogen.Phase;
import org.movsim.autogen.TrafficControlStrategy;
import org.movsim.autogen.TrafficLightState;
import org.movsim.autogen.TrafficLightStatus;
import org.movsim.simulator.SimulationTimeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class TrafficLightControlGroup implements SimulationTimeStep, TriggerCallback {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(TrafficLightControlGroup.class);

    private final List<Phase> phases;

    private final ControlStrategy controlStrategy;

    private final String groupId;

    private final String firstSignalId; // for logging

    private int currentPhaseIndex = 0;

    private int nextPhaseIndex = -1;

    private double currentPhaseDuration;

    private double currentIntergreenDuration;

    private double currentAllRedDuration;

    private boolean intergreen;

    private boolean allRed;

    /** mapping from the signal's name to the trafficlight */
    private final Map<String, TrafficLight> trafficLights = new HashMap<>();

    TrafficLightControlGroup(ControllerGroup controllerGroup, String firstSignalId) {
        Preconditions.checkNotNull(controllerGroup);
        this.groupId = controllerGroup.getId();
        this.firstSignalId = firstSignalId;

        this.phases = ImmutableList.copyOf(controllerGroup.getPhase()); // deep copy
        createTrafficlights();

        // TODO this could be improved
        TrafficControlStrategy strategy = controllerGroup.getTrafficControlStrategy();
        if (strategy.getType() == "VehicleActuated") {
            controlStrategy = new VehicleActuatedControlStrategy(strategy, phases, trafficLights);
        } else if (strategy.getType() == "PriorityActuated") {
            controlStrategy = new PriorityActuatedControlStrategy(strategy, phases, trafficLights);
        } else {
            // default is VehicleActuated
            controlStrategy = new VehicleActuatedControlStrategy(strategy, phases, trafficLights);
        }

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
        if (intergreen) { currentIntergreenDuration += dt; }
        if (allRed) { currentAllRedDuration += dt; }

        controlStrategy.update(dt);
 
        determinePhase();
        updateTrafficLightApproaches(dt);
        if (recordDataCallback != null) {
            recordDataCallback.recordData(simulationTime, iterationCount, trafficLights.values());
        }
    }

    private void determinePhase() {
        if (controlStrategy.checkNextPhaseRequest()) {
            updatePhase();
        }
    }

    public void updatePhase() {
        nextPhaseIndex = controlStrategy.getNextPhaseIndex();
        if (nextPhaseIndex == -1) { return; } //no update required
        Phase phase = phases.get(currentPhaseIndex);
        
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
        currentPhaseDuration = 0;
        currentIntergreenDuration = 0;
        currentAllRedDuration = 0;
        controlStrategy.acknowledgeNextPhaseSet();
        updateTrafficLights(phases.get(currentPhaseIndex));
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

    private void updateTrafficLights(Phase actualPhase) {
        for (TrafficLightState trafficLightState : actualPhase.getTrafficLightState()) {
            trafficLights.get(trafficLightState.getName()).setState(trafficLightState.getStatus());
        }
    }

    Iterable<TrafficLight> trafficLights() {
        return ImmutableList.copyOf(trafficLights.values().iterator());
    }

    TrafficLight getTrafficLight(String signalName) {
        return Preconditions.checkNotNull(trafficLights.get(signalName), "signalName=\"" + signalName
                + "\" not defined in controllerGroup=" + groupId);
    }

    private void updateTrafficLightApproaches(double dt) {
        for (TrafficLight trafficLight : trafficLights.values()) {
            trafficLight.updateVehicleApproaches(dt);
        }
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

    public String groupId() {
        return groupId;
    }

    String firstSignalId() {
        return firstSignalId;
    }

}
