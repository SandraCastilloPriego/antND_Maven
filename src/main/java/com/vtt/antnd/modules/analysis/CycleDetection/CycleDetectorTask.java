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
package com.vtt.antnd.modules.analysis.CycleDetection;

import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.Edge;
import com.vtt.antnd.data.network.Graph;
import com.vtt.antnd.data.network.Node;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.sbml.libsbml.KineticLaw;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Reaction;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class CycleDetectorTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private double finishedPercentage = 0.0f;
    private final JInternalFrame frame;
    private final JScrollPane panel;
    private final JTextArea tf;
    private final StringBuffer info;
    private final GetInfoAndTools tools;

    public CycleDetectorTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        networkDS = dataset;
        this.frame = new JInternalFrame("Result", true, true, true, true);
        this.tf = new JTextArea();
        this.panel = new JScrollPane(this.tf);
        this.info = new StringBuffer();
        this.tools = new GetInfoAndTools();
    }

    @Override
    public String getTaskDescription() {
        return "Clustering... ";
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
            if (this.networkDS == null) {
                setStatus(TaskStatus.ERROR);
                NDCore.getDesktop().displayErrorMessage("You need to select a metabolic model.");
            }

            if (this.networkDS.getGraph() == null) {
                setStatus(TaskStatus.ERROR);
                NDCore.getDesktop().displayErrorMessage("This data set doesn't contain a valid graph.");
            }

            DefaultDirectedGraph g = this.getGraphForClustering(this.networkDS.getDocument().getModel());
            
            CycleDetector cyclesDetector = new CycleDetector(g);
            Set<String> cycles = cyclesDetector.findCycles();
            this.createGraph(cycles,this.networkDS.getDocument().getModel());
            for (String n : cycles) {
                info.append(n).append("\n");
            }

            this.tf.setText(info.toString());
            frame.setSize(new Dimension(700, 500));
            frame.add(this.panel);
            NDCore.getDesktop().addInternalFrame(frame);

            finishedPercentage = 1.0f;
            setStatus(TaskStatus.FINISHED);
        } catch (Exception e) {
            setStatus(TaskStatus.ERROR);
            errorMessage = e.toString();
        }
    }

  
    public DefaultDirectedGraph getGraphForClustering(Model model) {

        DefaultDirectedGraph<String, DefaultEdge> jgraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        ListOf listOfSpecies = model.getListOfSpecies();
        for (int i = 0; i < model.getNumSpecies(); i++) {
            Species sp = (Species) listOfSpecies.get(i);
            jgraph.addVertex(sp.getId());
        }

       // HashMap<String, String[]> bounds = new GetInfoAndTools().readBounds(this.networkDS);
        ListOf listOfReactions = model.getListOfReactions();
        for (int i = 0; i < model.getNumReactions(); i++) {
            Reaction r = (Reaction) listOfReactions.get(i);
            jgraph.addVertex(r.getId());
           // String[] boundR = bounds.get(r.getId());
            // if (Double.valueOf(boundR[3]) < 0) {
            ListOf reactants = r.getListOfReactants();
            for (int e = 0; e < reactants.size(); e++) {
                SpeciesReference spref = (SpeciesReference) reactants.get(e);
                String id = spref.getSpecies();
                
                try {
                    if (model.getSpecies(id) != null) {
                        jgraph.addEdge(id, r.getId());
                        jgraph.addEdge(r.getId(),id);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.toString() + " - "+ id);
                }

            }
            ListOf products = r.getListOfProducts();
            for (int e = 0; e < products.size(); e++) {
                SpeciesReference spref = (SpeciesReference) products.get(e);
                String id = spref.getSpecies();
                try {
                    if (model.getSpecies(id) != null) {
                        jgraph.addEdge(r.getId(), id);
                        jgraph.addEdge(id, r.getId());
                    }
                } catch (Exception ex) {                    
                    System.out.println(ex.toString() + " - "+ id);
                }
            }
            // }
        }
        return jgraph;
    }

    private Node getNode(ArrayList<Node> nodes, String id){
        for( Node n :nodes){   
            if(n.getId().equals(id)) return n;
        }
        return null;
    }
    
    private void createGraph(Set<String> cycles, Model model) {
        
       
        ArrayList<Node> nodes = new ArrayList<>();
        ArrayList<Edge> edges = new ArrayList<>();
        for(String cycle : cycles){
            Node node = new Node(cycle);
            nodes.add(node);
        }
        for (String cycle: cycles){
            if(model.getReaction(cycle)!= null){
                  Reaction r = model.getReaction(cycle);
                  KineticLaw law = r.getKineticLaw();
                  Double lowerBound = law.getParameter("LOWER_BOUND").getValue();
                  Double upperBound = law.getParameter("UPPER_BOUND").getValue();                  
                  ListOf spref= r.getListOfReactants();
                  
                  for(int i = 0; i < spref.size(); i++){
                      String sp = ((SpeciesReference)spref.get(i)).getSpecies();
                  
                      if(lowerBound < 0 && upperBound > 0){
                          Edge e = new Edge(sp + uniqueId.nextId(), getNode(nodes, sp), getNode(nodes, r.getId()), false);
                          edges.add(e);
                      }else if(lowerBound < 0 && upperBound == 0){
                          Edge e = new Edge(sp + uniqueId.nextId(), getNode(nodes, r.getId()),getNode(nodes, sp), true);
                          edges.add(e);
                      }else if(lowerBound ==0  && upperBound > 0){
                          Edge e = new Edge(sp + uniqueId.nextId(), getNode(nodes, sp), getNode(nodes, r.getId()), true);
                          edges.add(e);
                      }
                      
                  }
                  
                  spref= r.getListOfProducts();
                  
                  for(int i = 0; i < spref.size(); i++){
                      String sp = ((SpeciesReference)spref.get(i)).getSpecies();
                  
                      if(lowerBound < 0 && upperBound > 0){
                          Edge e = new Edge(sp + uniqueId.nextId(), getNode(nodes, sp), getNode(nodes, r.getId()), false);
                          edges.add(e);
                      }else if(lowerBound < 0 && upperBound == 0){
                          Edge e = new Edge(sp + uniqueId.nextId(), getNode(nodes, r.getId()),getNode(nodes, sp), true);
                          edges.add(e);
                      }else if(lowerBound ==0  && upperBound > 0){
                          Edge e = new Edge(sp + uniqueId.nextId(), getNode(nodes, sp), getNode(nodes, r.getId()), true);
                          edges.add(e);
                      }
                      
                  }
            }
            
        }
        Graph g = new Graph(nodes,edges);
       
        this.tools.createDataFile(g, networkDS, "cycles", null, false,false);
    }

}
