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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
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
    private Dataset dataset;
    Map<String, Color> clusters;
    private boolean showInfo = false;
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

        for (AntNode node : nodes) {
            if (node != null) {
                String name = node.getCompleteId();
                String n = name.split(" - ")[0];
                String r = name.split(" : ")[0];
                g.addNode(name);
                if (node.getColor() != null) {
                    colors.put(name, node.getColor());
                }
                Node gNode = g.getNode(name);
                if (m.getReaction(r.trim()) != null || m.getReaction(name.trim()) != null) {
                    gNode.addAttribute("ui.style", "shape:box;fill-color:green;size: 15px;text-alignment: center;text-size:12px;");
                    gNode.addAttribute("ui.label", r);
                } else if (this.cofactors.contains(r)) {
                    gNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                    gNode.addAttribute("ui.label", n);
                } else {
                    gNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 15px;text-alignment: center;text-size:18px;text-style:bold;");
                    gNode.addAttribute("ui.label", n);

                }

            }
        }
        for (AntEdge edge : edges) {
            if (edge != null) {
                try {
                    if (edge.getDirection()) {
                        g.addEdge(edge.getId(), edge.getSource().getCompleteId(), edge.getDestination().getCompleteId(), Boolean.TRUE);
                    } else {
                        g.addEdge(edge.getId(), edge.getSource().getCompleteId(), edge.getDestination().getCompleteId(), Boolean.FALSE);
                    }
                } catch (Exception e) {
                }
            }
        }
        viewer = g.display();
       // viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
        viewer.enableAutoLayout();

        viewPanel = viewer.addDefaultView(false);

        // Zoom
        viewPanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent mwe) {
                zoomGraphMouseWheelMoved(mwe, viewPanel);
            }
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
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showSaveDialog(topPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    saveImage(file.getAbsolutePath());
                }
            }
        });

        this.area = new JTextArea();
        
        JScrollPane sc = new JScrollPane(this.area);
        sc.setAutoscrolls(true);
        sc.setPreferredSize(new Dimension(300, 170));   
        topPanel.add(sc);
        
        topPanel.add(saveButton);
        topPanel.setPreferredSize(new Dimension(650, 200));
        topPanel.setBackground(Color.WHITE);
        viewPanel.add(topPanel);
        viewPanel.setPreferredSize(new Dimension(700,700));
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
            writer.println("Creator \"AntND\"");
            writer.println("Version	1.0");
            writer.println("graph\t[");
            Map<AntNode, String> indexes = new HashMap<>();
            List<AntNode> nodes = antGraph.getNodes();
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

            List<AntEdge> edges = antGraph.getEdges();

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
            previousPoint = point3(e.getPoint());
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
            /*boolean isShiftPressed = e.isShiftDown();
            if (!isShiftPressed) {
                curElement = view.findNodeOrSpriteAt(e.getX(), e.getY());
                if (curElement != null) {
                    mouseButtonPressOnElement(curElement, e);
                }

                previousPoint = point3(e.getPoint());
            }*/

            curElement = view.findNodeOrSpriteAt(e.getX(), e.getY());

            if (curElement != null) {
                mouseButtonPressOnElement(curElement, e);
                Node node = displayedGraph.getNode(curElement.getId());
                if (node != null) {                   
                    String sourceLine = node.toString();
                    area.setText(data(sourceLine));
                    selectedNode.add(sourceLine);
                    node.addAttribute("ui.style", "shape:circle;fill-color:red;size: 15px;text-alignment: center;");
                    System.out.println(sourceLine);

                }
                System.out.println(node.toString());

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
                        gNode.addAttribute("ui.style", "shape:box;fill-color:green;size: 15px;text-alignment: center;text-size:12px;");
                        gNode.addAttribute("ui.label", r);
                    } else if (cofactors.contains(r)) {
                        gNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                        gNode.addAttribute("ui.label", n);
                    } else {
                        gNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 15px;text-alignment: center;text-size:18px;text-style:bold;");
                        gNode.addAttribute("ui.label", n);

                    }
                }
            }

            if (e.isPopupTrigger()) {
                popupMenu = new JPopupMenu();
                Model mInit = null;
                try {
                    mInit = NDCore.getDesktop().getSelectedDataFiles()[0].getDocument().getModel();
                } catch (Exception ex2) {
                    return;
                }
                for (Node node : graph) {
                    if (node.hasAttribute("ui.selected")) {
                        String spID = (String) node.getId();

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
                    }
                }

                popupMenu.show(e.getComponent(), e.getX(), e.getY());

            }

        }

        @Override
        public void mouseReleased(MouseEvent e) {

            /* boolean isShiftPressed = e.isShiftDown();
            if (!isShiftPressed) {
                if (curElement != null) {
                    mouseButtonReleaseOffElement(curElement, e);
                    curElement = null;
                }
                return;
            }*/
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
                    node.addAttribute("ui.style", "shape:circle;fill-color:red;size: 15px;text-alignment: center;");
                    System.out.println(sourceLine);

                }
                System.out.println(node.toString());

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("OKAY! Something went wrong."
                        + "\nFeel free to report this exception."
                        + "\nContinue using the command menu... ");
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
                    System.out.println("command: " + reaction);
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
            for (String nodeId : this.selectedNode) {
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

            }
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
        if (this.cofactors.contains(node)) {
            return null;
        }
        for (Node v : this.g.getEachNode()) {
            if (v.getId().contains(node)) {
                return v.getId();
            }
        }
        return null;
    }

    private void showReactions(String initialStringNode, String reaction) {

        // Gets the selected model. It is the source model for the reactions
        Dataset dInit =NDCore.getDesktop().getSelectedDataFiles()[0];
        Model mInit =dInit.getDocument().getModel();
        System.out.println(initialStringNode);
        String spID = initialStringNode;

        if (initialStringNode.contains(" : ")) {
            spID = initialStringNode.split(" : ")[0];
        }
        System.out.println(spID);
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
                String reactionName = null;
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
                    rNode.addAttribute("ui.style", "shape:box;fill-color:green;size: 15px;text-alignment: center;text-size:12px;");
                    rNode.addAttribute("ui.label", r);

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
                        String nodeReactant = this.isThere(spName);

                        if (nodeReactant == null) {
                            if (!spName.equals(spID)) {
                                String vName = spName + " : " + sps.getName() + " - " + uniqueId.nextId();
                                String eName = spName + " - " + uniqueId.nextId();

                                Node spNode = g.addNode(vName);
                                if (cofactors.contains(spName)) {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                                    spNode.addAttribute("ui.label", sps.getName());
                                } else {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 15px;text-alignment: center;text-size:18px;text-style:bold;");
                                    spNode.addAttribute("ui.label", sps.getName());
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
                                    g.addEdge(initSPName, initialStringNode, reactionName, eType);
                                    antGraph.addEdge(new AntEdge(sp.getId(), initNode, reactionNode, direction));
                                } else {
                                    g.addEdge(initSPName, reactionName, initialStringNode, eType);
                                    antGraph.addEdge(new AntEdge(sp.getId(), reactionNode, initNode, direction));
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

                        String nodeProduct = this.isThere(spId);
                        if (nodeProduct == null) {

                            if (!spId.equals(spID)) {
                                String vName = spId + " : " + sps.getName() + " - " + uniqueId.nextId();
                                String eName = spId + " - " + uniqueId.nextId();

                                Node spNode = g.addNode(vName);
                                if (cofactors.contains(spId)) {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:pink;size: 10px;text-alignment: center;text-size:10px;");
                                    spNode.addAttribute("ui.label", sps.getName());
                                } else {
                                    spNode.addAttribute("ui.style", "shape:circle;fill-color:orange;size: 15px;text-alignment: center;text-size:18px;text-style:bold;");
                                    spNode.addAttribute("ui.label", sps.getName());
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
                                    g.addEdge(initSPName, initialStringNode, reactionName, eType);
                                    antGraph.addEdge(new AntEdge(sp.getId(), initNode, reactionNode, direction));

                                } else {
                                    g.addEdge(initSPName, reactionName, initialStringNode, eType);
                                    antGraph.addEdge(new AntEdge(sp.getId(), reactionNode, initNode, direction));
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
        r.setId(reaction.getId());
        r.setName(reaction.getName());
        System.out.println("Adding new reaction: " + reaction.getId());

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
        this.dataset.setLowerBound(r.getId(), datasetInit.getLowerBound(r.getId()));
        this.dataset.setUpperBound(r.getId(), datasetInit.getUpperBound(r.getId()));
        r.appendNotes(reaction.getNotes());

        m.addReaction(r);
    }

}
