package kg.apc.jmeter.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import org.apache.jmeter.gui.util.PowerTableModel;

/**
 *
 * @author undera
 */
public class ExtAddRowAction
        implements ActionListener {

    private JTable grid;
    private PowerTableModel tableModel;
    private JButton deleteRowButton;
    private Integer[] initValues;
    private Integer[] incValues;
    private JComponent sender;

    public ExtAddRowAction(JComponent aSender, JTable grid, PowerTableModel tableModel, JButton deleteRowButton, Integer[] initValues, Integer[] incValues) {
        this.grid = grid;
        this.tableModel = tableModel;
        this.deleteRowButton = deleteRowButton;
        this.initValues = initValues;
        this.incValues = incValues;
        this.sender = aSender;
    }

    public void actionPerformed(ActionEvent e) {
        if (grid.isEditing()) {
            TableCellEditor cellEditor = grid.getCellEditor(grid.getEditingRow(), grid.getEditingColumn());
            cellEditor.stopCellEditing();
        }

        int n = tableModel.getColumnCount();
        Object[] newRow = new Object[n];
        if (tableModel.getRowCount() == 0) {
            System.arraycopy(initValues, 0, newRow, 0, n);
        }
        else {
            Object[] lastRow = tableModel.getRowData(tableModel.getRowCount() - 1);
            System.arraycopy(lastRow, 0, newRow, 0, n);
        }
        for (int i = 0; i < newRow.length; i++) {
            newRow[i] = (Integer) newRow[i] + incValues[i];
        }
        tableModel.addRow(newRow);
        tableModel.fireTableDataChanged();

        // Enable DELETE (which may already be enabled, but it won't hurt)
        deleteRowButton.setEnabled(true);

        // Highlight (select) the appropriate row.
        int rowToSelect = tableModel.getRowCount() - 1;
        if (rowToSelect < grid.getRowCount()) {
            grid.setRowSelectionInterval(rowToSelect, rowToSelect);
        }
        sender.updateUI();
    }
}
