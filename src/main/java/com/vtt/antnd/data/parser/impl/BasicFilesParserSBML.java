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
package com.vtt.antnd.data.parser.impl;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.parser.Parser;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

/**
 *
 * @author scsandra
 */
public class BasicFilesParserSBML implements Parser {

    private final String datasetPath;
    private final SimpleBasicDataset dataset;
    private final int rowsNumber;
    private final int rowsReaded;
    private SBMLDocument document;
    private SBMLReader reader;

    public BasicFilesParserSBML(String datasetPath) {
        this.rowsNumber = 0;
        this.rowsReaded = 0;
        this.datasetPath = datasetPath;
        dataset = new SimpleBasicDataset();
    }

   /* static {
        try {
            System.loadLibrary("sbmlj");
            /* Extra check to be sure we have access to libSBML: */
          /*  Class.forName("org.sbml.libsbml.libsbml");
        } catch (Exception e) {
            System.err.println("Error: could not load the libSBML library");
            System.exit(1);
        }
    }*/

    @Override
    public void createDataset(String name) {
        try {
            SBMLReader reader = new SBMLReader();
            this.document = reader.readSBML(this.datasetPath);
            dataset.setDocument(document);
            dataset.setDatasetName(name);
            dataset.setPath(this.datasetPath);
            dataset.setIsParent(true);
           
            if (document.getNumErrors() > 0) {                
                System.exit(1);
            }
        } catch (XMLStreamException ex) {
            Logger.getLogger(BasicFilesParserSBML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BasicFilesParserSBML.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public float getProgress() {
        return (float) rowsReaded / rowsNumber;
    }

    @Override
    public Dataset getDataset() {
        return this.dataset;
    }

}
