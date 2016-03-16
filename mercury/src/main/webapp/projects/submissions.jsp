<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto.SubmissionField" %>

<%@ include file="/resources/layout/taglibs.jsp" %>
<%--@elvariable id="userBean" type="org.broadinstitute.gpinformatics.mercury.presentation.UserBean"--%>
<%--@elvariable id="submissionsTabSelector" type="java.lang.String"--%>
<%--@elvariable id="submissionType" type="java.lang.String"--%>
<%--@elvariable id="submissionRepository" type="java.lang.String"--%>
<%--@elvariable id="researchProject" type="java.lang.String"--%>
<%--@elvariable id="event" type="java.lang.String"--%>
<stripes:layout-definition>
    <stripes:useActionBean var="actionBean" event="${event}"
                           beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<head>
    <style type="text/css">
        .columnCheckbox { width: 3em; }
        .columnDataType { width: 4em; }
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
        .dataTables_processing {
            width:400px;
            border: 2px solid #644436;
            height: 55px;
            color: black;
            background-image: "${ctxpath}/images/spinner.gif";
        }
        .submission-status-tooltip {
            border-bottom: 1px dotted #000;
            text-decoration: none;
        }

        .control-group select {
            width: auto;
        }
    </style>
    <script type="text/javascript">
        function formatInput(item) {
            var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
            return "<li>" + item.dropdownItem + extraCount + '</li>';
        }
//        var oTable;

        $j(document).ready(function () {
            $j("#bioProject").tokenInput(
                    "${ctxpath}/projects/project.action?bioProjectAutocomplete=", {
                        hintText: "Type a BioProject Name",
                        prePopulate: ${actionBean.ensureStringResult(actionBean.bioProjectTokenInput.completeData)},
                        tokenDelimiter: "${actionBean.bioProjectTokenInput.separator}",
                        preventDuplicates: true,
                        tokenLimit: 1,
                        minChars: 2,
                        resultsFormatter: formatInput,
                        autoSelectFirstResult: true
                    }
            );


            function createErrorPopover(data, errors) {
                var span = document.createElement("span");
                $j(span).addClass("submission-status-tooltip popover-dismiss");
                $j(span).attr("title", data);
                $j(span).append(data);
                $j(span).attr("data-content", errors);
                return span.outerHTML;
            }

            var oTable;
            $j("${submissionsTabSelector}").click(function () {
            if (oTable == undefined) {
                function renderCheckbox(data, type, row) {
                    if (type === 'display') {
                        var inputTag = document.createElement("input");
                        $j(inputTag).attr("name", "selectedSubmissionSamples");
                        $j(inputTag).attr("value", data);
                        $j(inputTag).attr("type", "checkbox");
                        $j(inputTag).addClass("shiftCheckbox");
                        var status = row.<%=SubmissionField.SUBMITTED_STATUS %>;
                        if (status) {
                            $j(inputTag).attr('disabled', status);
                        }
                        return inputTag.outerHTML;
                    }
                    return data;
                }

                function displayDataList(data, type, row) {
                    if (type === 'display') {
                        if (Array.isArray(data)) {
                            return data.join("<br/>");
                        }
                    }
                    return data;
                }

                oTable = $j('#submissionSamples').dataTable({
                    "oLanguage": {
                        "sProcessing": "Please wait. Gathering data from Mercury, Bass, and Picard. This may take a few minutes."
                    },
                    "oTableTools": ttExportDefines,
                    "bStateSave": false,
                    "bProcessing": true,
                    "bDeferRender": true,
                    "sAjaxSource": '${ctxpath}/projects/project.action',
                    "fnServerData": function (sSource, aoData, fnCallback, oSettings) {
                        aoData.push({"name": "researchProject", "value": "${researchProject}"});
                        aoData.push({"name": "${event}", "value": "${event}"});

                        oSettings.jqXHR = $j.ajax({
                            "dataType": 'json',
                            "url": sSource,
                            "data": aoData,
                            "success": fnCallback
                        });
                    },

                    //needed when null value is returned in JSON
                    "aoColumnDefs": [{"sDefaultContent": "", "aTargets": ["_all"]}],
                    "aoColumns": [
                        {"mData": "<%=SubmissionField.SAMPLE_NAME%>", "mRender": renderCheckbox},
                        {"mData": "<%=SubmissionField.SAMPLE_NAME%>"},
                        {"mData": "<%=SubmissionField.SUBMISSION_SITE%>"},
                        {"mData": "<%=SubmissionField.LIBRARY_DESCRIPTOR%>"},
                        {"mData": "<%=SubmissionField.DATA_TYPE %>"},
                        { "mData": "<%=SubmissionField.PRODUCT_ORDERS %>", "sWidth": "140px", "sClass": "ellipsis",
                            "mRender": displayDataList },
                        {"mData": "<%=SubmissionField.AGGREGATION_PROJECT %>"},
                        {"mData": "<%=SubmissionField.FILE_TYPE %>"},
                        {"mData": "<%=SubmissionField.VERSION %>"},
                        {"mData": "<%=SubmissionField.QUALITY_METRIC %>"},
                        {"mData": "<%=SubmissionField.CONTAMINATION_STRING %>"},
                        {"mData": "<%=SubmissionField.FINGERPRINT_LOD %>"},
                        {"mData": "<%=SubmissionField.LANES_IN_AGGREGATION %>"},
                        {"mData": "<%=SubmissionField.SUBMITTED_VERSION %>"},
                        {
                            "mData": "<%=SubmissionField.SUBMITTED_STATUS %>",
                            "mRender": function (data, type, row) {
                                if (type === 'display') {
                                    var errors = row.<%=SubmissionField.SUBMITTED_ERRORS%>;
                                    if (errors) {
                                        return createErrorPopover(data, errors);
                                    }
                                }
                                return data;
                            }
                        },
                        {"mData": "<%=SubmissionField.STATUS_DATE %>"}
                    ],

                    "fnDrawCallback": function () {
                        $j('.shiftCheckbox').enableCheckboxRangeSelection();
                        $j(".submission-status-tooltip").popover({
                            trigger: "hover",
                            html: "true",
                            "data-container": "body",
                            "data-toggle": "popover"
                        });

                        $j(".submissionControls").show();
                        if ($j("#submissionSamples").height() > $j(window).height()) {
                            $j("#bottomSubmitButton").show();
                        } else {
                            $j("#bottomSubmitButton").hide();
                        }
                    }
                });
            }
            })
        });

    </script>
</head>


<stripes:form beanclass="${actionBean.class.name}" class="form-horizontal">
    <stripes:hidden name="submitString"/>
    <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>

    <div class="submissionControls" style="width: auto; margin-bottom: 20px;display:none" >
        <div class="control-group">
            <stripes:label for="bioProject" class="control-label label-form">Choose a BioProject *</stripes:label>

            <div class="controls">
                <stripes:text id="bioProject" name="bioProjectTokenInput.listOfKeys"/>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="submissionType"
                           class="control-label label-form">Choose a Library Descriptor *</stripes:label>

            <div class="controls">
                <stripes:select id="submissionType" name="selectedSubmissionLibraryDescriptor">
                    <option>Choose...</option>
                    <stripes:options-collection label="description" value="name"
                                                collection="${actionBean.submissionLibraryDescriptors}"/></stripes:select>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="submissionRepository"
                           class="control-label label-form">Choose a Repository *</stripes:label>
            <div class="controls">
                <stripes:select id="submissionRepository" name="selectedSubmissionRepository">
                    <option>Choose...</option>
                    <stripes:options-collection label="description" value="name"
                                                collection="${actionBean.activeRepositories}"/>
                </stripes:select>
            </div>
        </div>
        <stripes:submit name="<%=ResearchProjectActionBean.POST_SUBMISSIONS_ACTION%>"
                        value="Post Selected Submissions" class="btn submissionControls"
                        disabled="${!actionBean.submissionAllowed}" style="display:none;"/>
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
            <th width="80">BioSample</th>
            <th class="">Repository</th>
            <th class="">Library</th>
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
                <%--<th class="columnBioProject">Blacklistd</th>--%>
            <th class="columnSubmittedVersion">Submitted Version</th>
            <th class="columnSubmissionStatus">Current Status</th>
            <th class="columnSubmissionStatusDate">Status Date</th>

        </tr>
        </thead>
    </table>
    <stripes:submit name="<%=ResearchProjectActionBean.POST_SUBMISSIONS_ACTION%>"
                    id="bottomSubmitButton" value="Post Selected Submissions" class="btn submissionControls"
                    disabled="${!actionBean.submissionAllowed}" style="display:none;"/>

</stripes:form>
</stripes:layout-definition>
