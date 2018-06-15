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
package com.vtt.antnd.modules.reactionOp.showAllReaction;


import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.sbml.libsbml.KineticLaw;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Parameter;
import org.sbml.libsbml.Reaction;
import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.SpeciesReference;


/**
 *
 * @author scsandra
 */
public class ShowReactionTask extends AbstractTask {

        private final SimpleBasicDataset networkDS;
        private double finishedPercentage = 0.0f;
        private final JInternalFrame frame;
        private final JScrollPane panel;
        private final JTextArea tf;
        private final StringBuffer info;

        public ShowReactionTask(SimpleBasicDataset dataset) {
                networkDS = dataset;
                this.frame = new JInternalFrame("Result", true, true, true, true);
                this.tf = new JTextArea();
                this.panel = new JScrollPane(this.tf);

                this.info = new StringBuffer();
        }

        @Override
        public String getTaskDescription() {
                return "Showing all reactions... ";
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
                        if (this.networkDS == null) {
                                setStatus(TaskStatus.ERROR);
                                NDCore.getDesktop().displayErrorMessage("You need to select a metabolic model.");
                        }

                        SBMLDocument doc = this.networkDS.getDocument();
                        Model m = doc.getModel();

                        info.append("The model contains ").append(m.getNumReactions()).append(" reactions:\n");
                        info.append("----------------------------------- \n");

                        this.showReactions(m.getListOfReactions(), m);
                        
                        ListOf reactions = m.getListOfReactions();
                        for (int i = 0; i < reactions.size(); i++) {
                                Reaction r = (Reaction) reactions.get(i);
                                info.append(r.getId()).append(" ; ").append(r.getName()).append("\n");
                        }

                        this.tf.setText(info.toString());
                        frame.setSize(new Dimension(700, 500));
                        frame.add(this.panel);
                        NDCore.getDesktop().addInternalFrame(frame);

                        setStatus(TaskStatus.FINISHED);
                } catch (Exception e) {
                        setStatus(TaskStatus.ERROR);
                        errorMessage = e.toString();
                }
        }

        private void showReactions(ListOf possibleReactions, Model m) {
                float count = 0;
                
                for (int e= 0; e < possibleReactions.size(); e++) {
                        Reaction r = (Reaction) possibleReactions.get(e);
                        try {
                                try {
                                        KineticLaw law = r.getKineticLaw();
                                        if (law != null) {
                                                Parameter lbound = law.getParameter("LOWER_BOUND");
                                                Parameter ubound = law.getParameter("UPPER_BOUND");
                                                info.append(r.getId()).append(" - ").append(r.getName()).append(" lb: ").append(lbound.getValue()).append(" up: ").append(ubound.getValue()).append(":\n");
                                        } else {
                                                info.append(r.getId()).append(":\n");
                                        }
                                } catch (Exception n) {
                                }
                                info.append("Reactants: \n");
                                ListOf spref = r.getListOfReactants();
                                for (int i =0; i < spref.size(); i++) {
                                        SpeciesReference sr = (SpeciesReference) spref.get(i);
                                        Species sp = sr.getModel().getSpecies(sr.getSpecies());
                                        info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append("\n");
                                }
                                info.append("Products: \n");
                                spref = r.getListOfProducts();
                                for (int i =0; i < spref.size(); i++) {
                                        SpeciesReference sr = (SpeciesReference) spref.get(i);
                                        Species sp = sr.getModel().getSpecies(sr.getSpecies());
                                        info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append(" \n");
                                }
                                info.append("----------------------------------- \n");
                                this.finishedPercentage = count / possibleReactions.size();
                                count++;
                        } catch (Exception ex) {
                                System.out.println(ex.toString());
                                System.out.println(r.getId());
                        }
                }
        }
}
