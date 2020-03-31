package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
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
import javax.enterprise.context.Dependent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class generates diagrams of workflows.
 */
@Dependent
public class WorkflowDiagrammer implements Serializable {
    private static Log logger = LogFactory.getLog(WorkflowDiagrammer.class);
    private WorkflowConfig workflowConfig;
    static final String DOT_EXTENSION = ".dot";
    public static final String DIAGRAM_FILE_EXTENSION = ".png";
    public static final String DIAGRAM_DIRECTORY = System.getProperty("java.io.tmpdir") + File.separator +
                                                   "images" + File.separator + "workflow" + File.separator;

    public WorkflowDiagrammer() {
        workflowConfig = WorkflowLoader.getWorkflowConfig();
    }

    /** Setter used for testing purposes. */
    void setWorkflowConfig(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
    }

    @Nonnull
    public static String getWorkflowImageFileName(@Nonnull ProductWorkflowDef workflowDef,
                                                  @Nonnull Date effectiveDate) {
        return workflowDef.getName().replaceAll("\\s+", "_") + "_" +
               workflowDef.getEffectiveVersion(effectiveDate).getVersion() + "_" +
               effectiveDate.getTime() + WorkflowDiagrammer.DIAGRAM_FILE_EXTENSION;
    }


    /**
     * Parses the workflow config and creates directed graphs of all workflow-effectiveDate combinations,
     * with the effective date being the later of workflow def date and subordinate process def date.
     *
     * @return List of workflow graphs ordered as found in WorkflowConfig.
     */
    List<Graph> createGraphs() throws Exception {
        List<Graph> graphs = new ArrayList<>();
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            String workflowName = workflowDef.getName();

            // For each starting date relevant to this workflowDef, finds the workflow that is valid on that date,
            // and the process that is valid on that date, and graphs that.  If no valid combination exists the
            // graph will be empty, and is suppressed.
            for (Date startDate : workflowDef.getEffectiveDates()) {

                ProductWorkflowDefVersion workflow = workflowDef.getEffectiveVersion(startDate);
                if (workflow != null) {

                    Graph graph = new Graph(workflowName, workflow.getVersion(), startDate,
                            getWorkflowImageFileName(workflowDef, startDate));

                    List<Node> previousNodes = new ArrayList<>();
                    Set<String> visitedProcessStep = new HashSet<>();
                    int nodeIndex = 0;

                    for (WorkflowProcessDef processDef : workflow.getWorkflowProcessDefs()) {
                        WorkflowProcessDefVersion process = processDef.getEffectiveVersion(startDate);
                        if (process != null) {

                            for (WorkflowStepDef step : process.getWorkflowStepDefs()) {

                                if (!visitedProcessStep.add(processDef.getName() + "//" + step.getName() + "//" + step.getWorkflowQualifier())) {
                                    throw new Exception("Workflow config contains a cyclic graph in process " +
                                                        processDef.getName() + " at step " + step.getName());
                                }

                                if (CollectionUtils.isEmpty(step.getLabEventTypes())) {

                                    // Workflow step having no events is either an entry point or ignored.
                                    if (CollectionUtils.isEmpty(previousNodes) &&
                                        CollectionUtils.isEmpty(graph.getNodes())) {
                                        Node entryNode =
                                                new Node(nodeIndex++, step.getName());
                                        graph.add(entryNode);
                                        previousNodes.add(entryNode);
                                    }

                                } else {
                                    for (LabEventType eventType : step.getLabEventTypes()) {

                                        // Creates entry point if this is the first node encountered.
                                        if (CollectionUtils.isEmpty(previousNodes) &&
                                            CollectionUtils.isEmpty(graph.getNodes())) {
                                            Node entryNode = new Node(nodeIndex++,
                                                    step.isReEntryPoint() ? "Reenter" : "Start");
                                            graph.add(entryNode);
                                            previousNodes.add(entryNode);
                                        }

                                        Node toNode = new Node(nodeIndex++, "");
                                        graph.add(toNode);

                                        for (Node fromNode : previousNodes) {
                                            graph.add(new Edge(fromNode, toNode, eventType.getName()));
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

        List<Graph> graphs = createGraphs();
        writeDotFiles(diagramDirectory, graphs);

        // Runs dot on each .dot file to create a diagram file.
        for (Graph graph : graphs) {
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

    private static int COMMAND_TIMEOUT = 1000 * 60 * 60;

    private static void runCommand(@Nonnull final String command) throws Exception {
        ByteArrayOutputStream stdOutAndErr = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(stdOutAndErr);
        CommandLine commandLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(COMMAND_TIMEOUT);
        executor.setWatchdog(watchdog);
        executor.execute(commandLine);
        String output = stdOutAndErr.toString();
        if (!StringUtils.isEmpty(output)) {
            throw new Exception("Error running command \"" + command + "\" : " + output);
        }
    }


    /**
     * Given a workflow graph, formats the nodes and edges into a directed graph .dot file.
     * @param directory the directory that gets the .dot files
     * @param graphs the workflow graph objects.
     * @throws Exception
     */
    static void writeDotFiles(@Nonnull File directory, @Nonnull List<Graph> graphs) throws Exception {
        for (Graph graph : graphs) {
            File file = new File(directory, graph.getDiagramFileName() + DOT_EXTENSION);
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            // Writes .dot file header, nodes, edges, and trailer.
            writer.println("digraph G {");
            for (Node node : graph.getNodes()) {
                writer.println(node.toDotString());
            }
            for (Edge edge : graph.getEdges()) {
                writer.println(edge.toDotString());
            }
            writer.println("}");
            writer.flush();
            writer.close();
        }
    }

    /** Creates a new directory or reuses an existing one. */
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


    /** Represents a node in the workflow graph. */
    private class Node {
        private final int id;
        private final String label;

        Node(int id, String label) {
            this.id = id;
            this.label = label;
        }

        int getId() {
            return id;
        }

        /** Returns a .dot file representation of a node. */
        String toDotString() {
            return String.valueOf(id) + "[label=\"" + id + " " + label + "\"];";
        }
    }

    /** Represents an edge in the workflow graph. */
    private class Edge {
        private final Node fromNode;
        private final Node toNode;
        private final String eventName;

        Edge(Node fromNode, Node toNode, String eventName) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.eventName = eventName;
        }

        /** Returns a .dot file representation of an edge. */
        String toDotString() {
            return fromNode.getId() + " -> " + toNode.getId() + "[label=\"" + eventName + "\"];";
        }
    }


    /** Represents a workflow graphs, with all possible edges. */
    protected class Graph {
        private final String workflowName;
        private final String workflowVersion;
        private final Date effectiveDate;
        private final Collection<Node> nodes = new ArrayList<>();
        private final Collection<Edge> edges = new ArrayList<>();
        private final String diagramFileName;

        Graph(String workflowName, String workflowVersion, Date effectiveDate, String diagramFileName) {
            this.workflowName = workflowName;
            this.workflowVersion = workflowVersion;
            this.effectiveDate = effectiveDate;
            this.diagramFileName = diagramFileName;
        }

        void add(Node node) {
            nodes.add(node);
        }

        void add(Edge edge) {
            edges.add(edge);
        }

        Collection<Node> getNodes() {
            return nodes;
        }

        Collection<Edge> getEdges() {
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

    }

}
