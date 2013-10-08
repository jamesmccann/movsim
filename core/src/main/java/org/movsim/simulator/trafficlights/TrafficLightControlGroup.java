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

    public final ControlStrategy controlStrategy;

    private final String groupId;

    private final String firstSignalId; // for logging

    private int currentPhaseIndex = 0;

    private int nextPhaseIndex = -1;

    private double currentPhaseDuration;

    private double currentIntergreenDuration;

    private double currentAllRedDuration;

    private boolean intergreen;

    private boolean allRed;

    private double cumulativeStoppingCost;

    private double cumulativeDelayCost;

    private int phaseCount;

    private static final int logTimeInterval = 3600;

    private double logTimeDuration;

    /** mapping from the signal's name to the trafficlight */
    private final Map<String, TrafficLight> trafficLights = new HashMap<>();

    TrafficLightControlGroup(ControllerGroup controllerGroup, String firstSignalId) {
        Preconditions.checkNotNull(controllerGroup);
        this.groupId = controllerGroup.getId();
        this.firstSignalId = firstSignalId;
        this.phaseCount = 0;

        this.phases = ImmutableList.copyOf(controllerGroup.getPhase()); // deep copy
        createTrafficlights();

        // TODO this could be improved
        TrafficControlStrategy strategy = controllerGroup.getTrafficControlStrategy();
        if (strategy.getType().equals("VehicleActuated")) {
            controlStrategy = new VehicleActuatedControlStrategy(strategy, phases, trafficLights);
        } else if (strategy.getType().equals("PriorityActuated")) {
            controlStrategy = new PriorityActuatedControlStrategy(strategy, phases, trafficLights);
        } else if (strategy.getType().equals("PriorityLookahead")) {
            controlStrategy = new PriorityLookaheadControlStrategy(strategy, phases, trafficLights);
        } else if (strategy.getType().equals("SCATSData")) {
            controlStrategy = new ParsedSCATSDataControlStrategy(strategy, phases, trafficLights);
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
        logTimeDuration += dt;
        if (intergreen) { currentIntergreenDuration += dt; }
        if (allRed) { currentAllRedDuration += dt; }

        controlStrategy.update(dt);
 
        determinePhase();
        updatePhase(simulationTime, iterationCount);
        updateTrafficLightApproaches(dt);
        if (recordDataCallback != null && logTimeDuration >= logTimeInterval) {
            logTimeDuration = 0;
            recordDataCallback.recordTimeInterval(simulationTime);
        }
    }

    private void determinePhase() {
        if (nextPhaseIndex == -1 && controlStrategy.checkNextPhaseRequest()) {
            nextPhaseIndex = controlStrategy.getNextPhaseIndex();
        }
    }

    public void updatePhase(double simulationTime, long iterationCount) {
        if (nextPhaseIndex == -1) { return; } //no update required

        Phase phase = phases.get(currentPhaseIndex);
        // check if we need to initiate a phase change
        if (!intergreen && currentIntergreenDuration == 0) {
            LOG.debug("setting current phase to intergreen");
            setIntergreen(phase);
        }

        // check if a current intergreen period has finished, start all red period
        if (!allRed && currentIntergreenDuration >= phase.getIntergreen()) {
            LOG.debug("setting current phase to allred");
            setAllRed(phase);
        }

        // check if a current all red period has finished, go to the next phase
        if (currentAllRedDuration >= phase.getAllRed()) {
            LOG.debug("setting next phase");
            nextPhase(simulationTime, iterationCount);
        }
    }

    public void nextPhase(double simulationTime, long iterationCount) {
        // before changing, output the current phase
        recordDataCallback.recordPhase(simulationTime, iterationCount, phaseCount, controlStrategy,
                trafficLights.values());
        phaseCount += 1;

        nextPhase();

        // after changing, reset the phase costs for each traffic light
        for (TrafficLight trafficLight : trafficLights.values()) {
            trafficLight.resetPhaseCosts();
        }
    }

    @Override
    public void nextPhase() {
        // if next phase index is not set, just increment a phase
        if (nextPhaseIndex != -1) {
            currentPhaseIndex = nextPhaseIndex;
            nextPhaseIndex = -1;
        } else {
            currentPhaseIndex += 1;
            if (currentPhaseIndex == phases.size()) {
                currentPhaseIndex = 0;
            }
        }

        intergreen = false;
        allRed = false;
        currentPhaseDuration = 0;
        currentIntergreenDuration = 0;
        currentAllRedDuration = 0;
        controlStrategy.acknowledgeNextPhaseSet(currentPhaseIndex);
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
        public void recordVehicleApproach(VehicleApproach approach);

        public void recordData(double simulationTime, long iterationCount, Iterable<TrafficLight> trafficLights);

        public void recordTimeInterval(double simulationTime);

        public void recordPhase(double simulationTime, long iterationCount, int phaseCount,
                ControlStrategy controlStrategy,
                Iterable<TrafficLight> trafficLights);

        public void recordComplete();
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

    @Override
    public double getGapTime() {
        return 0.0;
    }
    
    public int getNextPhase() {
        return controlStrategy.getNextPhaseIndex();
    }

    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public Phase getCurrentPhase() {
        return phases.get(currentPhaseIndex);
    }

    public double getTotalStoppingCost() {
        double totalCost = 0.0;
        for (TrafficLight trafficLight : trafficLights.values()) {
            totalCost += trafficLight.getApproachCost(0);
        }
        return totalCost;
    }

    public double getTotalDelayCost() {
        double totalCost = 0.0;
        for (TrafficLight trafficLight : trafficLights.values()) {
            totalCost += trafficLight.getDelayCost();
        }
        return totalCost;
    }

    public double getTotalCost() {
        double totalCost = 0.0;
        for (TrafficLight trafficLight : trafficLights.values()) {
            totalCost += trafficLight.getDelayCost();
            totalCost += trafficLight.getApproachCost(0);
        }
        return totalCost;
    }

    public double getCumulativeStoppingCost() {
        double cumulativeStoppingCost = 0.0;
        for (TrafficLight trafficLight : trafficLights.values()) {
            cumulativeStoppingCost += trafficLight.getCumulativeStoppingCost();
        }
        return cumulativeStoppingCost;
    }

    public double getCumulativeDelayCost() {
        double cumulativeDelayCost = 0.0;
        for (TrafficLight trafficLight : trafficLights.values()) {
            cumulativeDelayCost += trafficLight.getCumulativeDelayCost();
        }
        return cumulativeDelayCost;
    }

    public Map<Integer, Double> getCumulativeDelayTimesPerUrgency() {
        Map<Integer, Double> delayTimesPerUrgency = new HashMap<Integer, Double>();
        for (int i = 1; i <= 5; i++) {
            delayTimesPerUrgency.put(i, 0.0);
        }

        for (TrafficLight trafficLight : trafficLights.values()) {
            for (int i = 1; i <= 5; i++) {
                delayTimesPerUrgency.put(i, delayTimesPerUrgency.get(i) + trafficLight.getCumulativeDelayTime(i));
            }
        }
        return delayTimesPerUrgency;
    }

    public Map<Integer, Integer> getCumulativeNumberOfVehiclesPerUrgency() {
        Map<Integer, Integer> vehiclesPerUrgency = new HashMap<Integer, Integer>();
        for (int i = 1; i <= 5; i++) {
            vehiclesPerUrgency.put(i, 0);
        }

        for (TrafficLight trafficLight : trafficLights.values()) {
            for (int i = 1; i <= 5; i++) {
                vehiclesPerUrgency.put(i, vehiclesPerUrgency.get(i) + trafficLight.getCumulativeNumberOfVehicles(i));
            }
        }
        return vehiclesPerUrgency;
    }

    public Map<Integer, Double> getAverageDelayTimePerUrgency() {
        Map<Integer, Integer> vehiclesPerUrgency = getCumulativeNumberOfVehiclesPerUrgency();
        Map<Integer, Double> delayTimesPerUrgency = getCumulativeDelayTimesPerUrgency();
        Map<Integer, Double> avgDelayTimesPerUrgency = new HashMap<Integer, Double>();

        for (int i = 1; i <= 5; i++) {
            if (vehiclesPerUrgency.get(i) > 0) {
                double avgDelayTime = delayTimesPerUrgency.get(i) / (1.0 * vehiclesPerUrgency.get(i));
                avgDelayTimesPerUrgency.put(i, avgDelayTime);
            } else {
                avgDelayTimesPerUrgency.put(i, 0.0);
            }
        }
        return avgDelayTimesPerUrgency;
    }

    @Override
    public void recordVehicleApproach(VehicleApproach va) {
        recordDataCallback.recordVehicleApproach(va);
    }

    public void recordComplete() {
        recordDataCallback.recordComplete();
    }
}
