/*
 * Copyright 2007-2012 
 *
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
package com.vtt.antnd.main;

import com.vtt.antnd.modules.analysis.CycleDetection.CycleDetectorModule;
import com.vtt.antnd.modules.analysis.KNeighborhood.KNeighborhoodModule;
import com.vtt.antnd.modules.analysis.clusteringBetweenness.ClusteringBetweennessModule;
import com.vtt.antnd.modules.analysis.clusteringBicomponent.ClusteringBicomponentModule;
import com.vtt.antnd.modules.analysis.clusteringkmeans.ClusteringModule;
import com.vtt.antnd.modules.configuration.cofactors.CofactorConfModule;
import com.vtt.antnd.modules.file.openProject.OpenProjectModule;
import com.vtt.antnd.modules.file.openfile.OpenBasicFileModule;
import com.vtt.antnd.modules.file.saveProject.SaveProjectModule;
import com.vtt.antnd.modules.pathfinders.somePaths.SomePathsModule;
import com.vtt.antnd.modules.pathfinders.superAnt.SuperAntModule;
import com.vtt.antnd.modules.reactionOp.compareModels.CompareModule;
import com.vtt.antnd.modules.reactionOp.showAllCompoundList.ShowAllCompoundsModule;
import com.vtt.antnd.modules.reactionOp.showAllReaction.ShowAllReactionsModule;
import com.vtt.antnd.modules.reactionOp.showCompound.ShowCompoundModule;
import com.vtt.antnd.modules.reactionOp.showReaction.ShowReactionModule;
import com.vtt.antnd.modules.simulation.FBA.LPModule;
import com.vtt.antnd.modules.simulation.pFBA.LPModuleMinimize;
import com.vtt.antnd.modules.visualization.FBAVisualization.FluxAnalysisModule;
import com.vtt.antnd.modules.visualization.ReactionVisualization.ReactionVisualizationModule;



/**
 * List of modules included in MM
 */
public class NDModulesList {

    /**
     *
     */
    public static final Class<?> MODULES[] = new Class<?>[]{
        OpenBasicFileModule.class,
        OpenProjectModule.class,
        SaveProjectModule.class,
        CofactorConfModule.class,
        ShowCompoundModule.class,
        ShowAllCompoundsModule.class,
        ShowReactionModule.class,
        ShowAllReactionsModule.class,
        FluxAnalysisModule.class,
        CompareModule.class,        
        SuperAntModule.class,
        SomePathsModule.class,
        LPModule.class,
        LPModuleMinimize.class,        
        ClusteringBicomponentModule.class,
        ClusteringModule.class,        
        KNeighborhoodModule.class,
        ClusteringBetweennessModule.class,
        CycleDetectorModule.class,
        ReactionVisualizationModule.class
        
        };
}
