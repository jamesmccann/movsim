package org.movsim.simulator.vehicles;

import java.util.Random;


public class VehiclePriority {

    private Vehicle vehicle;
    
    /**
     * Urgency is a value between 1 and 5 (inclusive) set
     * by the "driver" of the vehicle based on their own
     * urgency of travel.
     * 
     * For example, a vehicle with passengers running late for
     * an important meeting or airport departure will set a high
     * priority for themselves, indicating they are willing
     * to incur a cost to minimise traffic delay on their journey
     * 
     * A vehicle with a higher urgency has a higher priority
     */
    public int urgency;

    /**
     * The number of passengers in the vehicle. A higher number of
     * passengers increases the priority of the vehicle
     */
    private int numberOfPassengers;

    public VehiclePriority(Vehicle vehicle) {
        this.vehicle = vehicle;
        if (vehicle.getVehicleClass().getClass() == TruckVehicleClass.class) {
            urgency = VehicleUrgency.sampleTruck();
            numberOfPassengers = 1;
        }
        if (vehicle.getVehicleClass().getClass() == BusVehicleClass.class) {
            urgency = VehicleUrgency.sampleBus();
            numberOfPassengers = 15; // average
        }
        if (vehicle.getVehicleClass().getClass() == LightVehicleClass.class) {
            urgency = VehicleUrgency.sampleCar();
            numberOfPassengers = new Random().nextInt(5) + 1;
        }
    }

    /**
     * The value of petrol consumed when stopping the vehicle from its current
     * speed, idling while delayed, and accelerating back to a cruise speed when the vehicle is requested to stop
     * at a traffic light.
     * 
     * @return
     */
    public double stoppingCost() {
        return 0.0;
    }

    public double passengersCost() {
        return 1.1 * numberOfPassengers;
    }

    public double urgencyCost() {
        return urgency;
    }

}
