<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>
<head>
    <style type="text/css">
        .columnCheckbox { width: 3em; }
        .columnDataType { width: 4em; }            `
        .columnPDOs { width: 12em; }
        .columnAggregationProject { width: 5em; }
        .columnFileType { width: 5em; }
        .columnVersion { width: 6em; }
        .columnQualityMetric { width: 5em; }
        .columnContamination { width: 5em; }
        .columnFingerprint { width: 6em; }
        .columnLanesInAggregation { width: 5em; }
        .columnBioProject { width: 6em; }
        .columnSubmittedVersion { width: 6em; }
        .columnSubmissionStatus { width: 6em; }
        .columnSubmissionStatusDate { width: 6em; }

        .submission-status-tooltip {
            border-bottom: 1px dotted #000;
            text-decoration: none;
        }
    </style>
    <script type="text/javascript">
        $j(document).ready(function () {
            $j("#bioProject").tokenInput(
                    "${ctxpath}/projects/project.action?bioProjectAutocomplete=", {
                        hintText: "Type a BioProject Name",
                        prePopulate: ${actionBean.ensureStringResult(actionBean.bioProjectTokenInput.completeData)},
                        tokenDelimiter: "${actionBean.bioProjectTokenInput.separator}",
                        preventDuplicates: true,
                        tokenLimit: 1,
                        resultsFormatter: formatInput
                    }
            );

        });
        function formatInput(item) {
                        var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                        return "<li>" + item.dropdownItem + extraCount + '</li>';
                    }
        $j(document).ready(function () {
            var oTable = $j('#submissionSamples').dataTable({
                "oTableTools": ttExportDefines,
                "aaSorting": [
                        [1, 'asc']
                ],
                "aoColumns": [
                    {"bSortable": false},               //Checkbox
                    {"bSortable": true},                //Sample
//                    {"bSortable": false},                //BioSample
                    {"bSortable": false},               //Data Type
                    {"bSortable": false},               //PDOs
                    {"bSortable": false},               //Aggregation Project
                    {"bSortable": false},               //File Type
                    {"bSortable": false},               //Version
                    {"bSortable": false},               //QualityMetric
                    {"bSortable": false},               //Contamination
                    {"bSortable": false},               //Fingerprint
                    {"bSortable": false},               //Lanes in Aggregation
                    {"bSortable": false},               //Blacklisted Lanes
                    {"bSortable": false},               //Submitted Version
                    {"bSortable": false},               //Current Status
                    {"bSortable": false}                //Status Date
                ]
            });
            $j('.shiftCheckbox').enableCheckboxRangeSelection();
            $j(".submission-status-tooltip").popover({
                trigger: "hover",
                html: "true",
                "data-container": "body",
                "data-toggle": "popover"
            });
        })
    </script>
</head>


<stripes:form beanclass="${actionBean.class.name}" class="form-horizontal">
    <stripes:hidden name="submitString"/>
    <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>

    <div class="control-group">
        <stripes:label for="bioProject" class="control-label">Choose a BioProject *</stripes:label>

        <div class="controls">
            <stripes:text id="bioProject" name="bioProjectTokenInput.listOfKeys"/>
        </div>
    </div>

    <table class="table simple" id="submissionSamples">
        <thead>
        <tr>
            <!-- add data type to big list -->
            <!-- only show latest single file -->
            <th class="columnCheckbox">
                <input for="count" type="checkbox" class="checkAll"/>
                <span id="count" class="checkedCount"></span>
            </th>
            <th>Sample</th>
            <%--<th width="80">BioSample</th>--%>
            <%--<th width="100">BioSample</th>--%>
            <th class="columnDataType">Data Type</th>
            <th class="columnPDOs">PDOs</th>
            <th class="columnAggregationProject">Agg. Project</th>
            <th class="columnFileType">File Type</th>
            <th class="columnVersion">Version</th>
            <th class="columnQualityMetric">Quality Metric</th>
            <th class="columnContamination">Contam.</th>
            <th class="columnFingerprint">Fingerprint</th>
            <!-- add # lanes, # lanes blacklisted, notes -->
            <th class="columnLanesInAggregation">Lanes in Agg.</th>
            <th class="columnBioProject">Bio Project</th>
            <th class="columnSubmittedVersion">Submitted Version</th>
            <th class="columnSubmissionStatus">Current Status</th>
            <th class="columnSubmissionStatusDate">Status Date</th>

        </tr>
        </thead>
        <tbody>
        <!-- http://localhost:8080/Mercury/projects/project.action?view=&researchProject=RP-13 -->
        <!-- http://localhost:8080/Mercury/projects/project.action?view=&researchProject=RP-356 -->
        <!-- $j('.fileCheckbox').children()[0].checked = true -->
        <!-- $j('.fileCheckbox').data('contamination') -->


        <c:forEach items="${actionBean.submissionSamples}" var="submissionSample">
            <tr>
                <td>
                    <stripes:checkbox name="selectedSubmissionSamples" class="shiftCheckbox"
                                      value="${submissionSample.sampleName}" disabled="${not empty submissionSample.submittedStatus ? 'disabled' : ''}" />
                </td>

                <td>${submissionSample.sampleName}</td>
                <%--<td><stripes:text name="bioSamples[${submissionSample.sampleName}]" size="4"/></td>--%>
                <%--<td>&lt;%&ndash;bio-sample&ndash;%&gt;</td>--%>
                <td>${submissionSample.dataType}</td>
                <td style="padding: 5px;
                                               text-align: center;">
                    <table class="simple" style="table-layout: fixed;">
                        <c:forEach items="${submissionSample.productOrders}" var="pdo">
                            <tr>
                                <td width="60">${pdo.businessKey}</td>
                                <td style="max-width: 140px; min-width: 100px; overflow: hidden;
                                text-overflow: ellipsis; white-space: nowrap;"
                                    class="ellipsis"
                                    title="${pdo.product.productName}">
                                        ${pdo.product.productName}</td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
                <td> ${submissionSample.aggregationProject} </td>
                <td> ${submissionSample.fileType} </td>
                <td> ${submissionSample.version}</td>
                <td> ${submissionSample.qualityMetricString}</td>
                <td>${submissionSample.contaminationString}</td>
                <td>${submissionSample.fingerprintLOD.displayString()}</td>
                <td>${submissionSample.lanesInAggregation}</td>
                <td>${submissionSample.bioProject}</td>
                <td>${submissionSample.version}</td>
                <td>
                        <c:choose><c:when test="${fn:length(submissionSample.submittedErrorsArray)>0}">
                        <span class="submission-status-tooltip popover-dismiss"
                              title="${ submissionSample.submittedStatus}"
                              data-content="${fn:join(submissionSample.submittedErrorsArray, "<br/>")}">
                          ${submissionSample.submittedStatus}
                        </span>
                        </c:when>
                        <c:otherwise>${submissionSample.submittedStatus}</c:otherwise></c:choose>
                </td>
                <td>${submissionSample.statusDate}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div class="span1">
        <stripes:submit name="<%=ResearchProjectActionBean.POST_SUBMISSIONS_ACTION%>" value="Post Selected Submissions"
                        class="btn btn-primary" id="postSubmissionBtn"/>
    </div>

    <%--<button>Submit these files</button>--%>
</stripes:form>
