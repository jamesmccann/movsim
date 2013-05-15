package org.movsim.simulator.vehicles;

public class VehicleConsumption {

    private Vehicle vehicle;

    /**
     * Cost per litre units in dollars of recorded fuel price in Wellington
     * region
     */
    private static final double PETROL_FUEL_PRICE_PER_LITRE = 1.969;
    private static final double DIESEL_FUEL_PRICE_PER_LITRE = 1.399;

    /**
     * Average weight in kilograms for light (cars) and heavy (trucks and buses)
     * vehicles.
     */
    private static final double LIGHT_VEHICLE_WEIGHT = 1400;
    private static final double HEAVY_VEHICLE_WEIGHT = 11000;

    /**
     * Average idle consumption rates for light and heavy vehicles.
     * Measured in milliliters per hour, mL/h.
     */
    private static final double LV_IDLE_CONSUMPTION_RATE = 1350;
    private static final double HV_IDLE_CONSUMPTION_RATE = 2000;

    private VehicleConsumption() {
        // no instantiation
    }

    public static double instantaneousCost(Vehicle v) {
        double fuelConsumptionRate = 0.0;
        double fuelPrice = 0.0;
        double vehicleWeight = 0.0;

        if (v.getSpeed() <= 10000) {
            if (v.getVehicleClass() == VehicleClass.CAR) {
                return (LV_IDLE_CONSUMPTION_RATE / 1000 * PETROL_FUEL_PRICE_PER_LITRE);
            } else {
                return (HV_IDLE_CONSUMPTION_RATE / 1000 * DIESEL_FUEL_PRICE_PER_LITRE);
            }
        }

        if (v.getVehicleClass() == VehicleClass.CAR) {
            vehicleWeight = LIGHT_VEHICLE_WEIGHT;
            fuelPrice = PETROL_FUEL_PRICE_PER_LITRE;
        } else {
            vehicleWeight = HEAVY_VEHICLE_WEIGHT;
            fuelPrice = DIESEL_FUEL_PRICE_PER_LITRE;
        }

        return fuelConsumptionRate * fuelPrice;
    }

}
