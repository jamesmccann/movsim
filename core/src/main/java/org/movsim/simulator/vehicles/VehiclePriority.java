package org.movsim.simulator.vehicles;


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
        switch (vehicle.getVehicleClass()) {
        case CAR:
            urgency = VehicleUrgency.sampleCar();
            break;
        case TRUCK:
            urgency = VehicleUrgency.sampleTruck();
            break;
        case BUS:
            urgency = VehicleUrgency.sampleBus();
            break;
        }
    }

    /**
     * The dollar value of petrol consumed when stopping the vehicle from its current
     * speed and accelerating back to a cruise speed when the vehicle is requested to stop
     * at a traffic light.
     * 
     * @return
     */
    public double stoppingCost() {
        return 0.0;
    }
}
