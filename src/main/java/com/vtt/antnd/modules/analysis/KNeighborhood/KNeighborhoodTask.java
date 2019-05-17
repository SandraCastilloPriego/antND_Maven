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
package com.vtt.antnd.modules.analysis.KNeighborhood;


import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.configuration.cofactors.CofactorConfParameters;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import edu.uci.ics.jung.algorithms.filters.KNeighborhoodFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;


/**
 *
 * @author scsandra
 */
public class KNeighborhoodTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private final int radiusk;
    private final String rootNode;
    private double finishedPercentage = 0.0f;
    private final List<String> cofactors;

    public KNeighborhoodTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        networkDS = dataset;
        this.radiusk = parameters.getParameter(KNeighborhoodParameters.radiusK).getValue();
        this.rootNode = parameters.getParameter(KNeighborhoodParameters.rootNode).getValue();
        //String excluded = parameters.getParameter(KNeighborhoodParameters.excluded).getValue();
        this.cofactors = new ArrayList<>();
        CofactorConfParameters conf = new CofactorConfParameters();
        String excluded = conf.getParameter(CofactorConfParameters.cofactors).getValue().replaceAll("[\\s|\\u00A0]+", "");
        System.out.println(excluded);
        String[] excludedCompounds = excluded.split(",");
        this.cofactors.addAll(Arrays.asList(excludedCompounds));
        //String[] excludedCompounds = excluded.split(",");
        //for (String cofactor : excludedCompounds) {
        //    this.cofactors.add(cofactor);
        //}

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
            
            AntGraph graph = createGraph();
           

            edu.uci.ics.jung.graph.Graph<String, String> g = this.getGraphForClustering(graph);
            String root = findTheRoot(this.rootNode, g);
            KNeighborhoodFilter filter = new KNeighborhoodFilter(root, this.radiusk, KNeighborhoodFilter.EdgeType.OUT);
            edu.uci.ics.jung.graph.Graph<String, String> g2 = filter.transform(g);

            createDataSet(g2);

            finishedPercentage = 1.0f;
            setStatus(TaskStatus.FINISHED);
        } catch (Exception e) {
            setStatus(TaskStatus.ERROR);
            errorMessage = e.toString();
        }
    }

    public edu.uci.ics.jung.graph.Graph<String, String> getGraphForClustering(AntGraph graph) {
        edu.uci.ics.jung.graph.Graph<String, String> g = new DirectedSparseMultigraph<>();

        List<AntNode> nodes = graph.getNodes();
        List<AntEdge> edges = graph.getEdges();
        System.out.println("Number of nodes: " + nodes.size() + " - " + edges.size());

        for (AntNode node : nodes) {
            if (node != null) {
                g.addVertex(node.getId());
            }
        }

        for (AntEdge edge : edges) {
            if (edge != null) {
                g.addEdge(edge.getId(), edge.getSource().getId(), edge.getDestination().getId(), EdgeType.DIRECTED);
            }
        }
        return g;

    }

    private void createDataSet(edu.uci.ics.jung.graph.Graph<String, String> g) {
        Map<String, AntNode> nodesMap = new HashMap<>();
        List<AntNode> nodes = new ArrayList<>();
        for (String n : g.getVertices()) {
            AntNode node = new AntNode(n);
            nodes.add(node);
            nodesMap.put(n, node);
        }
        List<AntEdge> edges = new ArrayList<>();
        for (String n : g.getEdges()) {
            // System.out.println(g.toString());
            System.out.println(n + " - " + g.getSource(n) + " - " + g.getDest(n));
            AntEdge e = new AntEdge(n, nodesMap.get(g.getSource(n)), nodesMap.get(g.getDest(n)));
            edges.add(e);
        }
        AntGraph graph = new AntGraph(nodes, edges);
        new GetInfoAndTools().createDataFile(graph, this.networkDS, "KNeighbourhood", this.networkDS.getSources(), false, false);

    }

    private String findTheRoot(String rootNode, edu.uci.ics.jung.graph.Graph<String, String> g) {
        for (String s : g.getVertices()) {
            if (s.equals(rootNode)) {
                return s;
            }
        }
        return rootNode;
    }

    private AntGraph createGraph() {
        Model m = this.networkDS.getDocument().getModel();
        AntGraph g = new AntGraph(null, null);
        
        for (Reaction reaction: m.getListOfReactions()) {           
            AntNode reactionNode = new AntNode(reaction.getId());
            g.addNode2(reactionNode);

  
            for (SpeciesReference reactant:reaction.getListOfReactants()) {              
                Species sp = m.getSpecies(reactant.getSpecies());
                if (!this.cofactors.contains(sp.getId())) {
                    //System.out.println(sp.getName());
                    AntNode reactantNode = g.getNode(sp.getId());
                    if (reactantNode == null) {
                        reactantNode = new AntNode(sp.getId(), sp.getName());
                    }
                    g.addNode2(reactantNode);
                    AntEdge edge = new AntEdge(reaction.getId() + " - " + uniqueId.nextId(), reactantNode, reactionNode);

                    g.addEdge(edge);
                }
            }
            
           
            for (SpeciesReference product:reaction.getListOfProducts()) {            
                Species sp = m.getSpecies(product.getSpecies());
                if (!this.cofactors.contains(sp.getId())) {
                    AntNode reactantNode = g.getNode(sp.getId());
                    if (reactantNode == null) {
                        reactantNode = new AntNode(sp.getId(), sp.getName());
                    }
                    g.addNode2(reactantNode);
                    AntEdge edge = new AntEdge(reaction.getName() + " - " + uniqueId.nextId(), reactionNode, reactantNode);

                    g.addEdge(edge);
                }
            }
        }

        return g;
    }
}
