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
package com.vtt.antnd.desktop.impl;

import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.configuration.cofactors.CofactorConfParameters;
import com.vtt.antnd.util.GUIUtils;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ChainedTransformer;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;

/**
 *
 * @author scsandra
 */
public class PrintPaths implements KeyListener, GraphMouseListener, ActionListener,
        ChangeListener {

    private final Model m;
    // private TransFrame transFrame = null;
    private final List<String> selectedNode;
    private edu.uci.ics.jung.graph.Graph<String, String> g;
    Map<String, Color> clusters;
    private boolean showInfo = false;
    private AntGraph graph;
    private VisualizationViewer vv;
    SpringLayout layout;
    private JPopupMenu popupMenu;
    JPanel topPanel;
    JColorChooser tcc;
    JButton banner;
    Color selectedColor;
    Map<String, Color> colors;
    String cofactors;
    JTextArea area;

    public PrintPaths(Model m) {
        CofactorConfParameters conf = new CofactorConfParameters();
        this.cofactors = conf.getParameter(CofactorConfParameters.cofactors).getValue();
        this.m = m;
        this.clusters = new HashMap<>();
        this.selectedNode = new ArrayList<>();
        this.popupMenu = new JPopupMenu();
    }

    public VisualizationViewer printPathwayInFrame(final AntGraph graph) {
        g = new SparseMultigraph<>();
        this.graph = graph;
        List<AntNode> nodes = graph.getNodes();
        List<AntEdge> edges = graph.getEdges();
        colors = new HashMap<>();
        layout = new SpringLayout<>(g);
        //layout = new KKLayout(g);       
        layout.setSize(new Dimension(2400, 1900)); // sets the initial size of the space
        vv = new VisualizationViewer<>(layout);

        System.out.println(nodes.size());
        for (AntNode node : nodes) {
            if (node != null) {
                String name = node.getCompleteId();
                g.addVertex(name);
                if (node.getPosition() != null) {
                    layout.setLocation(name, node.getPosition());
                    // System.out.println("Position: " + name + " : " + node.getPosition().toString());
                    vv.getGraphLayout().lock(name, true);
                }
                if (node.getColor() != null) {
                    colors.put(name, node.getColor());
                }
            }
        }

        for (AntEdge edge : edges) {
            if (edge != null) {
                try {
                    if (edge.getDirection()) {
                        g.addEdge(edge.getId(), edge.getSource().getCompleteId(), edge.getDestination().getCompleteId(), EdgeType.DIRECTED);
                    } else {
                        g.addEdge(edge.getId(), edge.getSource().getCompleteId(), edge.getDestination().getCompleteId(), EdgeType.UNDIRECTED);
                    }
                } catch (Exception e) {
                }
            }
        }

        vv.setPreferredSize(new Dimension(2400, 2000));
        Transformer<String, Paint> vertexPaint = new Transformer<String, Paint>() {
            @Override
            public Paint transform(String id) {

                String name = id.split(" - ")[0];
                String r = id.split(" : ")[0];
                try {
                    if (colors.containsKey(id)) {
                        return colors.get(id);
                    } else if (m.getReaction(r.trim()) != null || m.getReaction(name.trim()) != null) {
                        if (id.split(" : ").length > 1) {
                            return makeItDarKer(id.split(" : ")[1]);
                        } else {
                            return new Color(102, 194, 164);
                        }
                    } else if (NDCore.getCofactors().contains(id.split(" : ")[0])) {
                        return Color.ORANGE;
                    } else {
                        return new Color(156, 244, 125);
                    }
                } catch (Exception e) {
                    return new Color(156, 244, 125);
                }
            }

            private Paint makeItDarKer(String split) {
                try {
                    double flux = Double.valueOf(split);
                    if (Math.abs(flux) < 0.001) {
                        return new Color(102, 194, 164);
                    } else if (Math.abs(flux) < 0.001) {
                        return PrintPaths.lighter(new Color(190, 226, 133), 0.65);
                    } else if (Math.abs(flux) < 0.01) {
                        return PrintPaths.lighter(new Color(90, 226, 133), 0.35);
                    } else if (Math.abs(flux) < 0.1) {
                        return PrintPaths.lighter(new Color(90, 226, 133), 0.1);
                    } else if (Math.abs(flux) < 1) {
                        return PrintPaths.darken(new Color(90, 226, 133), 0.15);
                    } else if (Math.abs(flux) < 2) {
                        return PrintPaths.darken(new Color(90, 226, 133), 0.30);
                    } else if (Math.abs(flux) > 2) {
                        return PrintPaths.darken(new Color(90, 226, 133), 0.45);
                    }
                } catch (Exception e) {
                }
                return new Color(102, 194, 164);
            }
        };

        Transformer<String, Shape> vertexShape = new Transformer<String, Shape>() {
            public Shape transform(String v) {
                String name = v.split(" - ")[0];
                try {
                    String r = v.split(" : ")[0];
                    if (m.getReaction(r.trim()) != null || m.getReaction(name.trim()) != null) {
                        Rectangle2D circle = new Rectangle2D.Double(-15.0, -15.0, 50.0, 25.0);
                        return circle;
                    } else {
                        Ellipse2D circle = new Ellipse2D.Double(-15, -15, 20, 20);
                        return circle;
                    }
                } catch (Exception e) {
                    Ellipse2D circle = new Ellipse2D.Double(-15, -15, 20, 20);
                    return circle;
                }
            }
        };

        final PickedState<String> pickedState = vv.getPickedVertexState();

        pickedState.addItemListener(new ItemListener() {

            public String data(String name) {
                StringBuffer info;
                if (name.contains(" : ")) {
                    name = name.split(" : ")[0];
                }
                Model m = NDCore.getDesktop().getSelectedDataFiles()[0].getDocument().getModel();
                Reaction reaction = m.getReaction(name);
                info = new StringBuffer();
                if (reaction != null) {
                    KineticLaw law = reaction.getKineticLaw();
                    if (law != null) {
                        LocalParameter lbound = law.getLocalParameter("LOWER_BOUND");
                        LocalParameter ubound = law.getLocalParameter("UPPER_BOUND");
                        info.append(reaction.getId()).append(" - ").append(reaction.getName()).append(" lb: ").append(lbound.getValue()).append(" up: ").append(ubound.getValue()).append(":\n");
                    } else {
                        Double lb, ub;
                        FBCReactionPlugin plugin = (FBCReactionPlugin) reaction.getPlugin("fbc");
                        Parameter lp = plugin.getLowerFluxBoundInstance();
                        Parameter up = plugin.getUpperFluxBoundInstance();
                        lb = lp.getValue();
                        ub = up.getValue();
                        info.append(reaction.getId()).append(" - ").append(reaction.getName()).append(" lb: ").append(lb).append(" up: ").append(ub).append(":\n");
                    }
                    info.append("Reactants: \n");
                    //ListOf spref = reaction.getListOfReactants();
                    //for (int i = 0; i < spref.size(); i++) {
                    //    SpeciesReference sr = (SpeciesReference) spref.get(i);
                    for (SpeciesReference sr : reaction.getListOfReactants()) {
                        Species sp = m.getSpecies(sr.getSpecies());
                        info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append("\n");
                    }
                    info.append("Products: \n");
                    // spref = reaction.getListOfProducts();
                    //for (int i = 0; i < spref.size(); i++) {
                    for (SpeciesReference sr : reaction.getListOfProducts()) {
                        //SpeciesReference sr = (SpeciesReference) spref.get(i);
                        Species sp = m.getSpecies(sr.getSpecies());
                        info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append(" \n");
                    }
                } else {
                    Species sp = m.getSpecies(name);
                    if (sp != null) {
                        info.append(sp.getId()).append(" - ").append(sp.getName());
                        info.append("\n\nPresent in: \n");
                        int count = 0;
                        for (Reaction r : m.getListOfReactions()) {
                            if (r.getReactantForSpecies(name) != null || r.getProductForSpecies(name) != null) {
                                info.append(r.getId()).append(", ");
                                count++;
                                if (count == 2) {
                                    count = 0;
                                    info.append("\n");
                                }
                            }
                        }
                    }
                }
                return info.toString();
            }

            @Override
            public void itemStateChanged(ItemEvent e) {

                Object subject = e.getItem();
                if (subject instanceof String) {
                    String vertex = (String) subject;

                    if (pickedState.isPicked(vertex)) {
                        selectedNode.add(vertex);
                        //Update node position in the graph
                        if (vertex.contains(" / ")) {
                            vertex = vertex.split(" / ")[0];
                        }
                        String name = vertex.replace("sp:", "").split(" - ")[0];
                        if (name.contains(" : ")) {
                            name = name.split(" : ")[0];
                        }
                        area.setText(data(name));
//                        if (m != null && showInfo) {
//                            if (vertex.contains(" / ")) {
//                                vertex = vertex.split(" / ")[0];
//                            }
//                            String name = vertex.replace("sp:", "").split(" - ")[0];
//                            if (name.contains(" : ")) {
//                                name = name.split(" : ")[0];
//                            }
//                            //  transFrame = new TransFrame(name);
//                            
//
//                        } else {
//                            System.out.println("Vertex " + vertex
//                                    + " is now selected");
//                        }
                    } else {
                        selectedNode.remove(vertex);
                        //  System.out.println("Position:" + vertex);
                        AntNode n = graph.getNode(vertex.split(" : ")[0]);
                        if (n != null) {
                            n.setPosition(layout.getX(vertex), layout.getY(vertex));
                            //  System.out.println("New Position:" + vertex + " : " + layout.getX(vertex) + " - " + layout.getY(vertex));
                        }
                        if (/*transFrame != null &&*/showInfo) {
                            // transFrame.setVisible(false);
                            //transFrame.dispose();
                        } else {
                            System.out.println("Vertex " + vertex
                                    + " no longer selected");
                        }
                    }
                }
            }
        });

        final PickedState<String> pickedEdgeState = vv.getPickedEdgeState();
        pickedEdgeState.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Object subject = e.getItem();
                if (subject instanceof String) {
                    String edge = (String) subject;

                    if (pickedEdgeState.isPicked(edge)) {
                        selectedNode.add(edge);
                        if (m != null && showInfo) {
                            String name = edge.replace("sp:", "").split(" - ")[0];
                            if (name.contains(" : ")) {
                                name = name.split(" : ")[0];
                            }
                            //transFrame = new TransFrame(name);
                        } else {
                            System.out.println("Edge " + edge
                                    + " is now selected");
                        }
                    } else {
                        selectedNode.remove(edge);
                        if (/*transFrame != null && */showInfo) {
                            //  transFrame.setVisible(false);
                            // transFrame.dispose();
                        } else {
                            System.out.println("Edge " + edge
                                    + " no longer selected");
                        }
                    }

                }
            }
        });

        float dash[] = {1.0f};
        final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
        Transformer<String, Stroke> edgeStrokeTransformer
                = new Transformer<String, Stroke>() {
            @Override
            public Stroke transform(String s) {
                return edgeStroke;
            }
        };

        Transformer labelTransformer = new ChainedTransformer<>(new Transformer[]{
            new ToStringLabeller<>(),
            new Transformer<String, String>() {
                @Override
                public String transform(String input) {

                    String name = input.split(" - ")[0];
                    return "<html><b><font color=\"red\">" + name;
                }
            }});
        Transformer labelTransformer2 = new ChainedTransformer<>(new Transformer[]{
            new ToStringLabeller<>(),
            new Transformer<String, String>() {
                @Override
                public String transform(String input) {

                    String name = input.split(" - ")[0];
                    return "<html><b><font color=\"black\">" + name;

                }
            }});

        vv.getRenderContext().setVertexLabelTransformer(labelTransformer2);
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);
        vv.getRenderContext().getEdgeLabelRenderer().setRotateEdgeLabels(false);
        vv.getRenderContext().setEdgeLabelTransformer(labelTransformer);
        vv.getRenderContext().setVertexShapeTransformer(vertexShape);
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);

        DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
        gm.setMode(ModalGraphMouse.Mode.PICKING);
        vv.setGraphMouse(gm);

        vv.addKeyListener(this);
        vv.addGraphMouseListener(this);

        topPanel = new JPanel();

        final JButton button = new JButton("Show Node Info");
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (showInfo == false) {
                    showInfo = true;
                    button.setText("Hide Node Info");
                } else {
                    showInfo = false;
                    button.setText("Show Node Info");
                }
            }
        });

        final JButton saveButton = new JButton("Save Graph");
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showSaveDialog(topPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    saveImage3(file.getAbsolutePath());
                }
            }
        });
        //Set up color chooser for setting text color
        tcc = new JColorChooser();
        tcc.getSelectionModel().addChangeListener(this);
        tcc.setBorder(BorderFactory.createTitledBorder("Choose Text Color"));
        tcc.setAlignmentX(1000);
        tcc.setVisible(false);
        tcc.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    tcc.setVisible(false);
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
            }

            @Override
            public void mouseReleased(MouseEvent me) {
            }

            @Override
            public void mouseEntered(MouseEvent me) {
            }

            @Override
            public void mouseExited(MouseEvent me) {
            }

        });
        vv.add(tcc);

        banner = new JButton("Selected Color");
        banner.setBackground(Color.white);
        banner.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tcc.setVisible(true);
            }
        });
        final JTextField field = new JTextField("");
        field.setPreferredSize(new Dimension(350, 30));
        field.setBackground(Color.LIGHT_GRAY);
        field.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent ke) {
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    String[] reactions = field.getText().split(",");
                    for (String r : reactions) {
                        Collection<String> V = g.getVertices();
                        for (String v : V) {
                            if (v.contains(r)) {
                                colors.put(v, selectedColor);
                                String spID = v;

                                if (v.contains(" : ")) {
                                    spID = v.split(" : ")[0];
                                }
                                AntNode n = graph.getNode(spID);
                                if (n != null) {
                                    n.setColor(selectedColor);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {
            }

        });
        this.area = new JTextArea();
        this.area.setPreferredSize(new Dimension(650, 200));
        topPanel.add(this.area);
        // topPanel.add(field);
        //topPanel.add(banner);
        // topPanel.add(button);
        topPanel.add(saveButton);
        topPanel.setPreferredSize(new Dimension(1000, 200));
        topPanel.setBackground(Color.WHITE);
        vv.add(topPanel);
        vv.setBackground(Color.WHITE);
        return vv;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == '\u0008' || e.getKeyChar() == '\u007F') {
            if (this.selectedNode != null) {
                for (String v : this.selectedNode) {
                    g.removeVertex(v);

                    String name = v.split(" - ")[0];
                    if (name.contains(" : ")) {
                        name = name.split((" : "))[0];
                    }
                    if (m.getReaction(name) != null) {
                        this.m.removeReaction(name);

                    }
                    this.graph.removeNode(name);

                }
            }
        }
        if (e.getKeyChar() == 'e') {
            if (!this.selectedNode.isEmpty()) {
                showReactions(this.selectedNode.get(0), null);
            }
        }

        if (e.getKeyChar() == 'c') {
            removeCofactors();
        }

        if (e.getKeyChar() == 'l') {
            this.lock();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private void showReactions(String initialStringNode, String reaction) {
        Collection<String> V = g.getVertices();

        // Gets the selected model. It is the source model for the reactions
        Model mInit = NDCore.getDesktop().getSelectedDataFiles()[0].getDocument().getModel();

        String spID = initialStringNode;

        if (initialStringNode.contains(" : ")) {
            spID = initialStringNode.split(" : ")[0];
        }
        AntNode initNode = graph.getNode(spID);

        Species sp = mInit.getSpecies(spID);
        if (sp == null) {
            return;
        }

        for (Reaction r : mInit.getListOfReactions()) {

            if (reaction != null && !reaction.contains(r.getId())) {
                continue;
            }
            if (r.getReactantForSpecies(sp.getId()) != null || r.getProductForSpecies(sp.getId()) != null) {
                double lb = Double.NEGATIVE_INFINITY;
                double ub = Double.POSITIVE_INFINITY;
                Double flux = null;
                // read bounds to know the direction of the edges
                if (r.getKineticLaw() != null) {
                    KineticLaw law = r.getKineticLaw();
                    LocalParameter lbound = law.getLocalParameter("LOWER_BOUND");
                    lb = lbound.getValue();
                    LocalParameter ubound = law.getLocalParameter("UPPER_BOUND");
                    ub = ubound.getValue();
                    LocalParameter rflux = law.getLocalParameter("FLUX_VALUE");
                    try {
                        flux = rflux.getValue();
                    } catch (Exception e) {
                        flux = 0.0;
                    }
                } else {
                    FBCReactionPlugin plugin = (FBCReactionPlugin) r.getPlugin("fbc");
                    Parameter lp = plugin.getLowerFluxBoundInstance();
                    Parameter up = plugin.getUpperFluxBoundInstance();
                    lb = lp.getValue();
                    ub = up.getValue();
                    flux = 0.0;
                }

                // adds the new reaction node with and edge from the extended node
                String reactionName = null;
                if (flux != null) {
                    DecimalFormat df = new DecimalFormat("#.####");
                    reactionName = r.getId() + " : " + df.format(flux) + " - " + uniqueId.nextId();
                } else {
                    reactionName = r.getId() + " - " + uniqueId.nextId();
                }

                //  String initSPName = sp.getId() + " : " + sp.getName() + " - " + uniqueId.nextId();
                String initSPName = initialStringNode;

                String isThere = this.isThere(V, r.getId());

                // adds the rest of the compounds in the reaction, the direction of the edges 
                // should depend on the boundaries of the reaction
                if (isThere == null) {

                    //adds the new reaction to the new model
                    if (m.getReaction(r.getId()) == null) {
                        AddReaction(r);
                    }
                    // adds the new reaction to the visualization graph
                    g.addVertex(reactionName);

                    // Creates the node for the ANT graph
                    AntNode reactionNode = new AntNode(reactionName);
                    graph.addNode(reactionNode);

                    EdgeType eType = EdgeType.UNDIRECTED;
                    boolean direction = false;
                    if (lb == 0 || ub == 0) {
                        eType = EdgeType.DIRECTED;
                        direction = true;
                    }
                    //ListOf listOfSR = r.getListOfReactants();
                    //for (int e = 0; e < r.getNumReactants(); e++) {
                    //    SpeciesReference sr = (SpeciesReference) listOfSR.get(e);
                    for (SpeciesReference sr : r.getListOfReactants()) {
                        Species sps = mInit.getSpecies(sr.getSpecies());

                        //                 if (!this.m.containsSpecies(sp.getId())) {
                        //                   this.m.addSpecies(sps);
                        //             }
                        String spName = sps.getId();
                        String nodeReactant = isThere(V, spName);

                        if (nodeReactant == null) {
                            if (!spName.equals(spID)) {
                                String vName = spName + " : " + sps.getName() + " - " + uniqueId.nextId();
                                String eName = spName + " - " + uniqueId.nextId();

                                g.addVertex(vName);
                                //adds the node to the graph
                                AntNode n = new AntNode(vName);
                                graph.addNode(n);
                                if (lb == 0) {
                                    g.addEdge(eName, vName, reactionName, eType);
                                    graph.addEdge(new AntEdge(eName, n, reactionNode, direction));
                                } else {
                                    g.addEdge(eName, reactionName, vName, eType);
                                    graph.addEdge(new AntEdge(eName, reactionNode, n, direction));
                                }
                            } else {
                                if (lb == 0) {
                                    g.addEdge(initSPName, initialStringNode, reactionName, eType);
                                    graph.addEdge(new AntEdge(sp.getId(), initNode, reactionNode, direction));
                                } else {
                                    g.addEdge(initSPName, reactionName, initialStringNode, eType);
                                    graph.addEdge(new AntEdge(sp.getId(), reactionNode, initNode, direction));
                                }
                            }
                        } else {

                            AntNode reactantNode = graph.getNode(spName);
                            String eName = spName + " - " + uniqueId.nextId();
                            if (lb == 0) {
                                g.addEdge(eName, nodeReactant, reactionName, eType);
                                graph.addEdge(new AntEdge(eName, reactantNode, reactionNode, direction));
                            } else {
                                g.addEdge(eName, reactionName, nodeReactant, eType);
                                graph.addEdge(new AntEdge(eName, reactionNode, reactantNode, direction));
                            }

                        }
                    }

                    for (SpeciesReference sr : r.getListOfProducts()) {
                        String spId = sr.getSpecies();
                        Species sps = mInit.getSpecies(spId);

                        String nodeProduct = isThere(V, spId);
                        if (nodeProduct == null) {

                            if (!spId.equals(spID)) {
                                String vName = spId + " : " + sps.getName() + " - " + uniqueId.nextId();
                                String eName = spId + " - " + uniqueId.nextId();

                                g.addVertex(vName);
                                //adds the node to the graph
                                AntNode n = new AntNode(vName);
                                graph.addNode(n);
                                if (lb == 0) {
                                    g.addEdge(eName, reactionName, vName, eType);
                                    graph.addEdge(new AntEdge(eName, reactionNode, n, direction));
                                } else {
                                    g.addEdge(eName, vName, reactionName, eType);
                                    graph.addEdge(new AntEdge(eName, n, reactionNode, direction));
                                }
                            } else {
                                if (lb == 0) {
                                    g.addEdge(initSPName, initialStringNode, reactionName, eType);
                                    graph.addEdge(new AntEdge(sp.getId(), initNode, reactionNode, direction));

                                } else {
                                    g.addEdge(initSPName, reactionName, initialStringNode, eType);
                                    graph.addEdge(new AntEdge(sp.getId(), reactionNode, initNode, direction));
                                }
                            }
                        } else {
                            AntNode productNode = graph.getNode(spId);
                            String eName = spId + " - " + uniqueId.nextId();
                            if (lb == 0) {
                                g.addEdge(eName, reactionName, nodeProduct, eType);
                                graph.addEdge(new AntEdge(eName, reactionNode, productNode, direction));
                            } else {
                                g.addEdge(eName, nodeProduct, reactionName, eType);
                                graph.addEdge(new AntEdge(eName, productNode, reactionNode, direction));
                            }
                        }
                    }
                }
            }
        }

    }

    private String isThere(Collection<String> V, String node) {
        for (String v : V) {
            if (v.contains(node)) {
                return v;
            }
        }
        return null;
    }

    private void removeCofactors() {
        String[] cof = this.cofactors.split(",");
        Collection<String> Vertices = g.getVertices();
        for (String node : Vertices) {
            for (String c : cof) {
                if (node.contains(c)) {
                    g.removeVertex(node);
                    //   graph.removeNode(node);
                    removeCofactors();
                    break;
                }
            }
        }

    }

    private void lock() {
        Collection<String> V = g.getVertices();
        Layout<String, String> layout = vv.getGraphLayout();
        for (String v : V) {
            layout.lock(v, true);
            AntNode n = graph.getNode(v.split(" : ")[0]);
            if (n != null) {
                n.setPosition(this.layout.getX(v), this.layout.getY(v));
                //  System.out.println("New Position:" + vertex + " : " + layout.getX(vertex) + " - " + layout.getY(vertex));
            } else {
                System.out.println(v);
            }
        }
    }

    private void AddReaction(Reaction reaction) {
        Model mInit = NDCore.getDesktop().getSelectedDataFiles()[0].getDocument().getModel();

        Reaction r = new Reaction(reaction);
        r.setId(reaction.getId());
        r.setName(reaction.getName());
        System.out.println("Adding new reaction: "+reaction.getId());
      
        reaction.getListOfReactants().forEach((sp) -> {
            SpeciesReference spref = r.createReactant();
            spref.setId(sp.getSpecies());
            spref.setStoichiometry(sp.getStoichiometry());

            if (m.getSpecies(sp.getSpecies()) != null) {
                spref.setSpecies(sp.getSpecies());
            } else {
                Species specie = (Species) mInit.getSpecies(sp.getSpecies()).clone();
                m.addSpecies(specie);
                spref.setSpecies(sp.getSpecies());
            }
        });
        reaction.getListOfProducts().forEach((sp) -> {      
            SpeciesReference spref = r.createProduct();
            spref.setStoichiometry(sp.getStoichiometry());
            if (m.getSpecies(sp.getSpecies()) != null) {
                spref.setSpecies(sp.getSpecies());
            } else {
                Species specie = (Species) mInit.getSpecies(sp.getSpecies()).clone();
                m.addSpecies(specie);
                spref.setSpecies(sp.getSpecies());
            }
        });

        if (reaction.getKineticLaw() != null) {
            KineticLaw law = r.createKineticLaw();
            LocalParameter lboundP = law.createLocalParameter();
            lboundP.setId("LOWER_BOUND");
            if (reaction.getKineticLaw().getLocalParameter("LOWER_BOUND") != null) {
                lboundP.setValue(reaction.getKineticLaw().getLocalParameter("LOWER_BOUND").getValue());

                law.addLocalParameter(lboundP);
                LocalParameter uboundP = law.createLocalParameter();
                uboundP.setId("UPPER_BOUND");
                uboundP.setValue(reaction.getKineticLaw().getLocalParameter("LOWER_BOUND").getValue());
                law.addLocalParameter(uboundP);

                /*LocalParameter obj = new LocalParameter("OBJECTIVE_COEFFICIENT");
                obj.setValue(reaction.getKineticLaw().getLocalParameter("OBJECTIVE_COEFFICIENT").getValue());
                law.addLocalParameter(obj);*/
                LocalParameter obj = law.createLocalParameter();
                obj.setId("OBJECTIVE_COEFFICIENT");
                obj.setValue(0.0);
                law.addLocalParameter(obj);
                LocalParameter fv = law.createLocalParameter();
                fv.setId("FLUX_VALUE");
                try {
                    fv.setValue(reaction.getKineticLaw().getParameter("FLUX_VALUE").getValue());
                } catch (Exception e) {
                    fv.setValue(0.0);
                }
                law.addParameter(fv);

                r.setKineticLaw(law);
            }
        }else{
            FBCReactionPlugin plugin = (FBCReactionPlugin) reaction.getPlugin("fbc");                       
            Parameter lp = plugin.getLowerFluxBoundInstance();                        
            Parameter up = plugin.getUpperFluxBoundInstance();
            FBCReactionPlugin plugin2 = (FBCReactionPlugin) r.createPlugin("fbc");
            plugin2.setLowerFluxBound(lp.clone());
            plugin2.setUpperFluxBound(up.clone());
        }
        r.appendNotes(reaction.getNotes());

        m.addReaction(r);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Runtime.getRuntime().freeMemory();
        String command = ae.getActionCommand();
        if (command.equals("All")) {
            if (!this.selectedNode.isEmpty()) {
                showReactions(this.selectedNode.get(0), null);
            }
        } else {
            if (!this.selectedNode.isEmpty()) {
                String reaction = command.split(" - ")[0];
                System.out.println("command: " + reaction);
                showReactions(this.selectedNode.get(0), reaction);
            }
        }
    }

    @Override
    public void graphClicked(Object v, MouseEvent me) {
        if (me.getClickCount() == 2) {
            this.colors.put((String) v, selectedColor);
            String name = ((String) v).split(" - ")[0];
            if (name.contains(" : ")) {
                name = name.split((" : "))[0];
            }
            AntNode node = this.graph.getNode(name);
            if (node != null) {
                node.setColor(selectedColor);
            }
        }
    }

    @Override
    public void graphPressed(Object v, MouseEvent me) {
        if (me.isPopupTrigger()) {
            popupMenu = new JPopupMenu();
            Model mInit = NDCore.getDesktop().getSelectedDataFiles()[0].getDocument().getModel();
            String spID = (String) v;

            if (spID.contains(" : ")) {
                spID = spID.split(" : ")[0];
            }
            Species sp = mInit.getSpecies(spID);
            if (sp == null) {
                return;
            }
            GUIUtils.addMenuItem(popupMenu, "All", this, "All");
            int i = 0;
            //ListOf reactions = mInit.getListOfReactions();
            for (Reaction r : mInit.getListOfReactions()) {
                //Reaction r = (Reaction) reactions.get(e);
                if (r.getReactantForSpecies(sp.getId()) != null || r.getProductForSpecies(sp.getId()) != null) {
                    String reaction = r.getId() + " - " + r.getName();
                    GUIUtils.addMenuItem(popupMenu, reaction, this, reaction);
                    i++;
                }
                if (i > 35) {
                    GUIUtils.addMenuItem(popupMenu, "...", this, "...");
                    break;
                }
            }

            popupMenu.show(me.getComponent(), me.getX(), me.getY());
            System.out.println(v);
        }
    }

    @Override
    public void graphReleased(Object v, MouseEvent me) {

    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        Color newColor = tcc.getColor();
        this.banner.setBackground(newColor);
        this.selectedColor = newColor;
    }

    public static Color darken(Color color, double fraction) {

        int red = (int) Math.round(Math.max(0, color.getRed() - 255 * fraction));
        int green = (int) Math.round(Math.max(0, color.getGreen() - 255 * fraction));
        int blue = (int) Math.round(Math.max(0, color.getBlue() - 255 * fraction));

        int alpha = color.getAlpha();

        return new Color(red, green, blue, alpha);

    }

    public static Color lighter(Color color, double fraction) {

        int red = (int) Math.round(Math.max(0, color.getRed() + 255 * fraction));
        int green = (int) Math.round(Math.max(0, color.getGreen() + 255 * fraction));
        int blue = (int) Math.round(Math.max(0, color.getBlue() + 255 * fraction));

        int alpha = color.getAlpha();

        return new Color(red, green, blue, alpha);

    }

    public void saveImage(String path) {
        // Create the VisualizationImageServer
// vv is the VisualizationViewer containing my graph
        VisualizationImageServer<String, String> vis
                = new VisualizationImageServer<String, String>(vv.getGraphLayout(),
                        vv.getSize());

// Configure the VisualizationImageServer the same way
// you did your VisualizationViewer. In my case e.g.
        vis.setBackground(Color.WHITE);
        /* vis.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<>());
        vis.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<>());
        vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<>());
        vis.getRenderer().getVertexLabelRenderer()
            .setPosition(Renderer.VertexLabel.Position.CNTR);*/

// Create the buffered image
        BufferedImage image = (BufferedImage) vis.getImage(
                new Point2D.Double(vv.getGraphLayout().getSize().getWidth(),
                        vv.getSize().getHeight()),
                new Dimension(vv.getSize()));

// Write image to a png file
        File outputfile = new File(path);

        try {
            ImageIO.write(image, "png", outputfile);
        } catch (IOException e) {
            // Exception handling
        }

    }

    public void saveImage2() {
        try {
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout());
            panel.setBackground(Color.WHITE);
            panel.add(vv);

            Properties p = new Properties();
            p.setProperty("PageSize", "A4");

// vv is the VirtualizationViewer
            VectorGraphics g = new SVGGraphics2D(new File("/home/scsandra/Pictures/Network.svg"), vv);

            g.setProperties(p);
            g.startExport();
            panel.print(g);
            g.endExport();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintPaths.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PrintPaths.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveImage3(String path) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(path, "UTF-8");
            writer.println("Creator \"AntND\"");
            writer.println("Version	1.0");
            writer.println("graph\t[");
            Map<AntNode, String> indexes = new HashMap<>();
            List<AntNode> nodes = graph.getNodes();
            int i = 1;
            for (AntNode node : nodes) {
                indexes.put(node, String.valueOf(i));
                writer.println("\tnode\t[");
                writer.println("\t\troot_index\t" + i);
                writer.println("\t\tid\t" + i++);
                writer.println("\t\tlabel\t\"" + node.getCompleteId() + "\"");
                writer.println("\t\tgraphics\t[");
                if (node.getPosition() != null) {
                    writer.println("\t\t\tx\t" + node.getPosition().getX());
                    writer.println("\t\t\ty\t" + node.getPosition().getY());
                }
                writer.println("\t\t\tw\t35.0");
                writer.println("\t\t\th\t35.0");
                if (node.getColor() != null) {
                    String hex = "#" + Integer.toHexString(node.getColor().getRGB()).substring(2);
                    writer.println("\t\t\tfill\t\"" + hex + "\"");
                }
                writer.println("\t\t\ttype\t\"ellipse\"");
                writer.println("\t\t\toutline\t\"#3333ff\"");
                writer.println("\t\t\toutline_width\t5.0");
                writer.println("\t\t]");

                writer.println("\t]");
            }

            List<AntEdge> edges = graph.getEdges();

            for (AntEdge edge : edges) {
                writer.println("\tedge\t[");
                writer.println("\t\troot_index\t" + i++);
                writer.println("\t\ttarget\t" + indexes.get(edge.getDestination()));
                writer.println("\t\tsource\t" + indexes.get(edge.getSource()));
                writer.println("\t]");
            }
            writer.println("]");

            writer.println("Title\t\"" + this.m.getId() + "\"");
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintPaths.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(PrintPaths.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
