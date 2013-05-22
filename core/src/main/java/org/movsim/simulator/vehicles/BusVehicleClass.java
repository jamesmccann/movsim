package org.movsim.simulator.vehicles;

public class BusVehicleClass extends VehicleClass {

    /**
     * Cost per litre units in dollars of recorded fuel price in Wellington
     * region. Light vehicles use Petrol view, Heavy vehicles (trucks and buses)
     * use diesel fuel.
     */
    private final double FUEL_PRICE_PER_LITRE = 1.500;

    /**
     * Constant representing the calorimetric energy density of petrol
     * and diesel fuel, measured in kWh's per Litre (kWh/L)
     */
    private final double FUEL_ENERGY_DENSITY = 11;

    /**
     * Constrants representing the efficiency factor of the vehicle
     * engine
     */
    private final double ENGINE_EFFICIENCY_FACTOR = 0.3;

    /**
     * Average weight in kilograms
     */
    private final double WEIGHT = 1500;

    private final double FRICTION_COEFFICIENT = 0.02;
    private final double AERODYNAMIC_DRAG_COEFFICIENT = 0.3;
    private final double APPROXIMATE_FRONTAL_CROSS_SECTION = 2;

    @Override
    public double getFuelPricePerL() {
        return FUEL_PRICE_PER_LITRE;
    }

    @Override
    public double getFuelEnergyDensity() {
        return FUEL_ENERGY_DENSITY;
    }

    @Override
    public double getEngineEfficiencyFactor() {
        return ENGINE_EFFICIENCY_FACTOR;
    }

    @Override
    public double getWeight() {
        return WEIGHT;
    }

    @Override
    public double getFrictionCoefficient() {
        return FRICTION_COEFFICIENT;
    }

    @Override
    public double getDragCoefficient() {
        return AERODYNAMIC_DRAG_COEFFICIENT;
    }

    @Override
    public double getApproximateCrossSection() {
        return APPROXIMATE_FRONTAL_CROSS_SECTION;
    }

}
