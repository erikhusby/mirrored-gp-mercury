<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>
<head>
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

            $j('.submissionStatusClass').popover({ trigger:"hover", html:true });
//            $j('#postSubmissionBtn').prop('disabled', true);
//
//            $j('.shiftCheckbox').change(function (e) {
//                var len = $j("#general-content input[class='shiftCheckbox']:checked").length;
//                if(len>0) {
//                    $j('#postSubmissionBtn').prop('disabled', false);
//                } else {
//                    $j('#postSubmissionBtn').prop('disabled', true);
//                }
//            });
        });
        function formatInput(item) {
                        var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                        return "<li>" + item.dropdownItem + extraCount + '</li>';
                    }
        $j(document).ready(function () {
            var oTable = $j('#submissionSamples').dataTable({
                "aaSorting": [
                        [1, 'asc']
                ],
                "aoColumns": [
                    {"bSortable": false},               //Checkbox
                    {"bSortable": true},                //Sample
                    {"bSortable": false},                //BioSample
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
            $j(".tooltip").popover();
        })
    </script>
</head>


<stripes:form beanclass="${actionBean.class.name}">
    <stripes:hidden name="submitString"/>
    <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>

    <div class="control-group">
        <stripes:label for="bioProject" class="control-label">Choose a BioProject</stripes:label>

        <div class="controls">
            <stripes:text id="bioProject" name="bioProjectTokenInput.listOfKeys"/>
        </div>
    </div>

    <table class="table simple" id="submissionSamples" style="table-layout: fixed;">
        <thead>
        <tr>
            <!-- add data type to big list -->
            <!-- only show latest single file -->
            <th width="20">
                <input for="count" type="checkbox" class="checkAll"/>
                <span id="count" class="checkedCount"></span>
            </th>
            <th width="80">Sample</th>
            <th width="80">BioSample</th>
            <%--<th width="100">BioSample</th>--%>
            <th width="65">Data Type</th>
            <th width="200">PDOs</th>
            <th width="70">Aggregation Project</th>
            <th width="50">File Type</th>
            <th width="30">Version</th>
            <th width="40">Quality Metric</th>
            <th width="75">Contamination</th>
            <th width="70">Fingerprint</th>
            <!-- add # lanes, # lanes blacklisted, notes -->
            <th width="70">Lanes in Aggregation</th>
            <th width="60">Blacklisted Lanes</th>
            <th width="60">Submitted Version</th>
            <th width="50">Current Status</th>
            <th width="40">Status Date</th>

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
                                      value="${submissionSample.sampleName}" />
                </td>

                <td>${submissionSample.sampleName}</td>
                <td><stripes:text name="bioSamples[${submissionSample.sampleName}]" size="4"/></td>
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
                <td><%--blacklisted lanes--%></td>
                <td>${submissionSample.version}</td>
                <td><span class="tooltip" title="${fn:join(submissionSample.submittedErrors, "<br/>")}" rel="popover" data-trigger="hover" data-placement="left" data-html="true" >${submissionSample.submittedStatus}</span></td>
                <td>${submissionSample.statusDate}</td>

                    <%--<c:if test="${submissionSample.la == 0}">--%>
                    <%--<td colspan="11" style="text-align: center;">--%>
                    <%--no files available--%>
                    <%--</td>--%>
                    <%--</c:if>--%>
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
