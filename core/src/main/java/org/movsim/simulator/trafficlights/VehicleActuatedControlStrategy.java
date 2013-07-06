package org.movsim.simulator.trafficlights;

import java.util.List;
import java.util.Map;

import org.movsim.autogen.Phase;
import org.movsim.autogen.TrafficControlStrategy;
import org.movsim.autogen.TrafficLightCondition;
import org.movsim.autogen.TrafficLightState;
import org.movsim.autogen.TrafficLightStatus;
import org.movsim.simulator.roadnetwork.LaneSegment;
import org.movsim.simulator.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleActuatedControlStrategy implements ControlStrategy {
    
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(VehicleActuatedControlStrategy.class);

    private final List<Phase> phases;
    
    private int currentPhaseIndex = 0;

    private int nextPhaseIndex = -1;

    private double currentPhaseDuration;

    private double currentGapDuration;

    private double currentMaximumDuration;

    private final double conditionGapTime;
    
    private final double conditionRange;

    /** mapping from the signal's name to the trafficlight, constructed by ControlGroup */
    private final Map<String, TrafficLight> trafficLights;
    
    public VehicleActuatedControlStrategy(TrafficControlStrategy strategy, List<Phase> phases,
            Map<String, TrafficLight> trafficLights) {
        this.phases = phases;
        this.trafficLights = trafficLights;
        this.conditionGapTime = strategy.getGap();
        this.conditionRange = strategy.getRange();
    }

    @Override
    public void update(double dt) {
        currentPhaseDuration += dt;
        currentGapDuration += dt;
        if (nextPhaseIndex != -1) { currentMaximumDuration += dt; }

        Phase phase = phases.get(currentPhaseIndex);
        updateGapTimer(phase);
        determinePhase(phase);
    }

    public void determinePhase(Phase phase) {
        // if the current phase has extended beyond its minimum time
        // and a phase change is possible, check if any other phases
        // are demanded and assign the next target phase
        if (nextPhaseIndex == -1) {
            setNextPhaseIndex();
        }
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

    public boolean gapTimeoutConditionFulfilled() {
        return currentGapDuration > conditionGapTime;
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

    private void updateGapTimer(Phase phase) {
        for (TrafficLightState state : phase.getTrafficLightState()) {
            if (state.getStatus() == TrafficLightStatus.GREEN) {
                if (vehicleIsInFrontOfLight(trafficLights.get(state.getName()))) {
                    currentGapDuration = 0;
                    return;
                }
            }
        }
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
            System.out.println("current phase index: " + currentPhaseIndex);
            System.out.println("vehicle acutated strategy setting next phase to " + targetPhaseIndex);
            nextPhaseIndex = targetPhaseIndex;
        }
    }

    public int getNextPhaseIndex() {
        return nextPhaseIndex;
    }

    @Override
    /** 
     * If the next phase has been set, check to see if the change
     * should be made based on gap timer or max green conditions
     */
    public boolean checkNextPhaseRequest() {
        if (nextPhaseIndex == -1) { return false; }
        Phase currentPhase = phases.get(currentPhaseIndex);
        return (phaseMinimumConditionFulfilled(currentPhase) && (gapTimeoutConditionFulfilled() 
                || phaseMaximumConditionFulfilled(currentPhase)));
    }

    @Override
    public void acknowledgeNextPhaseSet() {
        currentPhaseIndex = nextPhaseIndex;
        nextPhaseIndex = -1;
        currentPhaseDuration = 0;
        currentGapDuration = 0;
        currentMaximumDuration = 0;
    }

}
