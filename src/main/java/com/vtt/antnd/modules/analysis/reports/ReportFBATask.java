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
package com.vtt.antnd.modules.analysis.reports;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.parameters.ParameterSet;
import com.vtt.antnd.util.taskControl.AbstractTask;
import com.vtt.antnd.util.taskControl.TaskStatus;
import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import net.sf.dynamicreports.report.constant.SplitType;
import net.sf.dynamicreports.report.exception.DRException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.util.Rotation;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 *
 * @author scsandra
 */
public class ReportFBATask extends AbstractTask {

    private Dataset dataset;
    private File fileName;
    private double finishedPercentage = 0.0f;

    public ReportFBATask(Dataset data, ParameterSet parameters) {
        // this.fileName = parameters.getParameter(ReportFBAParameters.fileName).getValue();
        this.dataset = data;
    }

    @Override
    public String getTaskDescription() {
        return "Writing report... ";
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
        setStatus(TaskStatus.PROCESSING);
        this.build();
        setStatus(TaskStatus.FINISHED);
    }

    public static List<PieDataset> createPieDataset(Dataset data) {
        List<PieDataset> datasets = new ArrayList<>();
        Map<String, Double> fluxes = new HashMap<>();
        Map<String, Double> carbons = new HashMap<>();
        double totalInCarbon = 0;
        Model m = data.getDocument().getModel();
        ListOf listOfReactions = m.getListOfReactions();
        for (int i = 0; i < m.getNumReactions(); i++) {
            Reaction r = (Reaction) listOfReactions.get(i);
            
            if (isBoundary(r)) {
                double flux = data.getFlux(r.getId());
                ListOf listOfProducts = r.getListOfProducts();
                for (int e = 0; e < r.getNumProducts(); e++) {
                    SpeciesReference spr = (SpeciesReference) listOfProducts.get(e);
                    Species sp = m.getSpecies(spr.getSpecies());

                    try {
                        String notes = sp.getNotesString();
                        // System.out.println(notes);
                        String carbonsString = notes.substring(notes.indexOf("CARBONS:") + 8, notes.lastIndexOf("</p>"));
                        carbons.put(r.getName(), Double.valueOf(carbonsString));
                        // System.out.println(carbonsString);
                        if (Double.valueOf(carbonsString) > 0.00001) {
                            fluxes.put(r.getName(), flux);
                        }
                        if (flux < -0.00001) {
                            totalInCarbon += Double.valueOf(carbonsString);
                        } else {
                            //totalOutCarbon += Double.valueOf(carbonsString);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }
        DefaultPieDataset datasetOut = new DefaultPieDataset();
        DefaultPieDataset datasetIn = new DefaultPieDataset();

        System.out.println(totalInCarbon);
        if (totalInCarbon == 0) {
            for (String r : fluxes.keySet()) {
                double flux = fluxes.get(r);
                if (flux < 0) {
                    datasetIn.setValue(r, Math.abs(flux));
                } else {
                    datasetOut.setValue(r, flux);
                }
            }
        } else {
            for (String r : fluxes.keySet()) {
                double flux = fluxes.get(r);
                System.out.println((carbons.get(r)) / totalInCarbon);
                if (flux < -0.00001) {
                    datasetIn.setValue(r, (carbons.get(r) / totalInCarbon) * Math.abs(flux));
                } else {
                    datasetOut.setValue(r, (carbons.get(r) / totalInCarbon) * flux);
                }
            }

        }

        datasets.add(datasetOut);
        datasets.add(datasetIn);

        return datasets;

    }

    public static CategoryDataset createBarExchangeDataset(Dataset dataset) {
        final DefaultCategoryDataset graphDataset = new DefaultCategoryDataset();
        Model m = dataset.getDocument().getModel();
        ListOf listOfReactions = m.getListOfReactions();
        for (int i = 0; i < m.getNumReactions(); i++) {
            Reaction r = (Reaction) listOfReactions.get(i);
            if (isBoundary(r)) {
                double flux = dataset.getFlux(r.getId());
                if (flux < 0) {
                    graphDataset.addValue(flux, "Exchanges In", r.getName());
                } else {
                    graphDataset.addValue(flux, "Exchanges Out", r.getName());
                }
            }

        }
        return graphDataset;
    }

    public static CategoryDataset createBarDataset(Dataset dataset) {
        Map<String, Double> data = new HashMap<>();
        Model m = dataset.getDocument().getModel();
        ListOf listOfReactions = m.getListOfReactions();
        for (int i = 0; i < m.getNumReactions(); i++) {
            Reaction r = (Reaction) listOfReactions.get(i);
            double flux = dataset.getFlux(r.getId());
            double realFlux = 0;
            if (flux > 500) {
                realFlux = Math.abs(1000 - flux);
            } else if (flux < -500) {
                realFlux = (1000 - Math.abs(flux)) * -1;
            } else {
                realFlux = flux;
            }
            if (realFlux > 1 || realFlux < -1) {
                if (realFlux <= 6) {
                    data.put(r.getName(), realFlux);
                }
            }

        }
        final DefaultCategoryDataset graphDataset = new DefaultCategoryDataset();
        for (String d : data.keySet()) {
            graphDataset.addValue(data.get(d), "Fluxes", d);
        }

        return graphDataset;

    }

    public static JFreeChart createPieChart(PieDataset dataset, String title) {
        JFreeChart chart = ChartFactory.createPieChart3D(
                title, // chart title 
                dataset, // data    
                true, // include legend   
                true,
                false);

        final PiePlot3D plot = (PiePlot3D) chart.getPlot();

        PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator(
                "{0}: {2}", new DecimalFormat("0.000"), new DecimalFormat("0%"));
        plot.setLabelGenerator(gen);
        plot.setStartAngle(290);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setForegroundAlpha(0.5f);
        plot.setBackgroundPaint(new Color(0, 0, 0, 0));
        return chart;
    }

    public static JFreeChart createBarChart(CategoryDataset dataset, String title) {
        JFreeChart barChart = ChartFactory.createBarChart3D(
                title,
                "Reactions",
                "Fluxes",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        CategoryPlot plot = barChart.getCategoryPlot();
        CategoryAxis axis = (CategoryAxis) plot.getDomainAxis();
        axis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
        plot.setBackgroundPaint(new Color(0, 0, 0, 0));
        return barChart;
    }

    private void build() {
        try {

            JasperReportBuilder report = report();

            report
                    .setTemplate(Templates.reportTemplate)
                    .title(Templates.createTitleComponent(this.dataset.getDatasetName()))
                    .setSummarySplitType(SplitType.IMMEDIATE)
                    .summary(
                            cmp.subreport(ChangesReport()),
                            cmp.pageBreak(),
                            cmp.subreport(ExchangesReportBar()),
                            cmp.pageBreak(),
                            cmp.subreport(ExchangesReportPie()),
                            cmp.pageBreak(),
                            cmp.subreport(ImportantFluxesReport()),
                            cmp.pageBreak(),
                            cmp.subreport(OxygenReport()),
                            cmp.pageBreak(),
                            cmp.subreport(CofactorReport())
                    )
                    .pageFooter(cmp.line(),
                            cmp.pageNumber().setStyle(Templates.boldCenteredStyle))
                    .show(false);

        } catch (DRException ex) {
            System.out.println(ex.toString());
        }
    }

    private static boolean isBoundary(Reaction r) {
        for (SpeciesReference spr : r.getListOfReactants()) {
            if (spr.getSpeciesInstance().isBoundaryCondition()) {
                return true;
            }
        }

        for (SpeciesReference spr : r.getListOfProducts()) {
            if (spr.getSpeciesInstance().isBoundaryCondition()) {
                return true;
            }
        }

        return false;
    }

    private JasperReportBuilder ChangesReport() throws DRException {
        return new ChangesReport(this.dataset).build();
    }

    private JasperReportBuilder ExchangesReportPie() throws DRException {
        return new ExchangesReportPie(this.dataset).build();
    }

    private JasperReportBuilder ExchangesReportBar() throws DRException {
        return new ExchangesReportBar(this.dataset).build();
    }

    private JasperReportBuilder ImportantFluxesReport() throws DRException {
        return new ImportantFluxesReport(this.dataset).build();
    }

    private JasperReportBuilder OxygenReport() throws DRException {
        return new OxygenReport(this.dataset).build();
    }

    private JasperReportBuilder CofactorReport() throws DRException {
        return new CofactorReport(this.dataset).build();
    }
}
