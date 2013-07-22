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
package org.movsim.simulator.vehicles.longitudinalmodel;

import org.movsim.autogen.TrafficLightStatus;
import org.movsim.simulator.MovsimConstants;
import org.movsim.simulator.roadnetwork.RoadSegment.TrafficLightLocationWithDistance;
import org.movsim.simulator.trafficlights.TrafficLight;
import org.movsim.simulator.trafficlights.VehicleApproach;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.VehiclePriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class TrafficLightApproaching.
 * 
 */
public class TrafficLightApproaching {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(TrafficLightApproaching.class);

    public static final double MAX_LOOK_AHEAD_DISTANCE = 1000;

    public static final double MAX_COMMUNICATION_RANGE = 150;

    private boolean considerTrafficLight;

    private double accTrafficLight;

    private double distanceToTrafficlight;

    private VehicleApproach lastBroadcastApproach;

    private TrafficLight lastTrafficLight;

    private double controlledDelayTime;

    /**
     * Seconds since last broadcast
     */
    private double lastBroadcastTime;

    /**
     * Instantiates a new traffic light approaching.
     */
    public TrafficLightApproaching() {
        considerTrafficLight = false;
        distanceToTrafficlight = MovsimConstants.INVALID_GAP;
        lastBroadcastApproach = null;
    }

    /**
     * Update.
     * 
     * @param me
     * @param trafficLight
     * @param distanceToTrafficlight
     * @param longModel
     */
    public void update(double dt, Vehicle me, TrafficLight trafficLight, double distanceToTrafficlight) {
        lastBroadcastTime += dt;
        accTrafficLight = 0;
        considerTrafficLight = false;
        lastTrafficLight = trafficLight;
        this.distanceToTrafficlight = distanceToTrafficlight;

        if (distanceToTrafficlight > MAX_LOOK_AHEAD_DISTANCE) {
            LOG.debug("traffic light at distance={} to far away -- MAX_LOOK_AHEAD_DISTANCE={}", distanceToTrafficlight,
                    MAX_LOOK_AHEAD_DISTANCE);
            return;
        }

        // TODO: refactor this into its own class?
        if (lastBroadcastTime >= 2.0 && distanceToTrafficlight <= MAX_COMMUNICATION_RANGE) {
            // update the traffic light with the approach distance and priority/cost
            // for this vehicle
            updateAndBroadcastApproach(me, trafficLight);
            lastBroadcastTime = 0;
        }

        // keep track of time delayed (when stopped) by this trafficLight
        if (me.getSpeed() <= 0.005) {
            controlledDelayTime += dt;
        }

        if (trafficLight.status() == TrafficLightStatus.GREEN && me.getLength() > 30
                && distanceToTrafficlight < 0.5 * MAX_LOOK_AHEAD_DISTANCE) {
            // special case here: only relevant if vehicle is really long and next trafficlight is quite close
            checkSpaceBeforePassingTrafficlight(me, trafficLight, distanceToTrafficlight);
        } else if (trafficLight.status() != TrafficLightStatus.GREEN) {
            final double maxRangeOfSight = MovsimConstants.GAP_INFINITY;
            if (distanceToTrafficlight < maxRangeOfSight) {
                accTrafficLight = calcAccelerationToTrafficlight(me, distanceToTrafficlight);
                if (accTrafficLight < 0) {
                    considerTrafficLight = true;
                    LOG.debug("distance to trafficLight = {}, accTL = {}", distanceToTrafficlight, accTrafficLight);
                }

                // TODO: decision logic while approaching yellow traffic light
                // ignore traffic light if accTL exceeds two times comfortable
                // deceleration or if kinematic braking is not possible anymore

                if (trafficLight.status() == TrafficLightStatus.AMBER) {
                    final double bKinMax = 6; // typical value: bIDM < comfortBrakeDecel < bKinMax < bMax
                    final double comfortBrakeDecel = 4;
                    final double brakeDist = (me.getSpeed() * me.getSpeed()) / (2 * bKinMax);
                    if ((accTrafficLight <= -comfortBrakeDecel || brakeDist >= distanceToTrafficlight)) {
                        // ignore traffic light
                        considerTrafficLight = false;
                    }
                }

                // traffic light is already red
                if (trafficLight.status() == TrafficLightStatus.RED) {
                    final double maxDeceleration = me.getMaxDeceleration();
                    final double minBrakeDist = (me.getSpeed() * me.getSpeed()) / (2 * maxDeceleration);
                    if (accTrafficLight <= -maxDeceleration || minBrakeDist >= distanceToTrafficlight) {
                        // ignore traffic light
                        LOG.info(String
                                .format("veh id=%d in dilemma zone is going to pass red light at distance=%.2fm due to physics (assuming user-defined max. possible braking=%.2fm/s^2!",
                                        me.getId(), distanceToTrafficlight, maxDeceleration));
                        considerTrafficLight = false;
                    }
                }
            }
        }
    }

    private static double calcAccelerationToTrafficlight(Vehicle me, double distanceToTrafficlight) {
        final double speed = me.getSpeed();
        return Math.min(0, me.getLongitudinalModel().calcAccSimple(distanceToTrafficlight, speed, speed));
    }

    /**
     * Consider traffic light.
     * 
     * @return true, if successful
     */
    public boolean considerTrafficLight() {
        return considerTrafficLight;
    }

    /**
     * Acc approaching.
     * 
     * @return the double
     */
    public double accApproaching() {
        return accTrafficLight;
    }

    /**
     * Gets the distance to trafficlight.
     * 
     * @return the distance to trafficlight
     */
    public double getDistanceToTrafficlight() {
        return distanceToTrafficlight;
    }

    private void checkSpaceBeforePassingTrafficlight(Vehicle me, TrafficLight trafficLight,
            double distanceToTrafficlight) {
        // relative to position of first traffic light
        TrafficLightLocationWithDistance nextTrafficlight = trafficLight.roadSegment().getNextDownstreamTrafficLight(
                trafficLight.position(), me.lane(), MAX_LOOK_AHEAD_DISTANCE);
        if (nextTrafficlight != null) {
            double distanceBetweenTrafficlights = nextTrafficlight.distance;
            if (distanceBetweenTrafficlights < 0.5 * MAX_LOOK_AHEAD_DISTANCE) {
                double effectiveFrontVehicleLengths = calcEffectiveFrontVehicleLengths(me, trafficLight,
                        distanceToTrafficlight + distanceBetweenTrafficlights);
                LOG.debug("distanceBetweenTrafficlights={}, effectiveLengths+ownLength={}",
                        distanceBetweenTrafficlights, effectiveFrontVehicleLengths + me.getEffectiveLength());
                if (effectiveFrontVehicleLengths > 0
                        && distanceBetweenTrafficlights < effectiveFrontVehicleLengths + me.getEffectiveLength()) {
                    considerTrafficLight = true;
                    accTrafficLight = calcAccelerationToTrafficlight(me, distanceToTrafficlight);
                    LOG.debug(
                            "stop in front of green trafficlight, not sufficient space: nextlight={}, space for vehicle(s)={}",
                            distanceBetweenTrafficlights, effectiveFrontVehicleLengths + me.getEffectiveLength());
                }
            }
        }
    }

    private static double calcEffectiveFrontVehicleLengths(Vehicle me, TrafficLight trafficLight,
            double distanceToSecondTrafficlight) {
        double sumEffectiveLengths = 0;
        Vehicle frontVehicle = trafficLight.roadSegment().laneSegment(me.lane()).frontVehicle(me);
        while (frontVehicle != null && me.getBrutDistance(frontVehicle) < distanceToSecondTrafficlight) {
            sumEffectiveLengths += frontVehicle.getEffectiveLength();
            Vehicle prevFront = frontVehicle;
            frontVehicle = trafficLight.roadSegment().laneSegment(frontVehicle.lane()).frontVehicle(frontVehicle);
            if (frontVehicle != null && prevFront.getId() == frontVehicle.getId()) {
                // FIXME seems to be a real bug: get back the *same* vehicle when its entered the downstream roadsegment
                break;
            }
        }
        return sumEffectiveLengths;
    }

    private void updateAndBroadcastApproach(Vehicle veh, TrafficLight trafficLight) {
        VehiclePriority p = veh.getPriority();
        VehicleApproach approach = new VehicleApproach(veh.getVehicleClass().getWeight(), veh.getAcc(), veh.getSpeed(),
                p.getUrgency(),
                veh.getInstantaneousCost(), controlledDelayTime, p.getNumberOfPassengers(), distanceToTrafficlight,
                veh.getVehicleClass());
        long vehicleId = veh.getId();

        // clear old approach and add new one
        trafficLight.removeVehicleApproach(vehicleId);
        trafficLight.addVehicleApproach(vehicleId, approach);
        lastBroadcastApproach = approach;
    }

    public void clearLastBroadcastApproach() {
        lastBroadcastApproach = null;
    }

    public VehicleApproach getVehicleApproach() {
        return lastBroadcastApproach;
    }

    public TrafficLight getLastTrafficLight() {
        return lastTrafficLight;
    }

    public double getLastBroadcastTime() {
        return lastBroadcastTime;
    }
}