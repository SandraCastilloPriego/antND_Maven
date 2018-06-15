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
import com.vtt.antnd.data.network.Edge;
import com.vtt.antnd.data.network.Graph;
import com.vtt.antnd.data.network.Node;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.main.NDCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import org.sbml.libsbml.KineticLaw;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Parameter;
import org.sbml.libsbml.Reaction;
import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.SpeciesReference;

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
    private List<Node> nodes;
    private List<Edge> edges;
    private List<String> sources;
    private Graph graph;
    private String biomassId;
    private boolean isCluster = false;
    private HashMap<String, ReactionFA> reactions;
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
    }

    public SimpleBasicDataset() {
        type = DatasetType.MODELS;
        this.infoDataset = new StringBuffer();
        this.textArea = new JTextArea();
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    @Override
    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    @Override
    public List<Node> getNodes() {
        return this.nodes;
    }

    @Override
    public List<Edge> getEdges() {
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
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Graph getGraph() {
           
        Model model = this.document.getModel();
        this.cofactors = NDCore.getCofactors();
        List<Node> nodeList = new ArrayList<>();
        List<Edge> edgeList = new ArrayList<>();

        Graph g = new Graph(nodeList, edgeList);

       
        try {
            // For eac reaction in the model, a Node is created in the graph
            
            ListOf listOfReactions = model.getListOfReactions(); 
            //if (listOfReactions.size() > 1000) return null;
            for (int i = 0; i < model.getNumReactions(); i++) {
                Reaction r = (Reaction) listOfReactions.get(i);
                
                //Node is created
                Node reactionNode = new Node(r.getId(), r.getName());
                this.setPosition(reactionNode);
               
                g.addNode2(reactionNode);
                int direction = this.getDirection(r);
                // read bounds to know the direction of the edges
                ListOf listOfReactants = r.getListOfReactants();
                for (int e = 0; e < r.getNumReactants(); e++) {
                    SpeciesReference spr = (SpeciesReference) listOfReactants.get(e);               
                    Species sp = model.getSpecies(spr.getSpecies());

                    if (this.cofactors.contains(sp.getId())) {                        
                        Node cof = new Node(sp.getId(), sp.getName()+ " - " + uniqueId.nextId());
                        g.addNode(cof);
                        g.addEdge(addEdge(cof, reactionNode, sp.getId(), direction));
                    } else {
                        Node spNode = g.getNode(sp.getId());
                        if (spNode == null) {
                            spNode = new Node(sp.getId(), sp.getName());
                        }
                        this.setPosition(spNode);
                        g.addNode2(spNode);
                        g.addEdge(addEdge(spNode, reactionNode, sp.getId(), direction));
                    }
                }

                ListOf listOfProducts = r.getListOfProducts();
                for (int e = 0; e < r.getNumProducts(); e++) {
                    SpeciesReference spr = (SpeciesReference) listOfProducts.get(e);               
                    Species sp = model.getSpecies(spr.getSpecies());

                    if (this.cofactors.contains(sp.getId())) {
                        Node cof = new Node(sp.getId(), sp.getName()+ " - " + uniqueId.nextId());
                        g.addNode(cof);
                        g.addEdge(addEdge(reactionNode, cof, sp.getId(), direction));
                    } else {
                        Node spNode = g.getNode(sp.getId());
                        if (spNode == null) {
                            spNode = new Node(sp.getId(), sp.getName());
                        }
                        this.setPosition(spNode);
                        g.addNode2(spNode);
                        g.addEdge(addEdge(reactionNode, spNode, sp.getId(), direction));
                    }
                }

            }
        } catch (Exception e) {         
            System.out.println(e.toString());
        }
        this.graph = g;
        System.out.println(g.getNumberOfNodes());
        return g;
    }

    private void setPosition(Node node) {
        if (this.graph != null) {
            Node gNode = this.graph.getNode(node.getId());
            if (gNode != null) {
                node.setPosition(gNode.getPosition());
            }else{
                System.out.println(node.getId());
            }
        }
    }

    private int getDirection(Reaction r) {
        int direction = 2;
        double lb = Double.NEGATIVE_INFINITY;
        double ub = Double.POSITIVE_INFINITY;
        Double flux = null;
        if (r.getKineticLaw() != null) {
            KineticLaw law = r.getKineticLaw();
            Parameter lbound = law.getParameter("LOWER_BOUND");
            lb = lbound.getValue();
            Parameter ubound = law.getParameter("UPPER_BOUND");
            ub = ubound.getValue();
            try{
            Parameter rflux = law.getParameter("FLUX_VALUE");            
            flux = rflux.getValue();         
            }catch(Exception e ){
                
            }
        }
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

    private Edge addEdge(Node node1, Node node2, String name, int direction) {
        Edge e = null;
        switch (direction) {
            case 1:
                e = new Edge(name+ " - " + uniqueId.nextId(), node1, node2);
                e.setDirection(true);
                break;
            case 2:
                e = new Edge(name+ " - " + uniqueId.nextId(), node1, node2);
                e.setDirection(false);
                break;
            case 0:
                e = new Edge(name+ " - " + uniqueId.nextId(), node2, node1);
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
}
