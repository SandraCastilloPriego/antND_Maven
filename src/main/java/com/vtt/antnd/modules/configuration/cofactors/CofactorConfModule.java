/*
 * Copyright 2007-2013 VTT Biotechnology
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
package com.vtt.antnd.modules.configuration.cofactors;

import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.NDModuleCategory;
import com.vtt.antnd.modules.NDProcessingModule;
import com.vtt.antnd.parameters.ParameterSet;
import com.vtt.antnd.util.taskControl.Task;



/**
 *
 * @author scsandra
 */
public class CofactorConfModule implements NDProcessingModule {

        public static final String MODULE_NAME = "Cofactor configuration";
        private final CofactorConfParameters parameters = new CofactorConfParameters();

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
                NDCore.setCofactors((CofactorConfParameters) parameters);     
                return null;
        }

        @Override
        public NDModuleCategory getModuleCategory() {
                return NDModuleCategory.CONFIGURATION;
        }

        @Override
        public String getIcon() {
                return "icons/cofactor.png";
        }

        @Override
        public boolean setSeparator() {
                return false;
        }
}
