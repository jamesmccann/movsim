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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.movsim.autogen.TrafficLightStatus;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * The Class TrafficLight.
 */
public class TrafficLight {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(TrafficLight.class);

    /** The status. */
    private TrafficLightStatus status;

    private double position = Double.NaN;

    private final String name; // not unique in network

    private final String groupId; // unique mapping to infrastructure

    private final TriggerCallback triggerCallback;

    private final Set<TrafficLightStatus> possibleStati = new HashSet<>();

    private RoadSegment roadSegment;

    private Map<Long, VehicleApproach> approachVehicles;
    
    private Map<Long, Double> approachVehiclesUpdatedAt;

    private double cumulativeStoppingCost;

    private double cumulativeDelayCost;

    public TrafficLight(String name, String groupId, TriggerCallback triggerCallback) {
        this.name = name;
        this.groupId = groupId;
        this.triggerCallback = Preconditions.checkNotNull(triggerCallback);
        this.approachVehicles = new HashMap<Long, VehicleApproach>();
        this.approachVehiclesUpdatedAt = new HashMap<Long, Double>();
    }

    /**
     * Returns the name of the trafficlight as referenced in the movsim input. The name does not reference a unique signal in the
     * infrastructure.
     * 
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the controllergroup-id which allows for a unique reference to a set of signals in the infrastructure.
     * 
     * @return groupId
     */
    public String groupId() {
        return groupId;
    }

    public TrafficLightStatus status() {
        return status;
    }

    void setState(TrafficLightStatus newStatus) {
        this.status = newStatus;
    }

    public double position() {
        Preconditions.checkArgument(!Double.isNaN(position), "traffic light without position");
        return position;
    }

    public boolean hasPosition() {
        return !Double.isNaN(position);
    }

    public void setPosition(double position) {
        Preconditions.checkArgument(Double.isNaN(this.position), "position already set: " + toString());
        this.position = position;
    }

    public void triggerNextPhase() {
        triggerCallback.nextPhase();
    }

    void addPossibleState(TrafficLightStatus status) {
        possibleStati.add(status);
    }

    /**
     * Return the number of lights this traffic light has, can be 1, 2 or 3.
     * 
     * @return
     */
    public int lightCount() {
        return Math.min(3, possibleStati.size());
    }

    public RoadSegment roadSegment() {
        return Preconditions.checkNotNull(roadSegment);
    }

    public void setRoadSegment(RoadSegment roadSegment) {
        Preconditions.checkArgument(this.roadSegment == null, "roadSegment already set");
        this.roadSegment = roadSegment;
    }

    @Override
    public String toString() {
        return "TrafficLight [status=" + status + ", position=" + position + ", name=" + name + "groupId = " + groupId
                + ", roadSegment.id=" + ((roadSegment == null) ? "null" : roadSegment.id()) + "]";
    }

    public double getPhaseTime() {
        return triggerCallback.getPhaseTime();
    }

    public double getGapTime() {
        return triggerCallback.getGapTime();
    }

    public void addVehicleApproach(long vehicleId, VehicleApproach approach) {
        if (approach.incurredStoppingCost != 0) {
            addToCumulativeStoppingCost(approach.incurredStoppingCost);
        }
        approachVehicles.put(vehicleId, approach);
        approachVehiclesUpdatedAt.put(vehicleId, 0.0);
    }

    public void removeVehicleApproach(long vehicleId) {
        approachVehicles.remove(vehicleId);
    }

    public void updateVehicleApproaches(double dt) {
        ArrayList<Long> toRemove = new ArrayList<Long>();
        // update each vehicle approach, assume perfect communication so anything over
        // a single cycle time of 2.0 seconds has stopped communicating
        for (Map.Entry<Long, Double> vehTimestamp : approachVehiclesUpdatedAt.entrySet()) {
            if (vehTimestamp.getValue() > 2.05) {
                toRemove.add(vehTimestamp.getKey());
            } else {
                approachVehiclesUpdatedAt.put(vehTimestamp.getKey(), vehTimestamp.getValue() + dt);
            }
        }

        // assume these vehicles are no longer communicating with the light
        // as they have not communicated within the last two "cycles"
        for (Long vehId : toRemove) {
            // remove vehicle approach and count delay cost
            VehicleApproach removeApproach = approachVehicles.get(vehId);
            addToCumulativeDelayCost(removeApproach.getDelayCost());

            approachVehicles.remove(vehId);
            approachVehiclesUpdatedAt.remove(vehId);
        }
    }

    public Collection<VehicleApproach> getVehicleApproaches() {
        return approachVehicles.values();
    }

    public double getApproachCost() {
        double result = 0.0;
        for (VehicleApproach approach : approachVehicles.values()) {
            result += approach.costOfStopping;
        }
        return result;
    }

    public double getDelayCost() {
        double result = 0.0;
        for (VehicleApproach approach : approachVehicles.values()) {
            result += approach.getDelayCost();
        }
        return result;
    }

    public void addToCumulativeStoppingCost(double stoppingCost) {
        cumulativeStoppingCost += stoppingCost;
    }

    public void addToCumulativeDelayCost(double delayCost) {
        cumulativeDelayCost += delayCost;
    }

    public double getCumulativeStoppingCost() {
        return cumulativeStoppingCost;
    }

    public double getCumulativeDelayCost() {
        return cumulativeDelayCost;
    }

}
