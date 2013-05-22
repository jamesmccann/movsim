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

    public static double instantaneousFuelConsumption(Vehicle v) {
        double consumptionRate, mass, consumptionPer100Km;
        VehicleClass vc = v.getVehicleClass();

        mass = vc.getWeight();
//        consumptionRate = instantaneousEnginePower(v) / 
//                (vc.getEngineEfficiencyFactor() * vc.getFuelEnergyDensity());
        
        double enginePower = instantaneousEnginePower(v);
        System.out.println(enginePower);

        consumptionPer100Km = (10000 / (vc.getEngineEfficiencyFactor() * vc.getFuelEnergyDensity()))
                * (enginePower / v.getKmphSpeed());
                
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
        
        aerodynamicDrag = 0.5 * dragC * AIR_DENSITY * crossSection * Math.pow(v.getKmphSpeed(), 2);

        System.out.println("inertia: " + inertiaForce);
        System.out.println("friction: " + frictionForce);
        System.out.println("aero drag: " + aerodynamicDrag);

        return Math.max(v.getKmphSpeed() * ((inertiaForce + frictionForce + aerodynamicDrag)), 0);
    }

}
