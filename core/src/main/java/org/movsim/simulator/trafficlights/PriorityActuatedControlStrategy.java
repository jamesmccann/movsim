package org.movsim.simulator.trafficlights;

import java.util.List;
import java.util.Map;

import org.movsim.autogen.Phase;
import org.movsim.autogen.TrafficControlStrategy;
import org.movsim.autogen.TrafficLightState;
import org.movsim.autogen.TrafficLightStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityActuatedControlStrategy implements ControlStrategy {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(PriorityActuatedControlStrategy.class);

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

    public PriorityActuatedControlStrategy(TrafficControlStrategy strategy, List<Phase> phases,
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
        if (nextPhaseIndex != -1) {
            currentMaximumDuration += dt;
        }

        Phase phase = phases.get(currentPhaseIndex);
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

    // private Map.Entry<String, TrafficLight> highestPriorityApproach() {
    // double highestPriority = Double.MIN_VALUE;
    // Map.Entry<String, TrafficLight> highestPriorityApproach = null;
    // for (Map.Entry<String, TrafficLight> trafficLight : trafficLights.entrySet()) {
    // double currentPriority = trafficLight.getValue().getApproachCost();
    // if (currentPriority > highestPriority) {
    // highestPriority = currentPriority;
    // highestPriorityApproach = trafficLight;
    // }
    // }
    // return highestPriorityApproach;
    // }

    private int highestPriorityPhase() {
        // for each phase
        // - add up all approach priorities
        // - find the phase which satisfies the approaches with the highest cost
        // - initial assumption is the highest priorty phase is the current one
        double highestApproachCost = Double.MIN_VALUE;
        int highestPhaseIndex = currentPhaseIndex;
        for (int i = 0; i < phases.size(); i++) {
            if (i == currentPhaseIndex) {
                continue;
            }
            Phase phase = phases.get(i);
            double currentPhaseCost = 0.0;
            for (TrafficLightState state : phase.getTrafficLightState()) {
                // look for approaches that will be made green by this phase
                // (but are currently red)
                if (state.getStatus() == TrafficLightStatus.GREEN) {
                    currentPhaseCost += trafficLights.get(state.getName()).getDelayCost();
                }
            }
            LOG.info("phase: " + i + ", currentCost: " + currentPhaseCost);
            if (currentPhaseCost > highestApproachCost) {
                highestApproachCost = currentPhaseCost;
                highestPhaseIndex = i;
            }
        }

        // compare delay cost to cost of stopping current phase
        // whichever is greater wins
        Phase currentPhase = phases.get(currentPhaseIndex);
        double currentApproachStoppingCost = 0.0;
        for (TrafficLightState state : currentPhase.getTrafficLightState()) {
            // look for approaches that will be made green by this phase
            // (but are currently red)
            if (state.getStatus() == TrafficLightStatus.GREEN) {
                currentApproachStoppingCost += trafficLights.get(state.getName()).getApproachCost(0);
            }
        }
        if (currentApproachStoppingCost < highestApproachCost) {
            LOG.info("hp phase stopping cost highest: " + highestPhaseIndex + ", " + highestApproachCost + " > "
                    + currentApproachStoppingCost);
            return highestPhaseIndex;
        }
        return currentPhaseIndex;
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

    private void setNextPhaseIndex() {
        if (nextPhaseIndex != -1) { return; }
        // set next to the current highest priority phase
        int targetPhaseIndex = highestPriorityPhase();
        System.out.println("current phase index: " + currentPhaseIndex);
        System.out.println("priority strategy setting next phase to " + targetPhaseIndex);

        if (targetPhaseIndex != currentPhaseIndex) {
            nextPhaseIndex = targetPhaseIndex;
        }
    }

    @Override
    /** 
     * If the next phase has been set, check to see if the change
     * should be made based on gap timer or max green conditions
     */
    public boolean checkNextPhaseRequest() {
        if (nextPhaseIndex == -1) {
            return false;
        }

        // If we have a next phase set and the minimum has been exceeded
        // then we can switch
        Phase currentPhase = phases.get(currentPhaseIndex);
        return (phaseMinimumConditionFulfilled(currentPhase));
    }

    @Override
    public void acknowledgeNextPhaseSet(int index) {
        currentPhaseIndex = index;
        nextPhaseIndex = -1;
        currentPhaseDuration = 0;
        currentGapDuration = 0;
        currentMaximumDuration = 0;
    }

    @Override
    public int getNextPhaseIndex() {
        return nextPhaseIndex;
    }

    public String getName() {
        return "Priority Actuated";
    }

}
