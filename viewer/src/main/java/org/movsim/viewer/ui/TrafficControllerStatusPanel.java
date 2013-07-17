package org.movsim.viewer.ui;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.trafficlights.TrafficLightControlGroup;

public class TrafficControllerStatusPanel extends JPanel implements SimulationRunnable.UpdateStatusCallback {

    private static final long serialVersionUID = 1L;
    
    private final Simulator simulator;
    
    private TrafficLightControlGroup trafficController;
    
    private JLabel lblControllerName;
    
    private JLabel lblCurrentControllerName;
    
    private JLabel lblStrategyName;
    
    private JLabel lblCurrentStrategyName;
    
    private JLabel lblDuration;
    
    private JLabel lblCurrentDuration;
    
    private JLabel lblPhase;
    
    private JLabel lblCurrentPhase;
    
    private JLabel lblGap;
    
    private JLabel lblCurrentGap;
    
    private JLabel lblNextPhase;
    
    private JLabel lblCurrentNextPhase;
    
    private JLabel lblTotalDelayCost;
    
    private JLabel lblCurrentTotalDelayCost;
    
    private JLabel lblTotalStoppingCost;
    
    private JLabel lblCurrentTotalStoppingCost;
    
    private JLabel lblTotalCost;
    
    private JLabel lblCurrentTotalCost;
    
    public TrafficControllerStatusPanel(Simulator simulator) {
        this.simulator = simulator;
        this.simulator.getSimulationRunnable().addUpdateStatusCallback(this);
        setPreferredSize(new Dimension(300, HEIGHT));
        init();
    }
    
    public void init() {
        final Font font = new Font("Dialog", Font.PLAIN, 11);
 
        // controller name
        lblControllerName = new JLabel("Controller: ");
        lblControllerName.setFont(font);
        lblCurrentControllerName = new JLabel("");
        lblCurrentControllerName.setFont(font);
        
        // strategy name
        lblStrategyName = new JLabel("Control Strategy: ");
        lblStrategyName.setFont(font);
        lblCurrentStrategyName = new JLabel("");
        lblCurrentStrategyName.setFont(font);
        
        // duration
        lblDuration = new JLabel("Current Phase Duration: ");
        lblDuration.setFont(font);
        lblCurrentDuration = new JLabel("");
        lblCurrentDuration.setFont(font);
        
        // phase
        lblPhase = new JLabel("Current Phase: ");
        lblPhase.setFont(font);
        lblCurrentPhase = new JLabel("");
        lblCurrentPhase.setFont(font);
        
        // next phase
        lblNextPhase = new JLabel("Current Phase: ");
        lblNextPhase.setFont(font);
        lblCurrentNextPhase = new JLabel("");
        lblCurrentNextPhase.setFont(font);
        
        
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(lblControllerName)
                                                        .addComponent(lblStrategyName)
                                                        .addComponent(lblDuration)
                                                        .addComponent(lblPhase)
                                                        .addComponent(lblNextPhase)
                                                 )
                                                 .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                         .addComponent(lblCurrentControllerName)
                                                         .addComponent(lblCurrentStrategyName)
                                                         .addComponent(lblCurrentDuration)
                                                         .addComponent(lblCurrentPhase)
                                                         .addComponent(lblCurrentNextPhase)
                                                 )
        );
        
        layout.setVerticalGroup(layout.createSequentialGroup()
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblControllerName)
                                                        .addComponent(lblCurrentControllerName) 
                                                )
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblStrategyName)
                                                        .addComponent(lblCurrentStrategyName) 
                                                )
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblDuration)
                                                        .addComponent(lblCurrentDuration) 
                                                )
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblPhase)
                                                        .addComponent(lblCurrentPhase) 
                                                )
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblNextPhase)
                                                        .addComponent(lblCurrentNextPhase) 
                                                )
        );
    }
    
    public void reset() {
        trafficController = null;
    }
    
    public void setController(TrafficLightControlGroup controller) {
        trafficController = controller;
        lblCurrentControllerName.setText(trafficController.groupId());
        lblCurrentStrategyName.setText(trafficController.controlStrategy.getName());
    }

    @Override
    public void updateStatus(double simulationTime) {
        if (trafficController == null) return; 
        lblCurrentDuration.setText(String.format("%.2f", trafficController.getPhaseTime()));
        lblCurrentPhase.setText(Integer.toString(trafficController.getCurrentPhase()));
        lblCurrentNextPhase.setText(Integer.toString(trafficController.getNextPhase()));
        
    }

}
