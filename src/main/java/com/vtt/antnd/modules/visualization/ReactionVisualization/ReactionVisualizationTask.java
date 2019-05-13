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
package com.vtt.antnd.modules.visualization.ReactionVisualization;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.desktop.impl.PrintPaths2;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.util.ArrayList;
import javax.swing.JInternalFrame;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class ReactionVisualizationTask extends AbstractTask {

    private final SimpleBasicDataset dataset;
    private final double finishedPercentage = 0.0f;
    private final String reactionId;
    private GetInfoAndTools tools;

    public ReactionVisualizationTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        this.dataset = dataset;
        this.reactionId = parameters.getParameter(ReactionVisualizationParameters.reactionId).getValue();
        this.tools = new GetInfoAndTools();
    }

    @Override
    public String getTaskDescription() {
        return "Starting Fluxes visualization... ";
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

            Model m = this.dataset.getDocument().getModel();
            AntGraph g = this.getGraph(m, this.reactionId);

            JInternalFrame frame = new JInternalFrame("ReactionVis", true, true, true, true);

            Dataset newDataset = tools.createDataFile(g, this.dataset, null, null, false, false);

            PrintPaths2 print = new PrintPaths2(newDataset);

            try {

                System.out.println("Visualize");
                frame.add(print.printPathwayInFrame(g));
               
            } catch (NullPointerException ex) {
                System.out.println(ex.toString());
            }
            NDCore.getDesktop().addInternalFrame(frame);
            frame.pack();
            setStatus(TaskStatus.FINISHED);

        } catch (Exception e) {
            System.out.println(e.toString());
            setStatus(TaskStatus.ERROR);
        }
    }

    public AntGraph getGraph(Model model, String reaction) {

        java.util.List<AntNode> nodeList = new ArrayList<>();
        java.util.List<AntEdge> edgeList = new ArrayList<>();

        AntGraph g = new AntGraph(nodeList, edgeList);

        try {
            // For eac reaction in the model, a Node is created in the graph

            Reaction r = model.getReaction(reaction);
            System.out.println(r.getId());
            //Node is created
            AntNode reactionNode = new AntNode(r.getId(), r.getName());
            // this.setPosition(reactionNode, g);

            g.addNode2(reactionNode);
            int direction = this.getDirection(r);
            // read bounds to know the direction of the edges
            // ListOf listOfReactants = r.getListOfReactants();
            //System.out.println(listOfReactants.size());
            //for (int e = 0; e < r.getNumReactants(); e++) {
            //    SpeciesReference spr = (SpeciesReference) listOfReactants.get(e);
            for (SpeciesReference spr : r.getListOfReactants()) {
                System.out.println(spr.getSpecies());
                Species sp = model.getSpecies(spr.getSpecies());
                System.out.println(sp.getName());
                AntNode spNode = g.getNode(sp.getId());
                if (spNode == null) {
                    spNode = new AntNode(sp.getId(), sp.getName());
                }
                //this.setPosition(spNode, g);
                g.addNode2(spNode);
                g.addEdge(addEdge(spNode, reactionNode, sp.getId(), direction));

            }

            // ListOf listOfProducts = r.getListOfProducts();
            //for (int e = 0; e < r.getNumProducts(); e++) {
            //    SpeciesReference spr = (SpeciesReference) listOfProducts.get(e);
            for (SpeciesReference spr : r.getListOfProducts()) {
                Species sp = model.getSpecies(spr.getSpecies());

                AntNode spNode = g.getNode(sp.getId());
                if (spNode == null) {
                    spNode = new AntNode(sp.getId(), sp.getName());
                }
                //this.setPosition(spNode, g);
                g.addNode2(spNode);
                g.addEdge(addEdge(reactionNode, spNode, sp.getId(), direction));

            }

        } catch (Exception e) {
            System.out.println(e.toString());
        }
        System.out.println(g.getNumberOfNodes());
        return g;
    }

    private int getDirection(Reaction r) {
        int direction = 2;

        Double lb = dataset.getLowerBound(r.getId());
        Double ub = dataset.getUpperBound(r.getId());
        Double flux = dataset.getFlux(r.getId());
        if (flux != null) {
            if (flux > 0) {
                direction = 1;
            } else if (flux < 0) {
                direction = 0;
            }
        } else {
            if (ub > 0 && lb < 0) {
                direction = 2;
            } else if (ub > 0) {
                direction = 1;
            } else {
                direction = 0;
            }
        }
        return direction;
    }

    private AntEdge addEdge(AntNode node1, AntNode node2, String name, int direction) {
        AntEdge e = null;
        switch (direction) {
            case 1:
                e = new AntEdge(name + " - " + uniqueId.nextId(), node1, node2);
                e.setDirection(true);
                break;
            case 2:
                e = new AntEdge(name + " - " + uniqueId.nextId(), node1, node2);
                e.setDirection(false);
                break;
            case 0:
                e = new AntEdge(name + " - " + uniqueId.nextId(), node2, node1);
                e.setDirection(true);
                break;
            default:
                break;
        }
        return e;
    }
}
