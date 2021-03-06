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
package com.vtt.antnd.modules.pathfinders.superAnt;


import com.vtt.antnd.data.antSimData.Ant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author scsandra
 */
public class SpeciesFA {

    private final String id;
    private final List<String> reactions;
    private Ant shortestAnt;
    private double pool = 0;
    private final String name;

    public SpeciesFA(String id, String name) {
        this.id = id;
        this.name = name;
        this.reactions = new ArrayList<>();
    }

    public Ant getAnt() {
        return this.shortestAnt;
    }

    public String getName() {
        return this.name;
    }

    public void addAnt(Ant ant) {
         if(this.shortestAnt == null || ant.getPathSize() < this.shortestAnt.getPathSize()){
             this.shortestAnt = ant;
         }    
    }

    public void addReaction(String id) {
        this.reactions.add(id);
    }

    public String getId() {
        return this.id;
    }

    public List<String> getReactions() {
        return this.reactions;
    }

   
    public double getPool() {
        return this.pool;
    }

    public void setPool(double pool) {
        this.pool = pool;
    }

   
    public Map<String, Boolean> getShortest() {
        return this.shortestAnt.getPath();
    }

    public double getFlux() {
        return 0.0;
    }

    public void setAnt(Ant ant) {
        this.shortestAnt = ant;
    }
}
