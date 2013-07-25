package org.broadinstitute.gpinformatics.mercury.presentation.transfervis;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxResources;
import com.mxgraph.view.mxGraph;
import org.broadinstitute.gpinformatics.mercury.boundary.graph.Graph;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferEntityGrapher;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Frame holding JGraph control and search controls
 */
public class TransferVisualizerFrame extends JFrame {
    private JTextField plateBarcodeTF;
    private mxGraph mxGraph;
    private String barcode;
    private TransferVisualizerClient transferVisualizerClient;
    private JPopupMenu popupMenu;
    private List<JCheckBox> alternativeIdCheckBoxes = new ArrayList<>();
    private static final Pattern FILL_COLOR_PATTERN = Pattern.compile("fillColor=[^;]*;");
    private mxICell clickedCell;
    private JLabel status = new JLabel(" ");
    private JFrame statusFrame = new JFrame();
    private mxGraphComponent graphComponent;

    public TransferVisualizerFrame() throws HeadlessException {
        this.setTitle("Transfer Visualizer");

        statusFrame.getContentPane().setLayout(new FlowLayout());
        statusFrame.getContentPane().add(new JLabel("Status: "));
        statusFrame.getContentPane().add(status);
        statusFrame.pack();

        // mxGraphComponent requires an mxGraph, so create a dummy one; TransferVisualizerClient will create its own.
        graphComponent = new mxGraphComponent(new mxGraph());
        graphComponent.setPreferredSize(new Dimension(800, 800));
        graphComponent.setVerticalPageCount(1);
        graphComponent.setHorizontalPageCount(1);
        graphComponent.setBackground(Color.WHITE);

        // Create controls for searching by barcodes
        getContentPane().setLayout(new BorderLayout());
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridLayout(2, 2));
        getContentPane().add(controlsPanel, BorderLayout.NORTH);
        JPanel idPanel = new JPanel();
        controlsPanel.add(idPanel);
        JPanel zoomPanel = new JPanel();
        controlsPanel.add(zoomPanel);
        JPanel displayPanel = new JPanel();
        controlsPanel.add(displayPanel);
        JPanel printPanel = new JPanel();
        controlsPanel.add(printPanel);

        idPanel.add(new JLabel("ID Type: "));
        final JComboBox searchEntityComboBox = new JComboBox();
        for (TransferVisualizerClient.SearchType searchType : TransferVisualizerClient.SearchType.values()) {
            searchEntityComboBox.addItem(searchType);
        }
        idPanel.add(searchEntityComboBox);

        idPanel.add(new JLabel("ID: "));
        plateBarcodeTF = new JTextField();
        plateBarcodeTF.setColumns(15);
        idPanel.add(plateBarcodeTF);

        displayPanel.add(new JLabel("Display: "));
        for (TransferEntityGrapher.AlternativeId alternativeId : TransferVisualizer.AlternativeId.values()) {
            JCheckBox jCheckBox = new JCheckBox(alternativeId.getDisplayName());
            alternativeIdCheckBoxes.add(jCheckBox);
            displayPanel.add(jCheckBox);
        }

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            /**
             * Called when user clicks Search button, invoke server
             * @param e event
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                barcode = plateBarcodeTF.getText();
                try {
                    statusFrame.setVisible(true);
                    status.setText("Searching");
                    SwingWorker<Graph, Void> swingWorker = new SwingWorker<Graph, Void>() {
                        @Override
                        protected Graph doInBackground() throws Exception {
                            List<TransferVisualizer.AlternativeId> alternativeDisplayIds = new ArrayList<>();
                            for (JCheckBox alternativeIdCheckBox : alternativeIdCheckBoxes) {
                                if(alternativeIdCheckBox.isSelected()) {
                                    boolean found = false;
                                    for (TransferEntityGrapher.AlternativeId alternativeId : TransferVisualizer.AlternativeId.values()) {
                                        if(alternativeId.getDisplayName().equals(alternativeIdCheckBox.getText())) {
                                            alternativeDisplayIds.add(alternativeId);
                                            found = true;
                                            break;
                                        }
                                    }
                                    if(!found) {
                                        throw new RuntimeException("Failed to find enum for " + alternativeIdCheckBox.getText());
                                    }
                                }
                            }
                            transferVisualizerClient = new TransferVisualizerClient(barcode, alternativeDisplayIds);
                            TransferVisualizerClient.SearchType searchType = (TransferVisualizerClient.SearchType) searchEntityComboBox.getSelectedItem();
                            return transferVisualizerClient.fetchGraph(TransferVisualizerFrame.this.barcode, searchType, alternativeDisplayIds);
                        }

                        @Override
                        protected void done() {
                            try {
                                Graph graph = get();
                                renderGraph(graph);
                            } catch (InterruptedException | ExecutionException e1) {
                                JOptionPane.showMessageDialog(TransferVisualizerFrame.this, e1);
                            } finally {
                                statusFrame.setVisible(false);
                                refreshGraph();
                            }
                        }
                    };
                    swingWorker.execute();
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(TransferVisualizerFrame.this, e1);
                }
            }
        });
        displayPanel.add(searchButton);

        JButton printButton = new JButton("Print");
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PrintPreview(graphComponent);
            }
        });
        printPanel.add(printButton);

        zoomPanel.add(new JLabel("Zoom: "));
        final JComboBox zoomCombo = new JComboBox(new Object[]{"400%",
                "200%", "150%", "100%", "75%", "50%", mxResources.get("page"),
                mxResources.get("width"), mxResources.get("actualSize")});
        zoomCombo.setEditable(true);
        zoomCombo.setMaximumRowCount(9);
        zoomCombo.setSelectedIndex(3);
        zoomPanel.add(zoomCombo);

        zoomCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // Zoomcombo is changed when the scale is changed in the diagram
                // but the change is ignored here
                String zoom = zoomCombo.getSelectedItem().toString();

                if (zoom.equals(mxResources.get("page"))) {
                    graphComponent.setPageVisible(true);
                    graphComponent
                            .setZoomPolicy(mxGraphComponent.ZOOM_POLICY_PAGE);
                } else if (zoom.equals(mxResources.get("width"))) {
                    graphComponent.setPageVisible(true);
                    graphComponent
                            .setZoomPolicy(mxGraphComponent.ZOOM_POLICY_WIDTH);
                } else if (zoom.equals(mxResources.get("actualSize"))) {
                    graphComponent.zoomActual();
                } else {
                    try {
                        zoom = zoom.replace("%", "");
                        graphComponent.zoomTo(
                                Double.parseDouble(zoom) / 100.0,
                                graphComponent.isCenterZoom());
                    }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(TransferVisualizerFrame.this, ex.getMessage());
                    }
                }
            }
        });

        getContentPane().add(graphComponent, BorderLayout.CENTER);
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    final mxCell cell = (mxCell) graphComponent.getCellAt(e.getX(), e.getY());

                    if (cell != null) {
                        // Left clicks: MoreDetails button (MsgBox), MoreTransfers button (server call), tube in a receptacle (MsgBox)
                        // Right clicks: parent, child
                        if(e.isPopupTrigger()) {
                            if(cell.getValue() instanceof TransferVisualizerClient.HandlesPopups) {
                                final TransferVisualizerClient.HandlesPopups handlesPopups = (TransferVisualizerClient.HandlesPopups) cell.getValue();

                                popupMenu = new JPopupMenu();
                                for (final String popupEntry : handlesPopups.getPopupList()) {
                                    popupMenu.add(new AbstractAction(popupEntry) {
                                        private static final long serialVersionUID = -8344352900195100451L;

                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            handlesPopups.handlePopup(popupEntry);
                                        }
                                    });
                                }
                                popupMenu.show(graphComponent,
                                        e.getXOnScreen() - (int)graphComponent.getLocationOnScreen().getX(),
                                        e.getYOnScreen() - (int)graphComponent.getLocationOnScreen().getY());
                            }
                        } else {
                            if(cell.getValue() instanceof TransferVisualizerClient.HandlesClicks) {
                                final TransferVisualizerClient.HandlesClicks handlesClicks = (TransferVisualizerClient.HandlesClicks) cell.getValue();
                                statusFrame.setVisible(true);
                                status.setText("Handling click");
                                statusFrame.pack();
                                SwingWorker<String, Void> swingWorker = new SwingWorker<String, Void>() {
                                    @Override
                                    protected String doInBackground() throws Exception {
                                        return handlesClicks.handleClick();
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            String response = get();
                                            statusFrame.setVisible(false);
                                            status.setText("");
                                            if(handlesClicks.scrollTo()) {
                                                graphComponent.scrollCellToVisible(cell, true);
                                            }
                                            if(response == null) {
                                                if(handlesClicks.deleteCell()) {
                                                    // Reset the color of the previously clicked cell, we want to highlight only
                                                    // one at a time
                                                    if(clickedCell != null) {
                                                        String style = clickedCell.getStyle();
                                                        style = FILL_COLOR_PATTERN.matcher(style).replaceFirst("fillColor=white;");
                                                        mxGraph.getModel().setStyle(clickedCell, style);
                                                    }
                                                    // Highlight the clicked cell, otherwise the user may lose track of it when
                                                    // JGraph scrolls it to the center of the window
                                                    clickedCell = cell.getParent();
                                                    String style = clickedCell.getStyle();
                                                    style = FILL_COLOR_PATTERN.matcher(style).replaceFirst("fillColor=green;");
                                                    mxGraph.getModel().setStyle(clickedCell, style);
                                                    mxGraph.removeCells(new Object[]{cell});
                                                }
                                            } else {
                                                JOptionPane.showMessageDialog(TransferVisualizerFrame.this, response);
                                            }
                                        } catch (InterruptedException | ExecutionException e1) {
                                            JOptionPane.showMessageDialog(TransferVisualizerFrame.this, e1);
                                        }
                                    }
                                };
                                swingWorker.execute();
                            }
                        }
                    }
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(TransferVisualizerFrame.this, e1.toString());
                }
            }
        });

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        String receptacleBarcode = System.getProperty("jnlp.receptacleBarcode");
        if(receptacleBarcode != null && receptacleBarcode.trim().length() > 0) {
            searchEntityComboBox.setSelectedItem(TransferVisualizerClient.SearchType.SEARCH_TUBE);
            plateBarcodeTF.setText(receptacleBarcode);
            searchButton.doClick();
        }
    }

    private void renderGraph(Graph graph) {
        if(graph.getMessage() == null) {
            transferVisualizerClient.renderAndLayoutGraph(graph);
        } else {
            JOptionPane.showMessageDialog(this, graph.getMessage());
        }
    }

    private void refreshGraph() {
        mxGraph = transferVisualizerClient.getMxGraph();
        graphComponent.setGraph(transferVisualizerClient.getMxGraph());
        graphComponent.refresh();
        if(transferVisualizerClient.getHighlightVertex() != null) {
            graphComponent.scrollCellToVisible(transferVisualizerClient.getHighlightVertex(), true);
        }
    }

    public void renderVessel(LabVessel labVessel) {
        transferVisualizerClient = new TransferVisualizerClient(labVessel.getLabel(), new ArrayList<TransferVisualizer.AlternativeId>());
        Graph graph = transferVisualizerClient.fetchGraph(labVessel);
        renderGraph(graph);
        refreshGraph();
    }

    public static void main(String[] args) {
        new TransferVisualizerFrame();
    }
}
