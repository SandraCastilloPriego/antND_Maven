package com.vtt.antnd.util.Tables.impl;

import com.vtt.antnd.data.Dataset;
import com.vtt.antnd.data.DatasetType;
import com.vtt.antnd.data.network.AntEdge;
import com.vtt.antnd.data.network.AntGraph;
import com.vtt.antnd.data.network.AntNode;
import com.vtt.antnd.data.network.uniqueId;
import com.vtt.antnd.desktop.impl.PrintPaths2;
import com.vtt.antnd.main.NDCore;
import com.vtt.antnd.util.GetInfoAndTools;
import com.vtt.antnd.util.Tables.DataTable;
import com.vtt.antnd.util.Tables.DataTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

/**
 * Creates a table for showing the data sets. It implements DataTable.
 *
 * @author scsandra
 */
public final class PushableTable implements DataTable, ActionListener {

    protected DataTableModel model;
    JTable table;
    private String rowstring, value;
    private Clipboard system;
    private StringSelection stsel;
    private ArrayList<register> registers;
    int indexRegister = 0;
    GetInfoAndTools tools;

    public PushableTable() {
        registers = new ArrayList<>();
    }

    public PushableTable(DataTableModel model) {
        this.model = model;
        this.tools = new GetInfoAndTools();
        ((AbstractTableModel) this.model).fireTableDataChanged();
        table = this.tableRowsColor(model);
        setTableProperties();
        registers = new ArrayList<>();
    }

    /**
     * Changes the model of the table.
     *
     * @param model
     */
    @Override
    public void createTable(DataTableModel model) {
        this.model = model;
        // Color of the cells
        table = this.tableRowsColor(model);
    }

    /**
     * Returns the table.
     *
     * @return Table
     */
    @Override
    public JTable getTable() {
        return table;
    }

    /**
     * Changes the color of the cells depending of determinates conditions.
     *
     * @param tableModel
     * @return table
     */
    protected JTable tableRowsColor(final DataTableModel tableModel) {
        JTable colorTable = new JTable(tableModel) {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                try {
                    // Coloring conditions
                    if (isDataSelected(Index_row)) {
                        comp.setBackground(new Color(173, 205, 203));
                        if (comp.getBackground().getRGB() != new Color(173, 205, 203).getRGB()) {
                            this.repaint();
                        }
                    } else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col) && getRowColor(Index_row) == null) {
                        comp.setBackground(new Color(234, 235, 243));
                    } else if (isCellSelected(Index_row, Index_col)) {
                        comp.setBackground(new Color(173, 205, 203));
                        if (comp.getBackground().getRGB() != new Color(173, 205, 203).getRGB()) {
                            this.repaint();
                        }

                    } else if (getRowColor(Index_row) != null) {
                        comp.setBackground(getRowColor(Index_row));
                    } else {
                        comp.setBackground(Color.white);
                    }

                    if (getCellColor(Index_row, Index_col) != null) {
                        comp.setBackground(getCellColor(Index_row, Index_col));
                    }

                    if (isExchange(Index_row)) {
                        comp.setBackground(Color.YELLOW);
                        this.repaint();
                    }
                    if (isTransport(Index_row)) {
                        comp.setBackground(Color.ORANGE);
                        this.repaint();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return comp;
            }

            private boolean isDataSelected(int row) {
                try {
                    return ((Boolean) table.getValueAt(row, 0)).booleanValue();
                } catch (Exception e) {
                    return false;
                }
            }

            private Color getRowColor(int row) {
                return tableModel.getRowColor(row);
            }

            private Color getCellColor(int row, int column) {
                return tableModel.getCellColor(row, column);
            }

            /*private boolean isExchange(int row){
                
                return isExchange(row);
            }
            
             private boolean isTransport(int row){
                return tableModel.isTransport(row);
            }*/
        };

        return colorTable;
    }

    public boolean isExchange(int row) {
        try {
            Model m = this.model.getDataset().getDocument().getModel();
            for (int i = 0; i < this.getTable().getColumnCount(); i++) {
                String columnName = this.getTable().getColumnName(i);
                if (columnName.matches("Id")) {
                    String id = (String) this.getTable().getValueAt(row, i);
                    Reaction reaction = m.getReaction(id);                   
                    for (SpeciesReference spr:reaction.getListOfProducts()) {          
                        Species sp = m.getSpecies(spr.getSpecies());
                        if (sp.getBoundaryCondition() == true) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTransport(int row) {
        try {

            Model m = this.model.getDataset().getDocument().getModel();
            for (int i = 0; i < this.getTable().getColumnCount(); i++) {
                String columnName = this.getTable().getColumnName(i);
                if (columnName.matches("Id")) {
                    String id = (String) this.getTable().getValueAt(row, i);
                    Reaction reaction = m.getReaction(id);
                    
                    ArrayList compartReactants = new ArrayList<String>();
                    for (SpeciesReference reactant:reaction.getListOfReactants()) {                   
                        Species sp = m.getSpecies(reactant.getSpecies());
                        compartReactants.add(sp.getCompartment());
                    }
          
                    ArrayList compartProducts = new ArrayList<String>();
                    for (SpeciesReference product: reaction.getListOfProducts()) {                       
                        Species sp = m.getSpecies(product.getSpecies());
                        compartProducts.add(sp.getCompartment());
                        if (sp.getBoundaryCondition() == true) {
                            return false;
                        }
                    }

                    for (int e = 0; e < compartReactants.size(); e++) {
                        if (!compartProducts.contains(compartReactants.get(e))) {
                            return true;
                        }
                    }
                    for (int e = 0; e < compartProducts.size(); e++) {
                        if (!compartReactants.contains(compartProducts.get(e))) {
                            return true;
                        }
                    }

                    Set<String> uniqueCom = new HashSet<String>(compartReactants);
                    if (uniqueCom.size() > 1) {
                        return true;
                    }
                    uniqueCom = new HashSet<String>(compartProducts);
                    if (uniqueCom.size() > 1) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sets the properties of the table: selection mode, tooltips, actions with
     * keys..
     *
     */
    public void setTableProperties() {

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setColumnSelectionAllowed(true);

        // Tooltips
        this.createTooltips();

        // Sorting
        RowSorter<DataTableModel> sorter = new TableRowSorter<DataTableModel>(model);
        table.setRowSorter(sorter);
        table.setUpdateSelectionOnSort(false);

        // Size
        table.setMinimumSize(new Dimension(300, 800));

        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        //key actions
        registerKey(KeyEvent.VK_C, ActionEvent.CTRL_MASK, "Copy");
        registerKey(KeyEvent.VK_V, ActionEvent.CTRL_MASK, "Paste");
        registerKey(KeyEvent.VK_DELETE, 0, "Delete");
        registerKey(KeyEvent.VK_Z, ActionEvent.CTRL_MASK, "Back");
        registerKey(KeyEvent.VK_Y, ActionEvent.CTRL_MASK, "Forward");
        registerKey(KeyEvent.VK_G, ActionEvent.CTRL_MASK, "Visualize");

        system = Toolkit.getDefaultToolkit().getSystemClipboard();

    }

    /**
     * Adds a concrete action to a combination of keys.
     *
     * @param key Key responsible of the action
     * @param mask Mask of the key
     * @param name Name of the action
     */
    private void registerKey(int key, int mask, String name) {
        KeyStroke action = KeyStroke.getKeyStroke(key, mask, false);
        table.registerKeyboardAction(this, name, action, JComponent.WHEN_FOCUSED);
    }

    /**
     * Formating of the numbers in the table depening on the data set type.
     *
     * @param type Type of dataset @see guineu.data.DatasetType
     */
    public void formatNumbers(DatasetType type) {
        try {
            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMinimumFractionDigits(7);
            int init = model.getColumnCount();

            for (int i = init; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(new NumberRenderer(format));
            }
        } catch (Exception e) {
        }

    }

    /**
     * Formating of the numbers in certaing column
     *
     * @param column Column where the numbers will be formated
     */
    public void formatNumbers(int column) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(7);
        table.getColumnModel().getColumn(column).setCellRenderer(new NumberRenderer(format));
    }

    /**
     * Creates the tooltips of the table.
     *
     */
    public void createTooltips() {
        try {
            ToolTipHeader toolheader;
            String[] toolTipStr = new String[model.getColumnCount()];
            for (int i = 0; i < model.getColumnCount(); i++) {
                toolTipStr[i] = model.getColumnName(i);
            }

            toolheader = new ToolTipHeader(table.getColumnModel());
            toolheader.setToolTipStrings(toolTipStr);
            table.setTableHeader(toolheader);
        } catch (Exception e) {
        }
    }

    public void actionPerformed(ActionEvent e) {
        System.out.print(e.getActionCommand());
        // Sets the action of the key combinations
        // Copy
        if (e.getActionCommand().compareTo("Copy") == 0) {
            StringBuffer sbf = new StringBuffer();
            // Check to ensure we have selected only a contiguous block of
            // cells
            int numcols = table.getSelectedColumnCount();
            int numrows = table.getSelectedRowCount();
            int[] rowsselected = table.getSelectedRows();
            int[] colsselected = table.getSelectedColumns();
            if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0]
                    && numrows == rowsselected.length)
                    && (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0]
                    && numcols == colsselected.length))) {
                JOptionPane.showMessageDialog(null, "Invalid Copy Selection",
                        "Invalid Copy Selection",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int i = 0; i < numrows; i++) {
                for (int j = 0; j < numcols; j++) {
                    sbf.append(table.getValueAt(rowsselected[i], colsselected[j]));
                    if (j < numcols - 1) {
                        sbf.append("\t");
                    }
                }
                sbf.append("\n");
            }
            stsel = new StringSelection(sbf.toString());
            system = Toolkit.getDefaultToolkit().getSystemClipboard();
            system.setContents(stsel, stsel);
        }

        // Paste
        if (e.getActionCommand().compareTo("Paste") == 0) {

            int startRow = (table.getSelectedRows())[0];
            int startCol = (table.getSelectedColumns())[0];
            register newRegister = null;
            String rtrstring;
            try {
                rtrstring = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
                StringTokenizer rst1 = new StringTokenizer(rtrstring, "\n");
                rowstring = rst1.nextToken();
                StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
                newRegister = new register(startRow, rst1.countTokens() + 1, startCol, st2.countTokens());
                newRegister.getValues();
            } catch (Exception ex) {
                Logger.getLogger(PushableTable.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                String trstring = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
                StringTokenizer st1 = new StringTokenizer(trstring, "\n");
                for (int i = 0; st1.hasMoreTokens(); i++) {
                    rowstring = st1.nextToken();
                    StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
                    for (int j = 0; st2.hasMoreTokens(); j++) {
                        value = st2.nextToken();
                        if (startRow + i < table.getRowCount()
                                && startCol + j < table.getColumnCount()) {
                            table.setValueAt(value, startRow + i, startCol + j);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            newRegister.getNewValues();
            this.registers.add(newRegister);
            this.indexRegister = this.registers.size() - 1;
        }

        // Delete
        if (e.getActionCommand().compareTo("Delete") == 0) {   
            int[] selectedRow = table.getSelectedRows();
            ArrayList<String> r = new ArrayList<>();
            for (int i = 0; i < selectedRow.length; i++) {  
                r.add((String) table.getValueAt(selectedRow[i], 1));
            }   
            model.removeRows(r);           
        }

        // Undo
        if (e.getActionCommand().compareTo("Back") == 0) {
            this.registers.get(indexRegister).back();
            if (indexRegister > 0) {
                indexRegister--;
            }
        }

        // Redo
        if (e.getActionCommand().compareTo("Forward") == 0) {
            this.registers.get(indexRegister).forward();
            if (indexRegister < this.registers.size() - 1) {
                indexRegister++;
            }
        }

        if (e.getActionCommand().compareTo("Visualize") == 0) {
            int[] selectedRow = table.getSelectedRows();
            ArrayList<String> reactions = new ArrayList();
            for (int i = 0; i < selectedRow.length; i++) {
                String id = (String) table.getValueAt(selectedRow[i], 1);
                reactions.add(id);
            }
            Model m = this.model.getDataset().getDocument().getModel();
            AntGraph g = this.getGraph(m, reactions);

            JInternalFrame frame = new JInternalFrame("ReactionVis", true, true, true, true);

            NDCore.getDesktop().addInternalFrame(frame);
            Dataset newDataset = tools.createDataFile(g, this.model.getDataset(), null, null, false, false);

            PrintPaths2 print = new PrintPaths2(newDataset);

            try {

                System.out.println("Visualize");
                frame.add(print.printPathwayInFrame(g));
                frame.pack();
            } catch (NullPointerException ex) {
                System.out.println(ex.toString());
            }
            // your valueChanged overridden method }*/
        }

        System.gc();
    }

    public AntGraph getGraph(Model model, ArrayList<String> reactions) {

        java.util.List<AntNode> nodeList = new ArrayList<>();
        java.util.List<AntEdge> edgeList = new ArrayList<>();

        AntGraph g = new AntGraph(nodeList, edgeList);

        try {
            // For eac reaction in the model, a Node is created in the graph

            for (int i = 0; i < reactions.size(); i++) {
                Reaction r = model.getReaction(reactions.get(i));
                System.out.println(r.getId());
                //Node is created
                AntNode reactionNode = new AntNode(r.getId(), r.getName());
                // this.setPosition(reactionNode, g);

                g.addNode2(reactionNode);
                int direction = this.getDirection(r);
                // read bounds to know the direction of the edges
                // ListOf listOfReactants = r.getListOfReactants();
                //System.out.println(listOfReactants.size());
                //for (int e = 0; e < r.getNumReactants(); e++) {
                //    SpeciesReference spr = (SpeciesReference) listOfReactants.get(e);
                for (SpeciesReference spr : r.getListOfReactants()) {
                    System.out.println(spr.getSpecies());
                    Species sp = model.getSpecies(spr.getSpecies());
                    System.out.println(sp.getName());
                    AntNode spNode = g.getNode(sp.getId());
                    if (spNode == null) {
                        spNode = new AntNode(sp.getId(), sp.getName());
                    }
                    //this.setPosition(spNode, g);
                    g.addNode2(spNode);
                    g.addEdge(addEdge(spNode, reactionNode, sp.getId(), direction));

                }

                // ListOf listOfProducts = r.getListOfProducts();
                //for (int e = 0; e < r.getNumProducts(); e++) {
                //    SpeciesReference spr = (SpeciesReference) listOfProducts.get(e);
                for (SpeciesReference spr : r.getListOfProducts()) {
                    Species sp = model.getSpecies(spr.getSpecies());

                    AntNode spNode = g.getNode(sp.getId());
                    if (spNode == null) {
                        spNode = new AntNode(sp.getId(), sp.getName());
                    }
                    //this.setPosition(spNode, g);
                    g.addNode2(spNode);
                    g.addEdge(addEdge(reactionNode, spNode, sp.getId(), direction));

                }

            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        System.out.println(g.getNumberOfNodes());
        return g;
    }

    private void setPosition(AntNode node, AntGraph graph) {
        if (graph != null) {
            AntNode gNode = graph.getNode(node.getId());
            if (gNode != null) {
                node.setPosition(gNode.getPosition());
            }
        }
    }

    private int getDirection(Reaction r) {
        int direction = 2;

        Dataset dataset = this.model.getDataset();
        Double lb = dataset.getLowerBound(r.getId());
        Double ub = dataset.getUpperBound(r.getId());
        Double flux = dataset.getFlux(r.getId());
        if (flux != null) {
            if (flux > 0) {
                direction = 1;
            } else if (flux < 0) {
                direction = 0;
            }
        } else {
            if (ub > 0 && lb < 0) {
                direction = 2;
            } else if (ub > 0) {
                direction = 1;
            } else {
                direction = 0;
            }
        }
        return direction;
    }

    private AntEdge addEdge(AntNode node1, AntNode node2, String name, int direction) {
        AntEdge e = null;
        switch (direction) {
            case 1:
                e = new AntEdge(name + " - " + uniqueId.nextId(), node1, node2);
                e.setDirection(true);
                break;
            case 2:
                e = new AntEdge(name + " - " + uniqueId.nextId(), node1, node2);
                e.setDirection(false);
                break;
            case 0:
                e = new AntEdge(name + " - " + uniqueId.nextId(), node2, node1);
                e.setDirection(true);
                break;
            default:
                break;
        }
        return e;
    }

    /**
     * Tooltips
     *
     */
    class ToolTipHeader extends JTableHeader {

        private static final long serialVersionUID = 1L;
        String[] toolTips;

        public ToolTipHeader(TableColumnModel model) {
            super(model);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int col = columnAtPoint(e.getPoint());
            int modelCol = getTable().convertColumnIndexToModel(col);

            String retStr;
            try {
                retStr = toolTips[modelCol];
            } catch (NullPointerException ex) {
                retStr = "";
                System.out.println("NullPointer Exception tooltips");
            } catch (ArrayIndexOutOfBoundsException ex) {
                retStr = "";
                System.out.println("ArrayIndexOutOfBoundsException tooltips");
            }
            if (retStr.length() < 1) {
                retStr = super.getToolTipText(e);
            }
            return retStr;
        }

        public void setToolTipStrings(String[] toolTips) {
            this.toolTips = toolTips;
        }
    }

    /**
     * Push header
     *
     */
    class HeaderListener extends MouseAdapter {

        JTableHeader header;
        ButtonHeaderRenderer renderer;

        HeaderListener(JTableHeader header, ButtonHeaderRenderer renderer) {
            this.header = header;
            this.renderer = renderer;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            int col = header.columnAtPoint(e.getPoint());
            renderer.setPressedColumn(col);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            renderer.setPressedColumn(-1); // clear                        
        }
    }

    /**
     * Button header
     *
     */
    class ButtonHeaderRenderer extends JButton implements TableCellRenderer {

        int pushedColumn;

        public ButtonHeaderRenderer() {
            pushedColumn = -1;
            setMargin(new Insets(0, 0, 0, 0));
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            setText((value == null) ? "" : value.toString());
            boolean isPressed = (column == pushedColumn);
            getModel().setPressed(isPressed);
            getModel().setArmed(isPressed);
            return this;
        }

        public void setPressedColumn(int col) {
            pushedColumn = col;
        }
    }

    /**
     * Number renderer
     *
     */
    class NumberRenderer
            extends DefaultTableCellRenderer {

        private NumberFormat formatter;

        public NumberRenderer() {
            this(NumberFormat.getNumberInstance());
        }

        public NumberRenderer(NumberFormat formatter) {
            super();
            this.formatter = formatter;
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public void setValue(Object value) {
            if ((value != null) && (value instanceof Number)) {
                value = formatter.format(value);
            }

            super.setValue(value);
        }
    }

    /**
     * Defines the action of the keys in the table
     *
     */
    class register {

        int[] columnIndex;
        int[] rowIndex;
        Object[] values;
        Object[] newValues;

        public register(int[] columnIndex, int[] rowIndex) {
            this.columnIndex = columnIndex;
            this.rowIndex = rowIndex;
            values = new Object[columnIndex.length * rowIndex.length];
            newValues = new Object[columnIndex.length * rowIndex.length];
        }

        private register(int startRow, int rowCount, int startCol, int columnCount) {
            rowIndex = new int[rowCount];
            columnIndex = new int[columnCount];
            for (int i = 0; i < rowCount; i++) {
                rowIndex[i] = startRow + i;
            }
            for (int i = 0; i < columnCount; i++) {
                columnIndex[i] = startCol + i;
            }
            values = new Object[columnIndex.length * rowIndex.length];
            newValues = new Object[columnIndex.length * rowIndex.length];
        }

        public void getValues() {
            int cont = 0;
            for (int row : rowIndex) {
                for (int column : columnIndex) {
                    try {
                        values[cont++] = table.getValueAt(row, column);
                    } catch (Exception e) {
                    }
                }
            }
        }

        public void getNewValues() {
            int cont = 0;
            for (int row : rowIndex) {
                for (int column : columnIndex) {
                    try {
                        newValues[cont++] = table.getValueAt(row, column);
                    } catch (Exception e) {
                    }
                }
            }
        }

        public void back() {
            int cont = 0;
            for (int row : rowIndex) {
                for (int column : columnIndex) {
                    table.setValueAt(values[cont++], row, column);
                }
            }
        }

        public void forward() {
            int cont = 0;
            for (int row : rowIndex) {
                for (int column : columnIndex) {
                    table.setValueAt(newValues[cont++], row, column);
                }
            }
        }
    }
}
