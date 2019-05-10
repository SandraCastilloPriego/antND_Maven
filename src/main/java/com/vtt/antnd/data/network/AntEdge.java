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

import org.jgrapht.graph.DefaultEdge;


/**
 *
 * @author scsandra
 */
public class AntEdge extends DefaultEdge{

        private String id;
        private AntNode source;
        private AntNode destination;
        private boolean directional;
        
               
        public AntEdge(String id, AntNode source, AntNode destination) {
                this.directional = true;
                this.id = id;
                this.source = source;
                this.destination = destination;
        }
        
         public AntEdge(String id, AntNode source, AntNode destination, boolean directional) {                
                this.id = id;
                this.source = source;
                this.destination = destination;
                this.directional = directional;
        }
        
        public void setDirection(boolean directional){
            this.directional = directional;
        }
        
        public boolean getDirection(){
            return this.directional;
        }

        public String getId() {
                return id;
        }

        public AntNode getDestination() {
                return destination;
        }

        public AntNode getSource() {
                return source;
        }

        @Override
        public String toString() {
                return source + " " + destination;
        }
        
        public void setSource(AntNode source){
                this.source = source;
        }
        
        public void setDestination(AntNode destination){
                this.destination = destination;
        }
        
        public void setId(String id){
                this.id = id;
        }       
      
        @Override
        public AntEdge clone(){
                AntEdge e = new AntEdge(this.id, this.source, this.destination);
                return e;
        }
             
}
