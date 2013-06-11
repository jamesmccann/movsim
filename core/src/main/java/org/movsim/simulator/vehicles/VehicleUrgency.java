package org.movsim.simulator.vehicles;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

public class VehicleUrgency {

    /**
     * Urgency is mapped to an set of integer values
     * This is designed to be set by the driver of a vehicle, whether
     * manually or automatically by an onboard vehicle computer.
     */
    private static int[] urgencyValues = new int[] { 1, 2, 3, 4, 5 };

    /**
     * The following arrays hold probabilities for each of the urgencyValues for each class
     * of vehicle considered. These probabilties are estimates and should be revised with
     * field analysis
     * 
     */
    private static double[] carUrgencyProbabilities = new double[] { 0.2, 0.25, 0.395, 0.15, 0.005 };
    private static double[] busUrgencyProbabilities = new double[] { 0.3, 0.5, 0.2, 0.0, 0.0 };
    private static double[] truckUrgencyProbabilities = new double[] { 0.1, 0.5, 0.3, 0.1, 0.0 };

    private static EnumeratedIntegerDistribution carUrgencyDistribution = new EnumeratedIntegerDistribution(
            urgencyValues, carUrgencyProbabilities);
    private static EnumeratedIntegerDistribution busUrgencyDistribution = new EnumeratedIntegerDistribution(
            urgencyValues, busUrgencyProbabilities);
    private static EnumeratedIntegerDistribution truckUrgencyDistribution = new EnumeratedIntegerDistribution(
            urgencyValues, truckUrgencyProbabilities);

    public static int sampleCar() {
        return carUrgencyDistribution.sample();
    }

    public static int sampleBus() {
        return busUrgencyDistribution.sample();
    }

    public static int sampleTruck() {
        return truckUrgencyDistribution.sample();
    }

}
