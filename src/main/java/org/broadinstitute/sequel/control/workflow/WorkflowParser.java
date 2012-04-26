package org.broadinstitute.sequel.control.workflow;

import org.broadinstitute.sequel.entity.workflow.WorkflowState;
import org.broadinstitute.sequel.entity.workflow.WorkflowTransition;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parses a workflow defined in Business Process Model and Notation (BPMN) 2.0
 */
@SuppressWarnings({"HardcodedFileSeparator", "rawtypes"})
public class WorkflowParser {
    private Map<String, List<WorkflowTransition>> mapNameToTransitionList = new HashMap<String, List<WorkflowTransition>>();
    private WorkflowState startState;
    private String workflowName;

    public WorkflowParser(InputStream xml) {
        parse(xml);
    }

    private void parse(InputStream xml) {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(xml);

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            xpath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return prefix.equals("m") ? "http://www.omg.org/spec/BPMN/20100524/MODEL" : null;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });
            Map<String, WorkflowState> mapIdToWorkflowState = new HashMap<String, WorkflowState>();

            XPathExpression processProperty = xpath.compile("//m:process");
            Node processNode = (Node)processProperty.evaluate(doc, XPathConstants.NODE);
            if (processNode != null) {
                NamedNodeMap processNodeAttributes = processNode.getAttributes();
                workflowName = processNodeAttributes.getNamedItem("name").getNodeValue();
            }

            XPathExpression startEventExpr = xpath.compile("//m:startEvent");
            Node startNode = (Node) startEventExpr.evaluate(doc, XPathConstants.NODE);
            NamedNodeMap startNodeAttributes = startNode.getAttributes();
            this.startState = new WorkflowState(startNodeAttributes.getNamedItem("name").getNodeValue());
            mapIdToWorkflowState.put(startNodeAttributes.getNamedItem("id").getNodeValue(),
                    this.startState);

            XPathExpression endEventExpr = xpath.compile("//m:endEvent");
            Node endNode = (Node) endEventExpr.evaluate(doc, XPathConstants.NODE);
            NamedNodeMap endNodeAttributes = endNode.getAttributes();
            WorkflowState endWorkflowState = new WorkflowState(endNodeAttributes.getNamedItem("name").getNodeValue());
            mapIdToWorkflowState.put(endNodeAttributes.getNamedItem("id").getNodeValue(),
                    endWorkflowState);

            XPathExpression taskExpr = xpath.compile("//m:receiveTask");
            NodeList taskNodes = (NodeList) taskExpr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < taskNodes.getLength(); i++) {
                NamedNodeMap attributes = taskNodes.item(i).getAttributes();
                mapIdToWorkflowState.put(attributes.getNamedItem("id").getNodeValue(),
                        new WorkflowState(attributes.getNamedItem("name").getNodeValue()));
            }

            XPathExpression flowExpr = xpath.compile("//m:sequenceFlow");

            NodeList flowNodes = (NodeList) flowExpr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < flowNodes.getLength(); i++) {
                NamedNodeMap attributes = flowNodes.item(i).getAttributes();
                WorkflowState fromState = mapIdToWorkflowState.get(attributes.getNamedItem("sourceRef").getNodeValue());
                WorkflowState toState = mapIdToWorkflowState.get(attributes.getNamedItem("targetRef").getNodeValue());
                String transitionName = attributes.getNamedItem("name").getNodeValue();
                WorkflowTransition workflowTransition = new WorkflowTransition(transitionName,
                        fromState, toState);
                if (fromState != null) {
                    fromState.getExits().add(workflowTransition);
                }
                if (toState != null) {
                    toState.getEntries().add(workflowTransition);
                }
                List<WorkflowTransition> workflowTransitions = this.mapNameToTransitionList.get(transitionName);
                if(workflowTransitions == null) {
                    workflowTransitions = new ArrayList<WorkflowTransition>();
                    this.mapNameToTransitionList.put(transitionName, workflowTransitions);
                }
                workflowTransitions.add(workflowTransition);
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

    public String getWorkflowName() {
        return this.workflowName;
    }

    public WorkflowState getStartState() {
        return this.startState;
    }

    public Map<String, List<WorkflowTransition>> getMapNameToTransitionList() {
        return mapNameToTransitionList;
    }
}
