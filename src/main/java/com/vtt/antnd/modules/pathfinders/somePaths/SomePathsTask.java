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
package com.vtt.antnd.modules.pathfinders.somePaths;

import com.vtt.antnd.data.antSimData.Ant;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.desktop.impl.PrintPaths;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.configuration.cofactors.CofactorConfParameters;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;

/**
 *
 * @author scsandra
 */
public class SomePathsTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private double finishedPercentage = 0.0f;
    private final String biomassID;
    private final HashMap<String, ReactionFA> reactions;
    private final HashMap<String, SpeciesFA> compounds;
    private final HashMap<String, String[]> bounds;
    private final Map<String, Double[]> sources;
    private final List<String> sourcesList, cofactors;
    private final JInternalFrame frame;
    private final JScrollPane panel;
    private final JPanel pn;
    private final int iterations;
    private final GetInfoAndTools tools;

    public SomePathsTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        this.networkDS = dataset;
        this.biomassID = parameters.getParameter(SomePathsParameters.objectiveReaction).getValue();
        this.iterations = parameters.getParameter(SomePathsParameters.numberOfIterations).getValue();

        this.sourcesList = new ArrayList<>();

        this.sources = new HashMap<>();
        this.cofactors = new ArrayList<>();
        CofactorConfParameters conf = new CofactorConfParameters();
        String excluded = conf.getParameter(CofactorConfParameters.cofactors).getValue().replaceAll("[\\s|\\u00A0]+", "");
        System.out.println(excluded);
        String[] excludedCompounds = excluded.split(",");
        this.cofactors.addAll(Arrays.asList(excludedCompounds));

        this.reactions = new HashMap<>();
        this.compounds = new HashMap<>();
        this.bounds = new HashMap<>();

        this.frame = new JInternalFrame("Result", true, true, true, true);
        this.pn = new JPanel();
        this.panel = new JScrollPane(pn);
        this.tools = new GetInfoAndTools();

    }

    @Override
    public String getTaskDescription() {
        return "Starting Getting all paths... ";
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
            frame.setSize(new Dimension(700, 500));
            frame.add(this.panel);
            NDCore.getDesktop().addInternalFrame(frame);

            for (int i = 0; i < this.iterations; i++) {
                this.cicle();
                finishedPercentage = (double) i / this.iterations;
                if (getStatus() == TaskStatus.CANCELED || getStatus() == TaskStatus.ERROR) {
                    break;
                }
            }
            if (getStatus() == TaskStatus.PROCESSING) {
                Ant ant = this.compounds.get(this.biomassID).getAnt();
                System.out.println("location: " + ant.getLocation());
                AntGraph g = this.createGraph(ant.getPath());
                this.tools.createDataFile(g, networkDS, biomassID, sourcesList, false, false);
                PrintPaths print = new PrintPaths(this.tools.getModel());
                try {
                    this.pn.add(print.printPathwayInFrame(g));
                } catch (NullPointerException ex) {
                    System.out.println(ex.toString());
                }
                if (ant == null) {
                    NDCore.getDesktop().displayMessage("No path was found.");
                }
            }

            setStatus(TaskStatus.FINISHED);

        } catch (Exception e) {
            System.out.println(e.toString());
            setStatus(TaskStatus.ERROR);
        }
    }

    private void createWorld() {
        SBMLDocument doc = this.networkDS.getDocument();
        Model m = doc.getModel();       
        for (Species s: m.getListOfSpecies()) {          
            SpeciesFA specie = new SpeciesFA(s.getId(), s.getName());
            this.compounds.put(s.getId(), specie);
        }

      
        for (Reaction r: m.getListOfReactions()) {         
            boolean biomass = false;
            //System.out.println(r.getId());
            ReactionFA reaction = new ReactionFA(r.getId());
            String[] b = this.bounds.get(r.getId());
            if (b != null) {
                reaction.setBounds(Double.valueOf(b[3]), Double.valueOf(b[4]));
            } else {
                try {                   
                    reaction.setBounds(this.networkDS.getLowerBound(r.getId()), this.networkDS.getUpperBound(r.getId()));
                } catch (Exception ex) {
                    try {
                        FBCReactionPlugin plugin = (FBCReactionPlugin) r.getPlugin("fbc");
                        Parameter lp = plugin.getLowerFluxBoundInstance();
                        Parameter up = plugin.getUpperFluxBoundInstance();
                        reaction.setBounds(lp.getValue(), up.getValue());
                    } catch (Exception ex2) {
                        reaction.setBounds(-1000, 1000);
                    }
                }
            }
          
            for (SpeciesReference s: r.getListOfReactants()) {               
                Species sp = m.getSpecies(s.getSpecies());

                reaction.addReactant(sp.getId(), sp.getName(), s.getStoichiometry());
                SpeciesFA spFA = this.compounds.get(sp.getId());
                if (biomass) {
                    spFA.setPool(Math.abs(s.getStoichiometry()));
                }
                if (spFA != null) {
                    spFA.addReaction(r.getId());
                } else {
                    System.out.println(sp.getId());
                }
            }

           
            for (SpeciesReference s : r.getListOfProducts()) {
                Species sp = m.getSpecies(s.getSpecies());
                //System.out.println(sp.getName());
                if (sp.getBoundaryCondition() && reaction.getlb() < 0) {
                    // System.out.println(reaction.getId()+"-"+reaction.getlb());
                    SpeciesFA specie = this.compounds.get(sp.getId());
                    if (specie.getAnt() == null) {
                        Ant ant = new Ant(specie.getId());
                        Double[] sb = new Double[2];
                        sb[0] = reaction.getlb();
                        sb[1] = reaction.getub();
                        ant.initAnt(Math.abs(reaction.getlb()));
                        specie.addAnt(ant);
                        this.sourcesList.add(sp.getId());
                        this.sources.put(sp.getId(), sb);
                    }
                }

                reaction.addProduct(sp.getId(), sp.getName(), s.getStoichiometry());
                SpeciesFA spFA = this.compounds.get(sp.getId());
                if (spFA != null) {
                    spFA.addReaction(r.getId());
                } else {
                    System.out.println(sp.getId());
                }
            }

            if (r.getNumProducts() == 0) {            
                for (SpeciesReference s: r.getListOfReactants()) {                
                    Species sp = m.getSpecies(s.getSpecies());
                    SpeciesFA specie = this.compounds.get(sp.getId());
                    if (specie.getAnt() == null) {
                        Ant ant = new Ant(specie.getId());
                        Double[] sb = new Double[2];
                        sb[0] = reaction.getlb();
                        sb[1] = reaction.getub();
                        if (reaction.getlb() < 0.0) {
                            ant.initAnt(Math.abs(reaction.getlb()));
                            specie.addAnt(ant);
                            this.sourcesList.add(sp.getId());
                            this.sources.put(sp.getId(), sb);
                        }
                    }
                }
            }
            this.reactions.put(r.getId(), reaction);
        }
        //System.out.println(this.reactions.size());
        List<String> toBeRemoved = new ArrayList<>();
        for (String compound : compounds.keySet()) {
            if (compounds.get(compound).getReactions().isEmpty()) {
                toBeRemoved.add(compound);
            }
        }
        for (String compound : toBeRemoved) {
            this.compounds.remove(compound);
        }

    }

    public void cicle() {
        // System.out.println("---------------------------");
        for (String compound : compounds.keySet()) {
            if (this.compounds.get(compound).getAnt() == null) {
                continue;
            }
            //System.out.println(this.compounds.get(compound).getName());
            List<String> possibleReactions = getPossibleReactions(compound);
            //System.out.println(compound + "- " +possibleReactions.size());
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
                //  List<List<Ant>> paths = new ArrayList<>();
                List<Ant> com = new ArrayList<>();
                for (String s : toBeRemoved) {
                    SpeciesFA spfa = this.compounds.get(s);
                    if (spfa.getAnt() != null/* && !this.cofactors.contains(s)*/) {
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

    private boolean hasOutput(Ant ant, SpeciesFA sp) {
        boolean hasOutput = false;
        for (String r : ant.getPath().keySet()) {
            if (reactions.containsKey(r)) {
                ReactionFA reaction = this.reactions.get(r);
                if ((ant.getPath().get(r) && reaction.hasReactant(sp.getId())) || (!ant.getPath().get(r) && reaction.hasProduct(sp.getId()))) {
                    //if (!reaction.isBidirecctional()) {
                    hasOutput = true;
                    //}

                }
            }

        }
        return hasOutput;
    }

    private List<String> getPossibleReactions(String compound) {

        List<String> possibleReactions = new ArrayList<>();
        SpeciesFA sp = this.compounds.get(compound);
        Ant ant = sp.getAnt();
        /* if (!this.sourceID.equals(compound) && ant == null) {
         return possibleReactions;
         }*/

        List<String> connectedReactions = sp.getReactions();
        //System.out.println(compound + "- "+connectedReactions.size());
        for (String reaction : connectedReactions) {

            //Get reaction direction and then check if there is and Ant in the compound. Problem with cycles..
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
            //return true;
        } else if (cofactors.contains(species)) {
            //this.objectives.add(species);           
            return true;
        }
        return false;
    }

    private AntGraph createGraph(Map<String, Boolean> path) {
        AntGraph g = new AntGraph(null, null);
        AntGraph previousG = this.networkDS.getGraph();

        for (String r : path.keySet()) {
            System.out.println(r);
            ReactionFA reaction = reactions.get(r);
            if (reaction != null) {
                AntNode reactionNode = new AntNode(reaction.getId(), String.valueOf(reaction.getFlux()));
                // if (previousG != null) {
                //     reactionNode.setPosition(previousG.getNode(reaction.getId()).getPosition());
                // }

                g.addNode2(reactionNode);

                for (String reactant : reaction.getReactants()) {
                    SpeciesFA sp = compounds.get(reactant);

                    AntNode reactantNode = g.getNode(reactant);
                    if (reactantNode == null) {
                        reactantNode = new AntNode(reactant, sp.getName());
                    }

                    // if (previousG != null) {
                    //    reactantNode.setPosition(previousG.getNode(reactant).getPosition());
                    //}
                    g.addNode2(reactantNode);
                    if (sp.getId().equals(this.biomassID)) {
                        reactantNode.setColor(Color.red);
                    }

                    if (this.sourcesList.contains(sp.getId())) {
                        reactantNode.setColor(Color.MAGENTA);
                    }

                    AntEdge e;
                    if (path.get(r)) {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactantNode, reactionNode);
                    } else {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactionNode, reactantNode);
                    }
                    g.addEdge(e);
                }

                for (String product : reaction.getProducts()) {
                    SpeciesFA sp = compounds.get(product);
                    AntNode reactantNode = g.getNode(product);
                    if (reactantNode == null) {
                        reactantNode = new AntNode(product, sp.getName());
                    }
                    // if (previousG != null) {
                    //    reactantNode.setPosition(previousG.getNode(product).getPosition());
                    // }
                    g.addNode2(reactantNode);
                    if (sp.getId().equals(this.biomassID)) {
                        reactantNode.setColor(Color.red);
                    }

                    if (this.sourcesList.contains(sp.getId())) {
                        reactantNode.setColor(Color.PINK);
                    }
                    AntEdge e;
                    if (path.get(r)) {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactionNode, reactantNode);
                    } else {
                        e = new AntEdge(r + " - " + uniqueId.nextId(), reactantNode, reactionNode);
                    }
                    g.addEdge(e);
                }
            }
        }
        return g;
    }
}
