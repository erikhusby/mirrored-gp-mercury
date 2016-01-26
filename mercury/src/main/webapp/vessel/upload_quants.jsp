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
                    "aoColumnDefs" : [
                        { "bSortable": false, "aTargets": "no-sort" },
                        { "bSortable": true, "sType": "numeric", "aTargets": "sort-numeric" }
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
                    <stripes:label for="allowRePico" class="control-label">Redo existing quants</stripes:label>
                    <div class="controls">
                        <stripes:checkbox id="allowRePico" name="acceptRePico"
                                          style="margin-top: 10px;" class="overrideCheckboxClass"
                                          title="Check this to upload a spreadsheet of quants when tubes already have quants of the same Quant Type and a new pico run was done.  If left unchecked, Mercury will error the upload if it finds existing quants."/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="uploadQuant" value="Upload Quants" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
        <c:if test="${!actionBean.hasErrors() && actionBean.labMetricRun != null}">
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
                        <c:if test="${actionBean.labMetricRun.metricType.category != 'QUALITY'}">
                            <th class="sort-numeric">Volume</th>
                            <th class="sort-numeric">Total ng</th>
                        </c:if>
                        <th>Decision</th>
                        <th>Note</th>
                        <th>User</th>
                        <th>Date</th>
                        <th>Reason</th>
                        <th class="no-sort"></th>
                        <%--<th class="no-show"></th>--%>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.labMetricRun.labMetrics}" var="labMetric">
                        <c:if test="${labMetric.labMetricDecision != null}">
                            <tr <c:if test="${labMetric.labMetricDecision.needsReview}">
                                    class="warning"
                                </c:if>>
                                <td>
                                    ${labMetric.vesselPosition}
                                </td>
                                <td>
                                    ${labMetric.labVessel.label}
                                </td>
                                <td>
                                    ${fn:join(labMetric.labVessel.sampleNamesArray, " ")}
                                </td>
                                <td>
                                    ${fn:join(labMetric.labVessel.getMetadataValues("PATIENT_ID"), " ")}
                                </td>
                                <td>
                                    ${labMetric.value}
                                </td>
                                <c:if test="${labMetric.name.category != 'QUALITY'}">
                                    <td>
                                        ${labMetric.labVessel.volume}
                                    </td>
                                    <td>
                                        ${labMetric.totalNg}
                                    </td>
                                </c:if>
                                <td>
                                    ${labMetric.labMetricDecision.decision}
                                </td>
                                <td>
                                    ${labMetric.labMetricDecision.note}
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

            <stripes:layout-render name="/columns/configurable_list.jsp"
                    entityName="${actionBean.entityName}"
                    sessionKey="${actionBean.sessionKey}"
                    columnSetName="${actionBean.columnSetName}"
                    downloadColumnSets="${actionBean.downloadColumnSets}"
                    resultList="${actionBean.resultList}"
                    action="${ctxpath}/search/ConfigurableSearch.action"
                    downloadViewedColumns="True"
                    isDbSortAllowed="True"
                    dbSortPath=""/>

        </c:if>
    </stripes:layout-component>
</stripes:layout-render>