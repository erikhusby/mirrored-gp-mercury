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
                        {"bSortable": true, "sType": "numeric"}, // value
                        {"bSortable": true, "sType": "numeric"}, // volume
                        {"bSortable": true, "sType": "numeric"}, // total ng
                        {"bSortable": true},  // decision
                        {"bSortable": true},  // user
                        {"bSortable": true},  // date
                        {"bSortable": true},  // reason
                        {"bSortable": false},  // checkbox
                    ]
                })

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
            Run Date: ${actionBean.labMetricRun.runDate}
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
                        <th class="columnRevId">Position</th>
                        <th class="columnRevId">Barcode</th>
                        <th class="columnRevDate">Value</th>
                        <th class="columnRevDate">Volume</th>
                        <th class="columnRevDate">Total ng</th>
                        <th class="columnUser">Decision</th>
                        <th class="columnUser">User</th>
                        <th class="columnUser">Date</th>
                        <th class="columnUser">Reason</th>
                        <th class="columnUser"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.labMetricRun.labMetrics}" var="labMetric">
                        <c:if test="${labMetric.labMetricDecision != null}">
                            <tr>
                                <td class="columnRevId">
                                    ${labMetric.vesselPosition}
                                </td>
                                <td class="columnRevId">
                                    ${labMetric.labVessel.label}
                                </td>
                                <td class="columnRevDate">
                                    ${labMetric.value}
                                </td>
                                <td class="columnRevDate">
                                    ${labMetric.labVessel.volume}
                                </td>
                                <td class="columnRevDate">
                                    ${labMetric.totalNg}
                                </td>
                                <td class="columnUser">
                                    ${labMetric.labMetricDecision.decision}
                                </td>
                                <td class="columnUser">
                                    ${actionBean.getUserFullName(labMetric.labMetricDecision.deciderUserId)}
                                </td>
                                <td class="columnUser">
                                    ${labMetric.labMetricDecision.decidedDate}
                                </td>
                                <td class="columnUser">
                                    ${labMetric.labMetricDecision.overrideReason}
                                </td>
                                <td class="columnUser">
                                    <c:if test="${labMetric.labMetricDecision.decision.editable}">
                                        <stripes:checkbox name="selectedMetrics" value="${labMetric.labMetricId}"/>
                                    </c:if>
                                </td>
                            </tr>
                        </c:if>
                    </c:forEach>
                    </tbody>
                </table>
                <stripes:hidden name="labMetricRunId" value="${actionBean.labMetricRun.labMetricRunId}"/>
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
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>