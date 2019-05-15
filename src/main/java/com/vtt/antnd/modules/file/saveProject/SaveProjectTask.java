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
package com.vtt.antnd.modules.file.saveProject;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.antSimData.SpeciesFA;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.util.StreamCopy;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import de.schlichtherle.truezip.zip.ZipEntry;
import de.schlichtherle.truezip.zip.ZipOutputStream;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.Objective;

/**
 *
 * @author scsandra
 */
public class SaveProjectTask extends AbstractTask {

    private File file;
    private double finishedPercentage = 0.0f;
    private int i;

    public SaveProjectTask(File file) {
        if (file != null) {
            this.file = file;
        }
    }

    @Override
    public String getTaskDescription() {
        return "Saving Project... ";
    }

    @Override
    public double getFinishedPercentage() {
        return finishedPercentage;
    }

    @Override
    public void cancel() {
        setStatus(TaskStatus.CANCELED);
    }

    @Override
    public void run() {
        try {
            setStatus(TaskStatus.PROCESSING);
            File tempFile = File.createTempFile(file.getName(), ".tmp",
                    file.getParentFile());
            tempFile.deleteOnExit();
            // Create a ZIP stream writing to the temporary file
            FileOutputStream tempStream = new FileOutputStream(tempFile);
            try ( ZipOutputStream zipStream = new ZipOutputStream(tempStream)) {
                saveSBMLFiles(zipStream);
                saveHistory(zipStream);
                //savePaths(zipStream);
                zipStream.close();
            }
            boolean renameOK = tempFile.renameTo(file);
            if (!renameOK) {
                throw new IOException("Could not move the temporary file "
                        + tempFile + " to the final location " + file);
            }

            setStatus(TaskStatus.FINISHED);
        } catch (IOException e) {
            setStatus(TaskStatus.ERROR);
            errorMessage = e.toString();
        }

    }

    private void saveSBMLFiles(ZipOutputStream zipStream) throws IOException {
        Dataset[] selectedFiles = NDCore.getDesktop().getAllDataFiles();

        for (final Dataset datafile : selectedFiles) {
            if (datafile != null) {
                zipStream.putNextEntry(new ZipEntry(datafile.getDatasetName()));
                File tempFile = File.createTempFile(datafile.getDatasetName(), ".tmp");
                SBMLWriter writer = new SBMLWriter();
                try {
                    OutputStream out = new FileOutputStream(new File(tempFile.getAbsolutePath()));
                    try {
                        this.setObjective(datafile);
                        datafile.getDocument().removeDeclaredNamespaceByNamespace(datafile.getDocument().getNamespace());
                        writer.writeSBMLToFile(datafile.getDocument(), tempFile.getAbsolutePath());
                    } catch (FileNotFoundException | XMLStreamException ex) {
                        Logger.getLogger(SaveProjectTask.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (SBMLException ex) {
                    Logger.getLogger(SaveProjectTask.class.getName()).log(Level.SEVERE, null, ex);
                }

                try ( FileInputStream fileStream = new FileInputStream(tempFile)) {
                    StreamCopy copyMachine = new StreamCopy();
                    copyMachine.copy(fileStream, zipStream);
                }
                tempFile.delete();
                finishedPercentage = ((double) i++ / selectedFiles.length) / 2;
            }

        }

    }

    private void saveHistory(ZipOutputStream zipStream) throws IOException {
        Dataset[] selectedFiles = NDCore.getDesktop().getAllDataFiles();
        for (final Dataset datafile : selectedFiles) {
            boolean isParent = datafile.isParent();
            String parent = datafile.getParent();
            String info = datafile.getInfo().getText();
            String biomass = "";//datafile.getBiomassId();
            List<String> sources = datafile.getSources();
            AntGraph graph = datafile.getGraph();
            zipStream.putNextEntry(new ZipEntry(datafile.getDatasetName() + ".info"));
            File tempFile = File.createTempFile(datafile.getDatasetName() + "-info", ".tmp");

            BufferedWriter writer = null;
            // try {
            writer = new BufferedWriter(new FileWriter(tempFile.getAbsoluteFile()));

            // write fluxes          
            for (Reaction r : datafile.getDocument().getModel().getListOfReactions()) {
                writer.write("\nFluxes=" + r.getId() + " // " + datafile.getFlux(r.getId()));
            }
            writer.write("\n");
            if (isParent) {
                writer.write("Is Parent");
            } else {
                writer.write("Not Parent: " + parent);
            }
            writer.write("\n" + info);
            if (biomass != null) {
                writer.write("\nBiomass= " + biomass);
            }
            if (sources != null) {
                for (String source : sources) {
                    writer.write("\nSources= " + source);
                }
            }
            if (graph != null) {
                System.out.println("Graph" + graph.getNodes().size());
                for (AntNode node : graph.getNodes()) {
                    try {
                        String name = " ";
                        if (node.getName() != null) {
                            name = node.getName();
                            name = name.trim();
                        }
                        if (node.getPosition() != null) {
                            Point2D position = node.getPosition();
                            try {
                                writer.write("\nNodes= " + node.getId() + " : " + name + " // " + position.getX() + " , " + position.getY());
                            } catch (Exception e) {
                                System.out.print("Something is wrong putting Nodes");
                                writer.write("\nNodes= " + node.getId() + " : " + name + " // null");
                            }
                        } else {
                            try {
                                // System.out.print("No position in Nodes" + node.getId());
                                writer.write("\nNodes= " + node.getId() + " : " + name + " // null");
                            } catch (Exception eere) {
                                System.out.println("Node missing");
                            }
                        }
                    } catch (Exception nodeEx) {
                        System.out.println("Node failed");
                    }
                }
                for (AntEdge edge : graph.getEdges()) {
                    try {
                        writer.write("\nEdges= " + edge.getId() + " // " + edge.getSource().getId() + " || " + edge.getDestination().getId() + " // " + String.valueOf(edge.getDirection()));
                    } catch (Exception e) {
                        System.out.print("Something is wrong putting Edges");
                    }
                }
            }
            // } catch (IOException e) {
            //    System.out.println(e.toString());
            //} finally {
            //   try {
            if (writer != null) {
                writer.close();
            }
            //    } catch (IOException e) {
            //   }
            //}

            try ( FileInputStream fileStream = new FileInputStream(tempFile)) {
                StreamCopy copyMachine = new StreamCopy();
                copyMachine.copy(fileStream, zipStream);
            }
            tempFile.delete();
            finishedPercentage = ((double) i++ / selectedFiles.length) / 2;
        }
    }

    private void savePaths(ZipOutputStream zipStream) throws IOException {
        Dataset[] selectedFiles = NDCore.getDesktop().getAllDataFiles();
        for (final Dataset datafile : selectedFiles) {
            if (datafile.getPath() != null) {
                HashMap<String, SpeciesFA> compounds = datafile.getPaths();
                zipStream.putNextEntry(new ZipEntry(datafile.getDatasetName() + ".paths"));
                File tempFile = File.createTempFile(datafile.getDatasetName() + "-paths", ".tmp");

                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new FileWriter(tempFile.getAbsoluteFile()));
                    for (String c : compounds.keySet()) {
                        SpeciesFA compound = compounds.get(c);
                        if (compound.getAnt() != null) {
                            writer.write(compound.getId() + " - " + compound.getName() + " : ");
                            Map<String, Boolean> path = compound.getAnt().getPath();
                            for (String r : path.keySet()) {
                                writer.write(r + " - " + path.get(r) + ",");
                            }
                            writer.write("\n");
                        }
                    }
                } catch (Exception e) {
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                    } catch (IOException e) {
                    }
                }

                try ( FileInputStream fileStream = new FileInputStream(tempFile)) {
                    StreamCopy copyMachine = new StreamCopy();
                    copyMachine.copy(fileStream, zipStream);
                }
                tempFile.delete();
                finishedPercentage = ((double) i++ / selectedFiles.length) / 2;
            }
        }
    }

    private void removeObjective(String id, Model model) {
        FBCModelPlugin plugin = (FBCModelPlugin) model.getPlugin("fbc");
        for (Objective obj : plugin.getListOfObjectives()) {
            for (FluxObjective fobj : obj.getListOfFluxObjectives()) {
                if (fobj.getReaction().equals(id)) {
                    obj.removeFluxObjective(fobj);
                    break;
                }
            }
        }

    }

    private void setObjective(Dataset data) {
        Model m = data.getDocument().getModel();
        for (Reaction r : m.getListOfReactions()) {
            Double objval = data.getObjective(r.getId());
            if (objval != null && objval != 0) {
                removeObjective(r.getId(), m);
                FBCModelPlugin plugin = (FBCModelPlugin) m.getPlugin("fbc");
                if (plugin != null) {
                    FluxObjective fx = new FluxObjective();
                    fx.setReaction(r);
                    fx.setCoefficient(objval);
                    Objective.Type type = Objective.Type.MAXIMIZE;
                    if (objval < 0) {
                        type = Objective.Type.MINIMIZE;
                    }
                    Objective objf = null;
                    for (Objective obj : plugin.getListOfObjectives()) {
                        for (FluxObjective fobj : obj.getListOfFluxObjectives()) {
                            if (fobj.getReaction().equals(r.getId())) {
                                fobj.setCoefficient(objval);
                                objf = obj;
                                break;
                            }
                        }
                    }
                    if (objf == null) {
                        objf = plugin.createObjective("obj" + r.getId(), type);
                        objf.addFluxObjective(fx);
                        plugin.setActiveObjective(objf);
                    }
                }
            } else if (objval != null) {

                FBCModelPlugin plugin = new FBCModelPlugin(m);
                Objective.Type type = Objective.Type.MAXIMIZE;
                if (objval < 0) {
                    type = Objective.Type.MINIMIZE;
                }
                Objective obj = plugin.createObjective("obj" + r.getId(), type);
                FluxObjective fx = new FluxObjective();
                fx.setReaction(r);
                fx.setCoefficient(objval);
                obj.addFluxObjective(fx);
                plugin.setActiveObjective(obj);

            }
        }
    }
}
