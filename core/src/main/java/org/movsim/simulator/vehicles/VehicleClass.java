package org.movsim.simulator.vehicles;

public abstract class VehicleClass {

    public abstract double getFuelPricePerL();

    public abstract double getFuelEnergyDensity();

    public abstract double getEngineEfficiencyFactor();

    public abstract double getWeight();

    public abstract double getFrictionCoefficient();

    public abstract double getDragCoefficient();

    public abstract double getApproximateCrossSection();

}
