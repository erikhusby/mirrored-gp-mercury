package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.collections.CollectionUtils;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class generates diagrams of workflows.
 */
public class WorkflowDiagramer implements Serializable {
    private static Log logger = LogFactory.getLog(WorkflowDiagramer.class);
    private static Object nodeIdMutex = new Object();
    private static int nextNodeId = 0;
    private WorkflowLoader workflowLoader;
    private static final String DOT_EXTENSION = ".dot";
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

            for (Date startDate : workflowDef.getEffectiveDates()) {

                WorkflowGraph graph = new WorkflowGraph(workflowName, workflowDef.getEffectiveVersion().getVersion(),
                        startDate, workflowDef.getWorkflowImageFileName(startDate));

                List<WorkflowGraphNode> previousNodes = new ArrayList<>();

                ProductWorkflowDefVersion workflow = workflowDef.getEffectiveVersion(startDate);
                if (workflow != null) {

                    Set<String> visitedProcessStep = new HashSet<>();

                    for (WorkflowProcessDef processDef : workflow.getWorkflowProcessDefs()) {
                        WorkflowProcessDefVersion process = processDef.getEffectiveVersion(startDate);
                        if (process != null) {

                            for (WorkflowStepDef step : process.getWorkflowStepDefs()) {

                                if (!visitedProcessStep.add(processDef.getName() + "//" + step.getName())) {
                                    throw new Exception("Workflow config contains a cyclic graph in process " +
                                                        processDef.getName() + " at step " + step.getName());
                                }

                                if (graph.isEmpty() && CollectionUtils.isEmpty(step.getLabEventTypes())) {
                                    // Workflow step having no events can only be an entry point.
                                    WorkflowGraphNode entryNode = new WorkflowGraphNode(step.getName());
                                    graph.add(entryNode);
                                    previousNodes.add(entryNode);

                                } else {
                                    for (LabEventType eventType : step.getLabEventTypes()) {

                                        // Entry point is a free-standing node.
                                        if (step.isEntryPoint() || step.isReEntryPoint() || graph.isEmpty()) {
                                            WorkflowGraphNode entryNode =
                                                    new WorkflowGraphNode(step.isReEntryPoint() ? "Reenter" : "Start");
                                            graph.add(entryNode);
                                            previousNodes.add(entryNode);
                                        }

                                        WorkflowGraphNode toNode =
                                                new WorkflowGraphNode(step.isDeadEndBranch() ? "(dead end)" : "");
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
                }
                if (!graph.isEmpty() && isUniqueGraph(graph, graphs)) {
                    graphs.add(graph);
                }
            }
        }
        return graphs;
    }

    /**
     * Creates graphs, writes their .dot files, and converts the .dot file to a diagram file in DIAGRAM_DIRECTORY
     * @return the directory name where the diagram files were put
     */
    public String makeAllDiagramFiles() throws Exception {
        List<WorkflowGraph> graphs = createGraphs();

        File diagramDirectory = createOrCleanoutDir(DIAGRAM_DIRECTORY);
        writeDotFiles(diagramDirectory, graphs);

        // Runs dot on each .dot file to create a diagram file.
        for (WorkflowGraph graph : graphs) {
            String diagramFileName = graph.getDiagramFileName();

            // If filename has a 3 letter extension, uses it as the graph type, otherwise uses 'png'.
            int dotIdx = diagramFileName.lastIndexOf(".");
            String diagramFileType =
                    (dotIdx == diagramFileName.length() - 4) ? diagramFileName.substring(dotIdx + 1) : "png";

            File diagramFile = new File(DIAGRAM_DIRECTORY, diagramFileName);
            File dotFile = new File(DIAGRAM_DIRECTORY, graph.getDiagramFileName() + DOT_EXTENSION);

            logger.info("Writing workflow diagram file " + diagramFile.getAbsolutePath());

            runCommand("dot -T" + diagramFileType +
                       " -o " + diagramFile.getAbsolutePath() +
                       " " + dotFile.getAbsolutePath());

            dotFile.delete();
        }
        return DIAGRAM_DIRECTORY;
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
            if (file.exists()) {
                file.delete();
            }
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

    // Creates a new directory or reuses an existing one after deleting any files found in it.
    private File createOrCleanoutDir(String directoryName) throws Exception {
        File directory = new File(directoryName);
        if (directory.exists() && !directory.isDirectory()) {
            throw new Exception("Cannot use " + directory.getCanonicalPath() + " as a directory.");
        }
        if (!directory.exists()) {
            directory.mkdirs();
        } else {
            for (File file : directory.listFiles()) {
                file.delete();
            }
        }
        return directory;
    }

    // Determines if the testGraph matches one of the existing graphs, ignoring effective date.
    private boolean isUniqueGraph(WorkflowGraph testGraph, Collection<WorkflowGraph> existingGraphs) {
        for (WorkflowGraph graph : existingGraphs) {
            if (testGraph.getWorkflowName().equals(graph.getWorkflowName()) &&
                testGraph.getWorkflowVersion().equals(graph.getWorkflowVersion()) &&
                testGraph.equals(graph)) {
                return false;
            }
        }
        return true;
    }


    // Class represents a node in the workflow graph.
    private class WorkflowGraphNode implements Comparable {
        private final int id;
        private final String label;

        WorkflowGraphNode(String label) {
            synchronized (nodeIdMutex) {
                id = nextNodeId++;
            }
            this.label = label;
        }

        int getId() {
            return id;
        }

        boolean equals(WorkflowGraphNode other, int offset) {
            return id == (other.id + offset);
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

        int getFromNodeId() {
            return fromNodeId;
        }

        int getToNodeId() {
            return toNodeId;
        }

        boolean equals(WorkflowGraphEdge other, int offset) {
            return fromNodeId == other.fromNodeId + offset
                   && toNodeId == other.toNodeId + offset
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
        // a deterministic way, so that graph equivalence is when the graphs' node ids differ by a constant offset.
        boolean equals(WorkflowGraph other) {
            assert(!isEmpty());
            assert(!other.isEmpty());
            if (nodes.size() != other.nodes.size() || edges.size() != other.edges.size()) {
                return false;
            } else {
                int nodeIdOffset = other.nodes.first().getId() - nodes.first().getId();
                // Compares nodes.
                Iterator<WorkflowGraphNode> nodeIterator = other.nodes.iterator();
                for (WorkflowGraphNode node : nodes) {
                    if (!node.equals(nodeIterator.next(), nodeIdOffset)) {
                        return false;
                    }
                }
                // Compares edges.
                Iterator<WorkflowGraphEdge> edgeIterator = other.edges.iterator();
                for (WorkflowGraphEdge edge : edges) {
                    if (!edge.equals(edgeIterator.next(), nodeIdOffset)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
