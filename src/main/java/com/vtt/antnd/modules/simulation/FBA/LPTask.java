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
package com.vtt.antnd.modules.simulation.FBA;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.util.HashMap;
import java.util.Map;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class LPTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private double finishedPercentage = 0.0f;

    private final GetInfoAndTools tools;
    private Double objective;

    private HashMap<String, ReactionFA> reactions;

    public LPTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        this.networkDS = dataset;       
        this.tools = new GetInfoAndTools();

    }

    @Override
    public String getTaskDescription() {
        return "Starting LP optimization... ";
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
        setStatus(TaskStatus.PROCESSING);
        resetFluxes(networkDS.getDocument().getModel());
        finishedPercentage = 0.1f;
        AntGraph g = optimize();
        if (g != null) {
            Dataset newDataset = this.tools.createDataFile(g, networkDS, "- FBA", this.networkDS.getSources(), false, false);
            String info = "Objective value of the objective : " + this.objective;
            this.networkDS.addInfo(info);
            this.networkDS.setReactionsFA(reactions);
            newDataset.addInfo(this.networkDS.getInfo().getText());
            this.setFluxes(newDataset);
        }

        finishedPercentage = 1f;
        setStatus(TaskStatus.FINISHED);
    }

    public void resetFluxes(Model model) {

        for (Reaction r : model.getListOfReactions()) {
            try {
                this.networkDS.setFlux(r.getId(), 0.0);
            } catch (Exception ex) {

            }
        }
    }

    private AntGraph optimize() {
        createReactions();
        objective = this.getFlux();

        if (objective > -100.0) {
            AntGraph g = createGraph();
            return g;
        }
        return null;

    }

    private void addNodesAndEdges(ListOf<SpeciesReference> speciesRef, AntGraph g, AntNode reactionNode, String reactionId) {
        for (SpeciesReference reactant : speciesRef) {
            Species species = reactant.getSpeciesInstance();
            AntNode reactantNode = g.getNode(species.getId());
            if (reactantNode == null) {
                reactantNode = new AntNode(species.getId(), species.getName());
            }
            g.addNode2(reactantNode);
            AntEdge e = null;
            e = new AntEdge(reactionId + " - " + uniqueId.nextId(), reactantNode, reactionNode);
            g.addEdge(e);
        }
    }

    private AntGraph createGraph() {
        Model m = this.networkDS.getDocument().getModel();
        AntGraph g = new AntGraph(null, null);
        m.getListOfReactions().stream().filter((r) -> (this.networkDS.getFlux(r.getId()) > 0.0000001)).forEachOrdered((r) -> {
            AntNode reactionNode = new AntNode(r.getId(), String.format("%.3g%n", this.networkDS.getFlux(r.getId())));
            g.addNode2(reactionNode);
            
            addNodesAndEdges(r.getListOfReactants(), g, reactionNode, r.getId());
            addNodesAndEdges(r.getListOfProducts(), g, reactionNode, r.getId());
        });
        return g;
    }

    public double getFlux() {
        FBA fba = new FBA();
        fba.setModel(this.reactions, this.networkDS.getDocument().getModel());
        try {
            Map<String, Double> soln = fba.run();
            soln.keySet().stream().filter((r) -> (this.reactions.containsKey(r))).forEachOrdered((r) -> {
                double flux = soln.get(r);
                if (flux < 0.000001) {
                    flux = 0.0;
                }
                this.reactions.get(r).setFlux(flux);
                this.networkDS.setFlux(r, flux);
            });
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return fba.getMaxObj();
    }

    private void addCompoundToReaction(boolean isReactant, ListOf<SpeciesReference> spref, ReactionFA reaction, Model m) {
        for (int e = 0; e < spref.size(); e++) {
            SpeciesReference s = (SpeciesReference) spref.get(e);
            Species sp = m.getSpecies(s.getSpecies());
            if (isReactant) {
                reaction.addReactant(sp.getId(), sp.getName(), s.getStoichiometry());
            } else {
                reaction.addProduct(sp.getId(), sp.getName(), s.getStoichiometry());
            }
        }

    }

    private void createReactions() {

        this.reactions = new HashMap<>();

        SBMLDocument doc = this.networkDS.getDocument();
        Model m = doc.getModel();

        ListOf reactions = m.getListOfReactions();
        for (int i = 0; i < reactions.size(); i++) {
            Reaction r = (Reaction) reactions.get(i);
            ReactionFA reaction = new ReactionFA(r.getId());

            reaction.setObjective(this.networkDS.getObjective(r.getId()));
            reaction.setBounds(this.networkDS.getLowerBound(r.getId()), this.networkDS.getUpperBound(r.getId()));
            ListOf spref = r.getListOfReactants();
            addCompoundToReaction(true, spref, reaction, m);

            spref = r.getListOfProducts();
            addCompoundToReaction(false, spref, reaction, m);

            this.reactions.put(r.getId(), reaction);
        }
    }

    private void setFluxes(Dataset newDataset) {
        this.reactions.keySet().forEach((reaction) -> {
            newDataset.setFlux(reaction, this.reactions.get(reaction).getFlux());
        });
    }

}
