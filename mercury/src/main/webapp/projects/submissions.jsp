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
        .columnBioSample{ width: 6em; }
        .columnDataType { width: 4em; }
        .columnPDOs { width: 11em; }
        .columnAggregationProject { width: 5em; }
        .columnRepository { max-width: 30em; }
        .columnLibraryDescriptor { width: 11em; }
        .columnFileType { width: 5em; }
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
            return $j('#submissionSamples tbody tr td').not('.dataTables_empty').length > 0;
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
                        text: "Select Samples",
                        click: applyBarcodes
                    },
                    Cancel: function () {
                        dialog.dialog("close");
                    }
                }
            });

            $j("#PasteBarcodesList").on("click", function () {
                if (hasSubmissionSamples()) {
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
            $j('#BarcodeErrors tbody').append(
                    "<tr><td>" + barcode + "</td></tr>"
            );
        }

        function showBarcodeErrors() {
            $j('#BarcodeErrors').show();
        }

        function showNoEntriesErrors() {
            showBarcodeErrors();
            $j('#NoEntriesErrors').show();
        }

        function showAmbiguousEntryErrors() {
            showBarcodeErrors();
            $j('#AmbiguousEntriesErrors').show();
        }

        function clearBarcodesDialogErrors() {
            $j('#NoEntries tbody tr').remove();
            $j('#AmbiguousEntries tbody tr').remove();

            $j('#NoEntriesErrors').hide();
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
                var barcode = splitBarcodes[i];
                var barcodeInputSelector = uncheckedInputByValue(barcode);
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
            var messageBox = jQuery("<div></div>",{
                "class": "alert alertClass center".replace("alertClass", alertClass),
                "style": "width: 50em;"
            });
            var button = jQuery("<button type='button' class='close' data-dismiss='alert'>&times;</button>");
            messageBox.append(button);
            messageBox.append('<ul></ul>');
            if (fieldSelector != undefined) {
                $j(fieldSelector).empty();
                $j(fieldSelector).append(messageBox);
            } else {
                $j('.page-body').before(messageBox);
            }
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
                function buildMessage(jqXHR) {
                    var responseText = jqXHR.responseJSON;
                    if (responseText.stripesMessages) {
                        outerDiv = jQuery("<div></div>", {
                            "id": "stripesMessageOuter",
                            "style": "position: relative;z-index:5",
                            "class": "center"
                        });
                        $j("#submissionSamples_wrapper").prepend(outerDiv);
                        $j("#submissionSamples_processing").hide();
                        addStripesMessage(responseText.stripesMessages, responseText.messageType, "#stripesMessageOuter");
                    }
                }

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
                                pdoPair = data[i].split(/:\s+/);
                                pdos.push( jQuery("<a/>", {
                                    href: "${ctxpath}/orders/order.action?view=&productOrder=" + pdoPair[0],
                                    class: "noWrap",
                                    text: pdoPair[0],
                                    title: pdoPair[1]
                                })[0].outerHTML);
                            }
                            return pdos.join(", ");
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
                        {"mData": "<%=SubmissionField.SAMPLE_NAME%>","asSorting": ["desc", "asc"], "mRender": renderCheckbox},
                        {"mData": "<%=SubmissionField.SAMPLE_NAME%>"},
                        {"mData": "<%=SubmissionField.SUBMISSION_SITE%>", "sClass": "ellipsis"},
                        {"mData": "<%=SubmissionField.LIBRARY_DESCRIPTOR%>"},
                        {"mData": "<%=SubmissionField.DATA_TYPE %>"},
                        {"mData": "<%=SubmissionField.PRODUCT_ORDERS %>", "mRender": displayPdoList},
                        {"mData": "<%=SubmissionField.AGGREGATION_PROJECT %>"},
                        {"mData": "<%=SubmissionField.BIO_PROJECT%>"},
                        {"mData": "<%=SubmissionField.FILE_TYPE %>"},
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
                           class="control-label label-form">Choose a Library *</stripes:label>

            <div class="controls">
                <stripes:select id="submissionType" name="selectedSubmissionLibraryDescriptor">
                    <option>Choose...</option>
                    <stripes:options-collection label="description" value="name"
                                                collection="${actionBean.submissionLibraryDescriptors}"/></stripes:select>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="submissionRepository"
                           class="control-label label-form">Choose a Site *</stripes:label>
            <div class="controls">
                <stripes:select id="submissionRepository" name="selectedSubmissionRepository">
                    <option>Choose...</option>
                    <stripes:options-collection label="description" value="description"
                                                collection="${actionBean.activeRepositories}"/>
                </stripes:select>
            </div>
        </div>
    </div>
    <div class="submissionControls">
        <stripes:submit name="<%=ResearchProjectActionBean.POST_SUBMISSIONS_ACTION%>"
                        value="Post Selected Submissions" class="btn submissionControls"
                        disabled="${!actionBean.validateViewOrPostSubmissions(true)}"/>
        <a href="javascript:void(0)" id="PasteBarcodesList"
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
                    disabled="${!actionBean.validateViewOrPostSubmissions(true)}" style="display:none;"/>

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
                <div id="NoEntriesErrors">
                    <table id="NoEntries">
                        <thead>
                        <tr>
                            <th>No submission entries match</th>
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
