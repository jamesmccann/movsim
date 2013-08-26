/*
 * Copyright (C) 2010, 2011, 2012 by Arne Kesting, Martin Treiber, Ralph Germ, Martin Budden
 * <movsim.org@gmail.com>
 * -----------------------------------------------------------------------------------------
 * 
 * This file is part of
 * 
 * MovSim - the multi-model open-source vehicular-traffic simulator.
 * 
 * MovSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MovSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MovSim. If not, see <http://www.gnu.org/licenses/>
 * or <http://www.movsim.org>.
 * 
 * -----------------------------------------------------------------------------------------
 */
package org.movsim.simulator.trafficlights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.movsim.autogen.TrafficLightStatus;
import org.movsim.input.ProjectMetaData;
import org.movsim.output.fileoutput.FileOutputBase;

import com.google.common.base.Preconditions;

/**
 * The Class FileTrafficLightControllerRecorder.
 */
public class FileTrafficLightControllerRecorder extends FileOutputBase implements
        TrafficLightControlGroup.RecordDataCallback {

    private static final String extensionFormat = ".controllerGroup%s_%s.csv";
    private final int nTimestep;
    private final int sTimestep;
    private final int phaseStep;
    private double nextOutputTime;

    public List<VehicleApproach> totalVehicleApproachesForGroup;

    private TrafficLightControlGroup group;

    /**
     * Constructor.
     * 
     * @param nTimestep
     *            the n'th timestep
     */
    public FileTrafficLightControllerRecorder(TrafficLightControlGroup group, int nTimestep, int sTimestep,
            int phaseStep) {
        super(ProjectMetaData.getInstance().getOutputPath(), ProjectMetaData.getInstance().getProjectName());
        Preconditions.checkArgument(!group.groupId().isEmpty());
        Preconditions.checkArgument(!group.firstSignalId().isEmpty());
        totalVehicleApproachesForGroup = new ArrayList<VehicleApproach>();
        this.group = group;
        this.nTimestep = nTimestep;
        this.sTimestep = sTimestep;
        this.phaseStep = phaseStep;
        nextOutputTime = nTimestep;
        String groupName = group.groupId().replaceAll("\\s", "");
        writer = Preconditions.checkNotNull(createWriter(String.format(extensionFormat, "Phases", groupName)));
    }

    /**
     * Update.
     * 
     * @param simulationTime
     *            current simulation time, seconds
     * @param iterationCount
     *            the number of iterations that have been executed
     * @param trafficLights
     *            the traffic lights
     */
    @Override
    public void recordData(double simulationTime, long iterationCount, Iterable<TrafficLight> trafficLights) {
        if (iterationCount == 0) {
            writeHeader(trafficLights);
        }
        if (iterationCount % nTimestep != 0 || simulationTime <= nextOutputTime) {
            return;
        }
        nextOutputTime = simulationTime + sTimestep;

        String formattedTime = ProjectMetaData.getInstance().getFormatedTimeWithOffset(simulationTime);
        writeData(simulationTime, formattedTime, trafficLights);
    }

    @Override
    public void recordPhase(double simulationTime, long iterationCount, int phaseCount, ControlStrategy strategy,
            Iterable<TrafficLight> trafficLights) {
        if (phaseCount == 0) {
            writePhaseFileHeader(strategy, trafficLights);
            return;
        }
        if (simulationTime <= nextOutputTime || phaseCount % phaseStep != 0) {
            return;
        }
        nextOutputTime = simulationTime + sTimestep;

        writePhaseData(simulationTime, phaseCount, strategy, trafficLights);
    }

    private void writeData(double simulationTime, String formattedTime, Iterable<TrafficLight> trafficLights) {
        writer.printf("%8.2f, %s,  ", simulationTime, formattedTime);
        for (TrafficLight trafficLight : trafficLights) {
            writer.printf("%.1f,  %d,  ", trafficLight.position(), trafficLight.status().ordinal());
        }
        write("%n");
    }

    private void writePhaseData(double simulationTime, int phaseCount, ControlStrategy strategy,
            Iterable<TrafficLight> trafficLights) {
        double delayCostForPhase = 0.0;
        double stoppingCostForPhase = 0.0;
        int vehicleApproachCount = 0;
        for (TrafficLight trafficLight : trafficLights) {
            delayCostForPhase += trafficLight.delayCostForPhase;
            stoppingCostForPhase += trafficLight.stoppingCostForPhase;
            vehicleApproachCount += trafficLight.getVehicleApproaches().size();
        }
        Map<Integer, Double> delayTimePerUrgency = group.getAverageDelayTimePerUrgency();
        writer.printf("%.2f,%d,%d,%.2f,%.2f,", simulationTime, phaseCount, vehicleApproachCount, delayCostForPhase,
                stoppingCostForPhase);
        int i = 0;
        for (Double delayTime : delayTimePerUrgency.values()) {
            if (delayTime <= 0) {
                continue;
            }
            writer.printf("%.2f", delayTime);
            if (i < delayTimePerUrgency.size()-1) {
                writer.print(",");
            }
            i++;
        }
        write("%n");
    }

    /**
     * Write header.
     * 
     * @param trafficLights
     *            the traffic lights
     */
    private void writeHeader(Iterable<TrafficLight> trafficLights) {
        writer.printf(COMMENT_CHAR + " number codes for traffic lights status: %n");
        for (TrafficLightStatus status : TrafficLightStatus.values()) {
            writer.printf(COMMENT_CHAR + " %s --> %d %n", status.toString(), status.ordinal());
        }

        int counter = 0;
        for (final TrafficLight trafficLight : trafficLights) {
            writer.printf(COMMENT_CHAR + " position of traffic light no. %d: %5.2fm, name=%s, groupId=%s%n",
                    ++counter, trafficLight.position(), trafficLight.name(), trafficLight.groupId());
        }
        writer.printf(COMMENT_CHAR + " %-8s  %-8s  %-8s  %-8s %n", "time[s]", "position[m]_TL1", "status[1]_TL1",
                " etc.");
        writer.flush();
    }

    private void writePhaseFileHeader(ControlStrategy strategy, Iterable<TrafficLight> trafficLights) {
        writer.printf(COMMENT_CHAR + " per phase cost output for control strategy: %s %n", strategy.getName());
        writer.printf(COMMENT_CHAR
                + "simulationTime,phase count,vehicle approaches,delay cost,stopping cost, "
                + "delay for urgency 1,delay for urgency 2,delay for urgency 3,delay for urgency 4,delay for urgency 5 %n");
    }

    public void recordVehicleApproach(VehicleApproach approach) {
        totalVehicleApproachesForGroup.add(approach);
    }

    @Override
    public void recordComplete() {
        System.out.println("COMPLETE");
        System.out.println("total apporaches " + totalVehicleApproachesForGroup.size());
        writer = createWriter("approaches_" + group.controlStrategy.getName() + ".txt");
        Map<Integer, List<VehicleApproach>> vehicleApproachPerUrgency = new HashMap<Integer, List<VehicleApproach>>();
        for (int i = 1; i <= 5; i++) { vehicleApproachPerUrgency.put(i, new ArrayList<VehicleApproach>()); }
        for (VehicleApproach va: totalVehicleApproachesForGroup) { vehicleApproachPerUrgency.get(va.vehicleUrgency).add(va); }

        for (int i = 1; i <= 5; i++) {
            List<VehicleApproach> approaches = vehicleApproachPerUrgency.get(i);
            int count = approaches.size();
            double delayTime = 0.0;
            double delayCost = 0.0;
            double stoppingCost = 0.0;
            for (VehicleApproach approach : approaches) {
                delayTime += approach.delayTime;
                delayCost += approach.getDelayCost();
            }
            delayTime /= (1.0 * count);
            delayCost /= (1.0 * count);

            writer.printf("%d, %d, %.2f, %.2f %n", i, count, delayTime, delayCost);
        }
        
        writer.printf("total approaches: %d %n", totalVehicleApproachesForGroup.size());
        writer.flush();
        writer.close();
    }

}
