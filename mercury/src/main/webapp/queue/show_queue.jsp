<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.queueType.textName} Queue" sectionTitle="${actionBean.queueType.textName} Queue" showCreate="false">

    <stripes:layout-component name="extraHead">

        <style type="text/css">

            .barFull { height: 15px; width:80px; }

            .barNotNeedingPico { height: 15px; float:left; background-color: #dd2222; }
            .barNeedingPico { height: 15px; float:left; background-color: #22bb22; }

            .numberNeedingPico { float: left; margin-top: -16px; font-weight: bold; margin-left: 3px; color: white; }
            .numberNotNeedingPico { float: right; margin-top: -16px; font-weight: bold; margin-right: 3px; color: white; }

            .positioning-input {
                width: 40px;
            }

        </style>

        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#queueTable').dataTable({
                    "paging": false,
                    "ordering": false,
                    "info": false,
                    "bSort": false,
                    "iDisplayLength": 25
                });

                $j(".excludeSamplesDialog").dialog({ autoOpen: false, modal: true, title: "Sample IDs to exclude", resizable: false });
                $j("#exclude-opener").click(function() {
                    $j(".excludeSamplesDialog").dialog("open");
                });
                $j(".uploadSamplesDialog").dialog({ autoOpen: false, modal: true, title: "Upload Sample IDs", resizable: false });
                $j("#upload-opener").click(function() {
                    $j(".uploadSamplesDialog").dialog("open");
                });
            });

        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:hidden name="queueType" />
            <table class="table simple dataTable" id="queueTable">
                <thead>
                    <tr>
                        <th>Readable Text</th><th>Queue Priority Type</th><th>Origin</th><th>Queue Status</th><th>Positioning</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.queue.queueGroupings}" var="queueGrouping" varStatus="status">
                        <tr>
                            <td>
                                <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean" event="viewGrouping">
                                    <stripes:param name="queueGroupingId" value="${queueGrouping.queueGroupingId}" />
                                    <stripes:param name="queueType" value="${actionBean.queueType}" />

                                    ${queueGrouping.queueGroupingText}
                                </stripes:link>
                            </td>
                            <td>${queueGrouping.queuePriority.displayName}</td>
                            <td>${queueGrouping.queueOrigin.displayName} <c:if test="${not empty queueGrouping.queueSpecialization}">(${queueGrouping.queueSpecialization.displayName})</c:if> </td>

                            <c:set var="doNotNeedPico" value="${fn:length(queueGrouping.queuedEntities) - queueGrouping.remainingEntities}" />
                            <c:set var="needPico" value="${queueGrouping.remainingEntities}" />
                            <c:set var="percentNotNeedingPico" value="${doNotNeedPico * 100 / (doNotNeedPico + needPico)}" />
                            <c:set var="percentNeedingPico" value="${needPico * 100 / (doNotNeedPico + needPico)}" />

                            <td title="${needPico} need Pico, ${doNotNeedPico} do not need Pico" class="barFull">
                                <span class="barNeedingPico" style="width:${percentNeedingPico}%"></span>
                                <span class="barNotNeedingPico" style="width:${percentNotNeedingPico}%"></span>
                                <span class="numberNeedingPico">${needPico}</span>
                                <span class="numberNotNeedingPico">${doNotNeedPico}</span>
                            </td>
                            <td>
                                <span class="position">
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean" event="moveToTop">
                                        <stripes:param name="queueGroupingId" value="${queueGrouping.queueGroupingId}" />
                                        <stripes:param name="queueType" value="${actionBean.queueType}" />
                                        <img src="${ctxpath}/images/Double_arrow_green_up.png" title="Move To Top" style="height: 24px;" />
                                    </stripes:link>
                                    <input type="radio" name="queueGroupingId" value="${queueGrouping.queueGroupingId}" />
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean" event="moveToBottom">
                                        <stripes:param name="queueGroupingId" value="${queueGrouping.queueGroupingId}" />
                                        <stripes:param name="queueType" value="${actionBean.queueType}" />
                                        <img src="${ctxpath}/images/Double_arrow_green_down.png" title="Move To Bottom" style="height: 24px;"/>
                                    </stripes:link>
                                </span>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
                <tfoot>
                <tr><td colspan="5">
                        Total Vessels Needing Pico: ${actionBean.totalNeedPico}<br />
                        Exome Express Vessels: ${actionBean.totalExomeExpress}<br />
                        Clinical Vessels: ${actionBean.totalClinical}<br />
                        Total Vessels in Rework: ${actionBean.totalNeedRework}
                    </td>
                </tr>
                </tfoot>
            </table>

            Position to move Selected Items to: <input type="text" name="positionToMoveTo" value="" />
            <stripes:submit name="updatePositions" value="Update Positions" />
        </stripes:form>
        <p>&#160;</p>
        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:hidden name="queueType" />
            Enter the barcodes of the vessels to Remove. One per line.
            <stripes:textarea name="excludeVessels" /><br />
            <stripes:submit name="excludeLabVessels" value="Exclude Vessels" />
        </stripes:form>
        <p>&#160;</p>
        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:hidden name="queueType" />
            Enter the barcodes of the vessels to Add to the Queue.  One per line.
            <stripes:textarea name="enqueueSampleIds" /><br />
            <c:if test="${fn:length(actionBean.allowedQueueSpecializations) > 0}">
            Select applicable Queue Specialization:
            <stripes:select name="queueSpecialization">
                <stripes:option label="Select One: " value="" />
                <stripes:options-collection collection="${actionBean.allowedQueueSpecializations}" value="name" label="displayName" />
            </stripes:select><br />
            </c:if>
            <stripes:submit name="enqueueLabVessels" value="Add Vessels" />
        </stripes:form>

        <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:param name="queueType" />
            <stripes:param name="downloadFullQueueData" />
            Download Data Dump
        </stripes:link>
    </stripes:layout-component>
</stripes:layout-render>
