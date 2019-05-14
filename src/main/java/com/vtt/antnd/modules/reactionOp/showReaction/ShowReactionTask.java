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
package com.vtt.antnd.modules.reactionOp.showReaction;

import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.parameters.SimpleParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class ShowReactionTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private final String reactionName;
    private double finishedPercentage = 0.0f;
    private final JInternalFrame frame;
    private final JScrollPane panel;
    private final JTextArea tf;
    private final StringBuffer info;

    public ShowReactionTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        networkDS = dataset;
        this.reactionName = parameters.getParameter(ShowReactionParameters.reactionName).getValue().replaceAll("[\\s|\\u00A0]+", "");
        this.frame = new JInternalFrame("Result", true, true, true, true);
        this.tf = new JTextArea();
        this.panel = new JScrollPane(this.tf);

        this.info = new StringBuffer();
    }

    @Override
    public String getTaskDescription() {
        return "Showing reaction... ";
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

            List<Reaction> possibleReactions = new ArrayList<>();
            String[] reactionNames = new String[1];
            if (this.reactionName.contains(",")) {
                reactionNames = this.reactionName.split(",");
            } else {
                reactionNames[0] = this.reactionName;
            }

            for (String reactionName : reactionNames) {
                reactionName = reactionName.trim();
                ListOf reactions = m.getListOfReactions();
                for (int i = 0; i < reactions.size(); i++) {
                    Reaction r = (Reaction) reactions.get(i);
                    if (r.getId().contains(reactionName) || r.getName().contains(reactionName)) {
                        possibleReactions.add(r);
                    }
                }
            }

            if (possibleReactions.isEmpty()) {               
                NDCore.getDesktop().displayMessage("The reaction " + reactionName + " doesn't exist in this model.");
            } else {
                this.showReactions(possibleReactions);
                frame.setSize(new Dimension(700, 500));
                frame.add(this.panel);
                NDCore.getDesktop().addInternalFrame(frame);
            }
            finishedPercentage = 1.0f;
            setStatus(TaskStatus.FINISHED);
        } catch (Exception e) {
            setStatus(TaskStatus.ERROR);
            errorMessage = e.toString();
        }
    }

    private void showReactions(List<Reaction> possibleReactions) {

        for (Reaction r : possibleReactions) {

            Double lbound = this.networkDS.getLowerBound(r.getId());
            Double ubound = this.networkDS.getUpperBound(r.getId());
            info.append(r.getId()).append(" - ").append(r.getName()).append(" lb: ").append(lbound).append(" up: ").append(ubound).append(":\n");

            info.append("Reactants: \n");
            ListOf spref = r.getListOfReactants();
            for (int i = 0; i < spref.size(); i++) {
                SpeciesReference sr = (SpeciesReference) spref.get(i);
                Species sp = sr.getModel().getSpecies(sr.getSpecies());
                info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append("\n");
            }
            info.append("Products: \n");
            spref = r.getListOfProducts();
            for (int i = 0; i < spref.size(); i++) {
                SpeciesReference sr = (SpeciesReference) spref.get(i);
                Species sp = sr.getModel().getSpecies(sr.getSpecies());
                info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append(" \n");
            }
            info.append("----------------------------------- \n");
        }
        //this.networkDS.setInfo(info.toString());
        this.tf.setText(info.toString());
    }
}
