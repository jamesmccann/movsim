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
    public Map<String, TrafficSourceMicro> trafficSources;

    public TrafficLightControlGroup trafficLightControlGroup;

    public ParsedSCATSDataControlStrategy controlStrategy;

    private File dataFile;

    private BufferedReader dataFileReader;
    
    /**
     * Some SCATS data files contain information for multiple intersections
     * in a subsystem. The intersectionId is a string representing the identifier
     * of a particular intersection in the SCATS data file.
     */
    private String intersectionId;

    public SCATSFileReader(Simulator simulator, String SCATSDataFileName, String controlGroupId, String intersectionId) {
        this.intersectionId = intersectionId;
        dataFile = FileUtils.lookupFilename(scatsFileDir + SCATSDataFileName);
        System.out.println(dataFile);
        try {
            dataFileReader = new BufferedReader(new FileReader(dataFile));
        } catch (FileNotFoundException e) {
            LOG.error("Couldn't load SCATS Data File!");
            e.printStackTrace();
        }

        for (TrafficLightControlGroup tlcg: simulator.getTrafficLights().getTrafficLightControlGroups()) {
            if (tlcg.groupId().equals(controlGroupId)) {
                trafficLightControlGroup = tlcg;
                controlStrategy = (ParsedSCATSDataControlStrategy) trafficLightControlGroup.controlStrategy;
                break;
            }
        }
    }

    /**
     * Updates traffic controller with next cycle time, and desired phase green times
     * Updates each source with number of vehicles for the period
     */
    public void update() {
        try {
            Cycle nextCycle = parseNextCycle();
            controlStrategy.targetCycleDuration = nextCycle.cycleDuration;
            controlStrategy.targetPhaseDurations = nextCycle.phaseDurations;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception reading scats data");
        }

    }
    
    public Cycle parseNextCycle() throws IOException {
        Cycle cycle = new Cycle();

        // first line is the cycle information
        // Thursday 20-June-2013 06:00 SS  63   PL 3.1  PVs3.3 CT   33 +0 RL 33  SA 301 DS 14
        String data = dataFileReader.readLine();
        cycle.cycleDuration = Integer.parseInt(getData(data, 55, 60));

        if (cycle.cycleDuration == 0) {
            throw new IOException();
        }

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
            }

            String approachId = getData(data, 8, 13);
            Integer approachCount = 0;

            // approach counts on V0
            try {
                Integer apc = Integer.parseInt(getData(data, 29, 31));
                approachCount += apc;
                apc = Integer.parseInt(getData(data, 42, 44));
                approachCount += apc;
            } catch (Exception e) { }

            cycle.approachInflows.put(approachId, approachCount);
        }

        // next line is the calculated allocation of the cycle
        // A=<64> B=36

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

