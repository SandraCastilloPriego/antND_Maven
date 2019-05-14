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
package com.vtt.antnd.modules.simulation.pFBA;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.modules.simulation.FBA.FBA;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.util.HashMap;
import java.util.Map;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.Objective;

/**
 *
 * @author scsandra
 */
public class LPTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private double finishedPercentage = 0.0f;
    //  private final String objectiveSpecie;

    private final GetInfoAndTools tools;
    private Double objective;

    private HashMap<String, ReactionFA> reactions;

    public LPTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        this.networkDS = dataset;
        // this.objectiveSpecie = parameters.getParameter(LPParameters.objective).getValue();
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
            Dataset newDataset = this.tools.createDataFile(g, networkDS, " ", this.networkDS.getSources(), false, false);
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
    
    private void setFluxes(Dataset newDataset) {
        for (String reaction : this.reactions.keySet()) {
            newDataset.setFlux(reaction, this.reactions.get(reaction).getFlux());
        }
    }

    private AntGraph optimize() {
        createReactions();
        objective = this.getFlux();
        objective = this.minimizeFluxes(objective);

        AntGraph g = createGraph();
        return g;

    }

    private AntGraph createGraph() {
        Model m = this.networkDS.getDocument().getModel();
        AntGraph g = new AntGraph(null, null);
        for (Reaction r : m.getListOfReactions()) {
            if (this.networkDS.getFlux(r.getId()) > 0.0000001) {
                AntNode reactionNode = new AntNode(r.getId(), String.format("%.3g%n", this.networkDS.getFlux(r.getId())));
                g.addNode2(reactionNode);

                for (SpeciesReference reactant : r.getListOfReactants()) {
                    Species species = reactant.getSpeciesInstance();
                    AntNode reactantNode = g.getNode(species.getId());
                    if (reactantNode == null) {
                        reactantNode = new AntNode(species.getId(), species.getName());
                    }
                    g.addNode2(reactantNode);
                    AntEdge e = new AntEdge(r + " - " + uniqueId.nextId(), reactantNode, reactionNode);
                    g.addEdge(e);
                }
                for (SpeciesReference product : r.getListOfProducts()) {
                    Species species = product.getSpeciesInstance();
                    AntNode reactantNode = g.getNode(species.getId());
                    if (reactantNode == null) {
                        reactantNode = new AntNode(species.getId(), species.getName());
                    }
                    g.addNode2(reactantNode);
                    AntEdge e = new AntEdge(r + " - " + uniqueId.nextId(), reactionNode, reactantNode);
                    g.addEdge(e);
                }
            }
        }
        return g;
    }

    /*private AntGraph createGraph() {
        Model m = this.networkDS.getDocument().getModel();
        AntGraph g = new AntGraph(null, null);
        AntGraph previousG = this.networkDS.getGraph();
        for (String r : reactions.keySet()) {
            ReactionFA reaction = reactions.get(r);
            if (reaction != null && Math.abs(reaction.getFlux()) > 0.000001) {
                AntNode reactionNode = new AntNode(reaction.getId(), String.format("%.3g%n", reaction.getFlux()));
                if (previousG != null && previousG.getNode(reaction.getId()) != null) {
                    reactionNode.setPosition(previousG.getNode(reaction.getId()).getPosition());
                }
                g.addNode2(reactionNode);
                Reaction modelReaction = m.getReaction(r);
                if (modelReaction != null) {
                    this.networkDS.setFlux(modelReaction.getId(), reaction.getFlux());
                }

                for (String reactant : reaction.getReactants()) {
                    String name = m.getSpecies(reactant).getName();
                    AntNode reactantNode = g.getNode(reactant);
                    if (reactantNode == null) {
                        reactantNode = new AntNode(reactant, name);
                    }
                    g.addNode2(reactantNode);
                    if (previousG != null && previousG.getNode(reactant) != null) {
                        reactantNode.setPosition(previousG.getNode(reactant).getPosition());
                    }
                    AntEdge e = null;
                    if (reaction.getFlux() > 0) {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactantNode, reactionNode);
                    } else {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactionNode, reactantNode);
                    }

                    g.addEdge(e);
                }
                for (String product : reaction.getProducts()) {
                    String name = m.getSpecies(product).getName();
                    AntNode reactantNode = g.getNode(product);
                    if (reactantNode == null) {
                        reactantNode = new AntNode(product, name);
                    }
                    if (previousG != null && previousG.getNode(product) != null) {
                        reactantNode.setPosition(previousG.getNode(product).getPosition());
                    }
                    g.addNode2(reactantNode);
                    AntEdge e = null;
                    if (reaction.getFlux() > 0) {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactionNode, reactantNode);
                    } else {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactantNode, reactionNode);
                    }

                    g.addEdge(e);
                }
            }
        }
        return g;
    }*/
    public double getFlux() {
        FBA fba = new FBA();
        fba.setModel(this.reactions, this.networkDS.getDocument().getModel());
        try {
            Map<String, Double> soln = fba.run();
            for (String r : soln.keySet()) {
                soln.get(r);
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return fba.getMaxObj();
    }

    private double minimizeFluxes(Double objective) {
        FBAm fbam = new FBAm();
        fbam.setModel(this.reactions, this.networkDS.getDocument().getModel(), objective);
        try {
            Map<String, Double> soln = fbam.run();
            for (String r : soln.keySet()) {
                if (this.reactions.containsKey(r)) {
                    double flux = soln.get(r);
                    if (flux < 0.000001) {
                        flux = 0.0;
                    }
                    this.reactions.get(r).setFlux(flux);
                    this.networkDS.setFlux(r, flux);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return fbam.getMaxObj();
    }

    private double getObjectiveFBC(String r, Model model) {
        try {
            FBCModelPlugin plugin = (FBCModelPlugin) model.getPlugin("fbc");
            for (Objective obj : plugin.getListOfObjectives()) {
                for (FluxObjective fobj : obj.getListOfFluxObjectives()) {
                    if (fobj.getReaction().equals(r)) {
                        return fobj.getCoefficient();
                    }
                }
            }
        } catch (NullPointerException n) {
            return 0.0;
        }
        return 0.0;
    }

    private void createReactions() {

        this.reactions = new HashMap<>();

        SBMLDocument doc = this.networkDS.getDocument();
        Model m = doc.getModel();

        ListOf reactions = m.getListOfReactions();
        for (int i = 0; i < reactions.size(); i++) {
            Reaction r = (Reaction) reactions.get(i);
            ReactionFA reaction = new ReactionFA(r.getId());

            try {
                KineticLaw law = r.getKineticLaw();
                LocalParameter objective = law.getLocalParameter("OBJECTIVE_COEFFICIENT");
                // System.out.println(objective.getValue() + " - "+ lbound + " - "+ ubound);
                reaction.setObjective(objective.getValue());
            } catch (Exception ex) {
                try {
                    reaction.setObjective(getObjectiveFBC(r.getId(), m));
                } catch (Exception ex2) {
                    reaction.setObjective(0.0);
                }
            }
            reaction.setBounds(this.networkDS.getLowerBound(r.getId()), this.networkDS.getUpperBound(r.getId()));
            ListOf spref = r.getListOfReactants();
            for (int e = 0; e < spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
                Species sp = m.getSpecies(s.getSpecies());
                reaction.addReactant(sp.getId(), sp.getName(), s.getStoichiometry());
                // System.out.println(sp.getId() + " - "+ sp.getName() + " - "+ s.getStoichiometry());
            }

            spref = r.getListOfProducts();
            for (int e = 0; e < spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
                Species sp = m.getSpecies(s.getSpecies());
                reaction.addProduct(sp.getId(), sp.getName(), s.getStoichiometry());
                // System.out.println(sp.getId() + " - "+ sp.getName() + " - "+ s.getStoichiometry());
            }

            this.reactions.put(r.getId(), reaction);
        }
    }
    /* private void createReactions() {

        this.reactions = new HashMap<>();

        SBMLDocument doc = this.networkDS.getDocument();
        Model m = doc.getModel();
        for (Reaction r: m.getListOfReactions()) {           
            ReactionFA reaction = new ReactionFA(r.getId());
            reaction.setBounds(this.networkDS.getLowerBound(r.getId()), this.networkDS.getUpperBound(r.getId()));
            try {
                KineticLaw law = r.getKineticLaw();
                LocalParameter objective = law.getLocalParameter("OBJECTIVE_COEFFICIENT");
                reaction.setObjective(objective.getValue());                
            } catch (Exception ex) {                
                try {
                    reaction.setObjective(getObjectiveFBC(r.getId(), m));
                } catch (Exception ex2) {
                    reaction.setObjective(0.0);
                }
            }

            ListOf spref = r.getListOfReactants();
            for (int e = 0; e < spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
                Species sp = m.getSpecies(s.getSpecies());
                reaction.addReactant(sp.getId(), sp.getName(), s.getStoichiometry());
                // System.out.println(sp.getId() + " - "+ sp.getName() + " - "+ s.getStoichiometry());
            }

            spref = r.getListOfProducts();
            for (int e = 0; e < spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
                Species sp = m.getSpecies(s.getSpecies());
                reaction.addProduct(sp.getId(), sp.getName(), s.getStoichiometry());
                // System.out.println(sp.getId() + " - "+ sp.getName() + " - "+ s.getStoichiometry());
            }
            //System.out.println("-----------------");
            this.reactions.put(r.getId(), reaction);
        }
    }*/

}
