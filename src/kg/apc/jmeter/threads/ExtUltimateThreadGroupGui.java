package kg.apc.jmeter.threads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.charting.AbstractGraphRow;
import kg.apc.charting.DateTimeRenderer;
import kg.apc.charting.GraphPanelChart;
import kg.apc.charting.rows.GraphRowSumValues;
import kg.apc.jmeter.gui.ButtonPanelAddCopyRemove;
import kg.apc.jmeter.gui.ExtButtonPanelAddCopyRemove;
import kg.apc.jmeter.gui.GuiBuilderHelper;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.gui.AbstractThreadGroupGui;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 *
 * @author apc
 */
public class ExtUltimateThreadGroupGui
        extends AbstractThreadGroupGui
        implements TableModelListener,
        CellEditorListener {

    public static final String WIKIPAGE = "ExtUltimateThreadGroup";
    private static final Logger log = LoggingManager.getLoggerForClass();
    /**
     *
     */
    protected ConcurrentHashMap<String, AbstractGraphRow> model;
    private GraphPanelChart chart;
    /**
     *
     */
    public static final String[] columnIdentifiers = new String[]{
        "Time, sec", "Threads Count"
    };
    /**
     *
     */
    public static final Class[] columnClasses = new Class[]{
        Integer.class, Integer.class
    };
    public static final Integer[] initValues = new Integer[]{
        0, 100
    };
    public static final Integer[] incValues = new Integer[]{
        60, 0
    };
    private LoopControlPanel loopPanel;
    protected PowerTableModel tableModel;
    protected JTable grid;
    protected ExtButtonPanelAddCopyRemove buttons;

    /**
     *
     */
    public ExtUltimateThreadGroupGui() {
        super();
        init();
    }

    /**
     *
     */
    protected final void init() {
        JMeterPluginsUtils.addHelpLinkToPanel(this, WIKIPAGE);
        JPanel containerPanel = new VerticalPanel();

        containerPanel.add(createParamsPanel(), BorderLayout.NORTH);
        containerPanel.add(GuiBuilderHelper.getComponentWithMargin(createChart(), 2, 2, 0, 2), BorderLayout.CENTER);
        add(containerPanel, BorderLayout.CENTER);

        // this magic LoopPanel provides functionality for thread loops
        createControllerPanel();
    }

    private JPanel createParamsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Threads Schedule"));
        panel.setPreferredSize(new Dimension(200, 200));

        JScrollPane scroll = new JScrollPane(createGrid());
        scroll.setPreferredSize(scroll.getMinimumSize());
        panel.add(scroll, BorderLayout.CENTER);
        buttons = new ExtButtonPanelAddCopyRemove(grid, tableModel, initValues, incValues);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JTable createGrid() {
        grid = new JTable();
        grid.getDefaultEditor(String.class).addCellEditorListener(this);
        createTableModel();
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setMinimumSize(new Dimension(200, 100));

        return grid;
    }

    @Override
    public String getLabelResource() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getStaticLabel() {
        return JMeterPluginsUtils.prefixLabel("Extended Ultimate Thread Group");
    }

    @Override
    public TestElement createTestElement() {
        //log.info("Create test element");
        ExtUltimateThreadGroup tg = new ExtUltimateThreadGroup();
        modifyTestElement(tg);
        tg.setComment(JMeterPluginsUtils.getWikiLinkText(WIKIPAGE));

        return tg;
    }

    @Override
    public void modifyTestElement(TestElement tg) {
        //log.info("Modify test element");
        if (grid.isEditing()) {
            grid.getCellEditor().stopCellEditing();
        }

        if (tg instanceof ExtUltimateThreadGroup) {
            ExtUltimateThreadGroup utg = (ExtUltimateThreadGroup) tg;
            CollectionProperty rows = JMeterPluginsUtils.tableModelRowsToCollectionProperty(tableModel, ExtUltimateThreadGroup.DATA_PROPERTY);
            utg.setData(rows);
            utg.setSamplerController((LoopController) loopPanel.createTestElement());
        }
        super.configureTestElement(tg);
    }

    @Override
    public void configure(TestElement tg) {
        //log.info("Configure");
        super.configure(tg);
        ExtUltimateThreadGroup utg = (ExtUltimateThreadGroup) tg;
        //log.info("Configure "+utg.getName());
        JMeterProperty threadValues = utg.getData();
        if (!(threadValues instanceof NullProperty)) {
            CollectionProperty columns = (CollectionProperty) threadValues;

            tableModel.removeTableModelListener(this);
            try {
                JMeterPluginsUtils.collectionPropertyToTableModelRows(columns, tableModel);
            } catch (IllegalArgumentException ex) {
                log.error("Error loading schedule, need to upgrade property", ex);
                JMeterPluginsUtils.collectionPropertyToTableModelCols(columns, tableModel);
            }
            tableModel.addTableModelListener(this);
            updateUI();
        } else {
            log.warn("Received null property instead of collection");
        }

        TestElement te = (TestElement) tg.getProperty(AbstractThreadGroup.MAIN_CONTROLLER).getObjectValue();
        if (te != null) {
            loopPanel.configure(te);
        }
        buttons.checkDeleteButtonStatus();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (tableModel != null) {
            ExtUltimateThreadGroup utgForPreview = new ExtUltimateThreadGroup();
            utgForPreview.setData(JMeterPluginsUtils.tableModelRowsToCollectionPropertyEval(tableModel, ExtUltimateThreadGroup.DATA_PROPERTY));
            updateChart(utgForPreview);
        }
    }

    private void updateChart(ExtUltimateThreadGroup tg) {
        tg.testStarted();
        model.clear();
        GraphRowSumValues row = new GraphRowSumValues();
        row.setColor(Color.RED);
        row.setDrawLine(true);
        row.setMarkerSize(AbstractGraphRow.MARKER_SIZE_NONE);
        row.setDrawThickLines(true);

        final HashTree hashTree = new HashTree();
        hashTree.add(new LoopController());
        int numThreads = tg.getNumThreads();
        List<JMeterThread> threads = new ArrayList<JMeterThread>();
        for (int i = 0; i < numThreads; i++) {
            threads.add(new JMeterThread(hashTree, null, null));
        }

        long now = System.currentTimeMillis();
        tg.scheduleThread(now, threads);

        chart.setxAxisLabelRenderer(new DateTimeRenderer(DateTimeRenderer.HHMMSS, now - 1)); //-1 because row.add(thread.getStartTime() - 1, 0)
        chart.setForcedMinX(now);

        row.add(now, 0);

        // users in
        log.debug("Num Threads: " + numThreads);
        for (int n = 0; n < numThreads; n++) {
            JMeterThread thread = threads.get(n);
            row.add(thread.getStartTime() - 1, 0);
            row.add(thread.getStartTime(), 1);
        }

        tg.testStarted();
        // users out
        for (int n = 0; n < tg.getNumThreads(); n++) {
            JMeterThread thread = threads.get(n);
            row.add(thread.getEndTime() - 1, 0);
            row.add(thread.getEndTime(), -1);
        }

        model.put("Expected parallel users count", row);
        chart.invalidateCache();
        chart.repaint();
    }

    private JPanel createControllerPanel() {
        loopPanel = new LoopControlPanel(false);
        LoopController looper = (LoopController) loopPanel.createTestElement();
        looper.setLoops(-1);
        looper.setContinueForever(true);
        loopPanel.configure(looper);
        return loopPanel;
    }

    private Component createChart() {
        chart = new GraphPanelChart(false, true);
        model = new ConcurrentHashMap<String, AbstractGraphRow>();
        chart.setRows(model);
        chart.getChartSettings().setDrawFinalZeroingLines(true);
        chart.setxAxisLabel("Elapsed time");
        chart.setYAxisLabel("Number of active threads");
        chart.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        return chart;
    }

    public void tableChanged(TableModelEvent e) {
        //log.info("Model changed");
        updateUI();
    }

    private void createTableModel() {
        tableModel = new PowerTableModel(columnIdentifiers, columnClasses);
        tableModel.addTableModelListener(this);
        grid.setModel(tableModel);
    }

    public void editingStopped(ChangeEvent e) {
        //log.info("Editing stopped");
        updateUI();
    }

    public void editingCanceled(ChangeEvent e) {
        // no action needed
    }

    @Override
    public void clearGui() {
        super.clearGui();
        tableModel.clearData();
    }
}
