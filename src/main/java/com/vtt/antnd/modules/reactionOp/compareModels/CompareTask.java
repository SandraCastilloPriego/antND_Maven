/*
 * Copyright 2013-2014 VTT Biotechnology
 * This file is part of AntND.
 *
 * AntND is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * AntND is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * AntND; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.vtt.antnd.modules.reactionOp.compareModels;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.main.NDCore;
import static com.vtt.antnd.modules.analysis.reports.ReportFBATask.createBarChart;
import static com.vtt.antnd.modules.analysis.reports.ReportFBATask.createPieChart;
import static com.vtt.antnd.modules.analysis.reports.ReportFBATask.createPieDataset;
import static com.vtt.antnd.modules.analysis.reports.ReportFBATask.createBarDataset;
import static com.vtt.antnd.modules.analysis.reports.ReportFBATask.createBarExchangeDataset;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.PieDataset;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class CompareTask extends AbstractTask {
    
    private final Dataset[] networkDS;
    private double finishedPercentage = 0.0f;
    private final JInternalFrame frame;
    private final JScrollPane panel;
    private final JTextArea tf;
    private final StringBuffer info;
    
    public CompareTask(Dataset[] datasets, SimpleParameterSet parameters) {
        networkDS = datasets;        
        this.frame = new JInternalFrame("Result", true, true, true, true);
        this.tf = new JTextArea();
        this.panel = new JScrollPane(this.tf);
        this.info = new StringBuffer();
    }
    
    @Override
    public String getTaskDescription() {
        return "Comparing... ";
    }
    
    @Override
    public double getFinishedPercentage() {
        return finishedPercentage;
    }
    
    @Override
    public void cancel() {
        setStatus(TaskStatus.CANCELED);
    }
    
    @Override
    public void run() {
        try {
            setStatus(TaskStatus.PROCESSING);
            try {
                writeGraphicReport();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            try {
                writeReport();
            } catch (Exception a) {
                System.out.println(a.toString());
            }
            finishedPercentage = 1.0f;
            setStatus(TaskStatus.FINISHED);
        } catch (Exception e) {
            setStatus(TaskStatus.ERROR);
            errorMessage = e.toString();
        }
    }
    
    private void writeReport() {
        if (this.networkDS == null) {
            setStatus(TaskStatus.ERROR);
            NDCore.getDesktop().displayErrorMessage("You need to select two metabolic models.");
        }
        
        Model model1 = this.networkDS[0].getDocument().getModel();
        Model model2 = this.networkDS[1].getDocument().getModel();
        
        this.info.append("Reactions present in only in ").append(this.networkDS[0].getDatasetName()).append(":\n");
        ListOf listOfReactions = model1.getListOfReactions();
        for (int i = 0; i < model1.getNumReactions(); i++) {
            
            Reaction r = (Reaction) listOfReactions.get(i);
            if (model2.getReaction(r.getId()) == null) {
                showReactions(model1, r, null);
            }
        }
        
        this.info.append("Reactions present in only in ").append(this.networkDS[1].getDatasetName()).append(":\n");
        ListOf listOfReactions2 = model2.getListOfReactions();
        for (int i = 0; i < model2.getNumReactions(); i++) {
            Reaction r = (Reaction) listOfReactions2.get(i);
            if (model1.getReaction(r.getId()) == null) {
                showReactions(model2, r, null);
            }
        }
        
        List<String> commonReactions = new ArrayList<>();
        ListOf listOfReactions3 = model1.getListOfReactions();
        for (int i = 0; i < model1.getNumReactions(); i++) {
            Reaction r = (Reaction) listOfReactions3.get(i);
            if (model2.getReaction(r.getId()) != null) {
                commonReactions.add(r.getId());
            }
        }
        
        try {
            this.info.append("Common reactions with different fluxes in ").append(this.networkDS[1].getDatasetName()).append(":\n");
            for (String re : commonReactions) {
                showReactions2(model1.getReaction(re), model2.getReaction(re));
            }
        } catch (Exception e) {
        }
        // this.networkDS.setInfo(info.toString());
        this.tf.setText(info.toString());
        frame.setSize(new Dimension(700, 500));
        frame.add(this.panel);
        NDCore.getDesktop().addInternalFrame(frame);
    }
    
    private void showReactions(Model model, Reaction reaction, Reaction reaction2) {
        DecimalFormat df = new DecimalFormat("#.####");
        
        Double lbound = this.networkDS[0].getLowerBound(reaction.getId());
        Double ubound = this.networkDS[0].getUpperBound(reaction.getId());
        Double flux = this.networkDS[0].getFlux(reaction.getId());
        info.append(reaction.getId()).append(" - ").append(reaction.getName()).append(" lb: ").append(lbound).append(" up: ").append(ubound).append("\n");
        try {
            if (reaction2 == null) {
                info.append("Flux: ").append(flux).append("\n");
                
            } else {
                Double flux2 = this.networkDS[1].getFlux(reaction2.getId());
                info.append("Flux1: ").append(df.format(flux)).append(" - Flux2: ").append(df.format(flux2)).append("\n");
                
            }
        } catch (Exception ex) {
        }
        
        info.append("Reactants: \n");
        ListOf listOfSR = reaction.getListOfReactants();
        for (int i = 0; i < reaction.getNumReactants(); i++) {
            SpeciesReference sr = (SpeciesReference) listOfSR.get(i);
            Species sp = model.getSpecies(sr.getSpecies());
            if (sp != null) {
                info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append("\n");
            }
        }
        
        info.append("Products: \n");
        ListOf listOfSP = reaction.getListOfProducts();
        for (int i = 0; i < reaction.getNumProducts(); i++) {
            SpeciesReference sr = (SpeciesReference) listOfSP.get(i);
            Species sp = model.getSpecies(sr.getSpecies());
            if (sp != null) {
                info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append(" \n");
            }
        }
        
        info.append("----------------------------------- \n");
        
    }
    
    private void showReactions2(Reaction reaction, Reaction reaction2) {
        DecimalFormat df = new DecimalFormat("#.####");
        Double flux = this.networkDS[0].getFlux(reaction.getId());
        if (reaction2 == null) {
            info.append(reaction.getId()).append(flux).append("\n");
        } else {
            Double flux2 = this.networkDS[1].getFlux(reaction2.getId());
            if (!df.format(flux).equals(df.format(flux2))) {
                info.append(reaction.getId()).append(":  ").append(df.format(flux)).append(" - ").append(df.format(flux2)).append(" --> ").append(reaction.getName()).append("\n");
            }
        }
    }
    
    private void writeGraphicReport() {
        
        JPanel localPanel = new JPanel();
        localPanel.setLayout(new BoxLayout(localPanel, BoxLayout.PAGE_AXIS));
        localPanel.setBackground(Color.white);
        
        String localInfo = "";
        for (Dataset data : this.networkDS) {
            Model m = data.getDocument().getModel();
            localInfo += "Model: " + data.getID() + ", " + data.getDatasetName() + "\n";
            localInfo += "Number of active reactions: " + m.getNumReactions() + "\n";
            localInfo += "Number of active reactions where the flux > abs(0.0001): " + getBigFluxes(data) + "\n";
            localInfo += "----------------------------- \n";
        }
        JTextArea area = new JTextArea(localInfo);
        area.setEditable(false);
        localPanel.add(area);
        
        for (Dataset data : this.networkDS) {           
            area = new JTextArea("Model: " + data.getID() + ", " + data.getDatasetName() + "\n");
            area.setEditable(false);
            localPanel.add(area);
            List<PieDataset> datasets = createPieDataset(data);
            JFreeChart exchangesPos = createPieChart(datasets.get(0), "Exchanges out");
            JFreeChart exchangesNeg = createPieChart(datasets.get(1), "Exchanges in");
            JPanel chartpanel = new JPanel();
            chartpanel.add(new ChartPanel(exchangesNeg));
            chartpanel.add(new ChartPanel(exchangesPos));
            chartpanel.setBackground(Color.white);
            localPanel.add(chartpanel);
        }
        
        for (Dataset data : this.networkDS) {
            Model m = data.getDocument().getModel();
            area = new JTextArea("Model: " + data.getID() + ", " + data.getDatasetName() + "\n");
            area.setEditable(false);
            localPanel.add(area);            
            CategoryDataset dataset = createBarExchangeDataset(data);
            JFreeChart fluxesChart = createBarChart(dataset, "Exchange reactions in " + m.getId());
            JPanel fPanel = new JPanel();
            fPanel.add(new ChartPanel(fluxesChart));
            fPanel.setPreferredSize(new Dimension(500, 500));
            fPanel.setBackground(Color.white);
            localPanel.add(fPanel);
            
        }
        
        for (Dataset data : this.networkDS) {
            Model m = data.getDocument().getModel();
            area = new JTextArea("Model: " + data.getID() + ", " + data.getDatasetName() + "\n");
            area.setEditable(false);
            localPanel.add(area);           
            CategoryDataset dataset = createBarDataset(data);
            JFreeChart fluxesChart = createBarChart(dataset, "Important fluxes");
            localPanel.add(new ChartPanel(fluxesChart));
        }
        
        JInternalFrame frameTable = new JInternalFrame("Report", true, true, true, true);
        JScrollPane scrollPanel = new JScrollPane(localPanel);
        frameTable.setSize(new Dimension(700, 500));
        frameTable.add(scrollPanel);
        
        NDCore.getDesktop().addInternalFrame(frameTable);
        frameTable.setVisible(true);
        
    }
    
    private String getBigFluxes(Dataset dataset) {
        int i = 0;        
        Model m = dataset.getDocument().getModel();
        for (Reaction r : m.getListOfReactions()) {            
            double flux = dataset.getFlux(r.getId());
            if (Math.abs(flux) >= 0.0001) {
                i++;
            }
        }
        return String.valueOf(i);
    }
    
}
