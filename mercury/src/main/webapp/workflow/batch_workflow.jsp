<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Batch Workflow" sectionTitle="Batch Workflow">
    <stripes:layout-component name="extraHead">
        <style type="text/css">
            label {
                display: inline;
                font-weight: bold;
            }
            input[type="text"] {
                width: 100px;
                font-size: 12px;
                padding: 2px 2px;
            }
        </style>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <h3>${actionBean.labBatch.batchName}</h3>
        ${actionBean.effectiveWorkflowDef.productWorkflowDef.name}, version ${actionBean.effectiveWorkflowDef.version},
        ${actionBean.effectiveWorkflowDef.effectiveDate}
        <table class="table simple">
            <tr>
                <th>Workflow Step</th>
                <th>Events</th>
            </tr>
            <c:forEach items="${actionBean.workflowEvents}" var="workflowEvent">
                <tr>
                    <td>
                        <div>
                            <b>${workflowEvent.workflowStepDef.name}</b>
                        </div>
                        <div>
                            ${workflowEvent.workflowStepDef.instructions}
                        </div>
                        <c:forEach items="${workflowEvent.workflowStepDef.labEventTypes}" var="labEventType">
                            <c:choose>
                                <c:when test="${not empty labEventType.manualTransferDetails.messageType}">
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"
                                            event="chooseEventType">
                                        <stripes:param name="stationEvents[0].eventType" value="${labEventType.name()}"/>
                                        <stripes:param name="workflowProcessName" value="${workflowEvent.workflowStepDef.processDef.name}"/>
                                        <stripes:param name="workflowStepName" value="${workflowEvent.workflowStepDef.name}"/>
                                        <stripes:param name="workflowEffectiveDate" value="${actionBean.labBatch.createdOn}"/>
                                        <stripes:param name="batchName" value="${actionBean.labBatch.batchName}"/>
                                        Manual Transfer
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean">
                                        <input type="hidden" name="batchName" value="${actionBean.batchName}"/>
                                        <input type="hidden" name="labEventType" value="${labEventType}"/>
                                        <input type="hidden" name="workflowQualifier" value="${workflowEvent.workflowStepDef.workflowQualifier}"/>
                                        <c:choose>
                                            <c:when test="${labEventType eq 'ADD_REAGENT'}">
                                                <c:forEach items="${workflowEvent.workflowStepDef.reagentTypes}" var="reagentType" varStatus="loop">
                                                    <div class="control-group">
                                                        <label for="rgtType${loop.index}">Type </label>
                                                        <input type="text" id="rgtType${loop.index}" name="reagentName" value="${reagentType}">

                                                        <label for="rgtBcd${loop.index}">Barcode </label>
                                                        <input type="text" id="rgtBcd${loop.index}" name="reagentLot">

                                                        <label for="rgtExp${loop.index}">Expiration </label>
                                                        <input type="text" id="rgtExp${loop.index}" name="reagentExpiration">
                                                    </div>
                                                </c:forEach>
                                                <stripes:submit name="${actionBean.batchReagentAction}" value="Add"/>
                                            </c:when>
                                            <c:otherwise>
                                                <stripes:submit name="${actionBean.batchEventAction}" value="Done"/>
                                            </c:otherwise>
                                        </c:choose>
                                    </stripes:form>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </td>
                    <td>
                        <c:forEach items="${workflowEvent.labEvents}" var="labEvent">
                            <div>
                                ${labEvent.labEventType.name} ${labEvent.workflowQualifier} ${actionBean.getUserFullName(labEvent.eventOperator)} ${labEvent.eventDate}
                            </div>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
