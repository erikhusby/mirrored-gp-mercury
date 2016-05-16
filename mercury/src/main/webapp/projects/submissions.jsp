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

        .control-group select {
            width: auto;
        }
        .ui-accordion-content {
            overflow: visible;
        }
        span.submission-status-tooltip{
            position:relative;
            padding: 12px;
            left: -12px;
        }

    </style>
    <link rel="stylesheet" href="${ctxpath}/resources/scripts/chosen_v1.5.1/chosen.min.css">
    <script type="text/javascript" src="${ctxpath}/resources/scripts/chosen_v1.5.1/chosen.jquery.min.js"></script>
    <script type="text/javascript" src="${ctxpath}/resources/scripts/dataTables-filterColumn.js"></script>
    <script type="text/javascript">
        function formatInput(item) {
            var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
            return "<li>" + item.dropdownItem + extraCount + '</li>';
        }

        function hasSubmissionSamples() {
            return $j('#submissionSamples tbody tr td').not('.dataTables_empty').length == 0;
        }

        function initializeBarcodeEntryDialog() {
            dialog = $j("#ListOfBarcodesForm").dialog({
                autoOpen: false,
                height: 400,
                width: 250,
                modal: false,
                buttons: {
                    "ApplyBarcodesButton": {
                        id: "applyBarcodes",
                        text: "Apply barcodes",
                        click: applyBarcodes
                    },
                    Cancel: function () {
                        dialog.dialog("close");
                    }
                }
            });

            $j("#PasteBarcodesList").on("click", function () {
                if (!hasSubmissionSamples()) {
                    clearBarcodesDialog();
                    dialog.dialog("open");
                }
            });

            hideBarcodeEntryDialog();
        }

        function addAmbiguousEntryError(barcode) {
            // todo avoid showing duplicate strings
            $j('#AmbiguousEntries tbody').append(
                    "<tr><td>" + barcode + "</td></tr>"
            );
        }


        function addBarcodeNotFoundError(barcode) {
            // todo avoid showing duplicate strings
            $j('#NoBucketEntries tbody').append(
                    "<tr><td>" + barcode + "</td></tr>"
            );
        }

        function showBarcodeErrors() {
            $j('#BarcodeErrors').show();
        }

        function showNoEntriesErrors() {
            showBarcodeErrors();
            $j('#NoSamplesErrors').show();
        }

        function showAmbiguousEntryErrors() {
            showBarcodeErrors();
            $j('#AmbiguousEntriesErrors').show();
        }

        function clearBarcodesDialogErrors() {
            $j('#NoEntries tbody tr').remove();
            $j('#AmbiguousEntries tbody tr').remove();

            $j('#NoSamplesErrors').hide();
            $j('#AmbiguousEntriesErrors').hide();
        }

        function clearBarcodesDialog() {
            $j('#barcodes').val('');
            $j('#BarcodeErrors').hide();
            clearBarcodesDialogErrors();
        }

        function uncheckedInputByValue(barcode) {
            return 'input[value="' + barcode + '"]';
        }

        function applyBarcodes() {
            clearBarcodesDialogErrors();

            var hasBarcodes = barcodes.value.trim().length > 0;
            var splitBarcodes = barcodes.value.trim().split(/\s+/);
            var hasAmbiguousEntryErrors = false;
            var hasBarcodesNotFoundErrors = false;

            if (!hasBarcodes) {
                alert("No barcodes were entered.");
                return;
            }

            for (var i = 0; i < splitBarcodes.length; i++) {
                var barcodeInputSelector = uncheckedInputByValue(splitBarcodes[i]);
                var numMatchingRows = $j(barcodeInputSelector).length;

                if (numMatchingRows > 1) {
                    hasAmbiguousEntryErrors = true;
                    addAmbiguousEntryError(barcode);
                }
                else if (numMatchingRows == 0) {
                    hasBarcodesNotFoundErrors = true;
                    addBarcodeNotFoundError(barcode);
                }
            }

            if (!hasAmbiguousEntryErrors && !hasBarcodesNotFoundErrors) {
                for (var i = 0; i < splitBarcodes.length; i++) {
                    var barcodeInputSelector = uncheckedInputByValue(splitBarcodes[i]);
                    var numMatchingRows = $j(barcodeInputSelector).length;

                    if (numMatchingRows == 1) {
                        // todo event handler for enter
                        var barcodeCheckbox = $j(barcodeInputSelector).not(":checked");
                        barcodeCheckbox.click();
                    }
                }
                dialog.dialog("close");
                addStripesMessage(splitBarcodes.length + " submissions chosen.");
            }
            else {
                if (hasAmbiguousEntryErrors) {
                    showAmbiguousEntryErrors();
                }
                if (hasBarcodesNotFoundErrors) {
                    showNoEntriesErrors();
                }
            }
        }

        function addStripesMessageDiv(alertType, fieldSelector) {
            var alertClass = 'alert-' + alertType;
            var messageBox = $j(document.createElement("div"));
            messageBox.css({"margin-left": "20%", "margin-right": "20%"});
            messageBox.addClass("alert").addClass(alertClass);
            if (fieldSelector != undefined) {
                $j(fieldSelector).addClass(alertType);
            }
            messageBox.append('<button type="button" class="close" data-dismiss="alert">&times;</button>')
            messageBox.append('<ul></ul>');

            $j('.page-body').before(messageBox);
            return messageBox;
        }

        function addStripesMessage(message, type, fieldSelector) {
            if (type == undefined) {
                type = "success";
            }
            var messageBoxJquery = $j("div.alert-" + type);
            if (messageBoxJquery.length == 0) {
                messageBoxJquery = addStripesMessageDiv(type, fieldSelector);
            }
            messageBoxJquery.find("ul").append("<li>" + message + "</li>");
        }

        function hideBarcodeEntryDialog() {
            $j('#BarcodeErrors').hide();
            $j('#NoEntriesErrors').hide();
            $j('#AmbiguousEntriesErrors').hide();
        }

        $j(document).ready(function () {
            initializeBarcodeEntryDialog();
            $j("#bioProject").tokenInput(
                    "${ctxpath}/projects/project.action?bioProjectAutocomplete=", {
                        hintText: "Type a Study Name",
                        prePopulate: ${actionBean.ensureStringResult(actionBean.bioProjectTokenInput.completeData)},
                        tokenDelimiter: "${actionBean.bioProjectTokenInput.separator}",
                        preventDuplicates: true,
                        tokenLimit: 1,
                        minChars: 2,
                        resultsFormatter: formatInput,
                        autoSelectFirstResult: true
                    }
            );


            function createPopover(data, title, errors) {
                var span = jQuery("<span/>", {
                    "class":"submission-status-tooltip popover-dismiss",
                    "title":title,
                    "data-content":errors,
                    "data-sort":data,
                    "data-toggle":"popover",
                    "data-placement":"top",
                    "text": data
                });
            }

            var oTable;
            $j("${submissionsTabSelector}").click(function () {
            if (oTable == undefined) {
                function renderCheckbox(data, type, row) {
                    if (type === 'display') {
                        var status = row.<%=SubmissionField.SUBMITTED_STATUS %>;
                        var tagAttributes = {};
                        if (status.length === 0) {
                            tagAttributes = {
                                "name": "selectedSubmissionSamples",
                                "value": data,
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
                    "bDeferRender": true,
                    "oLanguage": {
                        "sInfo": "_TOTAL_ submissions displayed.",
                        "sProcessing": "&nbsp;<img src='${ctxpath}/images/spinner.gif'>&nbsp;Please wait. Gathering data from Mercury, Bass, and Picard. This may take a few minutes."
                    },
                    "oTableTools": ttExportDefines,
                    "bStateSave": true,
                    "bProcessing": true,
                    "bInfo": true,
                    "sDom": "r<'#filtering.accordion'<'row-fluid'<'span12'<'columnFilter'>><'row-fluid'<'span8'f><'span4' iT>'span2'>>>t<'row-fluid'<'span6'><'span6'p>>",
                    "sAjaxSource": '${ctxpath}/projects/project.action',
                    "fnServerData": function (sSource, aoData, fnCallback, oSettings) {
                        aoData.push({"name": "researchProject", "value": "${researchProject}"});
                        aoData.push({"name": "${event}", "value": "${event}"});

                        oSettings.jqXHR = $j.ajax({
                            "method": 'POST',
                            "dataType": 'json',
                            "url": sSource,
                            "data": aoData,
                            "success": fnCallback
                        });
                    },

                    //needed when null value is returned in JSON
                    "aoColumnDefs": [
                        {"sDefaultContent": "", "aTargets": ["_all"]},
                        {"bSearchable": true, "aTargets": ["_all"]}
                    ],
                    "aoColumns": [
                        {"mData": "<%=SubmissionField.SAMPLE_NAME%>", "mRender": renderCheckbox},
                        {"mData": "<%=SubmissionField.SAMPLE_NAME%>"},
                        {"mData": "<%=SubmissionField.SUBMISSION_SITE%>"},
                        {"mData": "<%=SubmissionField.LIBRARY_DESCRIPTOR%>"},
                        {"mData": "<%=SubmissionField.DATA_TYPE %>"},
                        { "mData": "<%=SubmissionField.PRODUCT_ORDERS %>", "sWidth": "140px", "sClass": "ellipsis",
                            "mRender": displayDataList },
                        {"mData": "<%=SubmissionField.AGGREGATION_PROJECT %>"},
                        {"mData": "<%=SubmissionField.BIO_PROJECT%>"},
                        {"mData": "<%=SubmissionField.FILE_TYPE %>"},
                        {"mData": "<%=SubmissionField.VERSION %>"},
                        {"mData": "<%=SubmissionField.QUALITY_METRIC %>"},
                        {"mData": "<%=SubmissionField.CONTAMINATION_STRING %>"},
                        {"mData": "<%=SubmissionField.FINGERPRINT_LOD %>"},
                        {"mData": "<%=SubmissionField.LANES_IN_AGGREGATION %>"},
                        {"mData": "<%=SubmissionField.SUBMITTED_VERSION %>",
                            "mRender": function (data, type, row) {
                                if (type === 'display') {
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
                                        return createPopover(data, data, errors);
                                    }
                                }
                                return data;
                            }
                        },
                        {"mData": "<%=SubmissionField.STATUS_DATE %>"}
                    ], "fnInitComplete": function () {
                        function updateSearchText() {
                            var currentFullTextSearch = oTable.fnSettings().oPreviousSearch.sSearch;
                            if (currentFullTextSearch !== undefined) {
                                var matchContent = "any text";
                                if (!isBlank(currentFullTextSearch)) {
                                    matchContent = currentFullTextSearch;
                                }
                                $j(".dtFilters").html("<b>Search text matches</b>: " + matchContent);
                            }
                        }

                        updateSearchText();
                        $j(findFilterTextInput(oTable).on("change init", updateSearchText));
                    },
                    "fnDrawCallback": function () {
                        $j(".submissionControls").show();
                        $j(".accordion").show();
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
                    $j('#submissionSamples').filterColumn("Current Status", ${actionBean.submissionStatusesJson}, {
                        selectedValues: ${actionBean.preselectedStatusesJson},
                        filteringText: "#columnFilter_filteringText .headerText"
                    });
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
    </div>
    <div class="submissionControls">
        <stripes:submit name="<%=ResearchProjectActionBean.POST_SUBMISSIONS_ACTION%>"
                        value="Post Selected Submissions" class="btn submissionControls"
                        disabled="${!actionBean.submissionAllowed}"/>
        <a href="javascript:void(0)" id="PasteBarcodesList"
           title="Use a pasted-in list of tube barcodes to select samples">Choose via list of barcodes...</a>
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
            <th class="columnBioProject">Study</th>
            <th class="columnFileType">File Type</th>
            <th class="columnVersion">Version</th>
            <th class="columnQualityMetric">Quality Metric</th>
            <th class="columnContamination">Contam.</th>
            <th class="columnFingerprint">Fingerprint</th>
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
                    disabled="${!actionBean.submissionAllowed}" style="display:none;"/>

</stripes:form>
    <div id="ListOfBarcodesForm">
        <form>
            <div class="control-group">
                <label for="barcodes" class="control-label">Enter BioSample IDs, one per line</label>
                <textarea name="barcodes" id="barcodes" class="defaultText" cols="12" rows="5"></textarea>
            </div>
            <div id="BarcodeErrors">
                <p>We're sorry, but Mercury could not automatically choose submission entries
                    because of the following errors.</p>
                <div id="NoSamplesErrors">
                    <table id="NoSamples">
                        <thead>
                        <tr>
                            <th>No submission entries</th>
                        </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
                <div id="AmbiguousEntriesErrors">
                    <table id="AmbiguousEntries">
                        <thead>
                        <tr>
                            <th>Ambiguous submission entries</th>
                        </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </form>
    </div>
</stripes:layout-definition>
