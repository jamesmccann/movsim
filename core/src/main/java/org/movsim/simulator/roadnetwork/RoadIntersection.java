package org.movsim.simulator.roadnetwork;

import org.movsim.roadmappings.RoadMapping;

/**
 * Represents an intersection as a RoadSegment "joining" two or more
 * other RoadSegments. 
 *
 *
 * <br>created: 16/04/2013<br>
 *
 */
public class RoadIntersection extends RoadSegment {

    /**
     * Constructor calls superclass to create this Intersection as a new RoadSegment
     * 
     * @param roadLength
     * @param laneCount
     */
    public RoadIntersection(double roadLength, int laneCount) {
        super(roadLength, laneCount);
    }

    /**
     * Convenience constructor, creates intersection based on a given road mapping.
     * 
     * @param roadMapping
     */
    public RoadIntersection(RoadMapping roadMapping) {
        super(roadMapping);
    }

}
