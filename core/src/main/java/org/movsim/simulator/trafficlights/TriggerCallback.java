package org.movsim.simulator.trafficlights;

public interface TriggerCallback {

    /**
     * Triggers (interactively) the next phase of the controller group.
     */
    void nextPhase();

    /**
     * Get the current phase time of the controller group
     */
    double getPhaseTime();

    /**
     * Get the current gap timer of the controller group
     */
    double getGapTime();

}
