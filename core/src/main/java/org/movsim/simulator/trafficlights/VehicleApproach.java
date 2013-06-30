package org.movsim.simulator.trafficlights;

public class VehicleApproach {

    /** Fields used for priority calculation */
    public final int vehicleUrgency;

    public final double costOfStopping;

    public final double costOfDelay;

    public final int numberOfPassengers;

    public final double vehicleSpeed;

    public final double distanceToTrafficLight;

    public VehicleApproach(int urgency, double costOfStopping, double costOfDelay, int NoOfPassengers, double speed,
            double distance) {
        this.vehicleUrgency = urgency;
        this.costOfStopping = costOfStopping;
        this.costOfDelay = costOfDelay;
        this.numberOfPassengers = NoOfPassengers;
        this.vehicleSpeed = speed;
        this.distanceToTrafficLight = distance;
    }

}
