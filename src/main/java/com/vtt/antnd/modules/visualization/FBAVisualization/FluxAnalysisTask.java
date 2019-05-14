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
package com.vtt.antnd.modules.visualization.FBAVisualization;

import com.vtt.antnd.data.antSimData.Ant;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.antSimData.SpeciesFA;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jumpmind.symmetric.csv.CsvReader;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
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
public class FluxAnalysisTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private final double finishedPercentage = 0.0f;
    private final File fluxesFile;
    private final Map<String, Double[]> exchange;
    private final double threshold;
    private final Map<String, Double> fluxes;
    private final Map<String, Color> color;
    private final GetInfoAndTools tools;
    private final HashMap<String, ReactionFA> reactions;
    private final HashMap<String, SpeciesFA> compounds;

    public FluxAnalysisTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        this.networkDS = dataset;
        this.fluxesFile = parameters.getParameter(FluxAnalysisParameters.fluxes).getValue();
        this.threshold = parameters.getParameter(FluxAnalysisParameters.threshold).getValue();
        this.tools = new GetInfoAndTools();
        this.exchange = this.tools.GetSourcesInfo();
        this.color = new HashMap<>();
        this.fluxes = new HashMap<>();
        this.reactions = new HashMap<>();
        this.compounds = new HashMap<>();
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

            this.readFluxes();

            System.out.println(this.fluxes.size());

            SBMLDocument doc = this.networkDS.getDocument();
            Model m = doc.getModel();

            SBMLDocument newDoc = doc.clone();
            Model newModel = newDoc.getModel();
            ListOf reactions = m.getListOfReactions();
            for (int i = 0; i < reactions.size(); i++) {
                Reaction reaction = (Reaction) reactions.get(i);
                if (!this.fluxes.containsKey(reaction.getId())) {
                    newModel.removeReaction(reaction.getId());
                }
            }

            List<Species> toBeRemoved = new ArrayList<>();
            ListOf species = newModel.getListOfSpecies();
            for (int i = 0; i < species.size(); i++) {
                Species sp = (Species) species.get(i);
                if (!this.isInReactions(newModel.getListOfReactions(), sp)) {
                    toBeRemoved.add(sp);
                }
            }

            for (Species sp : toBeRemoved) {
                newModel.removeSpecies(sp.getId());
            }

            SimpleBasicDataset dataset = new SimpleBasicDataset();

            dataset.setDocument(newDoc);
            dataset.setDatasetName("Fluxes - " + this.networkDS.getDatasetName());
            Path path = Paths.get(this.networkDS.getPath());
            Path fileName = path.getFileName();
            String name = fileName.toString();
            String p = this.networkDS.getPath().replace(name, "");
            p = p + newModel.getId();
            dataset.setPath(p);

            NDCore.getDesktop().AddNewFile(dataset);
            this.createWorld(dataset.getDocument(), fluxes);
            dataset.setGraph(this.createGraph());
            dataset.setSources(this.networkDS.getSources());

            setStatus(TaskStatus.FINISHED);

        } catch (Exception e) {
            System.out.println(e.toString());
            setStatus(TaskStatus.ERROR);
        }
    }

    private void readFluxes() {

        try {
            CsvReader reader = new CsvReader(new FileReader(this.fluxesFile.getAbsolutePath()));
            reader.readHeaders();
            while (reader.readRecord()) {
                String[] data = reader.getValues();
                if (Math.abs(Double.valueOf(data[1])) > this.threshold) {
                    fluxes.put(data[0], Double.valueOf(data[1]));
                    try {
                        color.put(data[0], getColor(data[2]));
                    } catch (NullPointerException ee) {
                    } catch (ArrayIndexOutOfBoundsException eo) {
                    }
                }
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }

    }

    private Color getColor(String color) {
        String[] bgr = color.split(",");
        try {
            return new Color(Integer.parseInt(bgr[0]), Integer.parseInt(bgr[1]), Integer.parseInt(bgr[1]));
        } catch (Exception e) {
            return Color.white;
        }
    }

    public void createWorld(SBMLDocument doc, Map<String, Double> fluxes) {
        Model m = doc.getModel();
        ListOf species = m.getListOfSpecies();
        for (int i = 0; i < species.size(); i++) {
            Species s = (Species) species.get(i);
            SpeciesFA specie = new SpeciesFA(s.getId(), s.getName());
            this.compounds.put(s.getId(), specie);
        }

        ListOf reactions = m.getListOfReactions();
        for (int i = 0; i < reactions.size(); i++) {
            Reaction r = (Reaction) reactions.get(i);
            boolean biomass = false;

            ReactionFA reaction = new ReactionFA(r.getId());
            this.networkDS.setFlux(r.getId(), fluxes.get(r.getId()));
            
            reaction.setBounds(this.networkDS.getLowerBound(r.getId()),this.networkDS.getUpperBound(r.getId()));
            ListOf spref = r.getListOfReactants();
            for (int e = 0; e < spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
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

            spref = r.getListOfProducts();
            for (int e = 0; e < spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
                Species sp = m.getSpecies(s.getSpecies());

                if (sp.getName().contains("boundary") && reaction.getlb() < 0) {
                    SpeciesFA specie = this.compounds.get(sp.getId());
                    if (specie.getAnt() == null) {
                        Ant ant = new Ant(specie.getId());
                        Double[] sb = new Double[2];
                        sb[0] = reaction.getlb();
                        sb[1] = reaction.getub();
                        ant.initAnt(Math.abs(reaction.getlb()));
                        specie.addAnt(ant);
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
            this.reactions.put(r.getId(), reaction);
        }
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

    private AntGraph createGraph() {
        AntGraph g = new AntGraph(null, null);
        for (String r : reactions.keySet()) {
            ReactionFA reaction = reactions.get(r);
            if (reaction != null) {
                AntNode reactionNode = new AntNode(reaction.getId(), String.valueOf(reaction.getFinalFlux()));
                g.addNode2(reactionNode);
                for (String reactant : reaction.getReactants()) {
                    SpeciesFA sp = compounds.get(reactant);
                    AntNode reactantNode = g.getNode(reactant);
                    if (reactantNode == null) {
                        reactantNode = new AntNode(reactant, sp.getName());
                    }
                    g.addNode2(reactantNode);
                    AntEdge e;
                    e = new AntEdge(r + " - " + uniqueId.nextId(), reactantNode, reactionNode);

                    g.addEdge(e);
                }
                for (String product : reaction.getProducts()) {
                    SpeciesFA sp = compounds.get(product);
                    AntNode reactantNode = g.getNode(product);
                    if (reactantNode == null) {
                        reactantNode = new AntNode(product, sp.getName());
                    }
                    g.addNode2(reactantNode);
                    AntEdge e;
                    e = new AntEdge(r + " - " + uniqueId.nextId(), reactionNode, reactantNode);

                    g.addEdge(e);
                }
            }
        }
        return g;
    }

    private boolean isInReactions(ListOf listOfReactions, Species sp) {
        for (int i = 0; i < listOfReactions.size(); i++) {
            Reaction r = (Reaction) listOfReactions.get(i);
            if (r.getProduct(sp.getId()) != null || r.getReactant(sp.getId()) != null) {
                return true;
            }
        }
        return false;
    }

}
