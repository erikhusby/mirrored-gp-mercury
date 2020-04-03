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
                <%-- Clears all checkboxes. --%>
                $j('input.conditionalCheckboxClass').prop('checked',false);

                <%-- Hooks any override checkbox change and hides/shows the NextSteps button. --%>
                $j('input.conditionalCheckboxClass').change(function() {
                    var isChecked = $j('input.conditionalCheckboxClass:checked');
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
                                          style="margin-top: 10px;"
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
            <c:if test="${not empty actionBean.labMetricRun.metadata}">
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
            </c:if>
            <stripes:form beanclass="${actionBean.class.name}" id="metricsForm" class="form-horizontal">

                <stripes:layout-render name="/columns/configurable_list.jsp"
                        entityName="${actionBean.entityName}"
                        sessionKey="${actionBean.sessionKey}"
                        columnSetName="${actionBean.columnSetName}"
                        downloadColumnSets="${actionBean.downloadColumnSets}"
                        resultList="${actionBean.resultList}"
                        action="${ctxpath}/search/ConfigurableSearch.action"
                        downloadViewedColumns="False"
                        isDbSortAllowed="False"
                        dbSortPath=""
                        dataTable="true"/>

                <stripes:hidden name="labMetricRunId" value="${actionBean.labMetricRun.labMetricRunId}"/>
                <stripes:hidden name="tubeFormationLabels" value="${actionBean.tubeFormationLabels}"/>
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
                <stripes:form action="${actionBean.picoDispositionActionBeanUrl}" id="nextStepsForm"
                              class="form-horizontal">
                    <stripes:hidden name="labMetricRunId" value="${actionBean.labMetricRun.labMetricRunId}"/>
                    <stripes:submit name="buildFwdTableData" value="Manage Dispsitions" class="btn btn-primary"
                                    id="viewNextStepsBtn"/>
                </stripes:form>
            </c:if>

        </c:if>
    </stripes:layout-component>
</stripes:layout-render>