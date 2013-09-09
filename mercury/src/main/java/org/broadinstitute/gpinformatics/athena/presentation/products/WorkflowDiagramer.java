package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class generates diagrams of workflows.
 */
public class WorkflowDiagramer implements Serializable {
    private static Log logger = LogFactory.getLog(WorkflowDiagramer.class);
    private WorkflowLoader workflowLoader;
    public static final String DOT_EXTENSION = ".dot";
    public static final String DIAGRAM_FILE_EXTENSION = ".png";  // Could also be pdf.
    public static final String DIAGRAM_DIRECTORY = System.getProperty("java.io.tmpdir") + File.separator +
                                                   "images" + File.separator + "workflow" + File.separator;

    public WorkflowDiagramer() {
    }

    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
    }

    /**
     * Parses the workflow config and creates directed graphs of all workflow-effectiveDate combinations,
     * with the effective date being the later of workflow def date and subordinate process def date.
     *
     * @return List of workflow graphs ordered as found in WorkflowConfig.
     */
    List<WorkflowGraph> createGraphs() throws Exception {
        WorkflowConfig workflowConfig = workflowLoader.load();

        List<WorkflowGraph> graphs = new ArrayList<>();
        if (workflowConfig == null) {
            return graphs;
        }

        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            String workflowName = workflowDef.getName();

            // For each starting date relevant to this workflowDef, finds the workflow that is valid on that date,
            // and the process that is valid on that date, and graphs that.  If no valid combination exists the
            // graph will be empty, and is suppressed.
            for (Date startDate : workflowDef.getEffectiveDates()) {

                ProductWorkflowDefVersion workflow = workflowDef.getEffectiveVersion(startDate);
                if (workflow != null) {

                    WorkflowGraph graph = new WorkflowGraph(workflowName, workflow.getVersion(), startDate,
                            workflowDef.getWorkflowImageFileName(startDate));

                    List<WorkflowGraphNode> previousNodes = new ArrayList<>();
                    Set<String> visitedProcessStep = new HashSet<>();
                    int nodeIndex = 0;

                    for (WorkflowProcessDef processDef : workflow.getWorkflowProcessDefs()) {
                        WorkflowProcessDefVersion process = processDef.getEffectiveVersion(startDate);
                        if (process != null) {

                            for (WorkflowStepDef step : process.getWorkflowStepDefs()) {

                                if (!visitedProcessStep.add(processDef.getName() + "//" + step.getName())) {
                                    throw new Exception("Workflow config contains a cyclic graph in process " +
                                                        processDef.getName() + " at step " + step.getName());
                                }

                                if (CollectionUtils.isEmpty(step.getLabEventTypes())) {

                                    // Workflow step having no events is either an entry point or ignored.
                                    if (CollectionUtils.isEmpty(previousNodes) &&
                                        CollectionUtils.isEmpty(graph.getNodes())) {
                                        WorkflowGraphNode entryNode =
                                                new WorkflowGraphNode(nodeIndex++, step.getName());
                                        graph.add(entryNode);
                                        previousNodes.add(entryNode);
                                    }

                                } else {
                                    for (LabEventType eventType : step.getLabEventTypes()) {

                                        // Creates entry point if this is the first node encountered.
                                        if (CollectionUtils.isEmpty(previousNodes) &&
                                            CollectionUtils.isEmpty(graph.getNodes())) {
                                            WorkflowGraphNode entryNode = new WorkflowGraphNode(nodeIndex++,
                                                    step.isReEntryPoint() ? "Reenter" : "Start");
                                            graph.add(entryNode);
                                            previousNodes.add(entryNode);
                                        }

                                        WorkflowGraphNode toNode = new WorkflowGraphNode(nodeIndex++, "");
                                        graph.add(toNode);

                                        for (WorkflowGraphNode fromNode : previousNodes) {
                                            graph.add(new WorkflowGraphEdge(fromNode, toNode, eventType.getName()));
                                        }

                                        if (!step.isOptional()) {
                                            // All subsequent edges must come out of a non-optional step.
                                            previousNodes.clear();
                                        }
                                        previousNodes.add(toNode);
                                    }
                                }
                            }
                        }
                    }
                    if (!graph.isEmpty()) {
                        graphs.add(graph);
                    }
                }
            }
        }
        return graphs;
    }

    /**
     * Creates graphs, writes their .dot files, and converts the .dot file to a diagram file in DIAGRAM_DIRECTORY
     */
    public void makeAllDiagramFiles() throws Exception {
        File diagramDirectory = makeDiagramFileDir();
        for (File diagramFile : listDiagramFiles()) {
            FileUtils.deleteQuietly(diagramFile);
        }

        List<WorkflowGraph> graphs = createGraphs();
        writeDotFiles(diagramDirectory, graphs);

        // Runs dot on each .dot file to create a diagram file.
        for (WorkflowGraph graph : graphs) {
            String diagramFileName = graph.getDiagramFileName();

            File diagramFile = new File(diagramDirectory, diagramFileName);
            File dotFile = new File(diagramDirectory, graph.getDiagramFileName() + DOT_EXTENSION);

            logger.info("Writing workflow diagram file " + diagramFile.getAbsolutePath());

            runCommand("dot -T" + DIAGRAM_FILE_EXTENSION.substring(1) +
                       " -o " + diagramFile.getAbsolutePath() +
                       " " + dotFile.getAbsolutePath());

            FileUtils.deleteQuietly(dotFile);
        }
    }

    protected void runCommand(@Nonnull final String command) throws Exception {
        final Process proc = Runtime.getRuntime().exec(command);
        final StringBuilder stdOutString = new StringBuilder();
        final StringBuilder stdErrString = new StringBuilder();
        Thread stdOutThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                try {
                    while (reader.ready()) {
                        stdOutString.append(reader.readLine());
                    }
                } catch (IOException e) {
                    logger.error("While running \"" + command + "\" ", e);
                }
            }
        });
        stdOutThread.start();

        Thread stdErrThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                try {
                    while (reader.ready()) {
                        stdErrString.append(reader.readLine());
                    }
                } catch (IOException e) {
                    stdErrString.append(" Caught " + e);
                }
            }
        });
        stdErrThread.start();

        int returnCode = proc.waitFor();

        stdOutThread.join();
        stdErrThread.join();
        if (stdErrString.length() > 0) {
            throw new Exception("Error running command \"" + command + "\" : " + stdErrString.toString());
        }
        if (stdOutString.length() > 0) {
            logger.debug("stdOut: " + stdOutString.toString());
        }
    }


    /**
     * Given a workflow graph, formats the nodes and edges into a directed graph .dot file.
     * @param directory the directory that gets the .dot files
     * @param graphs the workflow graph objects.
     * @throws Exception
     */
    void writeDotFiles(@Nonnull File directory, @Nonnull List<WorkflowGraph> graphs) throws Exception {
        for (WorkflowGraph graph : graphs) {
            File file = new File(directory, graph.getDiagramFileName() + DOT_EXTENSION);
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            // Writes .dot file header, nodes, edges, and trailer.
            writer.println("digraph G {");
            for (WorkflowGraphNode node : graph.getNodes()) {
                writer.println(node.toDotString());
            }
            for (WorkflowGraphEdge edge : graph.getEdges()) {
                writer.println(edge.toDotString());
            }
            writer.println("}");
            writer.flush();
            writer.close();
        }
    }

    // Creates a new directory or reuses an existing one.
    public static File makeDiagramFileDir() throws Exception {
        File directory = new File(DIAGRAM_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    /** Returns the diagram files that have already been created. */
    public static File[] listDiagramFiles() throws Exception {
        File destDir = makeDiagramFileDir();
        File[] diagramFiles = destDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                return filename.endsWith(DIAGRAM_FILE_EXTENSION);
            }
        });
        return diagramFiles;
    }


    // Class represents a node in the workflow graph.
    private class WorkflowGraphNode implements Comparable {
        private final int id;
        private final String label;

        WorkflowGraphNode(int id, String label) {
            this.id = id;
            this.label = label;
        }

        int getId() {
            return id;
        }

        boolean equals(WorkflowGraphNode other) {
            return id == other.id;
        }

        @Override
        public int compareTo(Object o) {
            return id - ((WorkflowGraphNode) o).id;
        }

        /** Returns a .dot file representation of a node. */
        String toDotString() {
            return String.valueOf(id) + "[label=\"" + id + " " + label + "\"];";
        }
    }

    // Class represents an edge in the workflow graph.
    private class WorkflowGraphEdge implements Comparable {
        private final int fromNodeId;
        private final int toNodeId;
        private final String eventName;

        WorkflowGraphEdge(WorkflowGraphNode fromNode, WorkflowGraphNode toNodeId, String eventName) {
            this.fromNodeId = fromNode.getId();
            this.toNodeId = toNodeId.getId();
            assert (eventName != null);
            this.eventName = eventName;
        }

        boolean equals(WorkflowGraphEdge other) {
            return fromNodeId == other.fromNodeId
                   && toNodeId == other.toNodeId
                   && eventName.equals(other.eventName);
        }

        String toDotString() {
            return fromNodeId + " -> " + toNodeId + "[label=\"" + eventName + "\"];";
        }

        @Override
        public int compareTo(Object o) {
            WorkflowGraphEdge other = (WorkflowGraphEdge) o;
            if (eventName.equals(other.eventName)) {
                if (fromNodeId == other.fromNodeId) {
                    return toNodeId - other.toNodeId;
                } else {
                    return fromNodeId - other.fromNodeId;
                }
            }
            return eventName.compareTo(other.eventName);
        }
    }

    // Class represents a workflow graphs, with all possible edges.
    protected class WorkflowGraph {
        private final String workflowName;
        private final String workflowVersion;
        private final Date effectiveDate;
        private final SortedSet<WorkflowGraphNode> nodes = new TreeSet<>();
        private final SortedSet<WorkflowGraphEdge> edges = new TreeSet<>();
        private final String diagramFileName;

        WorkflowGraph(String workflowName, String workflowVersion, Date effectiveDate, String diagramFileName) {
            this.workflowName = workflowName;
            this.workflowVersion = workflowVersion;
            this.effectiveDate = effectiveDate;
            this.diagramFileName = diagramFileName;
        }

        void add(WorkflowGraphNode node) {
            nodes.add(node);
        }

        void add(WorkflowGraphEdge edge) {
            edges.add(edge);
        }

        SortedSet<WorkflowGraphNode> getNodes() {
            return nodes;
        }

        SortedSet<WorkflowGraphEdge> getEdges() {
            return edges;
        }

        String getWorkflowName() {
            return workflowName;
        }

        String getWorkflowVersion() {
            return workflowVersion;
        }

        Date getEffectiveDate() {
            return effectiveDate;
        }

        String getDiagramFileName() {
            return diagramFileName;
        }

        /** Returns true if graph has no nodes or edges. */
        boolean isEmpty() {
            return nodes.size() == 0 || edges.size() == 0;
        }

        // Comparison is based on graph topology and edge names.  Assumes graphs were built by the same code in
        // a deterministic way.
        boolean equals(WorkflowGraph other) {
            assert(!isEmpty());
            assert(!other.isEmpty());
            if (nodes.size() != other.nodes.size() || edges.size() != other.edges.size()) {
                return false;
            } else {
                // Compares nodes.
                Iterator<WorkflowGraphNode> nodeIterator = other.nodes.iterator();
                for (WorkflowGraphNode node : nodes) {
                    if (!node.equals(nodeIterator.next())) {
                        return false;
                    }
                }
                // Compares edges.
                Iterator<WorkflowGraphEdge> edgeIterator = other.edges.iterator();
                for (WorkflowGraphEdge edge : edges) {
                    if (!edge.equals(edgeIterator.next())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
