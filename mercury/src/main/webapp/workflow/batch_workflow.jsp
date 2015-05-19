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
                        <c:forEach items="${workflowEvent.workflowStepDef.labEventTypes}" var="labEvenType">
                            <c:choose>
                                <c:when test="${not empty labEvenType.messageType}">
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean">
                                        Manual Transfer
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <input type="checkbox">
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
