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
    <input id="clickMe" type="button" value="clickme" onclick="doFunction();" />
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

            <script src="${ctxpath}/resources/scripts/jsPlumb-2.1.4.js"></script>
            <script type="text/javascript" src="${ctxpath}/resources/scripts/CherryPick.js"></script>
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



                    // Some scanners send carriage return, we don't want this to submit the form
                    $j(document).on("keypress", ":input:not(textarea)", function(event) {
                        return event.keyCode != 13;
                    });
                </script>
            </stripes:layout-component>

            <stripes:layout-component name="content">
                <c:if test="${empty actionBean.workflowStepDef}">
                    <stripes:form beanclass="${actionBean.class.name}" id="eventForm">
                        <stripes:select name="stationEvents[0].eventType" id="eventType">
                            <stripes:options-collection collection="${actionBean.manualEventTypes}" label="name" value="name"/>
                        </stripes:select>
                        <stripes:submit name="chooseEventType" value="Choose Event Type" class="btn btn-primary"/>
                    </stripes:form>
                </c:if>

                <stripes:form beanclass="${actionBean.class.name}" id="transferForm">
                    <%-- See https://code.google.com/p/chromium/issues/detail?id=468153 --%>
            <div style="display: none;">
                <input type="text" id="PreventChromeAutocomplete" name="PreventChromeAutocomplete" autocomplete="address-level4" />
            </div>
            <%-- Can't use stripes:text because the value in the request takes precedence over the value set in the action bean. --%>
            <c:if test="${not empty actionBean.stationEvents}">
                ${empty actionBean.workflowStepDef ? '' : actionBean.workflowStepDef.instructions}
                <input type="hidden" name="workflowProcessName" value="${actionBean.workflowProcessName}"/>
                <input type="hidden" name="workflowStepName" value="${actionBean.workflowStepName}"/>
                <input type="hidden" name="workflowEffectiveDate" value="${actionBean.workflowEffectiveDate}"/>
                <input type="hidden" name="batchName" value="${actionBean.batchName}"/>
                <input type="hidden" name="anchorIndex" value="${actionBean.anchorIndex}"/>
                <%-- Set by transfer_plate.jsp --%>
                <input type="hidden" name="scanIndex" value="">
                <%-- Set by transfer_plate.jsp --%>
                <input type="hidden" name="scanSource" value="">

                <c:if test="${not empty actionBean.manualTransferDetails.machineNames}">
                    <stripes:label for="station">Machine </stripes:label>
                    <stripes:select name="stationEvents[0].station" id="station">
                        <stripes:options-collection collection="${actionBean.manualTransferDetails.machineNames}"/>
                    </stripes:select>
                </c:if>

                <c:if test="${not empty actionBean.manualTransferDetails.reagentNames}">
                    <h5>Reagents</h5>
                    Expiration date format is mm/dd/yyyy.
                    <c:set var="prevReagentType" value=""/>
                    <c:forEach items="${actionBean.stationEvents[0].reagent}" var="reagent" varStatus="loop">
                        <%--@elvariable id="reagent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType"--%>
                        <c:if test="${reagent.kitType != prevReagentType && !loop.first}">
                            </div>
                        </c:if>
                        <c:if test="${reagent.kitType != prevReagentType}">
                            <div class="control-group">
                            <label for="rgtType${loop.index}">Type </label>${reagent.kitType}
                        </c:if>
                        <input type="hidden" name="stationEvents[0].reagent[${loop.index}].kitType"
                                value="${reagent.kitType}"/>
                        <label for="rgtBcd${loop.index}">Barcode </label>
                        <input type="text" id="rgtBcd${loop.index}" name="stationEvents[0].reagent[${loop.index}].barcode"
                                value="${reagent.barcode}" class="barcode" autocomplete="off"/>
                        <c:if test="${actionBean.manualTransferDetails.expirationDateIncluded}">
                            <label for="rgtExp${loop.index}">Expiration </label>
                            <input type="text" id="rgtExp${loop.index}" name="stationEvents[0].reagent[${loop.index}].expiration"
                                    value="${reagent.expiration}" class="date" autocomplete="off"/>
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
                        <c:when test="${stationEvent.class.simpleName == 'PlateTransferEventType' or stationEvent.class.simpleName == 'PlateEventType' or stationEvent.class.simpleName == 'PlateCherryPickEvent'}">
                            <c:set var="plateTransfer" value="${stationEvent}"/>
                            <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>
                            <c:if test="${stationEvent.class.simpleName == 'PlateTransferEventType' or stationEvent.class.simpleName == 'PlateCherryPickEvent'}">

                                <c:if test="${empty actionBean.manualTransferDetails.secondaryEvent or not stationEventStatus.last}">

                                    <h4>Plate Transfer</h4>
                                    <h5>Source</h5>

                                    <c:set var="stationEvent" value="${stationEvent}" scope="request"/>
                                    <c:set var="plate" value="${plateTransfer.sourcePlate}" scope="request"/>
                                    <c:set var="positionMap" value="${plateTransfer.sourcePositionMap}" scope="request"/>
                                    <c:set var="stationEventIndex" value="${stationEventStatus.index}" scope="request"/>
                                    <c:set var="vesselTypeGeometry" value="${actionBean.manualTransferDetails.sourceVesselTypeGeometry}" scope="request"/>
                                    <c:set var="section" value="${actionBean.manualTransferDetails.sourceSection}" scope="request"/>
                                    <c:set var="source" value="${true}" scope="request"/>
                                    <c:set var="tableName" value="sourceTable" scope="request"/>
                                    <c:set var="transferType" value="${actionBean.stationEvents[stationEventStatus.index].eventType}"/>

                                    <c:choose>
                                        <c:when test="${transferType.equals('PoolingTransfer')}">
                                            <jsp:include page="transfer_plate_cherry_pick.jsp"/>
                                        </c:when>
                                        <c:otherwise>
                                            <jsp:include page="transfer_plate.jsp"/>
                                        </c:otherwise>
                                    </c:choose>

                                </c:if>

                                <h5>Destination</h5>
                            </c:if>
                            <c:set var="stationEvent" value="${stationEvent}" scope="request"/>
                            <c:set var="plate" value="${plateTransfer.plate}" scope="request"/>
                            <c:set var="positionMap" value="${plateTransfer.positionMap}" scope="request"/>
                            <c:set var="stationEventIndex" value="${stationEventStatus.index}" scope="request"/>
                            <c:set var="vesselTypeGeometry" value="${actionBean.manualTransferDetails.targetVesselTypeGeometry}" scope="request"/>
                            <c:set var="section" value="${actionBean.manualTransferDetails.targetSection}" scope="request"/>
                            <c:set var="source" value="${false}" scope="request"/>

                            <c:choose>
                                <c:when test="${transferType.equals('PoolingTransfer')}">
                                    <jsp:include page="transfer_plate_cherry_pick.jsp"/>
                                </c:when>
                                <c:otherwise>
                                    <jsp:include page="transfer_plate.jsp"/>
                                </c:otherwise>
                            </c:choose>

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
                            <input type="text" id="srcRcpBcd${stationEventStatus.index}" autocomplete="off"
                                    name="stationEvents[${stationEventStatus.index}].sourceReceptacle.barcode"
                                    value="${receptacleTransfer.sourceReceptacle.barcode}"
                                    class="clearable barcode unique" required/>
                            <label for="srcRcpVol${stationEventStatus.index}">Volume</label>
                            <input type="text" id="srcRcpVol${stationEventStatus.index}" autocomplete="off"
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
                                <input type="text" id="destRcpBcd${stationEventStatus.index}" autocomplete="off"
                                        name="stationEvents[${stationEventStatus.index}].receptacle.barcode"
                                        value="${receptacleTransfer.receptacle.barcode}"
                                        class="clearable barcode unique" required/>
                                <label for="destRcpVol${stationEventStatus.index}">Volume</label>
                                <input type="text" id="destRcpVol${stationEventStatus.index}" autocomplete="off"
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
                                <input type="text" id="destRcpBcd${stationEventStatus.index}" autocomplete="off"
                                        name="stationEvents[${stationEventStatus.index}].receptacle.barcode"
                                        value="${receptacleEvent.receptacle.barcode}"
                                        class="clearable barcode unique" required/>
                                <label for="destRcpVol${stationEventStatus.index}">Volume</label>
                                <input type="text" id="destRcpVol${stationEventStatus.index}" autocomplete="off"
                                        name="stationEvents[${stationEventStatus.index}].receptacle.volume"
                                        value="${receptacleEvent.receptacle.volume}" class="clearable barcode"/> ul
                            </div>
                        </c:when> <%-- end ReceptacleEventType --%>
                    </c:choose>
                </c:forEach>

                <c:if test="${transferType=='PoolingTransfer'}">
                    <input type="button" value="Preview" id="PreviewButton" name="PreviewButton" class="btn btn-primary" >
                </c:if>

                <stripes:submit name="fetchExisting" value="Validate Barcodes" class="btn"/>
                <stripes:submit name="transfer" value="${actionBean.manualTransferDetails.buttonValue}"
                        class="btn btn-primary"/>
                <input type="button" onclick="$('.clearable').each(function (){$(this).val('');});" value="Clear non-reagent fields">
            </c:if>
        </stripes:form>
        <c:if test="${not empty actionBean.batchName}">
            <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean">
                <stripes:param name="batchName" value="${actionBean.batchName}"/>
                <stripes:param name="anchorIndex" value="${actionBean.anchorIndex}"/>
                Return to Batch Workflow page
            </stripes:link>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
