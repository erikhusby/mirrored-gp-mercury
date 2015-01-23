<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadQuantsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Upload Quants" sectionTitle="Upload Quants">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#runTable').dataTable({
                    "oTableTools": ttExportDefines,
                    "aoColumns": [
                        {"bSortable": true}, // name
                        {"bSortable": true} // value
                    ]
                });
                $j('#metricsTable').dataTable({
                    "oTableTools": ttExportDefines,
                    "aoColumns": [
                        {"bSortable": true}, // rev id
                        {"bSortable": true}, // barcode
                        {"bSortable": true}, // sample ID
                        {"bSortable": true}, // collaborator patient ID
                        {"bSortable": true, "sType": "numeric"}, // value
                        {"bSortable": true, "sType": "numeric"}, // volume
                        {"bSortable": true, "sType": "numeric"}, // total ng
                        {"bSortable": true},  // decision
                        {"bSortable": true},  // user
                        {"bSortable": true},  // date
                        {"bSortable": true},  // reason
                        {"bSortable": false}  // checkbox
                    ]
                });
                <%-- Clears all checkboxes. --%>
                $j('input.overrideCheckboxClass').prop('checked',false);

                <%-- Hooks any override checkbox change and hides/shows the NextSteps button. --%>
                $j('input.overrideCheckboxClass').change(function() {
                    var isChecked = $j('input.overrideCheckboxClass:checked');
                    if (isChecked.length) {
                        $j("#viewNextStepsBtn").attr("disabled", "disabled");
                    } else {
                        $j("#viewNextStepsBtn").removeAttr("disabled");
                    }
                });

            });


        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="quantFormat" class="control-label">File Format</stripes:label>
                    <div class="controls">
                        <stripes:select name="quantFormat">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadQuantsActionBean.QuantFormat"
                                    label="displayName"/>
                        </stripes:select>
                    </div>
                    <stripes:label for="quantType" class="control-label">Quant Type</stripes:label>
                    <div class="controls">
                        <stripes:select name="quantType">
                            <stripes:options-collection collection="${actionBean.uploadEnabledMetricTypes}"
                                                        label="displayName"/>
                        </stripes:select>
                    </div>
                    <stripes:label for="quantFile" class="control-label">
                        Quant Spreadsheet
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="quantSpreadsheet" id="quantFile"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="uploadQuant" value="Upload Quants" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
        <c:if test="${actionBean.labMetricRun != null}">
            Type: ${actionBean.labMetricRun.metricType.displayName}
            <br/>
            Run Date: <fmt:formatDate value="${actionBean.labMetricRun.runDate}" pattern="${actionBean.dateTimePattern}"/>
            <br/>
            Run Name: ${actionBean.labMetricRun.runName}
            <table class="table simple" id="runTable">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Value</th>
                    </tr>
                </thead>
                <c:forEach items="${actionBean.labMetricRun.metadata}" var="metadata">
                    <tr>
                        <td>${metadata.key}</td>
                        <td>${metadata.value}</td>
                    </tr>
                </c:forEach>
            </table>
            <stripes:form beanclass="${actionBean.class.name}" id="metricsForm" class="form-horizontal">
                <table class="table simple" id="metricsTable">
                    <thead>
                    <tr>
                        <th>Position</th>
                        <th>Barcode</th>
                        <th>Sample ID</th>
                        <th>Collaborator Patient ID</th>
                        <th>Value</th>
                        <th>Volume</th>
                        <th>Total ng</th>
                        <th>Decision</th>
                        <th>User</th>
                        <th>Date</th>
                        <th>Reason</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.labMetricRun.labMetrics}" var="labMetric">
                        <c:if test="${labMetric.labMetricDecision != null}">
                            <tr>
                                <td>
                                    ${labMetric.vesselPosition}
                                </td>
                                <td>
                                    ${labMetric.labVessel.label}
                                </td>
                                <td>
                                    <c:if test="${!actionBean.hasErrors()}">
                                        ${fn:join(labMetric.labVessel.sampleNamesArray, " ")}
                                    </c:if>
                                </td>
                                <td>
                                    <c:if test="${!actionBean.hasErrors()}">
                                        ${fn:join(labMetric.labVessel.getMetadataValues("PATIENT_ID"), " ")}
                                    </c:if>
                                </td>
                                <td>
                                    ${labMetric.value}
                                </td>
                                <td>
                                    ${labMetric.labVessel.volume}
                                </td>
                                <td>
                                    ${labMetric.totalNg}
                                </td>
                                <td>
                                    ${labMetric.labMetricDecision.decision}
                                </td>
                                <td>
                                    ${actionBean.getUserFullName(labMetric.labMetricDecision.deciderUserId)}
                                </td>
                                <td>
                                    <fmt:formatDate value="${labMetric.labMetricDecision.decidedDate}" pattern="${actionBean.dateTimePattern}"/>
                                </td>
                                <td>
                                    ${labMetric.labMetricDecision.overrideReason}
                                </td>
                                <td>
                                    <c:if test="${labMetric.labMetricDecision.decision.editable}">
                                        <stripes:checkbox name="selectedMetrics" value="${labMetric.labMetricId}"
                                                class="overrideCheckboxClass"/>
                                    </c:if>
                                </td>
                            </tr>
                        </c:if>
                    </c:forEach>
                    </tbody>
                </table>
                <stripes:hidden name="labMetricRunId" value="${actionBean.labMetricRun.labMetricRunId}"/>
                <stripes:hidden name="tubeFormationLabel" value="${actionBean.tubeFormationLabel}"/>
                <stripes:hidden name="quantType" value="${actionBean.quantType}"/>
                <stripes:label for="overrideDecision" class="control-label">Override Decision</stripes:label>
                <div class="controls">
                    <stripes:select name="overrideDecision">
                        <stripes:options-collection collection="${actionBean.editableDecisions}"/>
                    </stripes:select>
                </div>
                <stripes:label for="overrideReason" class="control-label">Override Reason</stripes:label>
                <div class="controls">
                    <stripes:text name="overrideReason"/>
                </div>
                <stripes:submit name="saveMetrics" value="Save" class="btn btn-primary"/>

            </stripes:form>

            <c:if test="${actionBean.quantType == 'INITIAL_PICO'}">
                <stripes:form action="${actionBean.picoDispositionActionBeanUrl}" id="nextStepsForm" class="form-horizontal">
                    <stripes:hidden name="tubeFormationLabel" value="${actionBean.tubeFormationLabel}"/>
                    <stripes:submit name="view" value="View Next Steps" class="btn btn-primary" id="viewNextStepsBtn"/>
                </stripes:form>
            </c:if>

        </c:if>
    </stripes:layout-component>
</stripes:layout-render>
