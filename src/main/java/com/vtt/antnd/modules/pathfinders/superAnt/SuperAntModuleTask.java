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
package com.vtt.antnd.modules.pathfinders.superAnt;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.antSimData.Ant;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.desktop.impl.PrintPaths2;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.configuration.cofactors.CofactorConfParameters;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JInternalFrame;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class SuperAntModuleTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private double finishedPercentage = 0.0f;
    private final String biomassID;
    private final HashMap<String, ReactionFA> reactions;
    private final HashMap<String, SpeciesFA> compounds;
    private final Map<String, Double[]> sources;
    private final List<String> sourcesList, cofactors;
    private final JInternalFrame frame;
    private final int iterations;
    private final GetInfoAndTools tools;

    public SuperAntModuleTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        this.networkDS = dataset;
        this.biomassID = parameters.getParameter(SuperAntModuleParameters.objectiveReaction).getValue();
        this.iterations = parameters.getParameter(SuperAntModuleParameters.numberOfIterations).getValue();
        this.sourcesList = new ArrayList<>();
        this.sources = new HashMap<>();
        this.cofactors = new ArrayList<>();
        CofactorConfParameters conf = new CofactorConfParameters();
        String excluded = conf.getParameter(CofactorConfParameters.cofactors).getValue().replaceAll("[\\s|\\u00A0]+", "");
        String[] excludedCompounds = excluded.split(",");
        this.cofactors.addAll(Arrays.asList(excludedCompounds));
        this.reactions = new HashMap<>();
        this.compounds = new HashMap<>();
        this.frame = new JInternalFrame("Result", true, true, true, true);
        this.tools = new GetInfoAndTools();
    }

    @Override
    public String getTaskDescription() {
        return "Starting Ant Simulation... ";
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

            System.out.println("Creating world");
            this.createWorld();
            System.out.println("Starting simulation");
            this.runIterations();
            System.out.println("Visualizing result");
            this.createResult();
            setStatus(TaskStatus.FINISHED);

        } catch (Exception e) {
            System.out.println(e.toString());
            NDCore.getDesktop().displayMessage("No path was found.");
            setStatus(TaskStatus.FINISHED);
        }
    }

    private void createWorld() {
        SBMLDocument doc = this.networkDS.getDocument();
        Model m = doc.getModel();
        this.createListOfCompounds(m);
        this.createListOfReactions(m);
        this.removeCompoundsWithNoReactions();
    }

    private void createListOfCompounds(Model m) {
        m.getListOfSpecies().forEach((s) -> {
            SpeciesFA specie = new SpeciesFA(s.getId(), s.getName());
            this.compounds.put(s.getId(), specie);
        });
        System.out.println("Number of compounds: " + this.compounds.size());
    }

    private void createListOfReactions(Model m) {
        m.getListOfReactions().forEach((r) -> {
            ReactionFA antReaction = new ReactionFA(r.getId());
            antReaction.setBounds(this.networkDS.getLowerBound(r.getId()), this.networkDS.getUpperBound(r.getId()));

            r.getListOfReactants().forEach((s) -> {
                Species sp = s.getModel().getSpecies(s.getSpecies());
                antReaction.addReactant(sp.getId(), sp.getName(), s.getStoichiometry());
                SpeciesFA antSp = this.compounds.get(sp.getId());
                antSp.addReaction(r.getId());
                setInitialPool(r, antSp, antReaction, sp);
            });

            r.getListOfProducts().forEach((s) -> {
                Species sp = s.getModel().getSpecies(s.getSpecies());
                antReaction.addProduct(sp.getId(), sp.getName(), s.getStoichiometry());
                SpeciesFA antSp = this.compounds.get(sp.getId());
                antSp.addReaction(r.getId());
            });

            this.reactions.put(r.getId(), antReaction);
        });
        System.out.println("Number of reactions: " + this.reactions.size());
        System.out.println("Number of sources: " + this.sourcesList.size());

    }

    private void removeCompoundsWithNoReactions() {
        List<String> toBeRemoved = new ArrayList<>();
        for (String compound : compounds.keySet()) {
            if (compounds.get(compound).getReactions().isEmpty()) {
                toBeRemoved.add(compound);
            }
        }

        toBeRemoved.forEach((compound) -> {
            this.compounds.remove(compound);
        });
    }

    private void setInitialPool(Reaction r, SpeciesFA antSp, ReactionFA antReaction, Species sp) {
        if (r.getListOfProducts().isEmpty() || r.getListOfProducts().get(0).getSpeciesInstance().isBoundaryCondition()) {
            if (antSp.getAnt() == null) {
                if (antReaction.getlb() < 0.0) {
                    this.setPool(sp, antReaction, antSp);
                }
            }
        }
    }

    private void setPool(Species sp, ReactionFA reaction, SpeciesFA antsp) {
        Ant ant = new Ant(antsp.getId());
        Double[] sb = new Double[2];
        sb[0] = reaction.getlb();
        sb[1] = reaction.getub();

        ant.initAnt(Math.abs(reaction.getlb()));
        antsp.addAnt(ant);
        this.sourcesList.add(sp.getId());
        this.sources.put(sp.getId(), sb);
    }

    private void runIterations() {
        for (int i = 0; i < this.iterations; i++) {
            this.cicle();
            
            finishedPercentage = (double) i / this.iterations;
            if (getStatus() == TaskStatus.CANCELED || getStatus() == TaskStatus.ERROR) {
                break;
            }
        }
    }
  
    
    public void cicle() {
        for (String compound : compounds.keySet()) {
            if (this.compounds.get(compound).getAnt() == null) {
                continue;
            }
            List<String> possibleReactions = getPossibleReactions(compound);

            for (String reactionChoosen : possibleReactions) {

                ReactionFA rc = this.reactions.get(reactionChoosen);
                Boolean direction = true;
                List<String> toBeAdded, toBeRemoved;
                if (rc.hasReactant(compound)) {
                    toBeAdded = rc.getProducts();
                    toBeRemoved = rc.getReactants();
                } else {
                    toBeAdded = rc.getReactants();
                    toBeRemoved = rc.getProducts();
                    direction = false;
                }

                // get the ants that must be removed from the reactants ..
                // creates a superAnt with all the paths until this reaction joined..
                List<Ant> com = new ArrayList<>();
                for (String s : toBeRemoved) {
                    SpeciesFA spfa = this.compounds.get(s);
                    if (spfa.getAnt() != null) {
                        com.add(spfa.getAnt());
                    }

                }

                Ant superAnt = new Ant(null);

                superAnt.joinGraphs(reactionChoosen, direction, com, rc);

                for (String s : toBeAdded) {
                    SpeciesFA spfa = this.compounds.get(s);
                    Ant newAnt = superAnt.clone();
                    if (!hasOutput(newAnt, spfa)) {
                        newAnt.setLocation(compound);
                        spfa.addAnt(newAnt);
                    }
                }

            }
        }
    }

    private void createResult() {
        if (getStatus() == TaskStatus.PROCESSING) {
            Ant ant = this.compounds.get(this.biomassID).getAnt();
            AntGraph g = this.createGraph(ant.getPath());
            Dataset newDataset = this.tools.createDataFile(g, networkDS, biomassID, sourcesList, false, false);
            PrintPaths2 print = new PrintPaths2(newDataset);
            try {
                frame.add(print.printPathwayInFrame(g));
                NDCore.getDesktop().addInternalFrame(frame);
                frame.pack();
            } catch (NullPointerException ex) {
                System.out.println(ex.toString());
                NDCore.getDesktop().displayMessage("No path was found.");
            }
        }

    }

    private boolean hasOutput(Ant ant, SpeciesFA sp) {
        boolean hasOutput = false;
        for (String r : ant.getPath().keySet()) {
            if (reactions.containsKey(r)) {
                ReactionFA reaction = this.reactions.get(r);
                if ((ant.getPath().get(r) && reaction.hasReactant(sp.getId())) || (!ant.getPath().get(r) && reaction.hasProduct(sp.getId()))) {
                    hasOutput = true;
                }
            }

        }
        return hasOutput;
    }

    private List<String> getPossibleReactions(String compound) {

        List<String> possibleReactions = new ArrayList<>();
        SpeciesFA sp = this.compounds.get(compound);        

        List<String> connectedReactions = sp.getReactions();
        for (String reaction : connectedReactions) {

            ReactionFA r = this.reactions.get(reaction);
            if (r == null) {
                continue;
            }
            boolean isPossible = true;

            if (r.getlb() == 0 && r.getub() == 0) {
                isPossible = false;
            }

            if (r.hasReactant(compound)) {

                if (r.getub() > 0) {
                    List<String> reactants = r.getReactants();
                    boolean all = true;
                    for (String reactant : reactants) {

                        if (!allEnoughAnts(reactant, reaction)) {
                            isPossible = false;
                            break;
                        }

                        if (!cofactors.contains(reactant)) {
                            all = false;
                        }
                    }
                    if (all) {
                        isPossible = false;
                    }
                } else {
                    isPossible = false;
                }

            } else {
                if (r.getlb() < 0) {
                    List<String> products = r.getProducts();
                    boolean all = true;
                    for (String product : products) {
                        if (!allEnoughAnts(product, reaction)) {
                            isPossible = false;
                            break;
                        }

                        if (!cofactors.contains(product)) {
                            all = false;
                        }
                    }
                    if (all) {
                        isPossible = false;
                    }
                } else {
                    isPossible = false;
                }

            }

            if (isPossible) {
                possibleReactions.add(reaction);
            }

        }
        return possibleReactions;
    }

    private boolean allEnoughAnts(String species, String reaction) {
        SpeciesFA s = this.compounds.get(species);
        Ant ant = s.getAnt();
        if (ant != null) {
            return !ant.contains(reaction);
        } else if (cofactors.contains(species)) {
            return true;
        }
        return false;
    }

    private AntGraph createGraph(Map<String, Boolean> path) {
        AntGraph g = new AntGraph(null, null);
        for (String r : path.keySet()) {
            ReactionFA reaction = reactions.get(r);
            if (reaction != null) {
                AntNode reactionNode = new AntNode(reaction.getId(), String.valueOf(reaction.getFlux()));
                g.addNode2(reactionNode);
                int direction = this.getDirection(reaction.getId());
                for (String reactant : reaction.getReactants()) {
                    SpeciesFA sp = compounds.get(reactant);

                    if (this.cofactors.contains(sp.getId())) {
                        AntNode cof = new AntNode(sp.getId(), sp.getName() + " - " + uniqueId.nextId());
                        g.addNode(cof);
                        g.addEdge(addEdge(cof, reactionNode, sp.getId(), direction));
                    } else {
                        AntNode spNode = g.getNode(sp.getId());
                        if (spNode == null) {
                            spNode = new AntNode(sp.getId(), sp.getName());
                        }
                        if (sp.getId().equals(this.biomassID)) {
                            spNode.setColor(Color.red);
                        }
                        if (this.sourcesList.contains(sp.getId())) {
                            spNode.setColor(Color.blue);
                        }
                        g.addNode2(spNode);
                        g.addEdge(addEdge(spNode, reactionNode, sp.getId(), direction));
                    }

                }

                for (String product : reaction.getProducts()) {
                    SpeciesFA sp = compounds.get(product);

                    if (this.cofactors.contains(sp.getId())) {
                        AntNode cof = new AntNode(sp.getId(), sp.getName() + " - " + uniqueId.nextId());
                        g.addNode(cof);
                        g.addEdge(addEdge(reactionNode, cof, sp.getId(), direction));
                    } else {
                        AntNode spNode = g.getNode(sp.getId());
                        if (spNode == null) {
                            spNode = new AntNode(sp.getId(), sp.getName());
                        }

                        if (sp.getId().equals(this.biomassID)) {
                            spNode.setColor(Color.red);
                        }
                        if (this.sourcesList.contains(sp.getId())) {
                            spNode.setColor(Color.PINK);
                        }
                        g.addNode2(spNode);
                        g.addEdge(addEdge(reactionNode, spNode, sp.getId(), direction));
                    }
                }
            }
        }
        return g;
    }

    private int getDirection(String r) {
        int direction = 2;
        double lb, ub;
        lb = this.networkDS.getLowerBound(r);
        ub = this.networkDS.getUpperBound(r);

        // System.out.println(ub+lb);
        if (ub > 0 && lb < 0) {
            direction = 2;
        } else if (ub > 0) {
            direction = 1;
        } else {
            direction = 0;
        }

        //System.out.println(direction);
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
