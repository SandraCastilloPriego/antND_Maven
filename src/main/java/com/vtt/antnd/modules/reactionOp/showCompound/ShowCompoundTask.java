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
package com.vtt.antnd.modules.reactionOp.showCompound;

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

import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;

/**
 *
 * @author scsandra
 */
public class ShowCompoundTask extends AbstractTask {

    private final SimpleBasicDataset networkDS;
    private final String compoundName;
    private double finishedPercentage = 0.0f;
    private final JInternalFrame frame;
    private final JScrollPane panel;
    private final JTextArea tf;
    private final StringBuffer info;

    public ShowCompoundTask(SimpleBasicDataset dataset, SimpleParameterSet parameters) {
        networkDS = dataset;
        this.compoundName = parameters.getParameter(ShowCompoundParameters.compoundName).getValue().replaceAll("[\\s|\\u00A0]+", "");
        this.frame = new JInternalFrame("Result", true, true, true, true);
        this.tf = new JTextArea();
        this.panel = new JScrollPane(this.tf);

        this.info = new StringBuffer();
    }

    @Override
    public String getTaskDescription() {
        return "Showing compound... ";
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
            List<Species> possibleCompound = new ArrayList<>();
            //   ListOf species = m.getListOfSpecies();
            //  for (int i = 0; i < species.size(); i++) {
            //      Species sp = (Species) species.get(i);
            for (Species sp : m.getListOfSpecies()) {
                if (sp.getId().contains(this.compoundName) || sp.getName().contains(this.compoundName)) {
                    possibleCompound.add(sp);
                }
            }

            if (possibleCompound.isEmpty()) {
                // this.networkDS.setInfo("The compound" + compoundName + " doesn't exist in this model.");
                NDCore.getDesktop().displayMessage("The compound " + compoundName + " doesn't exist in this model.");
            } else {
                this.showReactions(possibleCompound, m);
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

    private void showReactions(List<Species> possibleReactions, Model m) {

        for (Species sp : possibleReactions) {
            info.append(sp.getId()).append(" - ").append(sp.getName());
            info.append("\nPresent in: ");
            // ListOf reactions = m.getListOfReactions();
            //for (int i = 0; i < reactions.size(); i++){
            //    Reaction r = (Reaction) reactions.get(i);
            for (Reaction r : m.getListOfReactions()) {
                if (r.getReactantForSpecies(sp.getId()) != null || r.getProductForSpecies(sp.getId()) != null) {
                    //info.append(r.getId()).append(", ");
                    showReactions(r, m);
                }
            }
            info.append("\n----------------------------------- \n");
        }
        //this.networkDS.setInfo(info.toString());
        this.tf.setText(info.toString());
    }

    private void showReactions(Reaction reaction, Model m) {

        Double lbound = this.networkDS.getLowerBound(reaction.getId());
        Double ubound = this.networkDS.getUpperBound(reaction.getId());
        info.append(reaction.getId()).append(" - ").append(reaction.getName()).append(" lb: ").append(lbound).append(" up: ").append(ubound).append(":\n");

        info.append("Reactants: \n");       
        for (SpeciesReference sr : reaction.getListOfReactants()) {
            Species sp = m.getSpecies(sr.getSpecies());
            info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append("\n");
        }
        info.append("Products: \n");       
        for (SpeciesReference sr : reaction.getListOfProducts()) {
            Species sp = m.getSpecies(sr.getSpecies());
            info.append(sr.getStoichiometry()).append(" ").append(sp.getId()).append(" - ").append(sp.getName()).append(" \n");
        }
        info.append("----------------------------------- \n");

    }
}
