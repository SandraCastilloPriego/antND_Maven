/*
 * Copyright 2007-2013 VTT Biotechnology
 * This file is part of Guineu.
 *
 * Guineu is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * Guineu is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Guineu; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.vtt.antnd.data.models;

import com.vtt.antnd.data.ColumnName;
import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.DatasetType;
import com.vtt.antnd.data.impl.datasets.SimpleBasicDataset;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.util.Tables.DataTableModel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import javax.xml.stream.XMLStreamException;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.Objective.Type;

public class ReactionsDataModel extends AbstractTableModel implements DataTableModel {

    private final Dataset dataset;
    private final List<ColumnName> columns;
    private Color[] rowColor;

    public ReactionsDataModel(Dataset dataset) {
        this.dataset = (SimpleBasicDataset) dataset;
        rowColor = new Color[(int) dataset.getDocument().getModel().getNumReactions()];
        columns = new ArrayList<>();
        columns.addAll(Arrays.asList(ColumnName.values()));
    }

    @Override
    public Color getRowColor(int row) {
        if (row < rowColor.length) {
            return rowColor[row];
        } else {
            return null;
        }
    }

    @Override
    public void addRowColor(Color[] color) {
        this.rowColor = color;
    }

    /**
     * @see guineu.util.Tables.DataTableModel
     */
    @Override
    public void removeRows() {
        Model model = dataset.getDocument().getModel();
        List<Reaction> toBeRemoved = new ArrayList();

        for (Reaction reaction : model.getListOfReactions()) {
            if (dataset.isReactionSelected(reaction)) {
                toBeRemoved.add(reaction);
                this.fireTableDataChanged();
                this.removeRows();
                break;
            }
        }
        for (Reaction reaction : toBeRemoved) {
            model.removeReaction(reaction.getId());
        }
    }

    @Override
    public int getColumnCount() {
        return 10;
    }

    @Override
    public int getRowCount() {
        return (int) this.dataset.getDocument().getModel().getNumReactions();
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        try {
            Model model = this.dataset.getDocument().getModel();
            Reaction r = model.getReaction(row);

            String value = columns.get(column).getColumnName();
            switch (value) {
                case "Number":
                    return row + 1;
                case "Id":
                    return r.getId();
                case "Name":
                    return r.getName();
                case "Reaction":
                    return getReactionNoExt(r);
                case "Reaction extended":
                    return getReactionExt(r, this.dataset.getDocument().getModel());
                case "Lower bound":
                    return this.dataset.getLowerBound(r.getId());
                case "Upper bound":
                    return this.dataset.getUpperBound(r.getId());
                case "Notes":
                    String notes;
                    try {
                        notes = r.getNotesString();
                        return notes;
                    } catch (XMLStreamException ex) {
                        Logger.getLogger(ReactionsDataModel.class.getName()).log(Level.SEVERE, null, ex);
                    }

                case "Objective":
                    if (r.getKineticLaw() != null) {
                        LocalParameter lp = r.getKineticLaw().getLocalParameter("OBJECTIVE_COEFFICIENT");
                        if (lp != null) {
                            return lp.getValue();
                        } else {
                            return this.getObjectiveFBC(r.getId(), model);
                        }
                    } else {

                        return 0.0;
                    }
                case "Fluxes":
                    return this.dataset.getFlux(r.getId());

            }
            return value;

        } catch (Exception e) {
            return null;
        }
    }

    private double getObjectiveFBC(String r, Model model) {
        try {
            FBCModelPlugin plugin = (FBCModelPlugin) model.getPlugin("fbc");
            for (Objective obj : plugin.getListOfObjectives()) {
                for (FluxObjective fobj : obj.getListOfFluxObjectives()) {
                    if (fobj.getReaction().equals(r)) {
                        return fobj.getCoefficient();
                    }
                }
            }
        } catch (NullPointerException n) {
            return 0.0;
        }
        return 0.0;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return (String) this.columns.get(columnIndex).toString();
    }

    @Override
    public Class<?> getColumnClass(int c) {
        if (getValueAt(0, c) != null) {
            return getValueAt(0, c).getClass();
        } else {
            return Object.class;
        }
    }

    @Override
    @SuppressWarnings("fallthrough")
    public void setValueAt(Object aValue, int row, int column) {
        // try {
        String info = "";
        Model model = this.dataset.getDocument().getModel();
        Reaction r = model.getReaction(row);

        String value = columns.get(column).getColumnName();
        switch (value) {
            case "Number":
                return;
            case "Id":
                if (aValue == null || aValue.toString().isEmpty() || aValue.equals("NA")) {
                    info = info + "\n- The reaction " + r.getId() + " - " + r.getName() + " has been removed : \n" + getReactionNoExt(r) + "\n" + getReactionExt(r, this.dataset.getDocument().getModel()) + "\n------------------";
                    dataset.addInfo(info);

                    this.dataset.getDocument().getModel().removeReaction(r.getId());
                    AntGraph g = this.dataset.getGraph();
                    AntNode n = g.getNode(r.getId());
                    if (n != null) {
                        g.removeNode(n.getId());
                    }

                } else {
                    info = info + "\n- Id of the reaction " + r.getId() + " - " + r.getName() + " has changed to " + aValue.toString();
                    dataset.addInfo(info);
                    r.setId(aValue.toString());
                }
                return;
            case "Name":
                r.setName(aValue.toString());
                info = info + "\n- Name of the reaction " + r.getId() + " - " + r.getName() + " has changed to " + aValue.toString();
                dataset.addInfo(info);
                return;
            case "Reaction":
                changeReaction(r, aValue.toString());
                return;
            case "Reaction extended":
                changeReaction(r, aValue.toString());
                dataset.addInfo(info);
                return;
            case "Lower bound":
                dataset.setLowerBound(r.getId(), Double.valueOf(aValue.toString()));
                return;
            case "Upper bound":
                dataset.setUpperBound(r.getId(), Double.valueOf(aValue.toString()));
                return;
            case "Notes": {
                try {
                    info = info + "\n- Notes of the reaction " + r.getId() + " - " + r.getName() + " have changed from " + r.getNotes().toXMLString() + " to " + aValue.toString();
                    dataset.addInfo(info);
                    r.setNotes(value);
                } catch (XMLStreamException ex) {
                    Logger.getLogger(ReactionsDataModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return;

            case "Objective":
                try {
                    if (Double.valueOf(aValue.toString()) == 0.0) {
                        removeObjective(r.getId(), model);
                    } else {
                        FBCModelPlugin plugin = (FBCModelPlugin) model.getPlugin("fbc");

                        FluxObjective fx = new FluxObjective();
                        fx.setReaction(r);
                        fx.setCoefficient(Double.valueOf(aValue.toString()));
                        Type type = Objective.Type.MAXIMIZE;
                        if (Double.valueOf(aValue.toString()) < 0) {
                            type = Objective.Type.MINIMIZE;
                        }
                        Objective objf = null;
                        for (Objective obj : plugin.getListOfObjectives()) {
                            for (FluxObjective fobj : obj.getListOfFluxObjectives()) {
                                if (fobj.getReaction().equals(r.getId())) {
                                    fobj.setCoefficient(Double.valueOf(aValue.toString()));
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
                } catch (NullPointerException n) {
                    if (Double.valueOf(aValue.toString()) != 0.0) {

                        System.out.println("there is no FBCPlugin");
                        FBCModelPlugin plugin = new FBCModelPlugin(model);
                        Type type = Objective.Type.MAXIMIZE;
                        if (Double.valueOf(aValue.toString()) < 0) {
                            type = Objective.Type.MINIMIZE;
                        }
                        Objective obj = plugin.createObjective("obj" + r.getId(), type);
                        FluxObjective fx = new FluxObjective();
                        fx.setReaction(r);
                        fx.setCoefficient(Double.valueOf(aValue.toString()));
                        obj.addFluxObjective(fx);
                        plugin.setActiveObjective(obj);
                    }
                }

                //    info = info + "\n- Objective coefficient of the reaction " + r.getId() + " - " + r.getName() + " has changed from " + parameter.getValue() + " to " + aValue.toString();
                //   dataset.addInfo(info);                   
                return;
            case "Fluxes":
                info = info + "\n- Flux of the reaction " + r.getId() + " - " + r.getName() + " has changed from " + this.dataset.getFlux(r.getId()) + " to " + aValue.toString();
                dataset.addInfo(info);
                this.dataset.setFlux(r.getId(), Double.valueOf(aValue.toString()));
                return;
        }

        fireTableCellUpdated(row, column);
        //} catch (Exception e) {

        //}
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    /**
     * @see guineu.util.Tables.DataTableModel
     */
    public DatasetType getType() {
        return this.dataset.getType();
    }

    /**
     * @see guineu.util.Tables.DataTableModel
     */
    public void addColumn(String columnName) {

    }

    @Override
    public Color getCellColor(int row, int column) {
        return null;
    }

    private String getReactionNoExt(Reaction r) {
        String reaction = "";
        //ListOf listOfReactants= r.getListOfReactants();
        //for (int i = 0; i < r.getNumReactants(); i++) {
        //    SpeciesReference reactant = (SpeciesReference) listOfReactants.get(i);   
        for (SpeciesReference reactant : r.getListOfReactants()) {
            reaction += reactant.getStoichiometry() + " " + reactant.getSpecies() + " + ";
        }
        reaction = reaction.substring(0, reaction.lastIndexOf(" + "));
        reaction += " <=> ";
        if (r.getNumProducts() > 0) {
            //ListOf listOfProducts= r.getListOfProducts();
            //for (int i = 0; i < r.getNumProducts(); i++) {
            //SpeciesReference product = (SpeciesReference) listOfProducts.get(i); 
            for (SpeciesReference product : r.getListOfProducts()) {
                reaction += product.getStoichiometry() + " " + product.getSpecies() + " + ";
            }
            reaction = reaction.substring(0, reaction.lastIndexOf(" + "));
        }
        return reaction;
    }

    private Object getReactionExt(Reaction r, Model m) {
        String reaction = "";
        /* ListOf listOfReactants= r.getListOfReactants();
        for (int i = 0; i < r.getNumReactants(); i++) {
            SpeciesReference reactant = (SpeciesReference) listOfReactants.get(i);  */
        for (SpeciesReference reactant : r.getListOfReactants()) {
            reaction += reactant.getStoichiometry() + " " + m.getSpecies(reactant.getSpecies()).getName() + " + ";
        }
        reaction = reaction.substring(0, reaction.lastIndexOf(" + "));
        reaction += " <=> ";
        if (r.getNumProducts() > 0) {
            //ListOf listOfProducts= r.getListOfProducts();
            // for (int i = 0; i < r.getNumProducts(); i++) {
            // SpeciesReference product = (SpeciesReference) listOfProducts.get(i);  
            for (SpeciesReference product : r.getListOfProducts()) {
                reaction += product.getStoichiometry() + " " + m.getSpecies(product.getSpecies()).getName() + " + ";
            }
            reaction = reaction.substring(0, reaction.lastIndexOf(" + "));
        }
        return reaction;
    }

    private void changeReaction(Reaction r, String value) {
        /*if (value == null) {
         this.dataset.getDocument().getModel().removeReaction(r);
         } else {
         //1.0 s_3713 <=> 1.0 s_1524
         try {
         String[] sides = value.split(" <=> ");
         String[] reactants = sides[0].split(" + ");
         String[] products = sides[1].split(" + ");
         r.getListOfReactants()
         } catch (Exception e) {
         }

         }*/

    }

    @Override
    public boolean isExchange(int row) {
        Reaction r = this.dataset.getDocument().getModel().getReaction(row);
        if (r.getName().contains("exchange")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isTransport(int row) {
        Reaction r = this.dataset.getDocument().getModel().getReaction(row);
        if (r.getName().contains("port")) {
            return true;
        }
        return false;
    }

    @Override
    public Dataset getDataset() {
        return this.dataset;
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
}
