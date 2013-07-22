package org.movsim.viewer.ui;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.movsim.autogen.Phase;
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
    
    private JLabel lblMinDuration;
    
    private JLabel lblCurrentMinDuration;
    
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
        
        // min duration
        lblMinDuration = new JLabel("Min Duration: ");
        lblMinDuration.setFont(font);
        lblCurrentMinDuration = new JLabel("");
        lblCurrentMinDuration.setFont(font);
        
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
        lblNextPhase = new JLabel("Next Phase: ");
        lblNextPhase.setFont(font);
        lblCurrentNextPhase = new JLabel("");
        lblCurrentNextPhase.setFont(font);
        
        // delay cost
        lblTotalDelayCost = new JLabel("Total Delay Cost: ");
        lblTotalDelayCost.setFont(font);
        lblCurrentTotalDelayCost = new JLabel("");
        lblCurrentTotalDelayCost.setFont(font);
        
        // stopping cost
        lblTotalStoppingCost = new JLabel("Total Stopping Cost: ");
        lblTotalStoppingCost.setFont(font);
        lblCurrentTotalStoppingCost = new JLabel("");
        lblCurrentTotalStoppingCost.setFont(font);
        
        // total cost
        lblTotalCost = new JLabel("Total Cost: ");
        lblTotalCost.setFont(font);
        lblCurrentTotalCost = new JLabel("");
        lblCurrentTotalCost.setFont(font);
        
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(lblControllerName)
                                                        .addComponent(lblStrategyName)
                                                        .addComponent(lblMinDuration)
                                                        .addComponent(lblDuration)
                                                        .addComponent(lblPhase)
                                                        .addComponent(lblNextPhase)
                                                        .addComponent(lblTotalDelayCost)
                                                        .addComponent(lblTotalStoppingCost)
                                                        .addComponent(lblTotalCost)
                                                 )
                                                 .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                         .addComponent(lblCurrentControllerName)
                                                         .addComponent(lblCurrentStrategyName)
                                                         .addComponent(lblCurrentMinDuration)
                                                         .addComponent(lblCurrentDuration)
                                                         .addComponent(lblCurrentPhase)
                                                         .addComponent(lblCurrentNextPhase)
                                                         .addComponent(lblCurrentTotalDelayCost)
                                                         .addComponent(lblCurrentTotalStoppingCost)
                                                         .addComponent(lblCurrentTotalCost)
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
                                                        .addComponent(lblMinDuration)
                                                        .addComponent(lblCurrentMinDuration) 
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
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblTotalDelayCost)
                                                        .addComponent(lblCurrentTotalDelayCost) 
                                                )
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblTotalStoppingCost)
                                                        .addComponent(lblCurrentTotalStoppingCost) 
                                                )
                                                .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
                                                        .addComponent(lblTotalCost)
                                                        .addComponent(lblCurrentTotalCost) 
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
        lblCurrentPhase.setText(Integer.toString(trafficController.getCurrentPhaseIndex()));
        lblCurrentNextPhase.setText(Integer.toString(trafficController.getNextPhase()));
        
        Phase current = trafficController.getCurrentPhase();
        lblCurrentMinDuration.setText(String.format("%.2f", current.getMin()));
        lblCurrentTotalDelayCost.setText(String.format("%.2f", trafficController.getTotalDelayCost()));
        lblCurrentTotalStoppingCost.setText(String.format("%.2f", trafficController.getTotalStoppingCost()));
        lblCurrentTotalCost.setText(String.format("%.2f", trafficController.getTotalCost()));
    }

}
