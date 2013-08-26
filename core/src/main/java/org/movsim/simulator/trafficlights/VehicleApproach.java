package org.movsim.simulator.trafficlights;

import org.movsim.simulator.vehicles.VehicleClass;
import org.movsim.simulator.vehicles.VehicleConsumption;

public class VehicleApproach {

    public final long vehId;

    public final double vehicleMass;

    public final double vehicleAcceleration;

    public final double vehicleSpeed;

    public final int vehicleUrgency;

    public final VehicleClass vehicleClass;

    /**
     * The cost of stopping when this VehicleApproach was broadcast
     * This is different to the estimated cost which looks ahead on
     * a time interval
     */
    public final double costOfStopping;

    public final double delayTime;

    public final int numberOfPassengers;

    public final double distanceToTrafficLight;

    public double incurredStoppingCost;

    public VehicleApproach(long vehId, double mass, double acceleration, double speed, int urgency,
            double costOfStopping,
            double delayTime, int NoOfPassengers, double distance, VehicleClass vehicleClass) {
        this.vehId = vehId;
        this.vehicleMass = mass;
        this.vehicleAcceleration = acceleration;
        this.vehicleSpeed = speed;
        this.vehicleClass = vehicleClass;
        this.vehicleUrgency = urgency;
        this.costOfStopping = costOfStopping;
        this.delayTime = delayTime;
        this.numberOfPassengers = NoOfPassengers;
        this.distanceToTrafficLight = distance;
    }

    public double getDelayCost() {
        return delayCost(delayTime);
    }

    public double delayCost(double delayTime) {
        // $26/hr
        return 0.007 * delayTime * vehicleUrgency;
    }

    public double estimatedClearTime() {
        // estimates the time in seconds it will take for this vehicle to
        // pass the stop line of the traffic light, based on current speed
        // and distance to traffic light
        return (distanceToTrafficLight / vehicleSpeed);
    }

    public double estimatedStoppingCost(double timeStep) {
        // estimates the stopping cost after the given timestep
        // using the current acceleration to estimate the
        // speed and cost of stopping the vehicle after K seconds
        // v = v0 + at
        double estimatedSpeed = vehicleSpeed + vehicleAcceleration * timeStep; 
        return VehicleConsumption.instantaneousStopFuelCost(vehicleMass, estimatedSpeed, vehicleClass);
    }
    
    public double estimatedDelayCost(double timeStep) {
        // estimates the accumulated cost of delay after the given timestep
        // this required to car to be stopped, otherwise 0 delay is estimated
        if (vehicleSpeed > 0.005) { return 0.0; }
        double estimatedDelay = delayTime + timeStep;
        return delayCost(estimatedDelay);
    }

    public double estimatedTotalCost(double timeStep, double estimatedDelay) {
        // given that we will stop the traffic light in K seconds
        // and the current vehicle will be delayed approximately X seconds
        // what is the total cost incurred
        return estimatedStoppingCost(timeStep) + estimatedDelayCost(estimatedDelay);
    }

}
