package org.broadinstitute.sequel.control.workflow;

import org.broadinstitute.sequel.entity.workflow.WorkflowAnnotation;
import org.broadinstitute.sequel.entity.workflow.WorkflowState;
import org.broadinstitute.sequel.entity.workflow.WorkflowTransition;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses a workflow defined in Business Process Model and Notation (BPMN) 2.0
 */
@SuppressWarnings({"HardcodedFileSeparator", "rawtypes"})
public class WorkflowParser {
    private final Map<String, List<WorkflowTransition>> mapNameToTransitionList = new HashMap<String, List<WorkflowTransition>>();
    private WorkflowState startState;
    private String workflowName;

    /** Represents a BPMN sub process.  Each sub process has a start event, an end event, and a sequence of flows and
     * tasks.  There can be flows between sub processes, and between the top level start and end events. */
    private static class SubProcess {
        private final String id;
        private final String name;
        private final List<WorkflowState> tasksAtEnd = new ArrayList<WorkflowState>();
        private final List<SubProcess> predecessorSubProcesses = new ArrayList<SubProcess>();
        private boolean predecessorTopLevelStartEvent = false;

        private SubProcess(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public List<WorkflowState> getTasksAtEnd() {
            return this.tasksAtEnd;
        }

        public List<SubProcess> getPredecessorSubProcesses() {
            return this.predecessorSubProcesses;
        }

        public boolean isPredecessorTopLevelStartEvent() {
            return this.predecessorTopLevelStartEvent;
        }

        public void setPredecessorTopLevelStartEvent(boolean predecessorTopLevelStartEvent) {
            this.predecessorTopLevelStartEvent = predecessorTopLevelStartEvent;
        }
    }

    public WorkflowParser(InputStream xml) {
        parse(xml);
    }

    /**
     * Parse BPMN subProcesses, tasks and sequenceFlows; build a workflow object representation.  This does not
     * currently handle nested subProcesses.
     * @param xml BPMN 2.0 XML
     */
    private void parse(InputStream xml) {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(false);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(xml);

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            // Define a prefix m for the default namespace (the Yaoqiang editor doesn't use a prefix, but xpath requires one).

            Map<String, WorkflowState> mapIdToWorkflowState = new HashMap<String, WorkflowState>();

            XPathExpression processProperty = xpath.compile("//process");
            Node processNode = (Node)processProperty.evaluate(doc, XPathConstants.NODE);
            if (processNode != null) {
                NamedNodeMap processNodeAttributes = processNode.getAttributes();
                if (processNodeAttributes != null) {
                    processNode = processNodeAttributes.getNamedItem("name");
                    if (processNode != null) {
                        workflowName = processNode.getNodeValue();
                    }
                }
            }

            // Get the top level start event
            XPathExpression startEventExpr = xpath.compile("//process/startEvent");
            NodeList startNodes = (NodeList) startEventExpr.evaluate(doc, XPathConstants.NODESET);
            if(startNodes.getLength() < 1) {
                throw new RuntimeException("Failed to find startEvent");
            }
            if(startNodes.getLength() > 1) {
                throw new RuntimeException("Found more than one startEvent");
            }
            NamedNodeMap startNodeAttributes = startNodes.item(0).getAttributes();
            this.startState = new WorkflowState(startNodeAttributes.getNamedItem("name").getNodeValue());
            String startStateId = startNodeAttributes.getNamedItem("id").getNodeValue();
            mapIdToWorkflowState.put(startStateId, this.startState);

            // Get the top level end event
            XPathExpression endEventExpr = xpath.compile("//process/endEvent");
            NodeList endNodes = (NodeList) endEventExpr.evaluate(doc, XPathConstants.NODESET);
            if(endNodes.getLength() < 1) {
                throw new RuntimeException("Failed to find endEvent");
            }
            if(endNodes.getLength() > 1) {
                throw new RuntimeException("Found more than one endEvent");
            }
            NamedNodeMap endNodeAttributes = endNodes.item(0).getAttributes();
            WorkflowState endWorkflowState = new WorkflowState(endNodeAttributes.getNamedItem("name").getNodeValue());
            String endStateId = endNodeAttributes.getNamedItem("id").getNodeValue();
            mapIdToWorkflowState.put(endStateId, endWorkflowState);

            // Get the tasks
            XPathExpression taskExpr = xpath.compile("//task");
            NodeList taskNodes = (NodeList) taskExpr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < taskNodes.getLength(); i++) {
                NamedNodeMap attributes = taskNodes.item(i).getAttributes();
                mapIdToWorkflowState.put(attributes.getNamedItem("id").getNodeValue(),
                        new WorkflowState(attributes.getNamedItem("name").getNodeValue()));
            }

            Map<String, SubProcess> mapIdToSubProcess = new HashMap<String, SubProcess>();
            Map<String, SubProcess> mapStartEventIdToSubProcess = new HashMap<String, SubProcess>();
            Map<String, SubProcess> mapEndEventIdToSubProcess = new HashMap<String, SubProcess>();

            // Get the sub processes
            XPathExpression subProcessStartEventExpr = xpath.compile("./startEvent");
            XPathExpression subProcessEndEventExpr = xpath.compile("./endEvent");
            XPathExpression subProcessExpr = xpath.compile("//subProcess");
            NodeList subProcessNodes = (NodeList) subProcessExpr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < subProcessNodes.getLength(); i++) {
                Node subProcessNode = subProcessNodes.item(i);
                NamedNodeMap attributes = subProcessNode.getAttributes();
                SubProcess subProcess = new SubProcess(attributes.getNamedItem("id").getNodeValue(),
                        attributes.getNamedItem("name").getNodeValue());
                mapIdToSubProcess.put(subProcess.getId(), subProcess);

                NodeList subProcessStartNodes = (NodeList) subProcessStartEventExpr.evaluate(subProcessNode, XPathConstants.NODESET);
                for(int j = 0; j < subProcessStartNodes.getLength(); j++) {
                    mapStartEventIdToSubProcess.put(
                            subProcessStartNodes.item(j).getAttributes().getNamedItem("id").getNodeValue(), subProcess);
                }
                NodeList subProcessEndNodes = (NodeList) subProcessEndEventExpr.evaluate(subProcessNode, XPathConstants.NODESET);
                for(int j = 0; j < subProcessEndNodes.getLength(); j++) {
                    mapEndEventIdToSubProcess.put(
                            subProcessEndNodes.item(j).getAttributes().getNamedItem("id").getNodeValue(), subProcess);
                }
            }


            // Get the lines between tasks
            XPathExpression flowExpr = xpath.compile("//sequenceFlow");
            NodeList flowNodes = (NodeList) flowExpr.evaluate(doc, XPathConstants.NODESET);

                // In the first pass, handle flows between sub processes
            for (int i = 0; i < flowNodes.getLength(); i++) {
                NamedNodeMap attributes = flowNodes.item(i).getAttributes();
                String sourceRef = attributes.getNamedItem("sourceRef").getNodeValue();
                String targetRef = attributes.getNamedItem("targetRef").getNodeValue();
                SubProcess sourceSubProcess = mapIdToSubProcess.get(sourceRef);
                SubProcess targetSubProcess = mapIdToSubProcess.get(targetRef);
                if(sourceSubProcess != null && targetSubProcess != null) {
                    // the line sourceref is a subProcess and the targetref is a subProcess so add the source as a predecessor of the target
                    targetSubProcess.getPredecessorSubProcesses().add(sourceSubProcess);
                } else if(sourceRef.equals(startStateId) && targetSubProcess != null) {
                    // the line sourceRef is the top level startEvent, and the targetRef is a subProcess
                    targetSubProcess.setPredecessorTopLevelStartEvent(true);
                }
            }
            // In the second pass, create transitions
            for (int i = 0; i < flowNodes.getLength(); i++) {
                NamedNodeMap attributes = flowNodes.item(i).getAttributes();
                String sourceRef = attributes.getNamedItem("sourceRef").getNodeValue();
                String targetRef = attributes.getNamedItem("targetRef").getNodeValue();
                WorkflowState sourceState = mapIdToWorkflowState.get(sourceRef);
                WorkflowState targetState = mapIdToWorkflowState.get(targetRef);
                SubProcess sourceSubProcess = mapIdToSubProcess.get(sourceRef);
                SubProcess targetSubProcess = mapIdToSubProcess.get(targetRef);
                SubProcess subProcessForSourceStartEvent = mapStartEventIdToSubProcess.get(sourceRef);
                SubProcess subProcessForTargetEndEvent = mapEndEventIdToSubProcess.get(targetRef);
                if(sourceSubProcess != null && targetSubProcess != null) {
                    // the line sourceref is a subProcess and the targetref is a subProcess, this is handled in the first pass
                } else if(subProcessForSourceStartEvent != null && targetState != null) {
                    // the line sourceref is a subProcess startEvent and the targetref is a task so associate it with the subProcess' predecessor tasksAtEnd
                    if(subProcessForSourceStartEvent.isPredecessorTopLevelStartEvent()) {
                        buildTransition(attributes, this.startState, targetState,xpath,doc);
                    } else {
                        for (SubProcess subProcess : subProcessForSourceStartEvent.getPredecessorSubProcesses()) {
                            for (WorkflowState workflowState : subProcess.getTasksAtEnd()) {
                                buildTransition(attributes, workflowState, targetState,xpath,doc);
                            }
                        }
                    }
                } else if(subProcessForTargetEndEvent != null && sourceState != null) {
                    // the line targetRef is a subProcess endEvent so add the sourceRef Task to the subProcesses' tasksAtEnd
                    subProcessForTargetEndEvent.getTasksAtEnd().add(sourceState);
                } else if(sourceState != null && targetState != null) {
                    // the line sourceRef is a task and the targetRef is a task so associate the transition with the states
                    buildTransition(attributes, sourceState, targetState,xpath,doc);
                } else if(sourceRef.equals(startStateId) && targetSubProcess != null) {
                    // the line sourceRef is the top level startEvent, and the targetRef is a subProcess, this is handled above
                } else if(sourceSubProcess != null && targetRef.equals(endStateId)) {
                    // the line sourceRef is a subProcess, and the targetRef is the end state
                    // todo jmt connect to end event
                } else {
                    throw new RuntimeException("Unknown combination of sourceRef " + sourceRef + " and targetRef " + targetRef);
                }
            }
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildTransition(NamedNodeMap attributes, WorkflowState sourceState, WorkflowState targetState,XPath xpath,Document doc) throws XPathExpressionException {
        String transitionName = attributes.getNamedItem("name").getNodeValue();
        WorkflowTransition workflowTransition = new WorkflowTransition(transitionName,
                sourceState, targetState);
        sourceState.getExits().add(workflowTransition);
        targetState.getEntries().add(workflowTransition);
        List<WorkflowTransition> workflowTransitions = this.mapNameToTransitionList.get(transitionName);
        if(workflowTransitions == null) {
            workflowTransitions = new ArrayList<WorkflowTransition>();
            this.mapNameToTransitionList.put(transitionName, workflowTransitions);
        }
        Collection<WorkflowAnnotation> workflowAnnotations = parseWorkflowAnnotations(attributes,xpath,doc);
        if (!workflowAnnotations.isEmpty()) {
            for (WorkflowAnnotation workflowAnnotation : workflowAnnotations) {
                workflowTransition.addWorkflowAnnotation(workflowAnnotation);
            }
        }

        workflowTransitions.add(workflowTransition);



    }

    private Collection<WorkflowAnnotation> parseWorkflowAnnotations(NamedNodeMap attributes,XPath xpath,Document doc) throws XPathExpressionException {
        final Collection<WorkflowAnnotation> workflowAnnotations = new HashSet<WorkflowAnnotation>();
        final String flowId = attributes.getNamedItem("id").getNodeValue();
        final String query = "//sequenceFlow[@id=" + "'" + flowId+ "'" + "]/extensionElements/Extensions/ModelExtension/ModelProperties/ModelProperty/Model/Children/Model[@name='Sequencing Library']/ModelProperties/TextModelProperty[@Name='value']";
        final XPathExpression seqLibParamExpr = xpath.compile(query);
        final NodeList seqLibNodeList = (NodeList)seqLibParamExpr.evaluate(doc,XPathConstants.NODESET);
        final Node flowNodeAttributes = attributes.getNamedItem("name");
        if (flowNodeAttributes != null) {
            if (seqLibNodeList.getLength() > 0) {
                final String value = seqLibNodeList.item(0).getAttributes().getNamedItem("Value").getTextContent();
                if (Boolean.parseBoolean(value)) {
                    workflowAnnotations.add(WorkflowAnnotation.SINGLE_SAMPLE_LIBRARY);
                }
            }
        }
        return workflowAnnotations;
    }

    public String getWorkflowName() {
        return this.workflowName;
    }

    public WorkflowState getStartState() {
        return this.startState;
    }

    public Map<String, List<WorkflowTransition>> getMapNameToTransitionList() {
        return this.mapNameToTransitionList;
    }
}
