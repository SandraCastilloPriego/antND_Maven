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

import com.vtt.antnd.parameters.Parameter;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.parameters.parametersType.IntegerParameter;
import com.vtt.antnd.parameters.parametersType.StringParameter;




public class KNeighborhoodParameters extends SimpleParameterSet {     
   

        public static final IntegerParameter radiusK = new IntegerParameter(
                "Radius", "The neighborhood radius around the root set", 2);
        public static final StringParameter rootNode = new StringParameter(
                "Root node", "The root node", ""
        );
       // public static final StringParameter excluded = new StringParameter(
        //        "Excluded compounds:", "ID of the compounds that will be excluded"); 

        public KNeighborhoodParameters() {
                super(new Parameter[]{radiusK, rootNode});
        }
}
