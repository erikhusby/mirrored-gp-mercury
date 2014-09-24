<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" sectionTitle="Select Bucket">
<stripes:layout-component name="extraHead">
    <style type="text/css">
        td.editable {
            width: 105px !important;
            height: 78px;
        }
        .editable select {
            width: auto !important;
        }

    </style>
    <script src="${ctxpath}/resources/scripts/jquery.jeditable.mini.js" type="text/javascript"></script>
    <script type="text/javascript">
        function submitBucket() {
            $j('#bucketForm').submit();
            showJiraInfo();
        }

        function submitWorkflow() {
            $j('#bucketWorkflowForm').submit();
            showJiraInfo();
        }

        function isBucketEmpty() {
            return $j('#bucketEntryView tbody tr td').not('.dataTables_empty').length == 0;
        }

        function addAmbiguousBucketEntryError(barcode) {
            // todo avoid showing duplicate strings
            $j('#AmbiguousBucketEntries tbody').append(
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

        function showNoBucketEntriesErrors() {
            showBarcodeErrors();
            $j('#NoBucketEntriesErrors').show();
        }

        function showAmbiguousBucketEntryErrors() {
            showBarcodeErrors();
            $j('#AmbiguousBucketEntriesErrors').show();
        }

        function clearBarcodesDialogErrors() {
            $j('#NoBucketEntries tbody tr').remove();
            $j('#AmbiguousBucketEntries tbody tr').remove();

            $j('#NoBucketEntriesErrors').hide();
            $j('#AmbiguousBucketEntriesErrors').hide();
        }

        function clearBarcodesDialog() {
            $j('#barcodes').val('');
            $j('#BarcodeErrors').hide();
            clearBarcodesDialogErrors();
        }

        function applyBarcodes() {
            clearBarcodesDialogErrors();

            var hasBarcodes = barcodes.value.trim().length > 0;
            var splitBarcodes = barcodes.value.trim().split(/\s+/);
            var hasAmbiguousBucketEntryErrors = false;
            var hasBarcodesNotFoundErrors = false;

            if (!hasBarcodes) {
                alert("No barcodes were entered.");
                return;
            }

            for (var i = 0; i < splitBarcodes.length; i++) {
                var barcode = splitBarcodes[i];
                var numMatchingRows = $j('[data-vessel-label="' + barcode + '"]>>input[type="checkbox"]').length;

                if (numMatchingRows > 1) {
                    hasAmbiguousBucketEntryErrors = true;
                    addAmbiguousBucketEntryError(barcode);
                }
                else if (numMatchingRows == 0) {
                    hasBarcodesNotFoundErrors = true;
                    addBarcodeNotFoundError(barcode);
                }
            }

            if (!hasAmbiguousBucketEntryErrors && !hasBarcodesNotFoundErrors) {
                for (var i = 0; i < splitBarcodes.length; i++) {
                    var barcode = splitBarcodes[i];
                    var numMatchingRows = $j('[data-vessel-label="' + barcode + '"]>>input[type="checkbox"]').length;

                    if (numMatchingRows == 1) {
                        // todo does not uptick the # in the upper left hand corner of the datatable,
                        // click() does not work either
                        // todo event handler for enter
                        $j('[data-vessel-label="' + barcode + '"]>>input[type="checkbox"]').prop('checked',true);
                    }
                }
                dialog.dialog("close");
                alert("Successfully chose " + splitBarcodes.length + " bucket entries");
            }
            else {
                if (hasAmbiguousBucketEntryErrors) {
                    showAmbiguousBucketEntryErrors();
                }
                if (hasBarcodesNotFoundErrors) {
                    showNoBucketEntriesErrors();
                }
            }
        }


        function hideBarcodeEntryDialog() {
            $j('#BarcodeErrors').hide();
            $j('#NoBucketEntriesErrors').hide();
            $j('#AmbiguousBucketEntriesErrors').hide();
        }

        function initializeBarcodeEntryDialog() {
            dialog = $j( "#ListOfBarcodesForm" ).dialog({
                autoOpen: false,
                height: 400,
                width: 250,
                modal: true,
                buttons: {
                    "Apply barcodes": applyBarcodes,
                    Cancel: function() {
                        dialog.dialog( "close" );
                    }
                }
            });

            $j( "#PasteBarcodesList").on( "click", function() {
                if (!isBucketEmpty()) {
                    clearBarcodesDialog();
                    dialog.dialog( "open" );
                }
                else {
                    alert("There are no samples in the bucket.");
                }
            });

            hideBarcodeEntryDialog();
        }

        $j(document).ready(function () {
            initializeBarcodeEntryDialog();

            var columnsEditable=false;
            <security:authorizeBlock roles="<%= roles(LabManager, PDM, PM, Developer) %>">
                columnsEditable=true;
            </security:authorizeBlock>

                var editablePdo = function()  {
                if (columnsEditable) {
                    var oTable = $j('#bucketEntryView').dataTable();
                    $j("td.editable").editable('${ctxpath}/view/bucketView.action?changePdo', {
                        'loadurl': '${ctxpath}/view/bucketView.action?findPdo',
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

            $j('#bucketEntryView').dataTable({
                "oTableTools":ttExportDefines,
                "aaSorting": [[1,'asc'], [7,'asc']],
                "aoColumns":[
                    {"bSortable":false},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true, "sType":"date"},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true},
                    {"bSortable":true}
                ],
                "fnDrawCallback": editablePdo
            });

            $j('.bucket-checkbox').enableCheckboxRangeSelection({
                checkAllClass:'bucket-checkAll',
                countDisplayClass:'bucket-checkedCount',
                checkboxClass:'bucket-checkbox'});

            $j("#dueDate").datepicker();
        });

        function showJiraInfo() {
            $j('#jiraTable').show();
        }
    </script>
</stripes:layout-component>

<stripes:layout-component name="content">
    <stripes:form id="bucketForm" class="form-horizontal" action="/view/bucketView.action?setBucket">
        <div class="form-horizontal">
            <div class="control-group">
                <stripes:label for="bucketselect" name="Select Bucket" class="control-label"/>
                <div class="controls">
                    <stripes:select id="bucketSelect" name="selectedBucket" onchange="submitBucket()">
                        <stripes:option value="">Select a Bucket</stripes:option>
                        <stripes:options-collection collection="${actionBean.buckets}"/>
                    </stripes:select>
                </div>
            </div>
        </div>
    </stripes:form>
    <stripes:form id="bucketWorkflowForm" class="form-horizontal" action="/view/bucketView.action?viewBucket">
        <div class="form-horizontal">
        <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
        <div class="control-group">
            <stripes:label for="workflowSelect" name="Select Workflow" class="control-label"/>
            <div class="controls">
                <stripes:select id="workflowSelect" name="selectedWorkflowDef" onchange="submitWorkflow()"
                                value="selectedWorkflowDef.name">
                    <stripes:option value="">Select a Workflow</stripes:option>
                    <stripes:options-collection collection="${actionBean.possibleWorkflows}" label="name" value="name"/>
                </stripes:select>
            </div>
        </div>
    </stripes:form>
    <stripes:form beanclass="${actionBean.class.name}"
                  id="bucketEntryForm" class="form-horizontal">
        <div class="form-horizontal">
        <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
        <stripes:hidden name="selectedWorkflowDef" value="${actionBean.selectedWorkflowDef}"/>
        <c:if test="${actionBean.jiraEnabled}">
            <div id="newTicketDiv">
                <div class="control-group">
                    <stripes:label for="lcsetText" name="LCSet Name" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="lcsetText" class="defaultText" name="selectedLcset"
                                      title="Enter if you are adding to a batch"/>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="summary" name="Summary" class="control-label"/>
                    <div class="controls">
                        <stripes:text name="summary" class="defaultText"
                                      title="Enter a summary for a new batch ticket" id="summary"
                                      value="${actionBean.summary}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="description" name="Description" class="control-label"/>
                    <div class="controls">
                        <stripes:textarea name="description" class="defaultText"
                                          title="Enter a description for a new batch ticket"
                                          id="description" value="${actionBean.description}"/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="important" name="Important Information"
                                   class="control-label"/>
                    <div class="controls">
                        <stripes:textarea name="important" class="defaultText"
                                          title="Enter important info for a new batch ticket"
                                          id="important"
                                          value="${actionBean.important}"/>
                    </div>
                </div>

                <div class="control-group">
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
            </div>
            </div>
        </c:if>
        <div class="borderHeader"><h4>Samples</h4></div>
        <br/>
        <ul><li>If you would like to change the value of a PDO for an item in the bucket, click on the value of the PDO in the table and select the new value.</li></ul>
        <div class="actionButtons">
            <stripes:submit name="createBatch" value="Create Batch" class="btn"/>
            <stripes:submit name="addToBatch" value="Add to Batch" class="btn"/>
            <stripes:submit name="removeFromBucket" value="Remove From Bucket" class="btn"/>
            <a href="javascript:void(0)" id="PasteBarcodesList" title="Use a pasted-in list of tube barcodes to select samples">Choose via list of barcodes...</a>
        </div>
        <table id="bucketEntryView" class="table simple">
            <thead>
            <tr>
                <th width="10">
                    <input type="checkbox" class="bucket-checkAll"/>
                    <span id="count" class="bucket-checkedCount"></span>
                </th>
                <th width="60">Vessel Name</th>
                <th width="50">Sample Name</th>
                <th>PDO</th>
                <th width="300">PDO Name</th>
                <th width="200">PDO Owner</th>
                <th>Batch Name</th>
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
                    <td>
                        <stripes:checkbox class="bucket-checkbox" name="selectedEntryIds"
                                          value="${entry.bucketEntryId}"/>
                    </td>
                    <td>
                        <a href="${ctxpath}/search/vessel.action?vesselSearch=&searchKey=${entry.labVessel.label}">
                                ${entry.labVessel.label}
                        </a></td>

                    <td>
                        <c:forEach items="${entry.labVessel.mercurySamples}"
                                   var="mercurySample"
                                   varStatus="stat">
                            <a href="${ctxpath}/search/sample.action?sampleSearch=&searchKey=${mercurySample.sampleKey}">
                                    ${mercurySample.sampleKey}
                            </a>

                            <c:if test="${!stat.last}">&nbsp;</c:if>
                        </c:forEach>
                    </td>
                    <td class="editable"><span class="ellipsis">${entry.productOrder.businessKey}</span><span style="display: none;"
                                                                                           class="icon-pencil"></span>
                    </td>
                    <td>
                        <div class="ellipsis" style="width: 300px">${entry.productOrder.title}</div>
                    </td>
                    <td class="ellipsis">
                            ${actionBean.getUserFullName(entry.productOrder.createdBy)}
                    </td>
                    <td>
                        <c:forEach items="${entry.labVessel.nearestWorkflowLabBatches}" var="batch"
                                   varStatus="stat">

                            ${batch.businessKey}
                            <c:if test="${!stat.last}">&nbsp;</c:if></c:forEach>

                    </td>
                    <td class="ellipsis">
                        <fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:mm:ss"/>
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

<div id="ListOfBarcodesForm">
    <form>
        <div class="control-group">
            <label for="barcodes" class="control-label">Enter 2D Barcodes, one per line</label>
            <textarea name="barcodes" id="barcodes" class="defaultText" cols="12" rows="5"></textarea>
        </div>
        <div id="BarcodeErrors">
            <p>We're sorry, but Mercury could not automatically choose bucket entries
               because of the following errors.</p>
            <div id="NoBucketEntriesErrors">
                <table id="NoBucketEntries">
                    <thead>
                        <tr><th>No bucket entries</th></tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
            <div id="AmbiguousBucketEntriesErrors">
                <table id="AmbiguousBucketEntries">
                    <thead>
                    <tr><th>Ambiguous bucket entries</th></tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
        </div>
    </form>
</div>

