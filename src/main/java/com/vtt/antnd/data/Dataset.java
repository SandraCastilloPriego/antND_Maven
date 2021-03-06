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
package com.vtt.antnd.data;


import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.data.antSimData.SpeciesFA;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;


/**
 * Interface for data set
 *
 * @author scsandra
 */
public interface Dataset {

        public SBMLDocument getDocument();

        public void setDocument(SBMLDocument document);

        /**
         * Constructs an exact copy of it self and returns it.
         *
         * @return Exact copy of itself
         */
        public Dataset clone();

        /**
         * Sets data set ID
         *
         */
        public void setID(int ID);

        /**
         * Returns data set ID
         *
         * @return data set ID
         */
        public int getID();

        /**
         * Every dataset has a name to allow the user to identify it Returns the
         * name of the data set.
         *
         * @return Name of the data set
         */
        public String getDatasetName();

        /**
         * Every dataset has a path to allow the user to identify it Returns the
         * name of the data set.
         *
         * @return path of the data set
         */
        public String getPath();

        public void setPath(String path);

        /**
         * Sets the name of the dataset.
         *
         * @param Name of the dataset
         */
        public void setDatasetName(String name);

        /**
         * The type of the data set can be LC-MS, GCxGC-Tof or others.
         *
         * @see ND.data.DatasetType
         *
         * @return DatasetType class
         */
        public DatasetType getType();

        /**
         * Sets the type of the data set. It can be LC-MS, GCxGC-Tof or others.
         *
         * @see ND.data.DatasetType
         *
         * @param type DatasetType
         */
        public void setType(DatasetType type);

        /**
         * Returns general information about the data set. It will be written by
         * the user.
         *
         * @return General information about the data set
         */
        public JTextArea getInfo();

        /**
         * Adds general information about the data set.
         *
         * @param info Information about the data set
         */
        public void addInfo(String info);
        
        public void setInfo(String info);

        public void setNodes(List<AntNode> nodes);

        public void setEdges(List<AntEdge> edges);

        public List<AntNode> getNodes();

        public List<AntEdge> getEdges();

        public void setSources(List<String> sources);
        
        public void addSource(String source);
       
        public List<String> getSources();
        
        public void setGraph(AntGraph graph);
        
        /**
         *
         * @return
         */
        public AntGraph getGraph();
        
        public void SetCluster(boolean isCluster);
        
        public boolean isCluster();
        
        public void setPaths(Map<String, SpeciesFA> paths);
        
        public HashMap<String, SpeciesFA> getPaths();
        
        public void setReactionsFA(Map<String, ReactionFA> reactions);
        
        public HashMap<String, ReactionFA> getReactionsFA();
        
        public void setSourcesMap(Map<String, Double[]> sources);
        
        public Map<String, Double[]> getSourcesMap();
        
        public void setCofactors(List<String> cofactor);
        
        public List<String> getCofactors();

        public boolean isReactionSelected(Reaction reaction);
        
        public boolean isMetaboliteSelected(Species species);
        
        public void setReactionSelectionMode(String reaction);
        
        public void setMetaboliteSelectionMode(String species);
        
        public String getParent();
        
        public void setParent(String dataset);
        
        public boolean isParent();  
        
        public void setIsParent(boolean isParent);
        
        public void setFlux(String reaction, Double flux);
        
        public Double getFlux(String reaction);
        
        public Double getLowerBound(String reaction);
        
        public Double getUpperBound(String reaction);
        
        public void setLowerBound(String reaction, Double bound);
        
        public void setUpperBound(String reaction, Double bound);  
        
        public void setObjective(String reaction, Double obj);
        
        public Double getObjective(String reaction);
      
}
