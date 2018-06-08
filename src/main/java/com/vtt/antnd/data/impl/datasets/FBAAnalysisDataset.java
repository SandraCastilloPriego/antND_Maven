/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vtt.antnd.data.impl.datasets;


import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.DatasetType;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.antSimData.SpeciesFA;
import com.vtt.antnd.data.network.Edge;
import com.vtt.antnd.data.network.Graph;
import com.vtt.antnd.data.network.Node;
import com.vtt.antnd.main.NDCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import org.sbml.libsbml.KineticLaw;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.LocalParameter;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Reaction;
import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class FBAAnalysisDataset implements Dataset {

    String datasetName, path;
    protected DatasetType type;
    StringBuffer infoDataset;
    private int ID;
    private SBMLDocument document;
    private final JTextArea textArea;
    private List<String> sources;
    private Graph graph;
    private boolean isCluster = false;
    private HashMap<String, ReactionFA> reactions;
    private HashMap<String, SpeciesFA> compounds;
    private List<String> cofactors;
    private Map<String, Double[]> sourcesMap;
    private String selectedReaction, selectedMetabolite;

    /**
     *
     * @param datasetName Name of the data set
     * @param path
     */
    public FBAAnalysisDataset(String datasetName, String path) {
        this.datasetName = datasetName;
        this.infoDataset = new StringBuffer();
        this.path = path;
        type = DatasetType.FBAAnalysis;
        this.textArea = new JTextArea();
    }

    public FBAAnalysisDataset() {
        type = DatasetType.FBAAnalysis;
        this.infoDataset = new StringBuffer();
        this.textArea = new JTextArea();
    }

    @Override
    public void setNodes(List<Node> nodes) {
        
    }

    @Override
    public void setEdges(List<Edge> edges) {
        
    }

    @Override
    public List<Node> getNodes() {
        return null;
    }

    @Override
    public List<Edge> getEdges() {
       return null;
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
  

    @Override
    public List<String> getSources() {
        return this.sources;
    }

    

    @Override
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Graph getGraph() {
         Model model = this.document.getModel();
        System.out.println("1");
        this.cofactors = NDCore.getCofactors();
        List<Node> nodeList = new ArrayList<>();
        List<Edge> edgeList = new ArrayList<>();

        Graph g = new Graph(nodeList, edgeList);

        System.out.println("2");
        try {
            ListOf listOfReactions = model.getListOfReactions();
            for (int i = 0; i < model.getNumReactions(); i++) {
            Reaction r = (Reaction) listOfReactions.get(i);           
                Node reactionNode = new Node(r.getId(), r.getName());
                this.setPosition(reactionNode);
                g.addNode2(reactionNode);
                int direction = this.getDirection(r);
                // read bounds to know the direction of the edges
                ListOf listOfReactans = r.getListOfReactants();
                for (int e = 0; e < r.getNumReactants(); e++) {
                    SpeciesReference spr = (SpeciesReference) listOfReactans.get(e);            
                    Species sp = model.getSpecies(spr.getSpecies());

                    if (this.cofactors.contains(sp.getId())) {
                        Node cof = new Node(sp.getId(), sp.getName());

                        g.addNode(cof);
                        g.addEdge(addEdge(cof, reactionNode, sp.getId(), direction));
                    } else {
                        Node spNode = g.getNode(sp.getId());
                        if (spNode == null) {
                            spNode = new Node(sp.getId(), sp.getName());
                        }
                        this.setPosition(spNode);
                        g.addNode(spNode);
                        g.addEdge(addEdge(spNode, reactionNode, sp.getId(), direction));
                    }
                }

                ListOf listOfProducts = r.getListOfProducts();
                for (int e = 0; e < r.getNumProducts(); e++) {
                    SpeciesReference spr = (SpeciesReference) listOfProducts.get(e);            
                    Species sp = model.getSpecies(spr.getSpecies());

                    if (this.cofactors.contains(sp.getId())) {
                        Node cof = new Node(sp.getId(), sp.getName());
                        g.addNode(cof);
                        g.addEdge(addEdge(reactionNode, cof, sp.getId(), direction));
                    } else {
                        Node spNode = g.getNode(sp.getId());
                        if (spNode == null) {
                            spNode = new Node(sp.getId(), sp.getName());
                        }
                        this.setPosition(spNode);
                        g.addNode(spNode);
                        g.addEdge(addEdge(reactionNode, spNode, sp.getId(), direction));
                    }
                }

            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        System.out.println("3");
        this.graph = g;
        return g;
    }
     private void setPosition(Node node) {
        Node gNode = this.graph.getNode(node.getId());
        if (gNode != null) {
            node.setPosition(gNode.getPosition());
        }
    }

    private int getDirection(Reaction r) {
        int direction = 2;
        double lb = Double.NEGATIVE_INFINITY;
        double ub = Double.POSITIVE_INFINITY;
        Double flux = null;
        if (r.getKineticLaw() != null) {
            KineticLaw law = r.getKineticLaw();
            LocalParameter lbound = law.getLocalParameter("LOWER_BOUND");
            lb = lbound.getValue();
            LocalParameter ubound = law.getLocalParameter("UPPER_BOUND");
            ub = ubound.getValue();
            LocalParameter rflux = law.getLocalParameter("FLUX_VALUE");
            flux = rflux.getValue();
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
                e = new Edge(name, node1, node2);
                e.setDirection(true);
                break;
            case 2:
                e = new Edge(name, node1, node2);
                e.setDirection(false);
                break;
            case 0:
                e = new Edge(name, node2, node1);
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
    public FBAAnalysisDataset clone() {
        FBAAnalysisDataset newDataset = new FBAAnalysisDataset(this.datasetName, this.path);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setParent(String dataset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isParent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setIsParent(boolean isParent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
