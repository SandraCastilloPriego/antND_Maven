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
package com.vtt.antnd.modules.analysis.KNeighborhood;

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
public class KNeighborhoodModule implements NDProcessingModule {

        public static final String MODULE_NAME = "KNeighborhood Filter";
        private final KNeighborhoodParameters parameters = new KNeighborhoodParameters();

        @Override
        public ParameterSet getParameterSet() {
                return parameters;
        }

        @Override
        public String toString() {
                return MODULE_NAME;
        }

        @Override
        public Task[] runModule(ParameterSet parameters) {

                // prepare a new group of tasks
                Task tasks[] = new KNeighborhoodTask[1];
                SimpleBasicDataset dataset = null;
                try {
                        dataset = (SimpleBasicDataset) NDCore.getDesktop().getSelectedDataFiles()[0];
                } catch (Exception e) {
                }
                tasks[0] = new KNeighborhoodTask(dataset, (SimpleParameterSet) parameters);

                NDCore.getTaskController().addTasks(tasks);

                return tasks;
        }

        @Override
        public NDModuleCategory getModuleCategory() {
                return NDModuleCategory.ANALYSIS;
        }

        @Override
        public String getIcon() {
                return "icons/kneighborhood.png";
        }

        @Override
        public boolean setSeparator() {
                return false;
        }
}
