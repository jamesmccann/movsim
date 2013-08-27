package org.movsim.simulator.trafficlights;

import java.util.HashMap;

public class EstimatedClearDistances extends HashMap<Integer, Double> {

    public EstimatedClearDistances() {
        super();
        this.put(10, 22.12);
        this.put(15, 40.03);
        this.put(20, 58.10);
        this.put(25, 80.15);
        this.put(30, 94.40);
        this.put(35, 111.47);
        this.put(40, 133.5);
    }

}
