package org.movsim.simulator.trafficlights;

public interface ControlStrategy {

    /**
     * Evaluates the demand at the intersection based on control strategy.
     * Returns the next phase index that the TrafficLightControlGroup should set, or
     * -1 if phase changing should be skipped.
     * 
     * @return index of next phase in ControlGroup phase array, or -1.
     */
    public int getNextPhaseIndex();

    /**
     * Checks strategy conditions to ascertain if the current phase should
     * be changed now
     * 
     * @return true if phase change required immediately, false otherwise
     */
    public boolean checkNextPhaseRequest();

    /**
     * Update the current strategy duration, used to determine when phase
     * switching is required
     * 
     * @param dt
     *            - simulator timestep
     */
    public void update(double dt);

    /**
     * Call to reset timers, notifying strategy that the controller has
     * successfully completed the requested phase change
     */
    public void acknowledgeNextPhaseSet(int index);

    public String getName();

}
