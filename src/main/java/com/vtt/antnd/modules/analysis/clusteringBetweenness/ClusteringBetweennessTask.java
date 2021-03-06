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
package com.vtt.antnd.modules.analysis.clusteringBetweenness;

import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author scsandra
 */
public class ClusteringBetweennessTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private final int numberOfEdges;
    private double finishedPercentage = 0.0f;
    private final JInternalFrame frame;
    private final JScrollPane panel;
    private final JTextArea tf;
    private final StringBuffer info;
    private final Random rand;

    public ClusteringBetweennessTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        networkDS = dataset;
        this.numberOfEdges = parameters.getParameter(ClusteringBetweennessParameters.numberOfEdges).getValue();
        this.frame = new JInternalFrame("Result", true, true, true, true);
        this.tf = new JTextArea();
        this.panel = new JScrollPane(this.tf);
        this.info = new StringBuffer();
        this.rand = new Random();
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
            AntGraph graph = this.networkDS.getGraph();

            edu.uci.ics.jung.graph.Graph<String, String> g = this.getGraphForClustering(graph);
            EdgeBetweennessClusterer cluster = new EdgeBetweennessClusterer(this.numberOfEdges);
            Set<Set<String>> result = cluster.transform(g);
            int i = 1;
            for (Set<String> clust : result) {
                info.append("Cluster ").append(i++).append(":\n");
                for (String nodes : clust) {
                    info.append(nodes).append("\n");
                }
            }
            this.tf.setText(info.toString());
            frame.setSize(new Dimension(700, 500));
            frame.add(this.panel);
            NDCore.getDesktop().addInternalFrame(frame);

            createDataSet(result);

            finishedPercentage = 1.0f;
            setStatus(TaskStatus.FINISHED);
        } catch (Exception e) {
            setStatus(TaskStatus.ERROR);
            errorMessage = e.toString();
        }
    }

    public edu.uci.ics.jung.graph.Graph<String, String> getGraphForClustering(AntGraph graph) {
        edu.uci.ics.jung.graph.Graph<String, String> g = new SparseMultigraph<>();

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

    private void createDataSet(Set<Set<String>> result) {
        Map<Integer, Color> colors = new HashMap<>();
        AntGraph graph = this.networkDS.getGraph().clone();
        List<AntNode> nodes = graph.getNodes();

        for (AntNode n : nodes) {
            int cluster = getClusterNumber(n.getId(), result);
            n.setId(n.getId() + " :" + n.getName() + "(" + cluster + ")");
            if (colors.containsKey(cluster)) {
                n.setColor(colors.get(cluster));
            } else {
                Color c = this.getClusterColor();
                colors.put(cluster, c);
                n.setColor(c);
            }
        }
        SimpleBasicDataset dataset = new GetInfoAndTools().createDataFile(graph, this.networkDS, "Betweenness Clustering", this.networkDS.getSources(), true, false);
        dataset.addInfo(this.info.toString());
    }

    private int getClusterNumber(String id, Collection<Set<String>> result) {
        int i = 0;
        for (Set<String> set : result) {
            if (set.contains(id)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private Color getClusterColor() {
        int r = rand.nextInt(155) + 100; // 128 ... 255
        int g = rand.nextInt(155) + 010; // 128 ... 255
        int b = rand.nextInt(155) + 001; // 128 ... 255

        return new Color(r, g, b).brighter();

    }
}
