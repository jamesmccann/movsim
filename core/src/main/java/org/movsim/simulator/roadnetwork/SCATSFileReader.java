package org.movsim.simulator.roadnetwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.movsim.simulator.Simulator;
import org.movsim.simulator.trafficlights.ParsedSCATSDataControlStrategy;
import org.movsim.simulator.trafficlights.TrafficLightControlGroup;
import org.movsim.utilities.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCATSFileReader {

    private static final Logger LOG = LoggerFactory.getLogger(SCATSFileReader.class);
    
    private static final String scatsFileDir = "scats/";

    /**
     * Sources are stored and accessed by their approach ID, read in
     * from the SCATS data
     */
    public Map<String, TrafficSourceMacro> trafficSources;

    public TrafficLightControlGroup trafficLightControlGroup;

    public ParsedSCATSDataControlStrategy controlStrategy;

    private File dataFile;

    private BufferedReader dataFileReader;
    
    private Cycle lastCycle;

    private Cycle lastInflowUpdateCycle;

    private boolean eof;
    
    private Cycle currentCycle;

    private int numVehiclesInFile;

    /**
     * Some SCATS data files contain information for multiple intersections
     * in a subsystem. The intersectionId is a string representing the identifier
     * of a particular intersection in the SCATS data file.
     */
    private String intersectionId;

    public SCATSFileReader(Simulator simulator, String SCATSDataFileName, String controlGroupId, String intersectionId) {
        this.intersectionId = intersectionId;
        dataFile = FileUtils.lookupFilename(scatsFileDir + SCATSDataFileName);
        try {
            dataFileReader = new BufferedReader(new FileReader(dataFile));
        } catch (FileNotFoundException e) {
            LOG.error("Couldn't load SCATS Data File!");
            e.printStackTrace();
        }

        trafficSources = new HashMap<String, TrafficSourceMacro>();

        // find the relevant intersection, check if it is using the SCATS control strategy
        for (TrafficLightControlGroup tlcg: simulator.getTrafficLights().getTrafficLightControlGroups()) {
            if (tlcg.groupId().equals(controlGroupId)) {
                trafficLightControlGroup = tlcg;
                if (trafficLightControlGroup.controlStrategy.getClass() == ParsedSCATSDataControlStrategy.class) {
                    controlStrategy = (ParsedSCATSDataControlStrategy) trafficLightControlGroup.controlStrategy;
                    break;
                }
            }
        }
        
        for (AbstractTrafficSource trafficSource : simulator.getRoadNetwork().getTrafficSources()) {
            TrafficSourceMacro source = (TrafficSourceMacro)trafficSource;
            source.setSCATSFileReader(this);
            trafficSources.put(source.getId(), source);
        }
    }

    /**
     * Updates traffic controller with next cycle time, and desired phase green times
     * Updates each source with number of vehicles for the period
     */
    public boolean update() {
        if (eof) {
            return false;
        }
        try {
            currentCycle = parseNextCycle();
            if (currentCycle == null) {
                // end of file reacehd
                eof = true;
                for (Map.Entry<String, Integer> approachInflow : lastCycle.approachInflows.entrySet()) {
                    TrafficSourceMacro source = trafficSources.get(approachInflow.getKey());
                    source.setInflow(0, 0, 0);
                }
                return false;
            }

            if (controlStrategy != null) {
                controlStrategy.targetCycleDuration = currentCycle.cycleDuration;
                controlStrategy.targetPhaseDurations = currentCycle.phaseDurations;
            }

            lastCycle = currentCycle;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception reading scats data");
        }

        return true;
    }
    
    public void updateInflow() {
        // if no scats then we need to manually call to get the next cycle
        if (controlStrategy == null) {
            if (!update()) return;
        }

        // if there is SCATS, let the traffic light controller request a new cycle first
        else if (currentCycle == lastInflowUpdateCycle || currentCycle == null) {
            return;
        }

        for (Map.Entry<String, Integer> approachInflow : currentCycle.approachInflows.entrySet()) {
            TrafficSourceMacro source = trafficSources.get(approachInflow.getKey());
            int numVehicles = approachInflow.getValue();
            double inflow = numVehicles / (1.0 * currentCycle.cycleDuration);
            System.out.println("Approach " + approachInflow.getKey() + ", vehicles: " + numVehicles + ", inflow: "
                    + inflow);
            source.setInflow(inflow, numVehicles, currentCycle.cycleDuration);
        }
        lastInflowUpdateCycle = currentCycle;
    }

    public Cycle parseNextCycle() throws IOException {
        Cycle cycle = new Cycle();

        // first line is the cycle information
        // Thursday 20-June-2013 06:00 SS  63   PL 3.1  PVs3.3 CT   33 +0 RL 33  SA 301 DS 14
        String data = dataFileReader.readLine();
        if (data == null) {
            return null;
        }

        // cycle.cycleDuration = Integer.parseInt(getData(data, 56, 59));
        // if (cycle.cycleDuration == 0) {
        // throw new IOException();
        // }

        // next line is column headers, skip it
        // Int SA/LK PH PT! DS VO VK! DS VO VK! DS VO VK! DS VO VK! ADS
        dataFileReader.readLine();

        // next X lines are information from all approaches recorded on that link
        // these lines always start with an integer representing the intersection id
        // 460 S 301 AE 32! 14 2 2! 12 2 1! - -! - -! 19
        while (true) {
            data = dataFileReader.readLine();

            try {
                Integer.parseInt(getData(data, 1, 6));
            } catch (Exception e) {
                break; // reached end of approach inputs
            }

            // check if this line describes the target intersection or another
            // intersection in the subsystem
            if (!getData(data, 1, 6).equals(intersectionId)) {
                continue;
            }

            String phaseId = getData(data, 16, 18);
            Integer phaseTime = Integer.parseInt(getData(data, 20, 22));
            if (phaseTime > 0) { // don't add 0 approaches
                cycle.phaseDurations.put(phaseId, phaseTime);
                cycle.cycleDuration += phaseTime;
            }

            String approachId = getData(data, 9, 11);
            Integer approachCount = 0;

            // approach counts on V0
            try {
                Integer apc = Integer.parseInt(getData(data, 29, 31));
                approachCount += apc;
                numVehiclesInFile += apc;
                apc = Integer.parseInt(getData(data, 42, 44));
                approachCount += apc;
                numVehiclesInFile += apc;
            } catch (Exception e) { }

            cycle.approachInflows.put(approachId, approachCount);
        }

        // next line is the calculated allocation of the cycle
        // A=<64> B=36
        System.out.println("number of vehicles" + numVehiclesInFile);
        return cycle;
    }
    
    public static String getData(String data, int i, int j) {
        return data.substring(i, j).trim();
    }

    private static final class Cycle {
        public int cycleDuration;
        public Map<String, Integer> phaseDurations;
        public Map<String, Integer> approachInflows;

        public Cycle() {
            phaseDurations = new HashMap<String, Integer>();
            approachInflows = new HashMap<String, Integer>();
        }
    }
}

