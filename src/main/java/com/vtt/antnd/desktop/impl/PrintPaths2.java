/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vtt.antnd.desktop.impl;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.configuration.cofactors.CofactorConfParameters;
import com.vtt.antnd.util.GUIUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.view.util.DefaultMouseManager;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class PrintPaths2 implements KeyListener {

    private final Model m;
    // private TransFrame transFrame = null;
    private final List<String> selectedNode;
    private AntGraph antGraph;
    private final Dataset dataset;
    Map<String, Color> clusters;
    private JPopupMenu popupMenu;
    JPanel topPanel;
    JColorChooser tcc;
    JButton banner;
    Color selectedColor;
    Map<String, Color> colors;
    String cofactors;
    JTextArea area;
    AdjacencyListGraph g;
    Viewer viewer;
    Boolean layoutActivation = Boolean.TRUE;
    Boolean cofactorActivation = Boolean.TRUE;
    ViewPanel viewPanel;

    public PrintPaths2(Dataset dataset) {
        this.dataset = dataset;
        CofactorConfParameters conf = new CofactorConfParameters();
        this.cofactors = conf.getParameter(CofactorConfParameters.cofactors).getValue();
        this.m = dataset.getDocument().getModel();
        this.clusters = new HashMap<>();
        this.selectedNode = new ArrayList<>();
        this.popupMenu = new JPopupMenu();

    }

    public ViewPanel printPathwayInFrame(final AntGraph graph) {
        this.g = new AdjacencyListGraph("");
        this.antGraph = graph;
        List<AntNode> nodes = graph.getNodes();
        List<AntEdge> edges = graph.getEdges();
        colors = new HashMap<>();

        nodes.stream().filter((node) -> (node != null)).forEachOrdered((node) -> {
            String name = node.getCompleteId();
            String n = name.split(" - ")[0];
            String r = name.split(" : ")[0];
            g.addNode(name);
            if (node.getColor() != null) {
                colors.put(name, node.getColor());
            }
            Node gNode = g.getNode(name);
            if (m.getReaction(r.trim()) != null || m.getReaction(name.trim()) != null) {
                gNode.addAttribute("ui.style", "shape:box;fill-color:green;size: 25px;text-alignment: center;text-size:12px;");
                gNode.addAttribute("ui.label", r);
            } else if (this.cofactors.contains(r)) {
                gNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                gNode.addAttribute("ui.label", n);
            } else {
                gNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 25px;text-alignment: center;text-size:18px;text-style:bold;");
                gNode.addAttribute("ui.label", n);

            }
        });
        edges.stream().filter((edge) -> (edge != null)).forEachOrdered((edge) -> {
            try {
                if (edge.getDirection()) {
                    g.addEdge(edge.getId(), edge.getSource().getCompleteId(), edge.getDestination().getCompleteId(), Boolean.TRUE);
                } else {
                    g.addEdge(edge.getId(), edge.getSource().getCompleteId(), edge.getDestination().getCompleteId(), Boolean.FALSE);
                }
            } catch (Exception e) {
            }
        });
        viewer = g.display();
        // viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
        viewer.enableAutoLayout();

        viewPanel = viewer.addDefaultView(false);

        // Zoom
        viewPanel.addMouseWheelListener((MouseWheelEvent mwe) -> {
            zoomGraphMouseWheelMoved(mwe, viewPanel);
        });

        ViewerPipe fromViewer = viewer.newViewerPipe();

        ViewerEventListener viewerListener = new ViewerEventListener(this.g);
        View view = viewer.getDefaultView();

        view.setMouseManager(viewerListener);
        view.addMouseListener(viewerListener);
        view.addKeyListener(this);
        fromViewer.addViewerListener(viewerListener);
        fromViewer.addSink(this.g);

        topPanel = new JPanel();

        final JButton saveButton = new JButton("Save Graph");
        saveButton.addActionListener((ActionEvent e) -> {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showSaveDialog(topPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                saveImage(file.getAbsolutePath());
            }
        });

        this.area = new JTextArea();

        JScrollPane sc = new JScrollPane(this.area);
        sc.setAutoscrolls(true);
        sc.setPreferredSize(new Dimension(300, 170));
        // topPanel.add(sc);

        topPanel.add(saveButton);
        topPanel.setPreferredSize(new Dimension(140, 50));
        topPanel.setBackground(Color.WHITE);
        try {
            viewPanel.add(topPanel);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        viewPanel.setPreferredSize(new Dimension(700, 700));
        viewPanel.setVisible(true);

        return viewPanel;

    }

    private void saveImage(String path) {
        String pathpng = path;
        if (!path.contains(".png")) {
            pathpng = path + ".png";
        }
        this.g.addAttribute("ui.screenshot", pathpng);
        PrintWriter writer;
        try {
            if (!path.contains(".gml")) {
                path = path + ".gml";
            }
            writer = new PrintWriter(path, "UTF-8");
            writer.println("Creator \"yFiles\"");
            writer.println("Version	\"2.16\"");
            writer.println("graph\n[");
            writer.println("\thierarchic\t1");
            writer.println("\tlabel\t\"\"");
            writer.println("\tdirected\t1");
            Map<Node, String> indexes = new HashMap<>();

            int i = 0;
            for (Node node : this.g.getEachNode()) {
                indexes.put(node, String.valueOf(i));
                writer.println("\tnode\n\t[");
                //writer.println("\t\troot_index\t" + i);
                writer.println("\t\tid\t" + i++);
                writer.println("\t\tlabel\t\"" + node.getId() + "\"");
                writer.println("\t\tgraphics\n\t\t[");

                //Position
                String gnodestr = isThere(node.getId());
                GraphicNode n = viewer.getGraphicGraph().getNode(gnodestr);
                double x = n.getX();
                double y = n.getY();
                Point3 pixels = viewer.getDefaultView().getCamera().transformGuToPx(x, y, 0);

                writer.println("\t\t\tx\t" + pixels.x);
                writer.println("\t\t\ty\t" + pixels.y);
                String name = node.getId();
                if (name.contains(" : ")) {
                    name = name.split(" : ")[0];
                }
                //Nodes
                if (this.m.getSpecies(name) != null) {
                    if (this.cofactors.contains(name)) {
                        writeSpecie(writer, 15, node.getAttribute("ui.label"));
                    } else {
                        writeSpecie(writer, 35, node.getAttribute("ui.label"));
                    }
                } else {
                    writeReaction(writer, node.getAttribute("ui.label"));
                }

                writer.println("\t\t]");

                writer.println("\t]");

            }

            for (Edge edge : g.getEachEdge()) {

                writer.println("\tedge\n\t[");
                // writer.println("\t\troot_index\t" + i++);
                writer.println("\t\ttarget\t" + indexes.get(edge.getTargetNode()));
                writer.println("\t\tsource\t" + indexes.get(edge.getSourceNode()));
                writer.println("\t\tgraphics\n\t\t[");
                writer.println("\t\t\tfill\t\"#000000\"");
                if (edge.isDirected()) {
                    writer.println("\t\t\ttargetArrow\t\"standard\"");
                }
                writer.println("\t\t]");
                writer.println("\t]");
            }
            writer.println("]");

            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(PrintPaths.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeSpecie(PrintWriter writer, double size, String label) {
        writer.println("\t\t\tw\t" + size);
        writer.println("\t\t\th\t" + size);
        writer.println("\t\t\ttype\t\"ellipse\"");
        if (size == 35.0) {
            writer.println("\t\t\tfill\t\"#FF9900\"");
            writer.println("\t\t\toutline\t\"#FF9900\"");
        } else {
            writer.println("\t\t\tfill\t\"#ffcc99\"");
            writer.println("\t\t\toutline\t\"#ffcc99\"");

        }
        writer.println("\t\t]");

        if (size == 35.0) {
            writer.println("\t\tLabelGraphics\n\t\t[");
            writer.println("\t\t\ttext\t\"" + label + "\"");
            writer.println("\t\t\tfontSize\t15");
            writer.println("\t\t\tfontName\t\"Dialog\"");
            writer.println("\t\t\tanchor\t\"c\"");
        } else {
            writer.println("\t\tLabelGraphics\n\t\t[");
            writer.println("\t\t\ttext\t\"" + label + "\"");
            writer.println("\t\t\tfontSize\t9");
            writer.println("\t\t\tfontName\t\"Dialog\"");
            writer.println("\t\t\tanchor\t\"c\"");
        }

    }

    private void writeReaction(PrintWriter writer, String id) {
        writer.println("\t\t\tw\t55.0");
        writer.println("\t\t\th\t35.0");
        writer.println("\t\t\ttype\t\"rectangle3d\"");
        writer.println("\t\t\tfill\t\"#2E8B57\"");
        writer.println("\t\t\toutline\t\"#2E8B57\"");
        writer.println("\t\t]");
        writer.println("\t\tLabelGraphics\n\t\t[");
        writer.println("\t\t\ttext\t\"" + id + "\"");
        writer.println("\t\t\tfontSize\t12");
        writer.println("\t\t\tfontName\t\"Dialog\"");

    }

    public class ViewerEventListener extends DefaultMouseManager implements ViewerListener, ActionListener {

        private final AdjacencyListGraph displayedGraph;
        private Point3 previousPoint;

        public ViewerEventListener(AdjacencyListGraph displayedGraph) {
            this.displayedGraph = displayedGraph;

        }

        @Override
        public void mouseClicked(MouseEvent e) {
            selectedNode.removeAll(selectedNode);
            if (e.isControlDown()) {
                centerOn(e.getPoint());
            }
        }

        private void centerOn(Point point) {
            Camera camera = view.getCamera();
            int x = point.x;
            int y = point.y;
            Point3 newCenter = camera.transformPxToGu(x, y);
            camera.setViewCenter(newCenter.x, newCenter.y, 0);
            previousPoint = newCenter;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            try {
                previousPoint = point3(e.getPoint());
            } catch (NullPointerException n) {
            }
        }

        public String data(String name) {
            Dataset dInit = NDCore.getDesktop().getSelectedDataFiles()[0];
            StringBuffer info;
            if (name.contains(" : ")) {
                name = name.split(" : ")[0];
            }
            Model m = dInit.getDocument().getModel();
            Reaction reaction = m.getReaction(name);
            info = new StringBuffer();
            if (reaction != null) {
                info.append(reaction.getId()).append(" - ").append(reaction.getName()).append(" lb: ").append(dInit.getLowerBound(reaction.getId())).append(" up: ").append(dInit.getUpperBound(reaction.getId())).append(":\n");

                info.append("Reactants: \n");
                reaction.getListOfReactants().forEach((sr) -> {
                    Species sp = m.getSpecies(sr.getSpecies());
                    info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append("\n");
                });
                info.append("Products: \n");
                reaction.getListOfProducts().forEach((sr) -> {
                    Species sp = m.getSpecies(sr.getSpecies());
                    info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append(" \n");
                });
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
                            if (count == 6) {
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
        public void mousePressed(MouseEvent e) {
            curElement = view.findNodeOrSpriteAt(e.getX(), e.getY());

            if (curElement != null) {
                mouseButtonPressOnElement(curElement, e);
                Node node = displayedGraph.getNode(curElement.getId());
                if (node != null) {
                    String sourceLine = node.toString();
                    area.setText(data(sourceLine));
                    selectedNode.add(sourceLine);
                    node.addAttribute("ui.style", "shape:circle;fill-color:red;size: 25px;text-alignment: center;");

                    if (e.isPopupTrigger()) {
                        popupMenu = new JPopupMenu();
                        Model mInit;
                        try {
                            mInit = NDCore.getDesktop().getSelectedDataFiles()[0].getDocument().getModel();
                        } catch (Exception ex2) {
                            return;
                        }
                        String spID = node.toString();

                        if (spID.contains(" : ")) {
                            spID = spID.split(" : ")[0];
                        }
                        Species sp = mInit.getSpecies(spID);
                        if (sp == null) {
                            return;
                        }
                        GUIUtils.addMenuItem(popupMenu, "All", this, "All");
                        int i = 0;
                        for (Reaction r : mInit.getListOfReactions()) {
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

                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

            } else {
                x1 = e.getX();
                y1 = e.getY();
                mouseButtonPress(e);
                view.beginSelectionAt(x1, y1);
                for (Node gNode : displayedGraph.getEachNode()) {
                    String sourceLine = gNode.toString();
                    selectedNode.remove(sourceLine);
                    String n = sourceLine.split(" - ")[0];
                    String r = sourceLine.split(" : ")[0];
                    if (m.getReaction(r.trim()) != null || m.getReaction(n.trim()) != null) {
                        gNode.addAttribute("ui.style", "shape:box;fill-color:green;size: 25px;text-alignment: center;text-size:12px;");
                        gNode.addAttribute("ui.label", r);
                    } else if (cofactors.contains(r)) {
                        gNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                        gNode.addAttribute("ui.label", n);
                    } else {
                        gNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 25px;text-alignment: center;text-size:18px;text-style:bold;");
                        gNode.addAttribute("ui.label", n);

                    }
                }
            }

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (curElement != null) {
                mouseButtonReleaseOffElement(curElement, e);
                curElement = null;
            } else {
                float x2 = e.getX();
                float y2 = e.getY();
                float t;

                if (x1 > x2) {
                    t = x1;
                    x1 = x2;
                    x2 = t;
                }
                if (y1 > y2) {
                    t = y1;
                    y1 = y2;
                    y2 = t;
                }

                mouseButtonRelease(e, view.allNodesOrSpritesIn(x1, y1, x2, y2));
                view.endSelectionAt(x2, y2);
            }

        }

        @Override
        public void mouseDragged(MouseEvent e) {
            boolean isShiftPressed = e.isShiftDown();
            if (!isShiftPressed) {
                if (curElement != null) {
                    elementMoving(curElement, e);
                }
                Point3 currentPoint = point3(e.getPoint());
                double xDelta = (currentPoint.x - previousPoint.x) / 1.5;
                double yDelta = (currentPoint.y - previousPoint.y) / 1.5;
                pan(xDelta, yDelta);
                previousPoint = currentPoint;
            } else {
                if (curElement != null) {
                    elementMoving(curElement, e);
                } else {
                    view.selectionGrowsAt(e.getX(), e.getY());

                    //        System.out.println(e.getPoint().toString());
                }
            }
        }

        private void pan(double xDelta, double yDelta) {
            Camera camera = view.getCamera();
            Point3 point = camera.getViewCenter();
            double x = point.x - xDelta;
            double y = point.y - yDelta;
            camera.setViewCenter(x, y, 0);
        }

        private Point3 point3(Point point) {
            Camera camera = view.getCamera();
            return camera.transformPxToGu(point.x, point.y);
        }

        @Override
        public void buttonPushed(String id) {

            try {
                Node node = displayedGraph.getNode(Integer.parseInt(id));
                if (node != null) {
                    String sourceLine = node.toString();
                    selectedNode.add(sourceLine);
                    node.addAttribute("ui.style", "shape:circle;fill-color:red;size: 25px;text-alignment: center;");
                }

            } catch (NumberFormatException ex) {
                System.out.println("OKAY! Something went wrong."
                        + "\nFeel free to report this exception.");
            }

        }

        @Override
        public void buttonReleased(String id) {

        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Runtime.getRuntime().freeMemory();
            String command = arg0.getActionCommand();
            if (command.equals("All")) {
                if (!selectedNode.isEmpty()) {
                    showReactions(selectedNode.get(0), null);
                }
            } else {
                if (!selectedNode.isEmpty()) {
                    String reaction = command.split(" - ")[0];
                    showReactions(selectedNode.get(0), reaction);
                }
            }
        }

        @Override
        public void viewClosed(String arg0) {
            viewPanel.setVisible(Boolean.FALSE);
        }
    }

    public static void zoomGraphMouseWheelMoved(MouseWheelEvent mwe, ViewPanel view_panel) {
        if (KeyEvent.ALT_DOWN_MASK != 0) {
            if (mwe.getWheelRotation() > 0) {
                double new_view_percent = view_panel.getCamera().getViewPercent() + 0.05;
                view_panel.getCamera().setViewPercent(new_view_percent);
            } else if (mwe.getWheelRotation() < 0) {
                double current_view_percent = view_panel.getCamera().getViewPercent();
                if (current_view_percent > 0.05) {
                    view_panel.getCamera().setViewPercent(current_view_percent - 0.05);
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == '\u0008' || e.getKeyChar() == '\u007F') {
            this.selectedNode.forEach((nodeId) -> {
                try {
                    Node node = g.getNode(nodeId);
                    g.removeNode(node);

                    String name = node.getId().split(" - ")[0];
                    if (name.contains(" : ")) {
                        name = name.split((" : "))[0];
                    }
                    if (m.getReaction(name) != null) {
                        this.m.removeReaction(name);
                    }

                    this.antGraph.removeNode(name);
                } catch (Exception ex) {
                }
            });
        }
        if (e.getKeyChar() == 's') {
            this.g.addAttribute("ui.screenshot", "/home/scsandra/Downloads/screenshot.png");
        }

        if (e.getKeyChar() == 'c') {
            if (this.cofactorActivation) {
                removeCofactors();
                this.cofactorActivation = Boolean.FALSE;
            } else {
                addCofactors();
                this.cofactorActivation = Boolean.TRUE;
            }
        }

        if (e.getKeyChar() == 'l') {
            if (this.layoutActivation) {
                viewer.disableAutoLayout();
                this.layoutActivation = Boolean.FALSE;
            } else {
                viewer.enableAutoLayout();
                this.layoutActivation = Boolean.TRUE;
            }
        }
    }

    private void removeCofactors() {
        for (Node node : this.g.getEachNode()) {
            String name = node.getId().split(" - ")[0];
            if (name.contains(" : ")) {
                name = name.split((" : "))[0];
            }
            if (this.cofactors.contains(name)) {
                node.addAttribute("ui.hide");
                for (Edge e : node.getEachEdge()) {
                    e.addAttribute("ui.hide");
                }
            }

        }
    }

    private void addCofactors() {
        for (Node node : this.g.getEachNode()) {
            String name = node.getId().split(" - ")[0];
            if (name.contains(" : ")) {
                name = name.split((" : "))[0];
            }
            if (this.cofactors.contains(name)) {
                node.removeAttribute("ui.hide");
                for (Edge e : node.getEachEdge()) {
                    e.removeAttribute("ui.hide");
                }
            }

        }
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
    }

    private String isThere(String node) {
        for (Node v : this.g.getEachNode()) {
            if (v.getId().contains(node)) {
                return v.getId();
            }
        }
        return null;
    }

    private void showReactions(String initialStringNode, String reaction) {

        // Gets the selected model. It is the source model for the reactions
        Dataset dInit = NDCore.getDesktop().getSelectedDataFiles()[0];
        Model mInit = dInit.getDocument().getModel();
        String spID = initialStringNode;

        if (initialStringNode.contains(" : ")) {
            spID = initialStringNode.split(" : ")[0];
        }

        AntNode initNode = this.antGraph.getNode(spID);

        Species sp = mInit.getSpecies(spID);
        if (sp == null) {
            return;
        }

        for (Reaction r : mInit.getListOfReactions()) {

            if (reaction != null && !reaction.contains(r.getId())) {
                continue;
            }
            if (r.getReactantForSpecies(sp.getId()) != null || r.getProductForSpecies(sp.getId()) != null) {
                double lb = dInit.getLowerBound(r.getId());
                double ub = dInit.getUpperBound(r.getId());
                Double flux = dInit.getFlux(r.getId());

                // adds the new reaction node with and edge from the extended node
                String reactionName;
                if (flux != null) {
                    DecimalFormat df = new DecimalFormat("#.####");
                    reactionName = r.getId() + " : " + df.format(flux) + " - " + uniqueId.nextId();
                } else {
                    reactionName = r.getId() + " - " + uniqueId.nextId();
                }

                //  String initSPName = sp.getId() + " : " + sp.getName() + " - " + uniqueId.nextId();
                String initSPName = initialStringNode;

                String gnode = this.isThere(r.getId());

                // adds the rest of the compounds in the reaction, the direction of the edges 
                // should depend on the boundaries of the reaction
                if (gnode == null) {

                    //adds the new reaction to the new model
                    if (m.getReaction(r.getId()) == null) {
                        AddReaction(r);
                    }
                    // adds the new reaction to the visualization graph
                    Node rNode = g.addNode(reactionName);
                    rNode.addAttribute("ui.style", "shape:box;fill-color:green;size: 25px;text-alignment: center;text-size:12px;");
                    rNode.addAttribute("ui.label", reactionName);

                    // Creates the node for the ANT graph
                    AntNode reactionNode = new AntNode(reactionName);
                    antGraph.addNode(reactionNode);

                    Boolean eType = Boolean.FALSE;
                    boolean direction = false;
                    if (lb == 0 || ub == 0) {
                        eType = Boolean.TRUE;
                        direction = true;
                    }

                    for (SpeciesReference sr : r.getListOfReactants()) {
                        Species sps = mInit.getSpecies(sr.getSpecies());
                        String spName = sps.getId();
                        String nodeReactant;
                        if (cofactors.contains(spName)) {
                            nodeReactant = null;
                        } else {
                            nodeReactant = this.isThere(spName);
                        }
                        if (nodeReactant == null) {
                            if (!spName.equals(spID)) {
                                String vName = spName + " : " + sps.getName() + " - " + uniqueId.nextId();
                                String eName = spName + " - " + uniqueId.nextId();

                                Node spNode = g.addNode(vName);
                                if (cofactors.contains(spName)) {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                                    spNode.addAttribute("ui.label", spName + " : " + sps.getName());
                                } else {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 25px;text-alignment: center;text-size:18px;text-style:bold;");
                                    spNode.addAttribute("ui.label", spName + " : " + sps.getName());
                                }
                                //adds the node to the graph
                                AntNode n = new AntNode(vName);
                                antGraph.addNode(n);
                                if (lb == 0) {
                                    g.addEdge(eName, vName, reactionName, eType);
                                    antGraph.addEdge(new AntEdge(eName, n, reactionNode, direction));
                                } else {
                                    g.addEdge(eName, reactionName, vName, eType);
                                    antGraph.addEdge(new AntEdge(eName, reactionNode, n, direction));
                                }
                            } else {
                                if (lb == 0) {
                                    String name = initSPName + uniqueId.nextId();
                                    g.addEdge(name, initialStringNode, reactionName, eType);
                                    antGraph.addEdge(new AntEdge(name, initNode, reactionNode, direction));
                                } else {
                                    String name = initSPName + uniqueId.nextId();
                                    g.addEdge(name, reactionName, initialStringNode, eType);
                                    antGraph.addEdge(new AntEdge(name, reactionNode, initNode, direction));
                                }
                            }
                        } else {

                            AntNode reactantNode = antGraph.getNode(spName);
                            String eName = spName + " - " + uniqueId.nextId();
                            if (lb == 0) {
                                g.addEdge(eName, nodeReactant, reactionName, eType);
                                antGraph.addEdge(new AntEdge(eName, reactantNode, reactionNode, direction));
                            } else {
                                g.addEdge(eName, reactionName, nodeReactant, eType);
                                antGraph.addEdge(new AntEdge(eName, reactionNode, reactantNode, direction));
                            }

                        }
                    }

                    for (SpeciesReference sr : r.getListOfProducts()) {
                        String spId = sr.getSpecies();
                        Species sps = mInit.getSpecies(spId);
                        String nodeProduct;
                        if (cofactors.contains(spId)) {
                            nodeProduct = null;
                        } else {
                            nodeProduct = this.isThere(spId);
                        }

                        if (nodeProduct == null) {

                            if (!spId.equals(spID)) {
                                String vName = spId + " : " + sps.getName() + " - " + uniqueId.nextId();
                                String eName = spId + " - " + uniqueId.nextId();

                                Node spNode = g.addNode(vName);
                                if (cofactors.contains(spId)) {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                                    spNode.addAttribute("ui.label", spId + " : " + sps.getName());
                                } else {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 25px;text-alignment: center;text-size:18px;text-style:bold;");
                                    spNode.addAttribute("ui.label", spId + " : " + sps.getName());
                                }
                                //adds the node to the graph
                                AntNode n = new AntNode(vName);
                                antGraph.addNode(n);
                                if (lb == 0) {
                                    g.addEdge(eName, reactionName, vName, eType);
                                    antGraph.addEdge(new AntEdge(eName, reactionNode, n, direction));
                                } else {
                                    g.addEdge(eName, vName, reactionName, eType);
                                    antGraph.addEdge(new AntEdge(eName, n, reactionNode, direction));
                                }
                            } else {
                                if (lb == 0) {
                                    String eName = spId + " - " + uniqueId.nextId();
                                    g.addEdge(eName, initialStringNode, reactionName, eType);
                                    antGraph.addEdge(new AntEdge(eName, initNode, reactionNode, direction));

                                } else {
                                    String eName = spId + " - " + uniqueId.nextId();
                                    g.addEdge(eName, reactionName, initialStringNode, eType);
                                    antGraph.addEdge(new AntEdge(eName, reactionNode, initNode, direction));
                                }
                            }
                        } else {
                            AntNode productNode = antGraph.getNode(spId);
                            String eName = spId + " - " + uniqueId.nextId();
                            if (lb == 0) {
                                g.addEdge(eName, reactionName, nodeProduct, eType);
                                antGraph.addEdge(new AntEdge(eName, reactionNode, productNode, direction));
                            } else {
                                g.addEdge(eName, nodeProduct, reactionName, eType);
                                antGraph.addEdge(new AntEdge(eName, productNode, reactionNode, direction));
                            }
                        }
                    }
                }
            }
        }

    }

    private void AddReaction(Reaction reaction) {
        Dataset datasetInit = NDCore.getDesktop().getSelectedDataFiles()[0];
        Model mInit = datasetInit.getDocument().getModel();

        Reaction r = new Reaction(reaction);
        //r.setId(reaction.getId());
        //r.setName(reaction.getName());

        reaction.getListOfReactants().forEach((sp) -> {
            SpeciesReference spref = r.createReactant();
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

        // r.appendNotes(reaction.getNotes());
        m.addReaction(reaction.clone());
        this.dataset.setLowerBound(r.getId(), datasetInit.getLowerBound(r.getId()));
        this.dataset.setUpperBound(r.getId(), datasetInit.getUpperBound(r.getId()));
    }

}
