/*
 * Copyright 2007-2012 
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
package com.vtt.antnd.data.impl.datasets;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.DatasetType;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.antSimData.SpeciesFA;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.main.NDCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.Objective;

/**
 * Basic data set implementation.
 *
 * @author SCSANDRA
 */
public class SimpleBasicDataset implements Dataset {

    String datasetName, path;
    protected DatasetType type;
    StringBuffer infoDataset;
    private int ID;
    private SBMLDocument document;
    private final JTextArea textArea;
    private List<AntNode> nodes;
    private List<AntEdge> edges;
    private List<String> sources;
    private AntGraph graph;
    private String biomassId;
    private boolean isCluster = false;
    private HashMap<String, ReactionFA> reactions;
    private HashMap<String, Double> lb;
    private HashMap<String, Double> ub;
    private HashMap<String, Double> fluxes;
    private HashMap<String, Double> objective;
    private HashMap<String, SpeciesFA> compounds;
    private List<String> cofactors;
    private Map<String, Double[]> sourcesMap;
    private String selectedReaction, selectedMetabolite;
    private String parent;
    private boolean isParent;

    /**
     *
     * @param datasetName Name of the data set
     * @param path
     */
    public SimpleBasicDataset(String datasetName, String path) {
        this.datasetName = datasetName;
        this.infoDataset = new StringBuffer();
        this.path = path;
        type = DatasetType.MODELS;
        this.textArea = new JTextArea();
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.fluxes = new HashMap<>();
        this.objective = new HashMap<>();
        this.lb = new HashMap<>();
        this.ub = new HashMap<>();
    }

    public SimpleBasicDataset() {
        type = DatasetType.MODELS;
        this.infoDataset = new StringBuffer();
        this.textArea = new JTextArea();
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.fluxes = new HashMap<>();
        this.objective = new HashMap<>();
        this.lb = new HashMap<>();
        this.ub = new HashMap<>();
    }

    @Override
    public void setNodes(List<AntNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void setEdges(List<AntEdge> edges) {
        this.edges = edges;
    }

    @Override
    public List<AntNode> getNodes() {
        return this.nodes;
    }

    @Override
    public List<AntEdge> getEdges() {
        return this.edges;
    }

    @Override
    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    @Override
    public void addSource(String source) {
        if (this.sources == null) {
            this.sources = new ArrayList<>();
        }
        this.sources.add(source);
    }

    public void setBiomass(String biomassId) {
        this.biomassId = biomassId;
    }

    @Override
    public List<String> getSources() {
        return this.sources;
    }

    public String getBiomassId() {
        return this.biomassId;
    }

    @Override
    public void setGraph(AntGraph graph) {
        this.graph = graph;
    }

    private void addNodeAndEdge(boolean reactant, SpeciesReference spr, AntNode reactionNode, AntGraph g, int direction) {
        Species sp = spr.getSpeciesInstance();

        if (this.cofactors.contains(sp.getId())) {
            AntNode cof = new AntNode(sp.getId(), sp.getName() + " - " + uniqueId.nextId());
            g.addNode(cof);
            if (reactant) {
                g.addEdge(addEdge(cof, reactionNode, sp.getId(), direction));
            } else {
                g.addEdge(addEdge(reactionNode, cof, sp.getId(), direction));
            }
        } else {
            AntNode spNode = g.getNode(sp.getId());
            if (spNode == null) {
                spNode = new AntNode(sp.getId(), sp.getName());
            }
            this.setPosition(spNode);
            g.addNode2(spNode);
            if (reactant) {
                g.addEdge(addEdge(spNode, reactionNode, sp.getId(), direction));
            } else {
                g.addEdge(addEdge(reactionNode, spNode, sp.getId(), direction));
            }
        }

    }

    @Override
    public AntGraph getGraph() {

        Model model = this.document.getModel();
        this.cofactors = NDCore.getCofactors();
        List<AntNode> nodeList = new ArrayList<>();
        List<AntEdge> edgeList = new ArrayList<>();

        AntGraph g = new AntGraph(nodeList, edgeList);

        // For eac reaction in the model, a Node is created in the graph
        model.getListOfReactions().stream().filter((r) -> !(onlyCofactors(r))).forEachOrdered((r) -> {
            //Node is created
            AntNode reactionNode = new AntNode(r.getId(), r.getName());
            this.setPosition(reactionNode);

            g.addNode2(reactionNode);
            int direction = this.getDirection(r);

            r.getListOfReactants().forEach((spr) -> {
                addNodeAndEdge(true, spr, reactionNode, g, direction);
            });
            r.getListOfProducts().forEach((spr) -> {
                addNodeAndEdge(false, spr, reactionNode, g, direction);
            });
        });

        this.graph = g;
        System.out.println(g.getNumberOfNodes());
        return g;
    }

    private void setPosition(AntNode node) {
        if (this.graph != null) {
            AntNode gNode = this.graph.getNode(node.getId());
            if (gNode != null) {
                node.setPosition(gNode.getPosition());
            } else {
                System.out.println(node.getId());
            }
        }
    }

    private int getDirection(Reaction r) {
        int direction = 2;
        double lb, ub;
        Double flux = null;
        if (this.fluxes.containsKey(r.getId())) {
            flux = this.fluxes.get(r.getId());
        }
        lb = this.getLowerBound(r.getId());
        ub = this.getUpperBound(r.getId());

        if (flux != null) {
            //  System.out.println("flux not null?");
            if (flux > 0.0) {
                direction = 1;
            } else if (flux < 0.0) {
                direction = 0;
            }
        } else {
            // System.out.println(ub+lb);
            if (ub > 0 && lb < 0) {
                direction = 2;
            } else if (ub > 0) {
                direction = 1;
            } else {
                direction = 0;
            }
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

    @Override
    public void setID(int ID) {
        this.ID = ID;
    }

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public String getDatasetName() {
        return this.datasetName;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public void setFlux(String reaction, Double flux) {
        this.fluxes.put(reaction, flux);
    }

    @Override
    public Double getFlux(String reaction) {
        return this.fluxes.get(reaction);
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    @Override
    public DatasetType getType() {
        return type;
    }

    @Override
    public void setType(DatasetType type) {
        this.type = type;
    }

    @Override
    public JTextArea getInfo() {
        return this.textArea;
    }

    @Override
    public void addInfo(String info) {
        this.infoDataset.append(info).append("\n");
        this.textArea.setText(infoDataset.toString());
    }

    @Override
    public SimpleBasicDataset clone() {
        SimpleBasicDataset newDataset = new SimpleBasicDataset(this.datasetName, this.path);
        newDataset.setType(this.type);
        newDataset.setDocument(this.getDocument());
        newDataset.setGraph(this.graph);
        return newDataset;
    }

    @Override
    public SBMLDocument getDocument() {
        return this.document;
    }

    @Override
    public void setDocument(SBMLDocument document) {
        this.document = document;
        Model model = this.document.getModel();
        for (Reaction r : model.getListOfReactions()) {
            if (r.getKineticLaw() != null) {
                LocalParameter lpobj = r.getKineticLaw().getLocalParameter("OBJECTIVE_COEFFICIENT");
                if (lpobj != null) {
                    this.objective.put(r.getId(), lpobj.getValue());
                } else {
                    this.objective.put(r.getId(), this.getObjectiveFBC(r.getId(), model));
                }
                LocalParameter lp = r.getKineticLaw().getLocalParameter("LOWER_BOUND");
                if (lp == null) {
                    lp = r.getKineticLaw().getLocalParameter("LB_" + r.getId());
                }
                this.setLowerBound(r.getId(), lp.getValue());

                lp = r.getKineticLaw().getLocalParameter("UPPER_BOUND");
                if (lp == null) {
                    lp = r.getKineticLaw().getLocalParameter("UB_" + r.getId());
                }
                this.setUpperBound(r.getId(), lp.getValue());
            } else {
                this.objective.put(r.getId(), this.getObjectiveFBC(r.getId(), model));
                FBCReactionPlugin plugin = (FBCReactionPlugin) r.getPlugin("fbc");
                Parameter lp = plugin.getLowerFluxBoundInstance();
                this.setLowerBound(r.getId(), lp.getValue());

                plugin = (FBCReactionPlugin) r.getPlugin("fbc");
                lp = plugin.getUpperFluxBoundInstance();
                this.setUpperBound(r.getId(), lp.getValue());
            }

            this.setFlux(r.getId(), 0.0);
        }
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

    @Override
    public void setInfo(String info) {
        this.infoDataset.delete(0, this.infoDataset.length());
        this.infoDataset.append(info);
    }

    @Override
    public void SetCluster(boolean isCluster) {
        this.isCluster = isCluster;
    }

    @Override
    public boolean isCluster() {
        return this.isCluster;
    }

    @Override
    public void setPaths(Map<String, SpeciesFA> paths) {
        this.compounds = (HashMap<String, SpeciesFA>) paths;
    }

    @Override
    public HashMap<String, SpeciesFA> getPaths() {
        return this.compounds;
    }

    @Override
    public void setReactionsFA(Map<String, ReactionFA> reactions) {
        this.reactions = (HashMap<String, ReactionFA>) reactions;
    }

    @Override
    public HashMap<String, ReactionFA> getReactionsFA() {
        return this.reactions;
    }

    @Override
    public void setSourcesMap(Map<String, Double[]> sources) {
        this.sourcesMap = sources;

    }

    @Override
    public Map<String, Double[]> getSourcesMap() {
        return this.sourcesMap;
    }

    @Override
    public void setCofactors(List<String> cofactor) {
        this.cofactors = cofactor;
    }

    @Override
    public List<String> getCofactors() {
        return this.cofactors;
    }

    @Override
    public boolean isReactionSelected(Reaction reaction) {
        if (this.selectedReaction.equals(reaction.getId())) {
            return true;
        }
        return false;
    }

    @Override
    public void setReactionSelectionMode(String reaction) {
        this.selectedReaction = reaction;
    }

    @Override
    public boolean isMetaboliteSelected(Species metabolite) {
        if (this.selectedMetabolite.equals(metabolite.getId())) {
            return true;
        }
        return false;
    }

    @Override
    public void setMetaboliteSelectionMode(String metabolite) {
        this.selectedMetabolite = metabolite;
    }

    @Override
    public String getParent() {
        return this.parent;
    }

    @Override
    public void setParent(String dataset) {
        this.parent = dataset;
    }

    @Override
    public boolean isParent() {
        return this.isParent;
    }

    @Override
    public void setIsParent(boolean isParent) {
        this.isParent = isParent;
    }

    private boolean onlyCofactors(Reaction r) {
        for (SpeciesReference rf : r.getListOfReactants()) {
            if (!this.cofactors.contains(rf.getSpecies())) {
                return Boolean.FALSE;
            }
        }
        for (SpeciesReference rf : r.getListOfProducts()) {
            if (!this.cofactors.contains(rf.getSpecies())) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public Double getLowerBound(String reaction) {
        return lb.get(reaction);
    }

    @Override
    public Double getUpperBound(String reaction) {
        return ub.get(reaction);
    }

    @Override
    public void setLowerBound(String reaction, Double bound) {
        if (this.lb == null) {
            this.lb = new HashMap<>();
        }
        this.lb.put(reaction, bound);
    }

    @Override
    public void setUpperBound(String reaction, Double bound) {
        if (this.ub == null) {
            this.ub = new HashMap<>();
        }
        this.ub.put(reaction, bound);

    }

    @Override
    public void setObjective(String reaction, Double obj) {
        if (this.objective == null) {
            this.objective = new HashMap<>();
        }
        this.objective.put(reaction, obj);
    }

    @Override
    public Double getObjective(String reaction) {
        return this.objective.get(reaction);
    }

}
