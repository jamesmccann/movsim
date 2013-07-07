package org.movsim.simulator.trafficlights;

public class VehicleApproach {

    /** Fields used for priority calculation */
    public final int vehicleUrgency;

    public final double costOfStopping;

    public final double delayTime;

    public final int numberOfPassengers;

    public final double vehicleSpeed;

    public final double distanceToTrafficLight;

    public VehicleApproach(int urgency, double costOfStopping, double delayTime, int NoOfPassengers, double speed,
            double distance) {
        this.vehicleUrgency = urgency;
        this.costOfStopping = costOfStopping;
        this.delayTime = delayTime;
        this.numberOfPassengers = NoOfPassengers;
        this.vehicleSpeed = speed;
        this.distanceToTrafficLight = distance;
    }

    public double getStoppingCost() {
        return 0.0;
    }

    public double getDelayCost() {
        // $26/hr
        return 0.007 * delayTime;
    }

}
