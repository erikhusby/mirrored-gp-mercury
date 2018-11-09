<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto.SubmissionField" %>

<%@ include file="/resources/layout/taglibs.jsp" %>
<%--@elvariable id="userBean" type="org.broadinstitute.gpinformatics.mercury.presentation.UserBean"--%>
<%--@elvariable id="submissionsTabSelector" type="java.lang.String"--%>
<%--@elvariable id="submissionDatatype" type="java.lang.String"--%>
<%--@elvariable id="submissionRepository" type="java.lang.String"--%>
<%--@elvariable id="researchProject" type="java.lang.String"--%>
<%--@elvariable id="event" type="java.lang.String"--%>
<stripes:layout-definition>
    <stripes:useActionBean var="actionBean" event="${event}"
                           beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<head>
    <style type="text/css">
        .columnCheckbox { width: 3em; }
        .columnBioSample{ width: 6em; }
        .columnDataType { width: 4em; }
        .columnPDOs { width: 11em; }
        .columnAggregationProject { width: 5em; }
        .columnRepository { max-width: 30em; }
        .columnLibraryDescriptor { width: 11em; }
        .columnVersion { width: 2em; }
        .columnQualityMetric { width: 3em; }
        .columnContamination { width: 5em; }
        .columnFingerprint { width: 6em; }
        .columnLanesInAggregation { width: 2em; }
        .columnBioProject { width: 6em; }
        .columnSubmittedVersion { width: 6em; }
        .columnSubmissionStatus { width: 6em; }
        .columnSubmissionStatusDate { width: 6em; }
        .dataTables_processing {
            width:400px;
            border: 2px solid #644436;
            height: 55px;
            color: black;
            left: 40%;
            visibility: visible;
            z-index: 10;
            top: 0;
        }

        .dataTables_info {
            float: right;
        }

        .submissionControls {
            width: auto;
            margin-bottom: 20px;
            display: none;
        }

        .columnFilter {
            margin-bottom: 5px
        }

        .form-horizontal .control-group {
            margin-bottom: 10px
        }
        .submission-tooltip {
            border-bottom: 1px dotted #000;
            text-decoration: none;
        }

        .chosen-container input {
            min-width: 150px !important;
        }

        .noWrap {
            white-space: nowrap;
        }
        .control-group select {
            width: auto;
        }
        .ui-accordion-content {
            overflow: visible;
        }
        div.submission-status-tooltip{
            position:relative;
            padding: 12px;
            left: -12px;
        }
        .autoWidth {
            width: auto;
        }
    </style>
    <link rel="stylesheet" href="${ctxpath}/resources/scripts/chosen_v1.6.2/chosen.min.css">
    <script type="text/javascript" src="${ctxpath}/resources/scripts/chosen_v1.6.2/chosen.jquery.min.js"></script>
    <script type="text/javascript" src="${ctxpath}/resources/scripts/dataTables-filterColumn.js"></script>
    <script type="text/javascript">
        function formatInput(item) {
            var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
            return "<li>" + item.dropdownItem + extraCount + '</li>';
        }

        function initPasteBarcodesDeprecated() {

            // GPLIM-5660
            // Inform users that this is deprecated on this page and show them how do do this using filters.
            var $pasteInBarcodesDisabledDialog = $j("#listOfBarcodesDisabledForm").dialog({
                buttons: {
                    Ok: function () {
                        $j(this).dialog("close");
                    }
                },
                autoOpen: false,
                title: "This functionality has been deprecated",
                width: '475',
                height: 'auto',
                modal: true,
            });
            $("#PasteBarcodesListDisabled").on("click", function () {
                $pasteInBarcodesDisabledDialog.dialog("open");
            });
        }

        $j(document).ready(function () {

            // GPLIM-5660 This should be removed in a future release.
            initPasteBarcodesDeprecated();

            $j("#bioProject").tokenInput(
                    "${ctxpath}/projects/project.action?bioProjectAutocomplete=", {
                        hintText: "Type a Study Name",
                        <enhance:out escapeXml="false">
                        prePopulate: ${actionBean.ensureStringResult(actionBean.bioProjectTokenInput.completeData)},
                        </enhance:out>
                        tokenDelimiter: "${actionBean.bioProjectTokenInput.separator}",
                        preventDuplicates: true,
                        tokenLimit: 1,
                        minChars: 2,
                        resultsFormatter: formatInput,
                        autoSelectFirstResult: true
                    }
            );


            function createPopover(data, title, errors) {
                var div = jQuery("<div></div>", {
                    "class":"submission-status-tooltip popover-dismiss",
                    "title":title,
                    "data-content":errors,
                    "data-sort":data,
                    "data-toggle":"popover",
                    "data-placement":"top",
                    "text": data
                });
                return div[0].outerHTML;
            }

            var oTable;
            <enhance:out escapeXml="false">
                var submissionsTabSelector = "${submissionsTabSelector}";
            </enhance:out>
            $j(submissionsTabSelector).click(function () {
                function buildMessage(jqXHR) {
                    var responseText = jqXHR.responseJSON;
                    if (responseText && responseText.stripesMessages) {
                        outerDiv = jQuery("<div></div>", {
                            "id": "stripesMessageOuter",
                            "style": "position: relative;z-index:5",
                            "class": "center"
                        });
                        $j("#submissionSamples_wrapper").prepend(outerDiv);
                        $j("#submissionSamples_processing").hide();
                        stripesMessage.set(responseText.stripesMessages.join("<br/>"), responseText.messageType, "#stripesMessageOuter");
                    }
                }

                if (oTable == undefined) {
                    function renderCheckbox(data, type, row) {
                        if (type === 'display') {
                            var status = row.<%=SubmissionField.SUBMITTED_STATUS %>;
                            var tagAttributes = {};
                            if (status.length === 0) {
                                tagAttributes = {
                                    "name": "<%=ResearchProjectActionBean.SUBMISSION_TUPLES_PARAMETER%>",
                                    "value": JSON.stringify(data),
                                    "type": "checkbox",
                                    "class": "shiftCheckbox"
                                };
                            } else {
                                tagAttributes = {
                                    "value": data,
                                    "type": "hidden"
                                };
                            }
                        return jQuery("<input/>", tagAttributes)[0].outerHTML;
                        }
                        if (type === 'sort') {
                            if (data === undefined || data === "") {
                                return 1;
                            }
                            var selector = 'input[type="checkbox"][value="data"]'.replace("data", data);
                            var result = $j(selector).prop("checked");
                            if (result === undefined) {
                                result = 1;
                            }
                            return result;
                        }
                        return data;
                }

                    function displayPdoList(data, type, row) {
                    if (type === 'display') {
                        if (Array.isArray(data)) {
                            var pdos=[];
                            for (var i = 0; i < data.length; i++) {
                                if (data[i] != null) {
                                    pdoPair = data[i].split(/:\s+/);
                                    pdos.push( jQuery("<a/>", {
                                        href: "${ctxpath}/orders/order.action?view=&productOrder=" + pdoPair[0],
                                        class: "noWrap",
                                        text: pdoPair[0],
                                    })[0].outerHTML);
                                }
                            }
                            return pdos.join(", ");
                        }
                    }
                    return data;
                }
                enableDefaultPagingOptions();
                oTable = $j('#submissionSamples').dataTable({
                    "bDeferRender": true,
                    "oLanguage": {
                        "sInfo": "Displaying _START_ to _END_ of _TOTAL_ submission entries",
                        "sInfoFiltered": "(filtered from _MAX_)",
                        "sProcessing": "&nbsp;<img src='${ctxpath}/images/spinner.gif'>&nbsp;Please wait. Gathering data from Mercury, Bass, and Picard. This may take a few minutes."
                    },
                    "oTableTools": ttExportDefines,
                    "bStateSave": true,
                    "bProcessing": true,
                    "bInfo": true,
                    "sDom": "r<'#filtering.accordion'<'row-fluid autoWidth'<'span12' <'columnFilter'i>><'row-fluid'<'span12'f>>>> <'row-fluid evenHeight'<'span6' l><'span6'T>>t   <'row-fluid'<'span6' l><'span6'p>>",
                    "sAjaxSource": '${ctxpath}/projects/project.action',
                    "fnServerData": function (sSource, aoData, fnCallback, oSettings) {
                        aoData.push({"name": "researchProject", "value": "${researchProject}"});
                        aoData.push({"name": "${event}", "value": "${event}"});

                        oSettings.jqXHR = $j.ajax({
                            "method": 'POST',
                            "dataType": 'json',
                            "url": sSource,
                            "data": aoData,
                            "success": fnCallback,
                            "complete": function (jqXHR) {
                                buildMessage(jqXHR);
                            }
                        });
                    },

                    //needed when null value is returned in JSON
                    "aoColumnDefs": [
                        {"sDefaultContent": "", "aTargets": ["_all"]},
                        {"bSearchable": true, "aTargets": ["_all"]}
                    ],
                    "aoColumns": [
                        {"mData": "<%=SubmissionField.SUBMISSION_TUPLE%>","asSorting": ["desc", "asc"], "mRender": renderCheckbox},
                        {"mData": "<%=SubmissionField.SAMPLE_NAME%>"},
                        {"mData": "<%=SubmissionField.SUBMISSION_SITE%>", "sClass": "ellipsis"},
                        {"mData": "<%=SubmissionField.LIBRARY_DESCRIPTOR%>"},
                        {"mData": "<%=SubmissionField.DATA_TYPE %>"},
                        {"mData": "<%=SubmissionField.PRODUCT_ORDERS %>", "mRender": displayPdoList},
                        {"mData": "<%=SubmissionField.AGGREGATION_PROJECT %>"},
                        {"mData": "<%=SubmissionField.BIO_PROJECT%>"},
                        {"mData": "<%=SubmissionField.VERSION %>"},
                        {"mData": "<%=SubmissionField.QUALITY_METRIC %>"},
                        {"mData": "<%=SubmissionField.CONTAMINATION_STRING %>",
                            "sType": "numeric",
                            "mRender": function (data, type) {
                                if (type === 'sort') {
                                    return data.replace("%", "");
                                }
                                return data;
                            }
                        },
                        {"mData": "<%=SubmissionField.FINGERPRINT_LOD_MIN %>"},
                        {"mData": "<%=SubmissionField.FINGERPRINT_LOD_MAX %>"},
                        {"mData": "<%=SubmissionField.LANES_IN_AGGREGATION %>"},
                        {"mData": "<%=SubmissionField.SUBMITTED_VERSION %>",
                            "mRender": function (data, type, row) {
                                if (type === 'display' && data) {
                                    var latestVersion = row.<%=SubmissionField.VERSION%>;
                                    if (latestVersion > data) {
                                        return createPopover(data+"*", "Submitted version: "+data, "A newer version is available: " + latestVersion);
                                    }else {
                                        return data;
                                    }
                                }
                                return data;
                            }
                        },
                        {
                            "mData": "<%=SubmissionField.SUBMITTED_STATUS %>",
                            "mRender": function (data, type, row) {
                                if (type === 'display') {
                                    var errors = row.<%=SubmissionField.SUBMITTED_ERRORS%>;
                                    if (errors) {
                                        var popoverText = data + " (errors)";
                                        return createPopover(popoverText, data, errors.join("<br/>"));
                                    }
                                }
                                return data;
                            }
                        },
                        {"mData": "<%=SubmissionField.STATUS_DATE %>", "sClass": "noWrap"}
                    ], "fnInitComplete": function () {
                        function updateSearchText() {
                            var currentFullTextSearch = oTable.fnSettings().oPreviousSearch.sSearch;
                            if (currentFullTextSearch !== undefined) {
                                var defaultText = "any text";
                                var matchContent = defaultText;
                                if (!isBlank(currentFullTextSearch)) {
                                    matchContent = currentFullTextSearch;
                                }
                                var textJQuery= jQuery("<span></span>", {text:matchContent});
                                textColor = "";
                                if (matchContent !== defaultText){
                                    textColor = "red";
                                }
                                textJQuery.css("color", textColor);
                                $j(".dtFilters").html("<b>Search text matches</b>: " + textJQuery[0].outerHTML);
                            }
                        }

                        // recheck previously checked values. Stripes can't do this itself since datatables is rendered later.
                        var selectedSubmissions = ${actionBean.selectedSubmissionTuples};
                        if (selectedSubmissions != undefined) {
                            for (var i = 0; i<selectedSubmissions.length; i++) {
                                $j("input[value='" + JSON.stringify(selectedSubmissions[i]) + "']").attr('checked','checked');
                            }
                        }

                        updateSearchText();
                        $j(findFilterTextInput(oTable).on("change init", updateSearchText));

                        $j(".submissionControls").show();
                        $j(".accordion").show();
                    },
                    "fnDrawCallback": function () {
                        $j(".ui-accordion-content").css('overflow', 'visible');
                        $j('.shiftCheckbox').enableCheckboxRangeSelection();

                        if ($("#columnFilter_filteringText").length === 0) {
                            filteringDiv = jQuery("<div></div>", {
                                "id": "columnFilter_filteringText",
                                "style": "padding-left:2em;"
                            }).prependTo($j("#filtering"));
                            $j(filteringDiv).prepend(jQuery("<div class='headerText'></div>"));
                            $j(filteringDiv).prepend(jQuery("<div class='dtFilters ellipsis'></div>"));
                        }

                        $j(".submission-status-tooltip").popover({
                            trigger: "hover",
                            html: "true",
                            "data-container": "body",
                            "data-toggle": "popover"
                        });
                        if ($j("#submissionSamples").height() > $j(window).height()) {
                            $j("#bottomSubmitButton").show();
                        } else {
                            $j("#bottomSubmitButton").hide();
                        }
                    }
                });
                $j("#filtering").hide();
                $j("#filtering").accordion({
                      "collapsible": true, "heightStyle": "content", 'active': false
                    });

                includeAdvancedFilter(oTable, "#submissionSamples");
                $j('#submissionSamples').one('init', function (event, oSettings, aaData) {
                    <enhance:out escapeXml="false">
                    $j('#submissionSamples').filterColumn("Current Status", ${actionBean.submissionStatusesJson}, {
                        selectedValues: ${actionBean.preselectedStatusesJson},
                        filteringText: "#columnFilter_filteringText .headerText"
                    });
                    </enhance:out>
                });
            }
            });
        });
</script>
</head>


<stripes:form beanclass="${actionBean.class.name}" class="form-horizontal">
    <stripes:hidden name="submitString"/>
    <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
    <div class="submissionControls">
        <div class="control-group">
            <stripes:label for="bioProject" class="control-label label-form">Choose a Study *</stripes:label>

            <div class="controls">
                <stripes:text id="bioProject" name="bioProjectTokenInput.listOfKeys"/>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="submissionDatatype"
                           class="control-label label-form">Choose a Library *</stripes:label>

            <div class="controls">
                <stripes:select id="submissionDatatype" name="selectedSubmissionLibraryDescriptor">
                    <stripes:option value="">Choose...</stripes:option>
                    <stripes:options-collection label="description" value="name"
                                                collection="${actionBean.submissionLibraryDescriptors}"/>
                </stripes:select>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="selectedSubmissionRepository"
                           class="control-label label-form">Choose a Site *</stripes:label>
            <div class="controls">
                <stripes:select id="selectedSubmissionRepository" name="selectedSubmissionRepository">
                    <stripes:option value="">Choose...</stripes:option>
                    <stripes:options-collection label="description" value="name"
                                                collection="${actionBean.activeRepositories}"/>
                </stripes:select>
            </div>
        </div>
    </div>
    <div class="submissionControls">
        <stripes:submit name="<%=ResearchProjectActionBean.POST_SUBMISSIONS_ACTION%>"
                        value="Post Selected Submissions" class="btn submissionControls"
                        disabled="${!actionBean.validateViewOrPostSubmissions(true)}"/>
        <a href="javascript:void(0)" id="PasteBarcodesListDisabled"
           title="Select samples using a pasted-in list of values.">Choose via list of samples...</a>
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
            <th class="columnBioSample">BioSample</th>
            <th class="columnRepository">Repository</th>
            <th class="columnLibraryDescriptor">Library</th>
            <th class="columnDataType">Data Type</th>
            <th class="columnPDOs">PDOs</th>
            <th class="columnAggregationProject">Agg. Project</th>
            <th class="columnBioProject">Study</th>
            <th class="columnVersion">Version</th>
            <th class="columnQualityMetric">Quality Metric</th>
            <th class="columnContamination">Contam.</th>
            <th title="Fingerprint LOD (min)" class="columnFingerprint">LOD Min</th>
            <th title="Fingerprint LOD (max)" class="columnFingerprint">LOD Max</th>
            <!-- add # lanes, # lanes blacklisted, notes -->
            <th class="columnLanesInAggregation">Lanes in Agg.</th>
            <th class="columnSubmittedVersion">Submitted Version</th>
            <th class="columnSubmissionStatus">Current Status</th>
            <th class="columnSubmissionStatusDate">Status Date</th>

        </tr>
        </thead>
    </table>
    <stripes:submit name="<%=ResearchProjectActionBean.POST_SUBMISSIONS_ACTION%>"
                    id="bottomSubmitButton" value="Post Selected Submissions" class="btn submissionControls"
                    disabled="${!actionBean.validateViewOrPostSubmissions(true)}" style="display:none;"/>

</stripes:form>
    <%--GPLIM-5660 This div should be removed in a future release.--%>
    <div id="listOfBarcodesDisabledForm" class="ui-dialog-content ui-widget-content" style="display:none;">
    <p style="font-weight: bold">To choose from a list of samples, paste the sample list into the text filter:</p>
        <div style="margin-left: 2em; border-style: dashed; border-width: 1px; padding: 5px; border-color: grey;">
            <img src="${ctxpath}/images/deprecate/filter-any.png" alt="">
        </div>
        <br/>
        <p style="font-weight: bold">and click the "select all" checkbox at the top of the table:</p>
    <div style="margin-left: 2em; border-style: dashed; border-width: 1px; padding: 5px; border-color: grey;">
            <img src="${ctxpath}/images/deprecate/check-all.png" alt="">
        </div>
</stripes:layout-definition>
