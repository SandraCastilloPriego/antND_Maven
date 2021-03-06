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
package com.vtt.antnd.modules.simulation.pFBA;

import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.NDModuleCategory;
import com.vtt.antnd.modules.NDProcessingModule;
import com.vtt.antnd.parameters.ParameterSet;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.Task;



/**
 *
 * @author scsandra
 */
public class LPModuleMinimize implements NDProcessingModule {

        public static final String MODULE_NAME = "pFBA";
        private final LPParameters parameters = new LPParameters();

        @Override
        public ParameterSet getParameterSet() {
               // return parameters;
            return null;
        }

        @Override
        public String toString() {
                return MODULE_NAME;
        }

        @Override
        public Task[] runModule(ParameterSet parameters) {

                // prepare a new group of tasks
                Task tasks[] = new LPTask[NDCore.getDesktop().getSelectedDataFiles().length];
        if (NDCore.getDesktop().getSelectedDataFiles().length == 0) {
            NDCore.getDesktop().displayErrorMessage("You need to select a metabolic model.");
        } else {
            for (int i = 0; i < NDCore.getDesktop().getSelectedDataFiles().length; i++) {
                tasks[i] = new LPTask((SimpleBasicDataset) NDCore.getDesktop().getSelectedDataFiles()[i], (SimpleParameterSet) parameters);
            }
            NDCore.getTaskController().addTasks(tasks);
        }

        return tasks;
        }

        @Override
        public NDModuleCategory getModuleCategory() {
                return NDModuleCategory.SIMULATION;
        }

        @Override
        public String getIcon() {
                return "icons/FBA.png";
        }

        @Override
        public boolean setSeparator() {
                return true;
        }
}
