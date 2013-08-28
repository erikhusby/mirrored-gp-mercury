package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import javax.validation.constraints.Null;
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
    private static final String PNG_EXTENSION = ".png";
    private static final String DOT_EXTENSION = ".dot";

    public WorkflowDiagramer() {
    }

    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
    }

    /**
     * Parses the workflow config and creates directed graphs of all defined workflows in combination with
     * all workflow def and process def date ranges.
     *
     * @return List of workflow graphs ordered as found in WorkflowConfig.
     */
    protected List<WorkflowGraph> createGraphs() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        List<WorkflowGraph> graphs = new ArrayList<>();
        if (workflowConfig == null) {
            return graphs;
        }

        // First collects the start dates for product-process pairings.  There can be multiple workflowDefs
        // having the same name (and version, which we ignore here) only distinguished by effective date.
        Map<String, Set<Date>> mapNameToStartDates = new HashMap<>();
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            Set<Date> startDates = mapNameToStartDates.get(workflowDef.getName());
            if (startDates == null) {
                startDates = new HashSet<Date>();
                mapNameToStartDates.put(workflowDef.getName(), startDates);
            }
            for (ProductWorkflowDefVersion workflow : workflowDef.getWorkflowVersionsDescEffDate()) {
                for (WorkflowProcessDef wf : workflow.getWorkflowProcessDefs()) {
                    for (WorkflowProcessDefVersion process : wf.getProcessVersionsDescEffDate()) {
                        if (workflow.getEffectiveDate().after(process.getEffectiveDate())) {
                            startDates.add(workflow.getEffectiveDate());
                        } else {
                            startDates.add(process.getEffectiveDate());
                        }
                    }
                }
            }
        }

        // Iterates on each workflowDef name and generates workflow graphs at each start date.
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            String workflowName = workflowDef.getName();
            // Examine dates in order.
            List<Date> startDates = new ArrayList<>(mapNameToStartDates.get(workflowName));
            Collections.sort(startDates);
            for (Date startDate : startDates) {
                WorkflowGraph graph =
                        new WorkflowGraph(workflowName, workflowDef.getEffectiveVersion().getVersion(), startDate);
                List<WorkflowGraphNode> previousNodes = new ArrayList<>();

                ProductWorkflowDefVersion workflow = workflowDef.getEffectiveVersion(startDate);
                if (workflow != null) {
                    for (WorkflowProcessDef processDef : workflow.getWorkflowProcessDefs()) {
                        WorkflowProcessDefVersion process = processDef.getEffectiveVersion(startDate);
                        if (process != null) {
                            for (WorkflowStepDef step : process.getWorkflowStepDefs()) {
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
                                                new WorkflowGraphNode(step.isDeadEndBranch() ? "(dead end)" : null);
                                        graph.add(toNode);
                                        for (WorkflowGraphNode fromNode : previousNodes) {
                                            graph.add(new WorkflowGraphEdge(fromNode, toNode, eventType.getName()));
                                        }
                                        // All subsequent edges must come out of a non-optional step.
                                        if (!step.isOptional()) {
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
     * Creates graphs, writes their .dot files, and convert the .dot file to a .png file that is put in an obscure
     * directory (java.io.tmpdir).
     * @return the directory name where the .png files were put
     */
    public String makeAllDiagramFiles() throws Exception {
        return makeAllDiagramFiles(null);
    }

    /**
     * Creates graphs, writes their .dot files, and convert the .dot file to a .png file that is put in the specified
     * directory.
     * @return the directory name where the .png files were put
     */
    public String makeAllDiagramFiles(String directoryName) throws Exception {
        String tmpDirName = System.getProperty("java.io.tmpdir");
        String destDirName = (StringUtils.isEmpty(directoryName) ? tmpDirName : directoryName);
        File destDir = new File(destDirName);
        if (!destDir.exists() || !destDir.isDirectory()) {
            throw new Exception("Cannot use " + destDir.getAbsolutePath() + " as a directory.");
        }

        List<WorkflowGraph> graphs = createGraphs();

        // Puts the .dot files in tmp dir
        List<String> fileNames = makeFileNames(graphs);
        writeDotFiles(tmpDirName, fileNames, graphs);

        // Runs dot on each .dot file to create the .png.
        for (String baseFileName : fileNames) {
            String dotFileName = baseFileName + DOT_EXTENSION;
            String pngFileName = baseFileName + PNG_EXTENSION;
            File dotFile = new File(tmpDirName, dotFileName);
            File pngFile = new File(destDirName, pngFileName);
            logger.info("Writing workflow diagram file " + pngFile.getAbsolutePath());
            runCommand("dot -Tpng -o " + pngFile.getAbsolutePath() + " " + dotFile.getAbsolutePath());
        }
        return destDirName;
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
                    logger.error("While running \"" + command + "\" ", e);
                }
            }
        });
        stdErrThread.start();

        int returnCode = proc.waitFor();

        stdOutThread.join();
        stdErrThread.join();
        if (stdErrString.length() > 0) {
            logger.error("stdErr: " + stdErrString.toString());
        }
        if (stdOutString.length() > 0) {
            logger.info("stdOut: " + stdOutString.toString());
        }
    }


    /**
     * Given the list of graphs, makes a suitable filename from graph name, version, and startDate.
     */
    public List<String> makeFileNames(@Nonnull List<WorkflowGraph> graphs) {
        List<String> list = new ArrayList<>();
        for (WorkflowGraph graph : graphs) {
            String filename = graph.getWorkflowName() + "_" + graph.getWorkflowVersion() + "_" +
                              graph.getEffectiveDate().getTime();
            list.add(filename.replaceAll("[ ]+", "_"));
        }
        return list;
    }

    /**
     * Given a workflow graph, formats the nodes and edges into a .dot file.
     * @param directoryName the directory that gets the .dot files
     * @param baseFileNames the base filenames for the .dot files, in the same order as the graphs list.
     * @param graphs the workflow graph objects.
     * @throws Exception
     */
    protected void writeDotFiles(@Nonnull String directoryName,
                                 @Nonnull List<String> baseFileNames,
                                 @Nonnull List<WorkflowGraph> graphs) throws Exception {

        File directory = new File(directoryName);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new Exception("Cannot use " + directory.getCanonicalPath() + " as a directory.");
        }
        if (baseFileNames.size() != graphs.size()) {
            throw new Exception("Number of filenames does not match number of graphs.");
        }

        for (int idx = 0; idx < baseFileNames.size(); ++idx) {
            File file = new File(directory, baseFileNames.get(idx) + DOT_EXTENSION);
            if (file.exists()) {
                file.delete();
            }
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            WorkflowGraph graph = graphs.get(idx);
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

        WorkflowGraphNode() {
            this(null);
        }

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
            return String.valueOf(id) + (StringUtils.isEmpty(label) ? ";" : "[label=\"" + label + "\"];");
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

        WorkflowGraph(String workflowName, String workflowVersion, Date effectiveDate) {
            this.workflowName = workflowName;
            this.workflowVersion = workflowVersion;
            this.effectiveDate = effectiveDate;
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
