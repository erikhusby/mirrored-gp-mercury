<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean" %>
<%--
  This page allows the user to record a manual transfer, i.e. a transfer not done on a liquid handling deck with
  messaging.
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manual Transfer" sectionTitle="Manual Transfer">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
        <style type="text/css">
            label {
                display: inline;
                font-weight: bold;
            }
            input[type="text"].smalltext {
                width: 70px;
                font-size: 12px;
                padding: 2px 2px;
            }
            input[type='text'].barcode {
                width: 100px;
                font-size: 12px;
            }
            input[type='text'].date {
                width: 70px;
                font-size: 12px;
            }
        </style>

        <script src="${ctxpath}/resources/scripts/jquery.validate-1.14.0.min.js"></script>
        <script type="text/javascript">
            $j(document).ready(function () {
                $j.validator.addMethod("unique", function(value, element) {
                    var parentForm = $j(element).closest('form');
                    var timeRepeated = 0;
                    if (value != '') {
                        $j(parentForm.find(':text')).each(function () {
                            if ($j(this).val() === value) {
                                timeRepeated++;
                            }
                        });
                    }
                    return timeRepeated === 1 || timeRepeated === 0;
                }, "* Duplicate");
                $j.validator.classRuleSettings.unique = { unique: true };
                $j("#transferForm").validate();
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="eventForm">
            <stripes:select name="stationEvents[0].eventType" id="eventType">
                <stripes:options-collection collection="${actionBean.manualEventTypes}" label="name" value="name"/>
            </stripes:select>
            <stripes:submit name="chooseEventType" value="Choose Event Type" class="btn btn-primary"/>
        </stripes:form>

        <stripes:form beanclass="${actionBean.class.name}" id="transferForm">
            <%-- Can't use stripes:text because the value in the request takes precedence over the value set in the action bean. --%>
            <c:if test="${not empty actionBean.stationEvents}">
                ${empty actionBean.workflowStepDef ? '' : actionBean.workflowStepDef.instructions}
                <input type="hidden" name="workflowProcessName" value="${actionBean.workflowProcessName}"/>
                <input type="hidden" name="workflowStepName" value="${actionBean.workflowStepName}"/>
                <input type="hidden" name="workflowEffectiveDate" value="${actionBean.workflowEffectiveDate}"/>
                <input type="hidden" name="batchName" value="${actionBean.batchName}"/>
                <%-- Set by transfer_plate.jsp --%>
                <input type="hidden" name="scanIndex" value="">
                <%-- Set by transfer_plate.jsp --%>
                <input type="hidden" name="scanSource" value="">

                <c:if test="${not empty actionBean.labEventType.manualTransferDetails.machineNames}">
                    <stripes:label for="station">Machine </stripes:label>
                    <stripes:select name="stationEvents[0].station" id="station">
                        <stripes:options-collection collection="${actionBean.labEventType.manualTransferDetails.machineNames}"/>
                    </stripes:select>
                </c:if>

                <c:if test="${not empty actionBean.labEventType.manualTransferDetails.reagentNames}">
                    <h5>Reagents</h5>
                    Expiration date format is mm/dd/yyyy.
                    <c:set var="prevReagentType" value=""/>
                    <c:forEach items="${actionBean.stationEvents[0].reagent}" var="reagent" varStatus="loop">
                        <%--@elvariable id="reagent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType"--%>
                        <c:if test="${reagent.kitType != prevReagentType && !loop.first}">
                            </div>
                        </c:if>
                        <c:choose>
                            <c:when test="${reagent.kitType != prevReagentType}">
                                <div class="control-group">
                                <label for="rgtType${loop.index}">Type </label>
                                <input type="text" id="rgtType${loop.index}" name="stationEvents[0].reagent[${loop.index}].kitType"
                                        value="${reagent.kitType}" class="barcode"/>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="stationEvents[0].reagent[${loop.index}].kitType"
                                        value="${reagent.kitType}"/>
                            </c:otherwise>
                        </c:choose>
                            <label for="rgtBcd${loop.index}">Barcode </label>
                            <input type="text" id="rgtBcd${loop.index}" name="stationEvents[0].reagent[${loop.index}].barcode"
                                    value="${reagent.barcode}" class="barcode"/>
                            <c:if test="${actionBean.labEventType.manualTransferDetails.expirationDateIncluded}">
                                <label for="rgtExp${loop.index}">Expiration </label>
                                <input type="text" id="rgtExp${loop.index}" name="stationEvents[0].reagent[${loop.index}].expiration"
                                        value="${reagent.expiration}" class="date"/>
                            </c:if>
                        <c:set var="prevReagentType" value="${reagent.kitType}"/>
                    </c:forEach>
                    </div>
                </c:if>

                <c:forEach items="${actionBean.stationEvents}" var="stationEvent" varStatus="stationEventStatus">
                    <input type="hidden" name="stationEvents[${stationEventStatus.index}].eventType"
                            value="${actionBean.stationEvents[stationEventStatus.index].eventType}"/>
                    <c:if test="${fn:length(actionBean.stationEvents) > 1}">
                        ${stationEventStatus.index + 1}
                        <input type="hidden" name="stationEvents[${stationEventStatus.index}].metadata[0].name" value="MessageNum"/>
                        <input type="hidden" name="stationEvents[${stationEventStatus.index}].metadata[0].value" value="${stationEventStatus.index + 1}"/>
                    </c:if>
                    <c:choose>
                        <c:when test="${stationEvent.class.simpleName == 'PlateTransferEventType' or stationEvent.class.simpleName == 'PlateEventType'}">
                            <c:set var="plateTransfer" value="${stationEvent}"/>
                            <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>
                            <c:if test="${stationEvent.class.simpleName == 'PlateTransferEventType'}">
                                <c:if test="${empty actionBean.labEventType.manualTransferDetails.secondaryEvent or not stationEventStatus.last}">
                                    <h4>Plate Transfer</h4>
                                    <h5>Source</h5>

                                    <c:set var="stationEvent" value="${stationEvent}" scope="request"/>
                                    <c:set var="plate" value="${plateTransfer.sourcePlate}" scope="request"/>
                                    <c:set var="positionMap" value="${plateTransfer.sourcePositionMap}" scope="request"/>
                                    <c:set var="stationEventIndex" value="${stationEventStatus.index}" scope="request"/>
                                    <c:set var="vesselTypeGeometry" value="${actionBean.labEventType.manualTransferDetails.sourceVesselTypeGeometry}" scope="request"/>
                                    <c:set var="section" value="${actionBean.labEventType.manualTransferDetails.sourceSection}" scope="request"/>
                                    <c:set var="source" value="${true}" scope="request"/>
                                    <jsp:include page="transfer_plate.jsp"/>

                                </c:if>

                                <h5>Destination</h5>
                            </c:if>
                            <c:set var="stationEvent" value="${stationEvent}" scope="request"/>
                            <c:set var="plate" value="${plateTransfer.plate}" scope="request"/>
                            <c:set var="positionMap" value="${plateTransfer.positionMap}" scope="request"/>
                            <c:set var="stationEventIndex" value="${stationEventStatus.index}" scope="request"/>
                            <c:set var="vesselTypeGeometry" value="${actionBean.labEventType.manualTransferDetails.targetVesselTypeGeometry}" scope="request"/>
                            <c:set var="section" value="${actionBean.labEventType.manualTransferDetails.targetSection}" scope="request"/>
                            <c:set var="source" value="${false}" scope="request"/>
                            <jsp:include page="transfer_plate.jsp"/>

                        </c:when> <%-- end PlateTransferEventType or PlateEventType--%>

                        <c:when test="${stationEvent.class.simpleName == 'ReceptacleTransferEventType'}">
                            <c:set var="receptacleTransfer" value="${stationEvent}"/>
                            <%--@elvariable id="receptacleTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType"--%>
                            <h4>Tube Transfer</h4>

                            <div class="control-group">
                            <h5>Source</h5>
                            <label>Type</label>
                            ${receptacleTransfer.sourceReceptacle.receptacleType}
                            <input type="hidden" name="stationEvents[${stationEventStatus.index}].sourceReceptacle.receptacleType"
                                    value="${receptacleTransfer.sourceReceptacle.receptacleType}"/>
                            <label for="srcRcpBcd${stationEventStatus.index}">Barcode</label>
                            <input type="text" id="srcRcpBcd${stationEventStatus.index}"
                                    name="stationEvents[${stationEventStatus.index}].sourceReceptacle.barcode"
                                    value="${receptacleTransfer.sourceReceptacle.barcode}" class="clearable barcode"/>
                            <label for="srcRcpVol${stationEventStatus.index}">Volume</label>
                            <input type="text" id="srcRcpVol${stationEventStatus.index}"
                                    name="stationEvents[${stationEventStatus.index}].sourceReceptacle.volume"
                                    value="${receptacleTransfer.sourceReceptacle.volume}" class="clearable barcode"/> ul
                            </div>
                            <div class="control-group">
                                <h5>Destination</h5>
                                <label>Type</label>
                                ${receptacleTransfer.receptacle.receptacleType}
                                <input type="hidden" name="stationEvents[${stationEventStatus.index}].receptacle.receptacleType"
                                        value="${receptacleTransfer.receptacle.receptacleType}"/>
                                <label for="destRcpBcd${stationEventStatus.index}">Barcode</label>
                                <input type="text" id="destRcpBcd${stationEventStatus.index}"
                                        name="stationEvents[${stationEventStatus.index}].receptacle.barcode"
                                        value="${receptacleTransfer.receptacle.barcode}" class="clearable barcode"/>
                                <label for="destRcpVol${stationEventStatus.index}">Volume</label>
                                <input type="text" id="destRcpVol${stationEventStatus.index}"
                                        name="stationEvents[${stationEventStatus.index}].receptacle.volume"
                                        value="${receptacleTransfer.receptacle.volume}" class="clearable barcode"/> ul
                            </div>
                        </c:when> <%-- end ReceptacleTransferEventType --%>

                        <c:when test="${stationEvent.class.simpleName == 'ReceptacleEventType'}">
                            <c:set var="receptacleEvent" value="${stationEvent}"/>
                            <%--@elvariable id="receptacleEvent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType"--%>
                            <h4>Tube Event</h4>
                            <div class="control-group">
                                <%-- todo jmt reduce copy / paste --%>
                                <label>Type</label>
                                    ${receptacleEvent.receptacle.receptacleType}
                                <input type="hidden" name="stationEvents[${stationEventStatus.index}].receptacle.receptacleType"
                                        value="${receptacleEvent.receptacle.receptacleType}"/>
                                <label for="destRcpBcd${stationEventStatus.index}">
                                    ${fn:containsIgnoreCase(receptacleEvent.receptacle.receptacleType, "matrix") ? '2D ' : ''}Barcode
                                </label>
                                <input type="text" id="destRcpBcd${stationEventStatus.index}"
                                        name="stationEvents[${stationEventStatus.index}].receptacle.barcode"
                                        value="${receptacleEvent.receptacle.barcode}" class="clearable barcode"/>
                                <label for="destRcpVol${stationEventStatus.index}">Volume</label>
                                <input type="text" id="destRcpVol${stationEventStatus.index}"
                                        name="stationEvents[${stationEventStatus.index}].receptacle.volume"
                                        value="${receptacleEvent.receptacle.volume}" class="clearable barcode"/> ul
                            </div>
                        </c:when> <%-- end ReceptacleEventType --%>
                    </c:choose>
                </c:forEach>
                <stripes:submit name="fetchExisting" value="Fetch Existing" class="btn"/>
                <stripes:submit name="transfer" value="Transfer" class="btn btn-primary"/>
                <input type="button" onclick="$('.clearable').each(function (){$(this).val('');});" value="Clear non-reagent fields">
            </c:if>
        </stripes:form>
        <c:if test="${not empty actionBean.batchName}">
            <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean">
                <stripes:param name="batchName" value="${actionBean.batchName}"/>
                Return to Batch Workflow page
            </stripes:link>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
