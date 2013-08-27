package org.movsim.simulator.trafficlights;

import java.util.List;
import java.util.Map;

import org.movsim.autogen.Phase;
import org.movsim.autogen.TrafficControlStrategy;
import org.movsim.autogen.TrafficLightState;
import org.movsim.autogen.TrafficLightStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityLookaheadControlStrategy implements ControlStrategy {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(PriorityLookaheadControlStrategy.class);

    private final List<Phase> phases;

    private int currentPhaseIndex = 0;

    private int nextPhaseIndex = -1;

    private double currentPhaseDuration;

    private double currentGapDuration;

    private double currentMaximumDuration;

    private double currentExtendedGreenDuration;

    private double targetExtendedGreenTime;

    private final double conditionGapTime;

    private final double conditionRange;

    private final double conditionLookahead;

    /** mapping from the signal's name to the trafficlight, constructed by ControlGroup */
    private final Map<String, TrafficLight> trafficLights;

    public PriorityLookaheadControlStrategy(TrafficControlStrategy strategy, List<Phase> phases,
            Map<String, TrafficLight> trafficLights) {
        this.phases = phases;
        this.trafficLights = trafficLights;
        this.conditionGapTime = strategy.getGap();
        this.conditionRange = strategy.getRange();
        this.conditionLookahead = 10;
        this.targetExtendedGreenTime = 0;
    }

    @Override
    public void update(double dt) {
        currentPhaseDuration += dt;
        currentGapDuration += dt;
        if (nextPhaseIndex != -1) {
            currentMaximumDuration += dt;
        }

        if (targetExtendedGreenTime != 0 && !extendedGreenTimeConditionFulfilled()) {
            currentExtendedGreenDuration += dt;
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

    private void setNextPhaseIndex() {
        // set next to the current highest priority phase
        int targetPhaseIndex = highestPriorityPhase();

        if (targetPhaseIndex != currentPhaseIndex) {
            System.out.println("current phase index: " + currentPhaseIndex);
            System.out.println("priority strategy setting next phase to " + targetPhaseIndex);
            nextPhaseIndex = targetPhaseIndex;
        }
    }

    public void determineTargetExtendedGreenTime() {
        // System.out.println("Determining target extended green time");
        // step a second at a time from now to the lookahead range
        // for each step: calculate the overall cost of switching phases
        // and compare to the cost of switching phases at the request time
        // extend the green time for the current phase by the
        // step that gives the lowest overall cost of switching phases
        double minCost = Double.MAX_VALUE;
        int minLookahead = 0;
        for (int currentLookahead = 0; currentLookahead <= this.conditionLookahead; currentLookahead += 1) {
            double currentCost = 0.0;
            for (TrafficLight trafficLight : trafficLights.values()) {
                for (VehicleApproach vehicleApproach : trafficLight.getVehicleApproaches()) {
                    if (!vehicleApproach.estimatedClearanceWithinTime(currentLookahead)) {
                        currentCost += vehicleApproach.estimatedDelayCost(currentLookahead);
                        currentCost += vehicleApproach.estimatedStoppingCost(currentLookahead);
                    }
                }
            }
            // System.out.println("Estimated cost for lookahead " + currentLookahead + ": " + currentCost);

            if (currentCost < minCost) {
                minCost = currentCost;
                minLookahead = currentLookahead;
            }
        }

        this.targetExtendedGreenTime = minLookahead;
        // System.out.println("Optimal lookahead for current phase: " + minLookahead);
    }

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
            LOG.debug("phase: " + i + ", currentCost: " + currentPhaseCost);
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
                currentApproachStoppingCost += trafficLights.get(state.getName()).getApproachCost();
            }
        }
        if (currentApproachStoppingCost < highestApproachCost) {
            LOG.debug("hp phase stopping cost highest: " + highestPhaseIndex + ", " + highestApproachCost + " > "
                    + currentApproachStoppingCost);
            return highestPhaseIndex;
        }
        return currentPhaseIndex;
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
        if (phaseMinimumConditionFulfilled(currentPhase) && targetExtendedGreenTime <= 0) {
            // we would switch now, but we can check if the green time should be extended
            determineTargetExtendedGreenTime();
        }

        return (phaseMinimumConditionFulfilled(currentPhase) && extendedGreenTimeConditionFulfilled());
    }

    @Override
    public void acknowledgeNextPhaseSet(int index) {
        currentPhaseIndex = index;
        nextPhaseIndex = -1;
        currentPhaseDuration = 0;
        currentGapDuration = 0;
        currentMaximumDuration = 0;
        currentExtendedGreenDuration = 0;
        targetExtendedGreenTime = 0;
    }

    public int getNextPhaseIndex() {
        return nextPhaseIndex;
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

    public boolean extendedGreenTimeConditionFulfilled() {
        return (currentExtendedGreenDuration >= targetExtendedGreenTime);
    }

    public String getName() {
        return "Priority Lookahead";
    }

}
