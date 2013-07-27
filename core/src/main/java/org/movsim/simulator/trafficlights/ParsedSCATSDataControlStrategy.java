package org.movsim.simulator.trafficlights;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.movsim.autogen.Phase;
import org.movsim.autogen.TrafficControlStrategy;
import org.movsim.simulator.roadnetwork.SCATSFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedSCATSDataControlStrategy implements ControlStrategy {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(ParsedSCATSDataControlStrategy.class);

    private final List<Phase> phases;

    private int currentPhaseIndex = 0;

    private int nextPhaseIndex = -1;

    private double currentPhaseDuration;

    private double targetPhaseDuration;

    private double currentCycleDuration;

    public double targetCycleDuration;

    public Map<String, Integer> targetPhaseDurations;
    
    public SCATSFileReader scatsFileReader;

    public boolean nextCycleOnPhaseChange;

    /** mapping from the signal's name to the trafficlight, constructed by ControlGroup */
    private final Map<String, TrafficLight> trafficLights;

    public ParsedSCATSDataControlStrategy(TrafficControlStrategy strategy, List<Phase> phases,
            Map<String, TrafficLight> trafficLights) {
        this.phases = phases;
        this.trafficLights = trafficLights;

        // immediately trigger update upon initialization
        targetCycleDuration = -1;
        currentPhaseIndex = -1;
        nextPhaseIndex = -1;
        targetPhaseDuration = -1;
        targetPhaseDurations = new HashMap<String, Integer>();
        nextCycleOnPhaseChange = true;
    }

    @Override
    public void update(double dt) {
        if (scatsFileReader == null) {
            return; //can't update
        }

        // check for initialization
        if (currentPhaseIndex == -1 && nextPhaseIndex == -1) {
            LOG.info("SCATS Data Strategy requesting new phase times");
            scatsFileReader.update();

            // initialize to first phase in targets
            for (Map.Entry<String, Integer> phaseTime : targetPhaseDurations.entrySet()) {
                String id = phaseTime.getKey();
                inner: for (int i = 0; i < phases.size(); i++) {
                    if (phases.get(i).getId().equals(id)) {
                        currentPhaseIndex = i;
                        break inner;
                    }
                }
                targetPhaseDuration = phaseTime.getValue();
                break; // first only
            }
            
            Phase currentPhase = phases.get(currentPhaseIndex);
            targetPhaseDurations.remove(currentPhase.getId());
        }

        currentPhaseDuration += dt;
        currentCycleDuration += dt;

        if (nextPhaseIndex == -1) {
            determineNextPhaseIndex();
        }

        Phase currentPhase = phases.get(currentPhaseIndex);
        // System.out.println("Current Phase: " + currentPhase.getId() + ", duration: "
        // + String.format("%.2f", currentPhaseDuration) + ", cycle: "
        // + String.format("%.2f", currentCycleDuration) + ", targetPhase: " + targetPhaseDuration
        // + ", targetCycle " + targetCycleDuration);
    }

    @Override
    public int getNextPhaseIndex() {
        return nextPhaseIndex;
    }

    @Override
    public boolean checkNextPhaseRequest() {
        return (nextPhaseIndex != -1);
    }

    public void determineNextPhaseIndex() {
        Phase currentPhase = phases.get(currentPhaseIndex);
        if (currentPhaseDuration + currentPhase.getAllRed() + currentPhase.getIntergreen() >= targetPhaseDuration) {
            System.out.println(targetPhaseDurations.toString());
            if (targetPhaseDurations.isEmpty()) {
                // trigger change to begin
                LOG.info("SCATS Data Strategy setting next phase index");
                scatsFileReader.update();
                nextCycleOnPhaseChange = true;
            }

            int targetPhaseIndex = currentPhaseIndex + 1;
            while (targetPhaseIndex != currentPhaseIndex) {
                if (targetPhaseIndex == phases.size()) {
                    targetPhaseIndex = 0;
                    continue; // trigger check against current again
                }

                Phase targetPhase = phases.get(targetPhaseIndex);
                if (targetPhaseDurations.containsKey(targetPhase.getId())) {
                    nextPhaseIndex = targetPhaseIndex; 
                    targetPhaseDuration = targetPhaseDurations.get(targetPhase.getId());

                    // remove the current phase from the targetPhaseDurations
                    targetPhaseDurations.remove(targetPhase.getId());
                    break;
                }
                targetPhaseIndex++;
            }

            // no other phase found, current phase is extended into the next cycle
            if (targetPhaseIndex == currentPhaseIndex && nextPhaseIndex == -1) {
                // no change, but reset phase counter
                currentPhaseDuration = 0;
                if (nextCycleOnPhaseChange) {
                    currentCycleDuration = 0;
                }
                targetPhaseDuration = targetPhaseDurations.get(phases.get(currentPhaseIndex).getId());
                targetPhaseDurations.remove(phases.get(currentPhaseIndex).getId());
            }
        }
    }

    @Override
    public void acknowledgeNextPhaseSet(int index) {
        // advance phase and cycle counter if required
        currentPhaseDuration = 0;
        currentPhaseIndex = nextPhaseIndex;
        if (nextCycleOnPhaseChange) {
            currentCycleDuration = 0;
            nextCycleOnPhaseChange = false;
        }
        nextPhaseIndex = -1;
    }

    @Override
    public String getName() {
        return "SCATS Data Strategy";
    }

}
