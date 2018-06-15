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
package com.vtt.antnd.modules.analysis.reports;


import com.vtt.antnd.parameters.Parameter;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.parameters.parametersType.FileNameParameter;
import org.w3c.dom.Element;

public class ReportFBAParameters extends SimpleParameterSet {     
   

        public static final FileNameParameter fileName = new FileNameParameter(
                "Report path", "Path where the report will be saved", null);
       
       

        public ReportFBAParameters() {
                super(new Parameter[]{fileName});
        }
        
        @Override
        public void loadValuesFromXML(Element xmlElement) {
		
	}

        @Override
	public void saveValuesToXML(Element xmlElement) {
		
	}
}
