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
package com.vtt.antnd.data.network;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author scsandra
 */
public class AntGraph {

    private final List<AntNode> nodes;
    private final List<AntEdge> edges;

    public AntGraph(List<AntNode> nodes, List<AntEdge> edges) {
        if (nodes == null) {
            this.nodes = new ArrayList<>();
        } else {
            this.nodes = nodes;
        }

        if (edges == null) {
            this.edges = new ArrayList<>();
        } else {
            this.edges = edges;
        }
    }

    public List<AntNode> getNodes() {
        return nodes;
    }

    public AntNode getNode(String id) {
        for (AntNode n : nodes) {            
            if (n.getId().contains(id)) {                
                return n;
            }
        }
        return null;
    }

    public boolean contains(AntNode node) {
        for (AntNode n : nodes) {
            if (n.getId().equals(node.getId())) {
                return true;
            }
        }
        return false;
    }

    public List<AntEdge> getEdges() {
        return edges;
    }

    public void addNode2(AntNode node) {
        if (!IsInNodes(node)) {
            this.nodes.add(node);
        }
    }

    public void addNode(AntNode node) {
        this.nodes.add(node);
    }

    public void addEdge2(AntEdge edge) {
        if (!isInEdges(edge)) {
            this.edges.add(edge);
        }
    }

    public void addEdge(AntEdge edge) {
        this.edges.add(edge);
    }  

    public int getNumberOfNodes() {
        return this.nodes.size();
    }

    public int getNumberOfEdges() {
        return this.edges.size();
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public AntNode getLastNode() {
        return this.nodes.get(this.nodes.size() - 1);
    }

    @Override
    public String toString() {
        String str = "";
        for (AntNode n : this.nodes) {
            str = str + n.getId().split(" - ")[0] + " - ";
        }
        return str;
    }

    public void addGraph(AntGraph g) {
        if (this == g) {
            return;
        }
        for (AntNode n : g.getNodes()) {
            if (n.getId() != null) {
                if (!this.IsInNodes(n)) {
                    this.nodes.add(n);
                }
            }
        }
        for (AntEdge e : g.getEdges()) {
            AntNode source = e.getSource();
            if (source != null) {
                // System.out.println("source : "+ source.getId());
                AntNode newSource = this.getNode(source.getId().split(" - ")[0]);
                if (newSource != null) {
                    e.setSource(newSource);
                }
                AntNode destination = e.getDestination();
                // System.out.println("destination : "+ destination.getId());
                if (destination.getId() != null) {
                    AntNode newDestination = this.getNode(destination.getId().split(" - ")[0]);
                    if (newDestination != null) {
                        e.setDestination(newDestination);
                    }
                   // if (!this.isInEdges(e)) {

                    // }
                }
            }
            this.edges.add(e);
        }
    }

    @Override
    public AntGraph clone() {
        AntGraph g = new AntGraph(null, null);
        for (AntNode n : this.nodes) {
            g.addNode(n.clone());
        }
        for (AntEdge e : this.edges) {
            AntEdge edge = e.clone();
            AntNode source = g.getNode(edge.getSource().getId());
            AntNode destination = g.getNode(edge.getDestination().getId());
            edge.setSource(source);
            edge.setDestination(destination);
            g.addEdge(edge);
        }

        return g;
    }

    public boolean IsInNodes(AntNode node) {
        for (AntNode n : this.nodes) {
            if (n.getId().split(" - ")[0].split(" : ")[0].equals(node.getId().split(" - ")[0].split(" : ")[0])) {
                return true;
            }
        }
        return false;
    }

    public boolean IsInNodes(String node) {
        for (AntNode n : this.nodes) {
            if (n.getId().equals(node)) {
                return true;
            }
        }
        return false;
    }

    public boolean IsInSource(String node) {
        for (AntEdge e : this.edges) {
            AntNode n = e.getSource();
            if (n == null) {
                continue;
            }
            if (n.getId().split(" - ")[0].split(" : ")[0].equals(node.split(" - ")[0].split(" : ")[0])) {
                return true;
            }
        }
        return false;
    }

    public boolean isInEdges(AntEdge edge) {
        for (AntEdge thisEdge : this.edges) {
            String source = edge.getSource().getId().split(" - ")[0];
            String destination = edge.getDestination().getId().split(" - ")[0];
            String thisSource = thisEdge.getSource().getId().split(" - ")[0];
            String thisDestination = thisEdge.getDestination().getId().split(" - ")[0];
            if (source.equals(thisSource) && destination.equals(thisDestination)) {
                return true;
            }
        }
        return false;
    }

    public List<AntEdge> getEdges(String n, boolean source) {
        List<AntEdge> edgesN = new ArrayList<>();
        for (AntEdge e : this.edges) {
            if (source) {
                if (e.getSource().getId().contains(n)) {
                    edgesN.add(e);
                }
            } else {
                if (e.getDestination().getId().contains(n)) {
                    edgesN.add(e);
                }
            }
        }
        return edgesN;
    }

    public List<AntNode> getConnectedAsSource(AntNode n) {
        List<AntNode> connectedNodes = new ArrayList<>();
        for (AntEdge e : edges) {
            if (e.getSource() == n) {
                connectedNodes.add(n);
            }
        }
        return connectedNodes;
    }

    public List<AntNode> getConnectedAsDestination(AntNode n) {
        List<AntNode> connectedNodes = new ArrayList<>();
        for (AntEdge e : edges) {
            if (e.getDestination() == n) {
                connectedNodes.add(n);
            }
        }
        return connectedNodes;
    }

    public List<String> getDeadEnds() {
        List<String> deadEnds = new ArrayList<>();

        for (AntEdge e : edges) {
            boolean isSource = false;
            AntNode destination = e.getDestination();
            if (destination.getId().contains("extracellular") || destination.getId().contains("boundary")) {
                continue;
            }
            for (AntEdge e2 : edges) {
                if (e2.getSource() != null && e2.getSource().getId().split(" - ")[0].equals(destination.getId().split(" - ")[0])) {
                    isSource = true;
                }
            }

            if (!isSource && !deadEnds.contains(destination.getId().split(" - ")[0])) {
                deadEnds.add(destination.getId().split(" - ")[0]);
            }
        }
        
        for (AntEdge e : edges) {
            boolean isSource = false;
            AntNode destination = e.getSource();
            if (destination == null) continue;
            if (destination.getId().contains("extracellular") || destination.getId().contains("boundary")) {
                continue;
            }
            for (AntEdge e2 : edges) {
                if (e2.getDestination() != null && e2.getDestination().getId().split(" - ")[0].equals(destination.getId().split(" - ")[0])) {
                    isSource = true;
                }
            }

            if (!isSource && !deadEnds.contains(destination.getId().split(" - ")[0])) {
                deadEnds.add(destination.getId().split(" - ")[0]);
            }
        }
        return deadEnds;
    }

    public void removeNode(String node) {
        List<AntNode> toBeRemove = new ArrayList<>();
        List<AntEdge> toBeRemoveEdge = new ArrayList<>();

        for (AntNode n : this.nodes) {
            if (n.getId().contains(node)) {
                toBeRemove.add(n);
            }
        }
        for (AntNode r : toBeRemove) {
            this.nodes.remove(r);
            for (AntEdge e : this.edges) {
                if (e.getSource() == r || e.getDestination() == r) {
                    toBeRemoveEdge.add(e);
                }
            }
        }
        for(AntEdge e : toBeRemoveEdge){
            this.edges.remove(e);
        }
    }


}
