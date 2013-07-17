package org.movsim.simulator.vehicles;

public class VehicleConsumption {

    private Vehicle vehicle;

    /**
     * Environmental constants
     */
    private static final double GRAVITATIONAL_ACCELERATION = 9.81;
    private static final double AIR_DENSITY = 1.3;
    private static final double IDLING_POWER = 3.0;

    private VehicleConsumption() {
        // no instantiation
    }

    public static double instantaneousStopFuelCost(Vehicle v) {
        VehicleClass vc = v.getVehicleClass();
        return instantaneousStopFuelCost(vc.getWeight(), v.getSpeed(), vc);
    }

    public static double instantaneousStopFuelCost(double mass, double approachVelocity, VehicleClass vc) {
        double kEnergy, litres;
        kEnergy = 0.5 * (mass * Math.pow(approachVelocity, 2));
        litres = kEnergy / (vc.getEngineEfficiencyFactor() * (vc.getFuelEnergyDensity()));
        return litres * vc.getFuelPricePerL();
    }

    public static double instantaneousFuelConsumption(Vehicle v) {
        double consumptionRate, mass, consumptionPer100Km;
        VehicleClass vc = v.getVehicleClass();

        mass = vc.getWeight();

        double enginePower = instantaneousEnginePower(v);

        consumptionPer100Km = (10000 / (vc.getEngineEfficiencyFactor() * vc.getFuelEnergyDensity()))
                * (enginePower / v.getSpeed());

        return consumptionPer100Km;
    }

    public static double instantaneousEnginePower(Vehicle v) {
        double mass, frictionC, dragC, crossSection;
        double inertiaForce, frictionForce, aerodynamicDrag;
        VehicleClass vc = v.getVehicleClass();

        mass = vc.getWeight();
        frictionC = vc.getFrictionCoefficient();
        dragC = vc.getDragCoefficient();
        crossSection = vc.getApproximateCrossSection();

        inertiaForce = mass * v.getAcc();

        frictionForce = mass * frictionC * GRAVITATIONAL_ACCELERATION;
        
        aerodynamicDrag = 0.5 * dragC * AIR_DENSITY * crossSection * Math.pow(v.getSpeed(), 2);

        return Math.max(v.getSpeed() * ((inertiaForce + frictionForce + aerodynamicDrag)), 0);
    }

}
