<%--suppress CssRedundantUnit --%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>
<c:set var="addToBatchText" value="Enter if you are adding to an existing batch"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" dataTablesVersion="1.10" withColVis="true"
                       sectionTitle="Create and Update Batches">
<stripes:layout-component name="extraHead">
    <style type="text/css">
        .search-button {
            background: url("${ctxpath}/images/search.png") no-repeat left;
            padding-left: 20px;;
        }

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

,        .dataTables_length, .dataTables_info, .dataTables_paginate {
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
        /* css used to indicate slow columns */
        .em-turtle {
            background-image: url("${ctxpath}/images/turtle.png") !important;
            background-size: 16px 16px;
            background-repeat: no-repeat;
            background-position: right 5px center;
        }
        #bucketEntryView th {
            vertical-align: top;
        }
        /* min width for columns excluding the checkbox column */
        #bucketEntryView th:nth-child(n+2) {
            width:auto;
            min-width: 5em;
        }
        #bucketEntryView td,span.title {
            white-space: nowrap;
        }
        .ellipsis {
            max-width: 250px;
        }

        /* add a line break after the header */
        .title:after {
            content: '\A';
            white-space: pre-wrap;
        }
    </style>

    <script src="${ctxpath}/resources/scripts/jquery.jeditable.mini.js" type="text/javascript"></script>
    <script src="${ctxpath}/resources/scripts/hSpinner.js"></script>
    <script src="${ctxpath}/resources/scripts/columnSelect.js"></script>
    <script src="${ctxpath}/resources/scripts/chosen_v1.6.2/chosen.jquery.min.js" type="text/javascript"></script>
    <link rel="stylesheet" type="text/css" href="${ctxpath}/resources/scripts/chosen_v1.6.2/chosen.min.css"/>

    <script type="text/javascript">
        function submitBucket() {
            $j("#spinner").show();
            if (oTable) {
                $j(oTable.rows().nodes()).hide();
                oTable.draw();
            }
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

                // hide pdo editable stuff
                $j('#editable-text').hide();
                $(".editable").removeClass("editable");
                $(".icon-pencil").removeClass("icon-pencil");

                // Hide the unchecked rows so the user knows what will be included in the request.
                $j("input[type=checkbox]").not(":checked").closest("tbody tr").hide();

                // Hide the column filters
                $j(".table-control").hide();
            });
        }

        function findCellColumnIndex(headerName){
            var index=-1;
            $j("#bucketEntryView .title").each(function () {
                if ($j(this).text() == headerName){
                    index = $j(this).closest("th").index();
                }
            });
            return index;
        }

        $j(document).ready(function () {
            setupBucketEvents();

            // Hide the input form when there are no bucket entries.
            if (isBucketEmpty()) {
                $j("bucketEntryForm").hide();
                $j(".actionControls").hide();
            } else {
                $j(".actionControls").show();
            }

            var columnsEditable=false;
            <security:authorizeBlock roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer) %>">
            columnsEditable = true;
            </security:authorizeBlock>

            // Initialize data-search attribute before datatables loads the table.
            // Note: Putting this in a 'preInit.dt' event callback should work but doesn't.
            $j('#bucketEntryView').find("td").each(function () {
                $td = $j(this);
                $td.attr('data-search', $td.text().trim());
            });

            var editablePdo = function () {
                if (columnsEditable) {
                    var oTable = $j('#bucketEntryView').DataTable();
                    var pdoOwnerIndex = findCellColumnIndex("PDO Owner");
                    var pdoTitleIndex = findCellColumnIndex("PDO Name");

                    $j("td.editable").editable('${ctxpath}/workflow/bucketView.action?changePdo', {
                        'loadurl': '${ctxpath}/workflow/bucketView.action?findPdo',
                        'callback': function (value, settings) {
                            var jsonValues = $j.parseJSON(value);
                            $j(this).text(jsonValues.jiraKey);
                            $j(this).append($j("<span></span>", {'style': 'display:none;', 'class': 'icon-pencil'}));
                            var pdoTitleTd = $j(this).closest("tr").find("td:nth("+pdoTitleIndex+")");
                            pdoTitleTd.html($j("<div></div>", {'class': 'ellipsis', 'style': 'width: 300px','text':jsonValues.pdoTitle}));
                            $j(this).closest("tr").find("td:nth("+pdoOwnerIndex+")").text(jsonValues.pdoOwner);
                            oTable.row(this).invalidate('dom').draw();
                        },
                        'submitdata': function (value, settings) {
                                return {
                                    "selectedEntryIds": $j(this).closest('tr').attr('id'),
                                    "newPdoValue": $j(this).find(':selected').text()
                                };
                        },
                        'loaddata': function (value, settings) {
                                return {
                                    "selectedEntryIds": $j(this).closest('tr').attr('id')
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

            function initColumnVisibility() {
                var columnVisibilityKey = "columnVisibility";
                $j('#bucketEntryView').on('column-visibility.dt', function (e, settings, column, state) {
                    if (state) {
                        $j("body").data(columnVisibilityKey, true);
                    }
                });

                // define columns which cause the page to render slowly.
                <enhance:out escapeXml="false">
                var slowColumns = ${actionBean.slowColumns}
                </enhance:out>

                // When the "Show or Hide" button is clicked
                $j(document.body).on("click", "a.buttons-colvis", function (event) {
                    // When colvis modal is loading
                    $j.when($j(event.target).load()).then(function (event2) {
                        var slowButtons = $j("a.buttons-columnVisibility").filter(function () {
                            var $button = $j(this);
                            // test if the column headers is in the slowColumns array.
                            // the check for undefined is to prevent this from being called very time
                            return $button.data('tooltip') === undefined && slowColumns.indexOf($button.text().trim()) >= 0;
                        });
                        slowButtons.addClass("em-turtle");
                        slowButtons.attr('title', 'Enabling this column may slow page loading');
                        slowButtons.tooltip();
                    });
                });

                // If a column that was previously hidden but becomes visible the page
                // must be reloaded since there is no data in that column.
                $j(document.body).on("click", ".dt-button-background", function () {
                    oTable.state.save();
                    var sessionVisibility = !undefined && $j("body").data(columnVisibilityKey) || false;
                    if (sessionVisibility) {
                        $j("#bucketForm").submit();
                    }
                });
            }

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
                renderer: "bootstrap",
                buttons: [standardButtons('bucket-checkbox', 'title'), {
                    extend: 'colvis',
                    className: 'show-or-hide',
                    text: "Show or Hide Columns",
                    columns: ':gt(0)'
                }],
                language: {
                    info: "Showing _START_ to _END_ of _TOTAL_ bucket entries displayed.",
                    lengthMenu: "Displaying _MENU_ bucket entries per page"
                },
                columns: [
                    {sortable: false},
                    {sortable: true, "sClass": "nowrap"},
                    {sortable: true, "sClass": "nowrap"},
                    {sortable: true, "sClass": "nowrap"},
                    {sortable: true, "sClass": "nowrap"},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true, "sType": "date"},
                    {sortable: true, "sType": "date"},
                    {sortable: true},
                    {sortable: true, "sClass": "nowrap"},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true},
                    {sortable: true}
                ],
                "aaSorting": [[1,'asc'], [7,'asc']],
                stateSaveCallback: function (settings, data) {
                    var api = new $j.fn.dataTable.Api(settings);
                    for (var index = 0; index < data.columns.length; index++) {
                        var item = data.columns[index];
                        var header = $j(api.column(index).header()).find(".title").text();
                        if (header) {
                            item.headerName = header.trim();
                        }
                    }

                    var batchSize = $j("input#batchSize").val();
                    var stateData = {
                        "<%= BucketViewActionBean.TABLE_STATE_KEY %>": JSON.stringify(data),
                        "<%= BucketViewActionBean.SELECTED_BUCKET_KEY %>": bucketName,
                        "<%= BucketViewActionBean.SELECT_NEXT_SIZE %>": batchSize
                    };
                    localStorage.setItem(localStorageKey, JSON.stringify(stateData));
                    $j.ajax({
                        'url': "${ctxpath}/workflow/bucketView.action?<%= BucketViewActionBean.SAVE_SEARCH_DATA %>=",
                        'data': stateData,
                        dataType: 'json',
                        type: 'POST'
                    });
                },
                "stateLoadCallback": function (settings, data) {
                    var storedJson = '${actionBean.tableState}';
                    var useLocalData = true;
                    if (storedJson && storedJson !== '{}') {
                        // if bad data was stored in the preferences it will cause problems here, so wrap
                        // it around an exception.
                        try {
                            data = JSON.parse(storedJson.replace(/\\/g,'\\\\'));
                            useLocalData = false;
                        } catch (e) { /* Nothing to do here */ }
                    }
                    if (useLocalData) {
                        storedJson = localStorage.getItem(localStorageKey);
                        if (storedJson) {
                            data = JSON.parse(storedJson);
                        }
                    }
                    return data;
                },
                "fnDrawCallback": editablePdo,
                "initComplete": function (settings) {
                    initColumnSelect(settings, [
                        {"Vessel Name": 'text'},
                        {"Nearest Sample": 'text'},
                        {"Root Sample": 'text'},
                        {"Sample Name": 'text'},
                        {"PDO": 'select'},
                        {"PDO Name": 'select'},
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
                    ], "#filtering", "table-control", "${ctxpath}");


                    $j("#filtering").accordion({
                        "collapsible": true, "heightStyle": "content", 'active': false
                    });
                    $j("#filtering").css("z-index: 0; display: inline-block; width: 100%;");
                    var api = new $j.fn.dataTable.Api(settings);

                    // attach event handler so preferences are saved when select next field is changed.
                    function saveState() {
                        api.state.save();
                    }

                    $j("input#batchSize").on("change blur input incremented decremented", saveState);
                    $j("#chooseNext").on("click", "button", saveState);
                }
            });
            // set up the "Show or Hide" buttons
            initColumnVisibility();

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
                    <enhance:out escapeXml="false">
                    prePopulate: ${actionBean.ensureStringResult(actionBean.jiraUserTokenInput.completeData)},
                    </enhance:out>
                    tokenDelimiter: "${actionBean.jiraUserTokenInput.separator}",
                    preventDuplicates: true,
                    queryParam: 'jiraUserQuery',
                    autoSelectFirstResult: true
                }
            );
            $j("#spinner").hide();

            // prevent submit when hitting the return key in an input so ajax validation can happen.
            $j("#bucketEntryForm :input:not(textarea)").keypress(function (event) {
                return event.keyCode != 13;
            });

            var hSpinner = $j("#batchSize").hSpinner();
            $j("#chooseNbutton").click(function () {
                var batchSize = $j("#batchSize").val();
                if (batchSize > 0) {
                    $j("#bucketEntryView").find("input[name='selectedEntryIds']:checked").click();
                    $j("#bucketEntryView").find("input[name='selectedEntryIds']:lt(" + batchSize + ")").click();
                }
            })
            $j("#bucketForm").on('submit', submitBucket)
        });
    </script>
</stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="bucketEntryForm" class="form-horizontal">
            <div class="form-horizontal">
                <stripes:submit name="<%=BucketViewActionBean.SEARCH_BUCKET_ACTION%>"
                                value="Return to Bucket Search Page" class="btn-link btn-large search-button"/>

                <stripes:hidden class="actionControls" name="selectedBucket" value="${actionBean.selectedBucket}"/>
                <stripes:hidden class="actionControls" name="projectType" id="projectType" value="${actionBean.projectType}"/>
                <stripes:hidden class="actionControls" name="searchString"/>
                <stripes:hidden class="actionControls" name="productOrderTokenInput.listOfKeys"/>
                <stripes:hidden class="actionControls" name="materialTypeTokenInput.listOfKeys"/>
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
                        <enhance:out escapeXml="false">
                        <stripes:text id="dueDate" name="dueDate" class="defaultText"
                                      title="enter date (MM/dd/yyyy)"
                                      value="${actionBean.dueDate}"
                                      formatPattern="MM/dd/yyyy"><fmt:formatDate
                                value="${actionBean.dueDate}"
                                dateStyle="short"/></stripes:text>
                        </enhance:out>
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
            <div class="actionControls">
                <stripes:submit name="createBatch" value="Create Batch" class="btn bucket-control" disabled="true"/>
                <stripes:submit name="addToBatch" value="Add to Batch" class="btn bucket-control" disabled="true"/>
                <stripes:submit name="removeFromBucket" value="Remove From Bucket" class="btn bucket-control"
                                disabled="true"/>
                <a href="#" id="cancel" onClick="$j('#bucketForm').submit()" style="display: none;">Cancel</a>
                <div id="chooseNext" class="table-control">Select Next <input value="${actionBean.selectNextSize}"
                                                                        style="width: 3em;" id="batchSize" title="Batch Size"/>&nbsp;
                    <input type="button" id="chooseNbutton" value="Select" class="btn"/>
                </div>
            </div>
            <table id="bucketEntryView" class="bucket-checkbox table simple compact">
                <thead>
                <tr>
                    <th width="10" class="bucket-control title">
                        <input type="checkbox" class="bucket-checkAll" title="Check All"/>
                        <span id="count" class="bucket-checkedCount"></span>
                    </th>
                    <th><span class="title">Vessel Name</span></th>
                    <th><span class="title">Nearest Sample</span></th>
                    <th><span class="title">Root Sample</span></th>
                    <th><span class="title">Sample Name</span></th>
                    <th><span class="title ">Material Type</span></th>
                    <th><span class="title">PDO</span></th>
                    <th><span class="title">PDO Name</span></th>
                    <th><span class="title">PDO Owner</span></th>
                    <th><span class="title">Batch Name</span></th>
                    <th><span class="title ">Workflow</span></th>
                    <th><span class="title">Product</span></th>
                    <th><span class="title">Add-ons</span></th>
                    <th><span class="title ">Receipt Date</span></th>
                    <th><span class="title">Created Date</span></th>
                    <th><span class="title">Bucket Entry Type</span></th>
                    <th><span class="title">Rework Reason</span></th>
                    <th><span class="title">Rework Comment</span></th>
                    <th><span class="title">Rework User</span></th>
                    <th><span class="title">Rework Date</span></th>
                </tr>
                </thead>
                <tbody>

                <c:forEach items="${actionBean.collectiveEntries}" var="entry">
                    <tr id="${entry.bucketEntryId}" data-vessel-label="${entry.labVessel.label}">
                        <td class="bucket-control">
                            <stripes:checkbox class="bucket-checkbox" name="selectedEntryIds"
                                              value="${entry.bucketEntryId}"/>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Vessel Name')}">
                                <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${entry.labVessel.label}">
                                        ${entry.labVessel.label}
                                </a>
                            </c:if></td>
                        <td>
                            <c:if test="${actionBean.showHeader('Nearest Sample')}">
                                <c:forEach items="${entry.labVessel.sampleInstancesV2}" var="sampleInstance">
                                    <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${sampleInstance.nearestMercurySampleName}">
                                            ${sampleInstance.nearestMercurySampleName}</a>
                                </c:forEach>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Root Sample')}">
                                <c:forEach items="${entry.labVessel.sampleInstancesV2}" var="sampleInstance">
                                    <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${sampleInstance.rootOrEarliestMercurySample.sampleKey}">
                                            ${sampleInstance.rootOrEarliestMercurySample.sampleKey}</a>
                                </c:forEach>
                            </c:if></td>
                        <td><c:if test="${actionBean.showHeader('Sample Name')}">
                            <c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample" varStatus="stat">
                                <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${mercurySample.sampleKey}">
                                        ${mercurySample.sampleKey}</a>
                                <c:if test="${!stat.last}">&nbsp;</c:if>
                            </c:forEach>
                        </c:if></td>
                        <td>
                            <c:if test="${actionBean.showHeader('Material Type')}">
                                ${entry.labVessel.latestMaterialType.displayName}
                            </c:if>
                        </td>
                        <td class="editable">
                        <c:if test="${actionBean.showHeader('PDO')}">
                            ${entry.productOrder.businessKey}
                            <span style="display: none;" class="icon-pencil"></span>
                        </c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('PDO Name')}">
                                <div class="ellipsis" title="${entry.productOrder.title}">${entry.productOrder.title}</div>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('PDO Owner')}">
                                ${actionBean.getUserFullName(entry.productOrder.createdBy)}</c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Batch Name')}">
                            <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch" varStatus="stat">
                            ${actionBean.getLink(batch.businessKey)} <c:if test="${!stat.last}">&nbsp;</c:if></c:forEach></c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Workflow')}">
                                <c:set var="workflows"
                                       value="${mercuryStatic:join(actionBean.bucketWorkflowNames(entry), '<br/>')}"/>
                                <div class="ellipsis" title="${workflows}">${workflows}</div>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Product')}">
                            <div class="ellipsis" title="${entry.productOrder.product.name}">${entry.productOrder.product.name}</div></c:if>
                        </td>
                        <td>
                            <c:if test="${headerVisibilityMap['Add-ons']}">
                                <div class="ellipsis" title="entry.productOrder.getAddOnList("<br/>")">
                                        ${entry.productOrder.getAddOnList("<br/>")}
                                </div>
                            </c:if></td>
                        <td>
                        <c:if test="${actionBean.showHeader('Receipt Date')}"><c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample" varStatus="stat">
                                <fmt:formatDate value="${mercurySample.receivedDate}" pattern="MM/dd/yyyy HH:mm"/>
                                <c:if test="${!stat.last}">&nbsp;</c:if>
                            </c:forEach></c:if>
                        </td>
                        <td>
                        <c:if test="${actionBean.showHeader('Created Date')}"><fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:mm"/></c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Bucket Entry Type')}">${entry.entryType.name}</c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Rework Reason')}">${entry.reworkDetail.reason.reason}</c:if>
                        </td>
                        <td>
                            <c:if test="${actionBean.showHeader('Rework Comment')}">
                                <div class="ellipsis">${entry.reworkDetail.comment}</div>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${entry.reworkDetail != null && actionBean.showHeader('Rework User')}">
                                ${actionBean.getUserFullName(entry.reworkDetail.addToReworkBucketEvent.eventOperator)}
                            </c:if>
                        </td>
                        <td>
                        <c:if test="${actionBean.showHeader('Rework Date')}"><fmt:formatDate value="${entry.reworkDetail.addToReworkBucketEvent.eventDate}"
                                    pattern="MM/dd/yyyy HH:mm:ss"/></c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
</stripes:layout-component>
</stripes:layout-render>

