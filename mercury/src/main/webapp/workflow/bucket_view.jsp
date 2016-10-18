<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>
<c:set var="addToBatchText" value="Enter if you are adding to an existing batch"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" sectionTitle="Select Bucket">
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
    </style>
    <script src="${ctxpath}/resources/scripts/jquery.pasteSelect.js" type="text/javascript"></script>
    <script src="${ctxpath}/resources/scripts/jquery.jeditable.mini.js" type="text/javascript"></script>
    <script type="text/javascript">
        function submitBucket() {
            $j("#spinner").show();
            $j('#bucketForm').submit();
        }

        function isBucketEmpty() {
            return $j('#bucketEntryView tbody tr td').not('.dataTables_empty').length == 0;
        }

        // Enable or disable the form buttons when bucket entries are selected or deselected.
        function showOrHideControls() {
            if ($j("input[name='selectedEntryIds']:checked").length == 0) {
                $j(".actionControls").find("input").attr("disabled", "disabled")
            } else {
                $j(".actionControls").find("input").removeAttr("disabled");
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
                $j('.dataTables_filter').closest('.row-fluid').hide();

                // hide pdo editable stuff
                $j('#editable-text').hide();
                $(".editable").removeClass("editable");
                $(".icon-pencil").removeClass("icon-pencil");

                // Hide the unchecked rows so the user knows what will be included in the request.
                $j("input[type=checkbox]").not(":checked").closest("tbody tr").hide();
            });
        }

        $j(document).ready(function () {
            $j("#bucketEntryView").pasteSelect({
                columnNames: ["Vessel Name", "Sample Name"],
                noun: "Bucket Entry",
                pluralNoun: "Bucket Entries"
            });
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
                    var oTable = $j('#bucketEntryView').dataTable();
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
                        "onblur" : "ignore",
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

            oTable = $j('#bucketEntryView').dataTable({
                "oTableTools":ttExportDefines,
                "aaSorting": [[1,'asc'], [7,'asc']],
                "aoColumns":[
                    {"bSortable":false},
                    {"bSortable": true, "sClass": "nowrap"},
                    {"bSortable": true, "sClass": "nowrap"},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
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
                "fnDrawCallback": editablePdo
            });
            includeAdvancedFilter(oTable, "#bucketEntryView");
            if ($j(".bucket-checkbox").is("visible")) {
                $j("bucketEntryView_filter").hide();
            } else {
                $j("bucketEntryView_filter").show();
            }

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
                    $j.ajax({
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

            // prevent submit when hitting the return key in an input so ajax validation can happen.
            $j("#bucketEntryForm :input:not(textarea)").keypress(function (event) {
                return event.keyCode != 13;
            });
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
            <a href="javascript:void(0)" id="PasteBarcodesList" class="bucket-control"
               title="Use a pasted-in list of tube barcodes to select samples">Choose via list of vessel or sample names...</a>
            <a href="#" id="cancel" onClick="submitBucket()" style="display: none;">Cancel</a>
        </div>
        <table id="bucketEntryView" class="bucket-checkbox table simple">
            <thead>
            <tr>
                <th width="10" class="bucket-control">
                    <input type="checkbox" class="bucket-checkAll"/>
                    <span id="count" class="bucket-checkedCount"></span>
                </th>
                <th width="60">Vessel Name</th>
                <th width="50">Sample Name</th>
                <th>Material Type</th>
                <th>PDO</th>
                <th>PDO Name</th>
                <th>PDO Owner</th>
                <th style="min-width: 50px">Batch Name</th>
                <th>Workflow</th>
                <th>Product</th>
                <th>Add-ons</th>
                <th width="100">Receipt Date</th>
                <th width="100">Created Date</th>
                <th>Bucket Entry Type</th>
                <th>Rework Reason</th>
                <th>Rework Comment</th>
                <th>Rework User</th>
                <th>Rework Date</th>
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
                        <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${entry.labVessel.label}">
                                ${entry.labVessel.label}
                        </a></td>

                    <td>
                        <c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample" varStatus="stat">
                            <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${mercurySample.sampleKey}">
                                    ${mercurySample.sampleKey}</a>
                            <c:if test="${!stat.last}">&nbsp;</c:if>
                        </c:forEach>
                    </td>
                    <td class="ellipsis">
                            ${entry.labVessel.latestMaterialType.displayName}
                    </td>
                    <td class="editable"><span class="ellipsis">${entry.productOrder.businessKey}</span><span
                            style="display: none;"
                            class="icon-pencil"></span>
                    </td>
                    <td>
                        <div class="ellipsis" style="width: 300px">${entry.productOrder.title}</div>
                    </td>
                    <td class="ellipsis">
                            ${actionBean.getUserFullName(entry.productOrder.createdBy)}
                    </td>
                    <td>
                        <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch" varStatus="stat">
                            ${actionBean.getLink(batch.businessKey)} <c:if test="${!stat.last}">&nbsp;</c:if></c:forEach>
                    </td>
                    <td>
                        <div class="ellipsis" style="max-width: 250px;">
                                ${mercuryStatic:join(entry.workflowNames, "<br/>")}
                        </div>
                    </td>
                    <td>
                        <div class="ellipsis" style="max-width: 250px;">${entry.productOrder.product.name}</div>
                    </td>
                    <td>
                        <div class="ellipsis" style="max-width: 250px;">
                                ${entry.productOrder.getAddOnList("<br/>")}
                        </div>
                    </td>
                    <td class="ellipsis">
                        <c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample" varStatus="stat">
                            <fmt:formatDate value="${mercurySample.receivedDate}" pattern="MM/dd/yyyy HH:mm"/>
                            <c:if test="${!stat.last}">&nbsp;</c:if>
                        </c:forEach>
                    </td>
                    <td class="ellipsis">
                        <fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:mm"/>
                    </td>
                    <td>
                            ${entry.entryType.name}
                    </td>
                    <td>
                            ${entry.reworkDetail.reason.reason}
                    </td>
                    <td>
                            ${entry.reworkDetail.comment}
                    </td>
                    <td>
                        <c:if test="${entry.reworkDetail != null}">
                            ${actionBean.getUserFullName(entry.reworkDetail.addToReworkBucketEvent.eventOperator)}
                        </c:if>
                    </td>
                    <td>
                        <fmt:formatDate value="${entry.reworkDetail.addToReworkBucketEvent.eventDate}"
                                        pattern="MM/dd/yyyy HH:mm:ss"/>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:form>

</stripes:layout-component>
</stripes:layout-render>

