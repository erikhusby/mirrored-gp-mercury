<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Batch Workflow" sectionTitle="Batch Workflow">
    <stripes:layout-component name="content">
        <table class="table simple">
            <tr>
                <th>Workflow Step</th>
                <th>Events</th>
            </tr>
            <c:forEach items="${actionBean.workflowEvents}" var="workflowEvent">
                <tr>
                    <td>
                        <div>
                            ${workflowEvent.workflowStepDef.name}
                        </div>
                        <div>
                            ${workflowEvent.workflowStepDef.instructions}
                        </div>
                        <c:forEach items="${workflowEvent.workflowStepDef.labEventTypes}" var="labEventType">
                            <c:choose>
                                <c:when test="${not empty labEventType.messageType}">
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"
                                            event="chooseEventType">
                                        <stripes:param name="stationEvent.eventType" value="${labEventType.name()}"/>
                                        Manual Transfer
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean">
                                        <input type="hidden" name="batchName" value="${actionBean.batchName}"/>
                                        <input type="hidden" name="labEventType" value="${labEventType}"/>
                                        <input type="hidden" name="workflowQualifer" value="${workflowEvent.workflowStepDef.workflowQualifier}"/>
                                        <stripes:submit name="${actionBean.batchEventAction}" value="Done"/>
                                    </stripes:form>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </td>
                    <td>
                        <c:forEach items="${workflowEvent.labEvents}" var="labEvent">
                            <div>
                                ${labEvent.labEventType.name} ${labEvent.eventDate}
                            </div>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
