<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>
<c:set var="addToBatchText" value="Enter if you are adding to an existing batch"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" dataTablesVersion="1.10" withColVis="true"
                       sectionTitle="Select Bucket">
<stripes:layout-component name="extraHead">
    <style type="text/css">
        .editable {
            white-space: nowrap;
        }

        .form-horizontal .control-group {
            margin-bottom: 10px;
        }

        td.nowrap {
            white-space: nowrap;
        }

        .filtering-label {
            font-weight: bold;
            padding: 5px;
        }

        .filter-header-item {
            color: #0088CC;
            /*padding: 5px;*/
        }

        list .filtering-label img {
            height: 10px;
            width: 10px;
            cursor: pointer;
            padding-bottom: 2px;
            padding-right: 2px;
        }
        ul.list li {
            display: inline;
            list-style: none;
            margin: 0;
            padding: 0;
        }

        li.filter-header-item  {
            display: inline;
            list-style-image:none;''
        }

        ul.filtered-items-header li:nth-child(2):before {
            content: "Filtering columns: ";
            font-weight: bold;
            padding-left: 1.5em;
        }

        ul.list li:after {
            content: ", ";
            color: #aaa;
        }

        ul.list li:last-child:after {
            content: "";
        }

        a.dt-button.buttons-columnVisibility span {
            padding-left: 12px;
        }

        a.dt-button.buttons-columnVisibility.active, a.dt-button.buttons-columnVisibility {
            position: relative;
            width: auto;
            /**width: 230px;*/
            text-align: left;
            *zoom: expression(
        this.runtimeStyle['zoom'] = '1',
        this.insertBefore(document.createElement("div"),
        this.childNodes[0]).className="before",
        this.appendChild(document.createElement("div")).className="after");
        }

        a.dt-button.buttons-columnVisibility:before, a.dt-button.buttons-columnVisibility.active:after {
            display: block;
            position: absolute;
            top: 1em;
            margin-top: -6px;
            margin-left: -6px;
            width: 16px;
            height: 16px;
            box-sizing: border-box;
        }

        a.dt-button.buttons-columnVisibility:before {
            content: ' ';
            border: 1px solid black;
            border-radius: 3px;
        }

        a.dt-button.buttons-columnVisibility.active:after {
            font: 0.9em Arial;
            content: '\2714';
            text-align: center;
        }

        * a.dt-button.buttons-columnVisibility .before, * a.dt-button.buttons-columnVisibility.active .after, * a.dt-button.buttons-columnVisibility.active:hover .after {
            position: absolute;
            margin: 6px 0px 0px -6px;
            width: 16px;
            height: 16px;
            background-image: url("${ctxpath}/resources/css/images/checkbox.png");
            background-position: 0px 0px;
        }

        * a.dt-button.buttons-columnVisibility .before {
            background-position: 0px 0px;
        }

        * a.dt-button.buttons-columnVisibility.active .before {
            background-position: 0px 16px;
        }

        * a.dt-button.buttons-columnVisibility.active:hover .before {
            background-position: 16px 0px;
        }

        .dataTables_length, .dataTables_info, .dataTables_paginate {
            padding-top: 10px;
            padding-right: 10px;
        }

        .info-header-display {
            overflow: hidden;
            padding-top: 2em;
        }

        .info-header-display .dt-buttons {
            display: initial;
        }

        .dataTables_processing {
            width: 400px;
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

        .em-turtle {
            <%--background-image: url("${ctxpath}/images/turtle.png");--%>
            <%--background-size: 16px 16px;--%>
            <%--background-repeat: no-repeat;--%>
        }
    </style>
    <%--<script src="${ctxpath}/resources/scripts/jquery.pasteSelect.js" type="text/javascript"></script>--%>
    <script src="${ctxpath}/resources/scripts/jquery.jeditable.mini.js" type="text/javascript"></script>

    <script src="${ctxpath}/resources/scripts/hSpinner.js"></script>
    <script src="${ctxpath}/resources/scripts/columnSelect.js"></script>
    <script src="${ctxpath}/resources/scripts/chosen_v1.6.2/chosen.jquery.min.js" type="text/javascript"></script>
    <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/scripts/chosen_v1.6.2/chosen.min.css"/>

    <script type="text/javascript">
        function submitBucket() {
            $j("#spinner").show();
            $j('#bucketForm').append('<input type="hidden" name="viewBucket"  />');
            $j('#bucketForm').submit();
        }

        function isBucketEmpty() {
            return $j('#bucketEntryView tbody tr td').not('.dataTables_empty').length == 0;
        }

        // Enable or disable the form buttons when bucket entries are selected or deselected.
        function showOrHideControls() {
            var inputControls = $j(".actionControls").find("input.bucket-control");
            if ($j("input[name='selectedEntryIds']:checked").length == 0) {
                inputControls.attr("disabled", "disabled");
                $j("#chooseNext.bucket-control").hide();
            } else {
                inputControls.removeAttr("disabled");
                $j("#chooseNext.bucket-control").show();
            }
        }

        function findWorkflowsFromSelectedRows() {
            var selectedWorkflows = [];
            var columnIndex = $j("bucketEntryView").columnIndexOfHeader("Workflow");

            $j("#bucketEntryView tr td input[name='selectedEntryIds']:checked").closest("tr").each(function () {
                var workflow = $j(this).find("td:nth-child(" + columnIndex + ")").text().trim();
                if ($j.inArray(workflow, selectedWorkflows) == -1) {
                    selectedWorkflows.push(workflow);
                }
            });
            return selectedWorkflows;
        }

        function setupBucketEvents() {
            var CONFIRM_TEXT = "Confirm";

            $j("input[name='addToBatch'],input[name='createBatch'],input[name='removeFromBucket']").on("click dblclick", function (event) {

                // Clear any errors that may be displayed.
                $j(".alert-error, .error").each(function () {
                    $j(this).removeClass("alert-error");
                    $j(this).removeClass("error");
                });
                if ($j(".alert").length > 0) {
                    $j(".alert").remove();
                }

                var clickedButton = $j(event.target);
                if (clickedButton == undefined) {
                    return;
                }

                // Show form input fields based on which button was clicked.
                var clickedButtonEvent = clickedButton.attr('name');
                if (clickedButtonEvent == "addToBatch") {
                    $j(".batch-append").slideDown(200);
                } else if (clickedButtonEvent == "createBatch") {
                    $j(".batch-create").slideDown(200);

                    // Test that a workflow is not already selected.
                    if ($j("#workflowSelect option:selected").attr('value') == "") {
                        // automatically select a workflow, if you can.
                        var selectedWorkflows = findWorkflowsFromSelectedRows();
                        if (selectedWorkflows.length == 1) {
                            $j("#workflowSelect option").filter(function () {
                                return $(this).text() == selectedWorkflows[0];
                            }).prop('selected', true);
                        } else {
                            var workflowErrorMessage = "Workflow could not be determined automatically, please choose one manually.";
                            stripesMessage.add(workflowErrorMessage, "error", "#workflowSelect");
                            event.preventDefault();
                            return;
                        }
                    }
                }

                // if the button starts with CONFIRM_TEXT, we know the user is on the confirmation page now, and the
                // form should submit as usual. Otherwise we prepend CONFIRM_TEXT to the button name so the next time
                // it is clicked it will submit.
                var clickedButtonName = clickedButton.attr("value");
                if (clickedButtonName.startsWith(CONFIRM_TEXT)) {
                    return;
                }
                clickedButton.attr('value', CONFIRM_TEXT + " " + clickedButtonName);

                // The page is now being set up as a confirmation screen.
                event.preventDefault();

                // The bucket-control class is used to indicate which controls are visible when creating, updating
                // or deleting bucket entries. Hide the elements who's value does not start with CONFIRM_TEXT.
                $j("#bucketEntryForm .bucket-control").filter(":not([value^='" + CONFIRM_TEXT + "'])").hide();

                // Show the cancel button so the user has a way out if they change their mind.
                $j("#cancel").show();

                // hide dataTables filter
//                $j('.column-filter').closest('.row-fluid').hide();

                // hide pdo editable stuff
                $j('#editable-text').hide();
                $(".editable").removeClass("editable");
                $(".icon-pencil").removeClass("icon-pencil");

                // Hide the unchecked rows so the user knows what will be included in the request.
                $j("input[type=checkbox]").not(":checked").closest("tbody tr").hide();
            });
        }

        $j(document).ready(function () {
//            $j("#bucketEntryView").pasteSelect({
//                columnNames: ["Vessel Name", "Sample Name"],
//                noun: "Bucket Entry",
//                pluralNoun: "Bucket Entries"
//            });
            setupBucketEvents();

            // Hide the input form when there are no bucket entries.
            if (isBucketEmpty()) {
                $j("bucketEntryForm").hide();
                $j(".actionControls").hide();
            } else {
                $j(".actionControls").show();
            }

            var columnsEditable=false;
            <security:authorizeBlock roles="<%= roles(LabManager, PDM, PM, Developer) %>">
                columnsEditable=true;
            </security:authorizeBlock>

                var editablePdo = function()  {
                if (columnsEditable) {
                    var oTable = $j('#bucketEntryView').DataTable();
                    $j("td.editable").editable('${ctxpath}/workflow/bucketView.action?changePdo', {
                        'loadurl': '${ctxpath}/workflow/bucketView.action?findPdo',
                        'callback': function (sValue, y) {
                            var jsonValues = $j.parseJSON(sValue);
                            var pdoKeyCellValue='<span class="ellipsis">'+jsonValues.jiraKey+'</span><span style="display: none;" class="icon-pencil"></span>';

                            var aPos = oTable.fnGetPosition(this);
                            oTable.fnUpdate(pdoKeyCellValue, aPos[0] /*row*/, aPos[1]/*column*/);

                            var pdoTitleCellValue = '<div class="ellipsis" style="width: 300px">'+jsonValues.pdoTitle+'</div>';
                            oTable.fnUpdate(pdoTitleCellValue, aPos[0] /*row*/, aPos[1]+1/*column*/);

                            var pdoCreatorCellValue = jsonValues.pdoOwner;
                            oTable.fnUpdate(pdoCreatorCellValue, aPos[0] /*row*/, aPos[1]+2/*column*/);
                        },
                        'submitdata': function (value, settings) {
                            return {
                                "selectedEntryIds": this.parentNode.getAttribute('id'),
                                "column": oTable.fnGetPosition(this)[2],
                                "newPdoValue": $j(this).find(':selected').text()
                            };
                        },
                        'loaddata': function (value, settings) {
                            return {
                                "selectedEntryIds": this.parentNode.getAttribute('id')
                            };
                        },
//                        If you need to debug the generated html you need to ignore onblur events
//                        "onblur" : "ignore",
                        cssclass: "editable",
                        tooltip: 'Click the value in this field to edit',
                        type: "select",
                        indicator : '<img src="${ctxpath}/images/spinner.gif">',
                        submit: 'Save',
                        cancel: 'Cancel',
                        height: "auto",
                        width: "auto"
                    });
                    $j(".icon-pencil").show();
                } else {
                    $j(".icon-pencil").hide();
                    $j(".editable").removeClass("editable")
                }

            };

            var bucketName = $j('#bucketSelect :selected').val();
            var localStorageKey = 'DT_bucketEntryView';
            oTable = $j('#bucketEntryView').DataTable({
                dom: "<'info-header-display'iB><'#filtering'>rtpl",
                saveState: true,
                info:true,
                paging: true,
                lengthMenu:[ [10, 25, 50, 100, 500, -1], [10, 25, 50, 100, 500, "All"] ],
                lengthChange:true,
                pageLength: 100,
                searchDelay: 500,
                deferRender: true,
                renderer: "bootstrap",
                buttons: [{
                    extend: 'colvis',
                    text: "Show or Hide Columns",
                    columns: ':gt(0)'
                }],
                "language": {
                    "info": "Showing _START_ to _END_ of _TOTAL_ bucket entries displayed.",
                    "lengthMenu": "Displaying _MENU_ bucket entries per page"
                    },
                "columns": [
                    {"bSortable": false},
                    {"bSortable": true, "sClass": "nowrap"},
                    {"bSortable": true, "sClass": "nowrap"},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable": true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true, "sType":"date"},
                    {"bSortable": true, "sType": "date"},
                    {"bSortable":true},
                    {"bSortable": true, "sClass": "nowrap"},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true}
                ],
                "aaSorting": [[1,'asc'], [7,'asc']],
                stateSaveCallback: function (settings, data) {
                    var api = new $j.fn.dataTable.Api(settings);
                        for (var index = 0; index < data.columns.length; index++) {
                            var item = data.columns[index];
                            var header = $j(api.data().column(index).header());
                            if (header) {
                                item.headerName = header.text().trim();
                            }
                        }

                        if (bucketName !== '') {
                            var stateData = {
                                "<%= BucketViewActionBean.TABLE_STATE_KEY %>": JSON.stringify(data),
                                "<%= BucketViewActionBean.SELECTED_BUCKET_KEY %>": bucketName
                            };
                            localStorage.setItem(localStorageKey, JSON.stringify(stateData) );
                            $j.ajax({
                                'url': "${ctxpath}/workflow/bucketView.action?<%= BucketViewActionBean.SAVE_SEARCH_DATA %>=",
                                'data': stateData,
                                dataType: 'json',
                                type: 'POST'
                            });
                        }
                },
                "stateLoadCallback": function (settings) {
                    var serverData = '${actionBean.tableState}';
                    if (!serverData){
                        serverData = localStorage.getItem(localStorageKey);
                    }
                    if (serverData) {
                        try {
                            serverData = JSON.parse(serverData);
                        } catch (e) {
                            console.log(e);
                        }
                    }
                    return serverData;
                },
                "fnDrawCallback": editablePdo,
                "initComplete": function (settings) {
                    initColumnSelect(settings, [
                        {"Vessel Name": 'text'},
                        {"Sample Name": 'text'},
                        {"PDO": 'select'},
                        {"PDO Name": 'text'},
                        {"PDO Owner": 'select'},
                        {"Batch Name": 'text'},
                        {"Product": 'select'},
                        {"Add-ons": 'select'},
                        {"Material Type": 'select'},
                        {"Bucket Entry Type": 'select'},
                        {"Rework Reason": 'select'},
                        {"Rework Comment": 'text'},
                        {"Rework User": 'select'},
                        {"Workflow": 'select'}
                    ], "#filtering", "column-filter", "${ctxpath}");

                    $j("#filtering").accordion({
                        "collapsible": true, "heightStyle": "content", 'active': false,
                    });
                    $j("#filtering").css("z-index: 0; display: inline-block; width: 100%;");
                }


            });

            $j('.bucket-checkbox').enableCheckboxRangeSelection({
                checkAllClass:'bucket-checkAll',
                countDisplayClass:'bucket-checkedCount',
                checkboxClass:'bucket-checkbox'});

            $j("#dueDate").datepicker();

            $j(".bucket-checkbox").on("click blur ready", function () {
                showOrHideControls();
            });

            $j("#lcsetText").change(function () {
                var jiraTicketId = $j("#lcsetText").val();
                var projectType = $j("#projectType").val();
                var hasDefaultTextChanged = jiraTicketId != "${addToBatchText}";
                var lcsetErrorTextClasses = "alert alert-error";
                var lcsetTextClasses = "error";

                function clearErrorText() {
                    $j("#lcsetErrorText").hide();
                    $j("#lcsetErrorText").empty();
                    $j("#lcsetErrorText").removeClass(lcsetErrorTextClasses);
                    $j("#lcsetText").removeClass(lcsetTextClasses);
                    $j("[name='addToBatch']").prop('disabled', false);
                }

                if (hasDefaultTextChanged) {
                    $j`.ajax`({
                        url: "${ctxpath}/workflow/bucketView.action?projectTypeMatches=",
                        type: 'GET',
                        data: {
                            'jiraTicketId': jiraTicketId,
                            'projectType': projectType
                        }
                    }).done(function (projectTypeMatches) {

                        if (projectTypeMatches != "true") {
                            $j("#lcsetText").addClass(lcsetTextClasses);
                            $j("#lcsetErrorText").text("Bucket entries can not be added to '" + jiraTicketId + "'. It is not a '" + projectType + "'.");
                            $j("#lcsetErrorText").addClass(lcsetErrorTextClasses);
                            $j("#lcsetErrorText").show();
                            $j("[name='addToBatch']").prop('disabled', true);
                        } else {
                            clearErrorText();
                        }
                    });
                } else if ($j("#lcsetErrorText").is(":visible")){
                    clearErrorText();
                }
            });
            $j("#watchers").tokenInput(
                    "${ctxpath}/workflow/bucketView.action?watchersAutoComplete=", {
                        prePopulate: ${actionBean.ensureStringResult(actionBean.jiraUserTokenInput.completeData)},
                        tokenDelimiter: "${actionBean.jiraUserTokenInput.separator}",
                        preventDuplicates: true,
                        queryParam: 'jiraUserQuery',
                        autoSelectFirstResult: true
                    }
            );
            $j("#spinner").hide();

            // prevent subprevent submit when hitting the return key in an input so ajax validation can happen.
            $j("#bucketEntryForm :input:not(textarea)").keypress(function (event) {
                return event.keyCode != 13;
            });

            var hSpinner = $j("#batchSize").hSpinner();
            $j("#chooseNbutton").click(function () {
                var batchSize = $j("#batchSize").val();
                if (batchSize > 0) {
                    $j("#bucketEntryView").find("input[name='selectedEntryIds']:checked").click();
                    $j("#bucketEntryView").find("input[name='selectedEntryIds']:lt(" + batchSize + ")").click();
//                    $j("input[value='Create Batch']").click();
                }
            })

        });

    </script>
</stripes:layout-component>

<stripes:layout-component name="content">
    <stripes:form style="margin-bottom: 10px" id="bucketForm" beanclass="${actionBean.class}">
        <div class="form-horizontal">
            <div class="control-group">
                <stripes:label for="bucketselect" name="Select Bucket" class="control-label"/>
                <div class="controls">
                    <stripes:select id="bucketSelect" name="selectedBucket" onchange="submitBucket()">
                        <stripes:param name="viewBucket"/>
                        <stripes:option value="">Select a Bucket</stripes:option>
                        <c:forEach items="${actionBean.mapBucketToBucketEntryCount.keySet()}" var="bucketName">
                            <c:set var="bucketCount" value="${actionBean.mapBucketToBucketEntryCount.get(bucketName)}"/>
                            <stripes:option value="${bucketName}"
                                            label="${bucketName} (${bucketCount.bucketEntryCount + bucketCount.reworkEntryCount} vessels)"/>
                        </c:forEach>
                    </stripes:select>
                    <img id="spinner" src="${ctxpath}/images/spinner.gif" style="display: none;" alt=""/>
                </div>
            </div>
        </div>
    </stripes:form>
    <stripes:form beanclass="${actionBean.class.name}" id="bucketEntryForm" class="form-horizontal">
        <div class="form-horizontal">
            <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
            <stripes:hidden name="projectType" id="projectType" value="${actionBean.projectType}"/>
            <div class="control-group batch-create" style="display: none;">
                            <stripes:label for="workflowSelect" name="Select Workflow" class="control-label"/>
                            <div class="controls">
                                <stripes:select id="workflowSelect" name="selectedWorkflow">
                                    <stripes:option value="">Select a Workflow</stripes:option>
                                    <stripes:options-collection collection="${actionBean.possibleWorkflows}"/>
                                </stripes:select>
                            </div>
                        </div>
            <div class="control-group batch-append" style="display: none;">
                            <stripes:label for="lcsetText" name="Batch Name" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="lcsetText" class="defaultText" name="selectedLcset"
                                              title="Enter if you are adding to an existing batch"/>
                                <span id="lcsetErrorText"></span>
                            </div>
                        </div>
            <div class="control-group batch-create" style="display: none;">
                            <stripes:label for="summary" name="Summary" class="control-label"/>
                            <div class="controls">
                                <stripes:text name="summary" class="defaultText"
                                              title="Enter a summary for a new batch ticket" id="summary"
                                              value="${actionBean.summary}"/>
                            </div>
                        </div>

            <div class="control-group batch-create" style="display: none;">
                            <stripes:label for="description" name="Description" class="control-label"/>
                            <div class="controls">
                                <stripes:textarea name="description" class="defaultText"
                                                  title="Enter a description for a new batch ticket"
                                                  id="description" value="${actionBean.description}"/>
                            </div>
                        </div>

            <div class="control-group batch-create" style="display: none;">
                            <stripes:label for="important" name="Important Information"
                                           class="control-label"/>
                            <div class="controls">
                                <stripes:textarea name="important" class="defaultText"
                                                  title="Enter important info for a new batch ticket"
                                                  id="important"
                                                  value="${actionBean.important}"/>
                            </div>
                        </div>

            <div class="control-group batch-create" style="display: none;">
                            <stripes:label for="dueDate" name="Due Date" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="dueDate" name="dueDate" class="defaultText"
                                              title="enter date (MM/dd/yyyy)"
                                              value="${actionBean.dueDate}"
                                              formatPattern="MM/dd/yyyy"><fmt:formatDate
                                        value="${actionBean.dueDate}"
                                        dateStyle="short"/></stripes:text>
                            </div>
                        </div>
            <div class="control-group batch-create batch-append" style="display: none;">
                            <stripes:label for="watchers" name="Watchers" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="watchers" name="jiraUserTokenInput.listOfKeys" class="defaultText"
                                              title="Entery users to add as watchers."/>
                            </div>
                        </div>
                    </div>
        <br/>
        <ul id="editable-text"> <li>If you would like to change the value of a PDO for an item in the bucket, click on the value of the PDO in the table and select the new value.</li> </ul>
        <div class="actionControls" style="margin-bottom: 20px">
            <stripes:submit name="createBatch" value="Create Batch" class="btn bucket-control" disabled="true"/>
            <stripes:submit name="addToBatch" value="Add to Batch" class="btn bucket-control" disabled="true"/>
            <stripes:submit name="removeFromBucket" value="Remove From Bucket" class="btn bucket-control"
                            disabled="true"/>
        <a href="#" id="cancel" onClick="submitBucket()" style="display: none;">Cancel</a>
        <div id="chooseNext" class="bucket-control table-control">Select Next <input value="92" style="width: 3em;" id="batchSize"/>&nbsp;<input
                type="button" id="chooseNbutton" value="Select" class="btn"/>
        </div>
</div>
        <table id="bucketEntryView" class="bucket-checkbox table simple dt-responsive">
            <thead>
            <tr>
                <th width="10" class="bucket-control">
                    <input type="checkbox" class="bucket-checkAll"/>
                    <span id="count" class="bucket-checkedCount"></span>
                </th>
                <th width="60">Vessel Name</th>
                <th width="50">Sample Name</th>
                <th class="em-turtle">Material Type</th>
                <th>PDO</th>
                <th>PDO Name</th>
                <th>PDO Owner</th>
                <th style="min-width: 50px" class="em-turtle">Batch Name</th>
                <th class="em-turtle">Workflow</th>
                <th>Product</th>
                <th>Add-ons</th>
                <th width="100" class="em-turtle">Receipt Date</th>
                <th width="100">Created Date</th>
                <th>Bucket Entry Type</th>
                <th>Rework Reason</th>
                <th>Rework Comment</th>
                <th>Rework User</th>
                <th>Rework Date</th>
            </tr>
            </thead>
            <tbody>

            <c:set var="headerVisibilityMap" value="${actionBean.headerVisibilityMap}"/>

            <c:forEach items="${actionBean.collectiveEntries}" var="entry">
                <tr id="${entry.bucketEntryId}" data-vessel-label="${entry.labVessel.label}">
                    <td class="bucket-control">
                        <stripes:checkbox class="bucket-checkbox" name="selectedEntryIds"
                                          value="${entry.bucketEntryId}"/>
                    </td>
                    <td>
                    <c:if test="${headerVisibilityMap['Vessel Name']}">
                        <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${entry.labVessel.label}">
                                ${entry.labVessel.label}
                        </a>
                    </c:if></td>
                    <td><c:if test="${headerVisibilityMap['Sample Name']}">
                        <c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample" varStatus="stat">
                            <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${mercurySample.sampleKey}">
                                    ${mercurySample.sampleKey}</a>
                            <c:if test="${!stat.last}">&nbsp;</c:if>
                        </c:forEach>
                    </c:if></td>
                    <td class="ellipsis">
                        <c:if test="${headerVisibilityMap['Material Type']}">
                        ${entry.labVessel.latestMaterialType.displayName}
                    </c:if>
                    </td>
                    <td><c:if test="${headerVisibilityMap['PDO']}">
                        <span class="ellipsis editable"> ${entry.productOrder.businessKey} </span>
                        <span style="display: none;" class="icon-pencil"></span>
                    </c:if>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['PDO Name']}">
                        <div class="ellipsis" style="width: 300px"> ${entry.productOrder.title} </div>
                        </c:if>
                    </td>
                    <td class="ellipsis">
                        <c:if test="${headerVisibilityMap['PDO Owner']}">
                            ${actionBean.getUserFullName(entry.productOrder.createdBy)}</c:if>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['Batch Name']}">
                            <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch" varStatus="stat">
                            ${actionBean.getLink(batch.businessKey)} <c:if test="${!stat.last}">&nbsp;</c:if></c:forEach></c:if>
                    </td>
                    <td>
                        <div class="ellipsis" style="max-width: 250px;">
                            <c:if test="${headerVisibilityMap['Workflow']}">
                                ${mercuryStatic:join(actionBean.getWorkflowNames(entry), "<br/>")}
                            </c:if>
                        </div>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['Product']}">
                            <div class="ellipsis" style="max-width: 250px;">${entry.productOrder.product.name}</div></c:if>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['Add-ons']}">
                            <div class="ellipsis" style="max-width: 250px;">
                                    ${entry.productOrder.getAddOnList("<br/>")}
                            </div>
                        </c:if></td>
                    <td class="ellipsis">
                        <c:if test="${headerVisibilityMap['Receipt Date']}"><c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample" varStatus="stat">
                            <fmt:formatDate value="${mercurySample.receivedDate}" pattern="MM/dd/yyyy HH:mm"/>
                            <c:if test="${!stat.last}">&nbsp;</c:if>
                        </c:forEach></c:if>
                    </td>
                    <td class="ellipsis">
                        <c:if test="${headerVisibilityMap['Created Date']}"><fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:mm"/></c:if>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['Bucket Entry Type']}">${entry.entryType.name}</c:if>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['Rework Reason']}">${entry.reworkDetail.reason.reason}</c:if>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['Rework Comment']}"><div class="ellipsis">${entry.reworkDetail.comment}</div></c:if>
                    </td>
                    <td>
                        <c:if test="${entry.reworkDetail != null && headerVisibilityMap['Rework User']}">
                            ${actionBean.getUserFullName(entry.reworkDetail.addToReworkBucketEvent.eventOperator)}
                        </c:if>
                    </td>
                    <td>
                        <c:if test="${headerVisibilityMap['Rework Date']}"><fmt:formatDate value="${entry.reworkDetail.addToReworkBucketEvent.eventDate}"
                                        pattern="MM/dd/yyyy HH:mm:ss"/></c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:form>
</stripes:layout-component>
</stripes:layout-render>

