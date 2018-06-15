/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vtt.antnd.modules.analysis.reports;


import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.modules.simulation.FBA.FBA;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import static net.sf.dynamicreports.report.builder.DynamicReports.cht;
import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.stl;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;
import net.sf.dynamicreports.report.builder.chart.XyChartSerieBuilder;
import net.sf.dynamicreports.report.builder.chart.XyLineChartBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.style.FontBuilder;
import net.sf.dynamicreports.report.constant.SplitType;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;
import org.sbml.libsbml.KineticLaw;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Parameter;
import org.sbml.libsbml.Reaction;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class OxygenReport {

    private Dataset data;
    private HashMap<String, ReactionFA> reactions;
    private ReactionFA oxygen;
    Map<String, Double> Fdata;

    public OxygenReport(Dataset dataset) {
        this.data = dataset;
    }

    public JasperReportBuilder build() throws DRException {
        JRDataSource source = this.createDataSource(this.data.getDocument().getModel());

        if (source == null) {
            return null;
        }

        JasperReportBuilder report = report();
        FontBuilder boldFont = stl.fontArialBold().setFontSize(12);
        List<TextColumnBuilder> columns = new ArrayList<>();

        TextColumnBuilder<Double> XColumn = col.column(oxygen.getName(), oxygen.getName(), type.doubleType());

        for (String data : Fdata.keySet()) {
            // System.out.println(data);
            if (!data.equals(this.oxygen.getName())) {
                TextColumnBuilder<Double> reactionColumn = col.column(data, data, type.doubleType());
                columns.add(reactionColumn);
            }
        }
        List<XyLineChartBuilder> charts = new ArrayList<>();
        List<XyLineChartBuilder> chartsRelative = new ArrayList<>();
        List<XyChartSerieBuilder> series = new ArrayList<>();
        List<XyChartSerieBuilder> seriesRelative = new ArrayList<>();

        for (TextColumnBuilder column : columns) {
            if (column.getName().contains("Relative")) {
                seriesRelative.add(cht.xySerie(column));
            } else {
                series.add(cht.xySerie(column));
            }
        }

        XyLineChartBuilder lineChart = cht.xyLineChart()
            .setTitle("Oxygen variations (All exchanges)")
            .setTitleFont(boldFont)
            .setHeight(650)
            .setXValue(XColumn)
            .series(series.toArray(new XyChartSerieBuilder[series.size()]))
            .setShowValues(true)
            .setXAxisFormat(cht.axisFormat().setLabel("Oxygen level"))
            .setYAxisFormat(cht.axisFormat().setLabel("Exchanges"));
        charts.add(lineChart);

        XyLineChartBuilder lineChart2 = cht.xyLineChart()
            .setTitle("Oxygen variations (All exchanges) - Relative to carbon source")
            .setTitleFont(boldFont)
            .setHeight(650)
            .setXValue(XColumn)
            .series(seriesRelative.toArray(new XyChartSerieBuilder[seriesRelative.size()]))
            .setShowValues(true)
            .setXAxisFormat(cht.axisFormat().setLabel("Oxygen level"))
            .setYAxisFormat(cht.axisFormat().setLabel("Exchanges"));
        charts.add(lineChart2);

        for (TextColumnBuilder column : columns) {
            XyLineChartBuilder lineChart3 = cht.xyLineChart()
                .setTitle("Oxygen variations (" + column.getName() + ")")
                .setTitleFont(boldFont)
                .setHeight(300)
                .setXValue(XColumn)
                .series(cht.xySerie(column))
                .setShowValues(true)
                .setXAxisFormat(cht.axisFormat().setLabel("Oxygen level"))
                .setYAxisFormat(cht.axisFormat().setLabel(column.getName()));

            if (column.getName().contains("Relative")) {
                chartsRelative.add(lineChart3);
            } else {
                charts.add(lineChart3);
            }

        }

        report
            .setTemplate(Templates.reportTemplate)
            .setSummarySplitType(SplitType.IMMEDIATE)
            .title(Templates.createTitleComponentSmall("Changes in the fluxes due to the Oxygen levels"))
            //.columns(columns.toArray(new TextColumnBuilder[columns.size()]))
            .summary(
                cmp.verticalList(charts.toArray(new XyLineChartBuilder[charts.size()])),
                cmp.verticalList(chartsRelative.toArray(new XyLineChartBuilder[chartsRelative.size()])),
                cmp.verticalGap(10)
            )
            .setDataSource(source);
        return report;
    }

    private JRDataSource createDataSource(Model m) {
        Fdata = new HashMap<>();
        Map<String, String> reactionIds = new HashMap<>();
        ListOf reactions = m.getListOfReactions();
        for (int i = 0; i < reactions.size(); i++) {
            Reaction r = (Reaction) reactions.get(i);
            if (r.getName().contains("exchange")|| r.getName().contains("Ex")  || r.getName().contains("growth")) {
                KineticLaw law = r.getKineticLaw();
                double flux = law.getParameter("FLUX_VALUE").getValue();
                Fdata.put(r.getName(), flux);
                ListOf spref = r.getListOfProducts();
                for (int e = 0; e < spref.size(); e++) {
                    SpeciesReference c = (SpeciesReference) spref.get(e);
                    Species sp = m.getSpecies(c.getSpecies());

                    try {
                        String notes = sp.getNotesString();
                        String carbonsString = notes.substring(notes.indexOf("CARBONS:") + 8, notes.lastIndexOf("</p>"));

                        if (Double.valueOf(carbonsString) > 0) {
                           // System.out.println(r.getName() + "-Relative");
                            Fdata.put(r.getName() + "-Relative", flux);
                        }
                    } catch (NumberFormatException ex) {
                    }
                }

                reactionIds.put(r.getName(), r.getId());
            }
        }

        String parent = this.data.getParent();
        Dataset parentDataset = NDCore.getDesktop().getParentDataset(parent);

        if (parentDataset != null) {
            DRDataSource dataSource = new DRDataSource((String[]) Fdata.keySet().toArray(new String[Fdata.keySet().size()]));

            Model model = parentDataset.getDocument().getModel();
            
            this.createReactions(model);
            
            if (oxygen != null) {
                this.setFluxes(model, reactionIds, dataSource, 0.0);
                this.setFluxes(model, reactionIds, dataSource, -0.05);
                this.setFluxes(model, reactionIds, dataSource, -0.4);
                this.setFluxes(model, reactionIds, dataSource, -0.8);
                this.setFluxes(model, reactionIds, dataSource, -1.6);
                this.setFluxes(model, reactionIds, dataSource, -2);
                this.setFluxes(model, reactionIds, dataSource, -10);

            }
            return dataSource;
        }
        return null;

    }

    private void setFluxes(Model model, Map<String, String> reactionIds, DRDataSource dataSource, double oxygenBound) {
        Map<String, Double> carbons = new HashMap<>();
        double totalInCarbon = 0;

        FBA fba = new FBA();
        oxygen.setBounds(oxygenBound, Double.POSITIVE_INFINITY);

        fba.setModel(this.reactions, model);
        try {
            Map<String, Double> soln = fba.run();

            for (String r : Fdata.keySet()) {
                Reaction reaction = model.getReaction(reactionIds.get(r));
                if (reaction != null) {
                    ListOf spref = reaction.getListOfProducts();
                    for (int e = 0; e < spref.size(); e++) {
                    SpeciesReference c = (SpeciesReference) spref.get(e);
                    Species sp = model.getSpecies(c.getSpecies());

                        try {
                            String notes = sp.getNotesString();
                            String carbonsString = notes.substring(notes.indexOf("CARBONS:") + 8, notes.lastIndexOf("</p>"));

                            if (Double.valueOf(carbonsString) > 0) {
                                carbons.put(r + "-Relative", Double.valueOf(carbonsString));
                               // System.out.println("carbons " + r + "-Relative");
                                if (soln.get(reactionIds.get(r)) <= 0) {
                                    totalInCarbon += Double.valueOf(carbonsString);
                                }
                            }
                        } catch (NumberFormatException ex) {
                        }
                    }
                }
            }

           // System.out.println(totalInCarbon);
            for (String r : Fdata.keySet()) {
                if (r.equals(this.oxygen.getName())) {
                    Fdata.put(r, Math.abs(soln.get(reactionIds.get(r))));
                } else {
                    Double flux = soln.get(reactionIds.get(r.replace("-Relative", "")));
                    if (flux != null) {
                        Fdata.put(r, flux);
                    }
                    if (carbons.containsKey(r)) {
                        if (flux <= 0) {
                            Fdata.put(r, (carbons.get(r) / totalInCarbon) * Math.abs(soln.get(reactionIds.get(r.replace("-Relative", "")))) * -1);

                           // System.out.println(r + " :" + (carbons.get(r) / totalInCarbon) * Math.abs(soln.get(reactionIds.get(r.replace("-Relative", "")))) * -1);
                        } else {
                            Fdata.put(r, (carbons.get(r) / totalInCarbon) * Math.abs(soln.get(reactionIds.get(r.replace("-Relative", "")))));
                        }

                    }

                }

            }

            for (String r : Fdata.keySet()) {
                if (Fdata.get(r) == null) {
                    Fdata.put(r, 0.0);
                }
            }
            if (fba.getMaxObj() > 0) {
                dataSource.add(Fdata.values().toArray());
               // System.out.println(Arrays.toString(Fdata.values().toArray()));
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    private void createReactions(Model m) {
        this.reactions = new HashMap<>();
        ListOf reactions = m.getListOfReactions();
        for (int i = 0; i < reactions.size(); i++) {
            Reaction r= (Reaction) reactions.get(i);
            ReactionFA reaction = new ReactionFA(r.getId(), r.getName());
            if (r.getName().contains("oxygen") && r.getName().contains("exchange")) {
                this.oxygen = reaction;
            }
            try {
                KineticLaw law = r.getKineticLaw();
                Parameter lbound = law.getParameter("LOWER_BOUND");
                Parameter ubound = law.getParameter("UPPER_BOUND");
                Parameter objective = law.getParameter("OBJECTIVE_COEFFICIENT");
                reaction.setObjective(objective.getValue());
                reaction.setBounds(lbound.getValue(), ubound.getValue());
            } catch (Exception ex) {
                reaction.setBounds(-1000, 1000);
            }
            ListOf spref = r.getListOfReactants();
            for (int e = 0; e< spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
                Species sp = m.getSpecies(s.getSpecies());
                reaction.addReactant(sp.getId(), sp.getName(), s.getStoichiometry());
            }

            spref = r.getListOfProducts();
            for (int e = 0; e< spref.size(); e++) {
                SpeciesReference s = (SpeciesReference) spref.get(e);
                Species sp = m.getSpecies(s.getSpecies());
                reaction.addProduct(sp.getId(), sp.getName(), s.getStoichiometry());
            }
            this.reactions.put(r.getId(), reaction);
        }
    }
}
