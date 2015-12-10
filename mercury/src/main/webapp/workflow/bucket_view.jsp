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
        }
        .editable select {
            width: auto !important;
        }

        .form-horizontal .control-group {
            margin-bottom: 10px;
        }

        td.nowrap {
            white-space: nowrap;
        }
    </style>
    <script src="${ctxpath}/resources/scripts/jquery.jeditable.mini.js" type="text/javascript"></script>
    <script type="text/javascript">
        function submitBucket() {
            $j('#bucketForm').submit();
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
                    "ApplyBarcodesButton": {
                        id: "applyBarcodes",
                        text: "Apply barcodes",
                        click: applyBarcodes
                    },
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

        function setupBucketEvents() {
            $j(".batch-create,.batch-append").hide();

            var bucketFormJquery = $j("#bucketEntryForm");
            bucketFormJquery.click(function(event) {
                if ($j(event.target).is('[type=submit')) {
                    $j(this).data('clicked', $j(event.target));
                }
            });
            bucketFormJquery.submit(function (event) {
                var clickedButton = $j(this).data('clicked');
                if (clickedButton == undefined) {
                    return;
                }
                var clickedButtonEvent = clickedButton.attr('name');
                if (clickedButtonEvent == "addToBatch") {
                    $j(".batch-append").slideDown(200);
                } else if (clickedButtonEvent == "createBatch") {
                    $j(".batch-create").slideDown(200);
                }

                var clickedButtonName = clickedButton.attr("value");

                if (clickedButtonName.startsWith("Confirm") || clickedButton == undefined) {
                    return true;
                }
                var buttons = bucketFormJquery.find(".bucket-control");
                for (var i = 0; i < buttons.length; i++) {
                    loopButton = buttons[i];
                    if (loopButton.value != clickedButtonName) {
                        $j(loopButton).hide();
                    }
                }
                // hide dataTables filter
                $j('.dataTables_filter').closest('.row-fluid').hide();

                // hide pdo editable stuff
                $j('#editable-text').hide();
                $(".editable").removeClass("editable")
                $(".icon-pencil").removeClass("icon-pencil")

                if ($j("input[type=checkbox]:checked").length > 0) {
                    $j("input[type=checkbox]").not(":checked").closest("tbody tr").hide()
                }
                clickedButton.attr('value', "Confirm "+clickedButtonName);

                return false;
            });
        }





        $j(document).ready(function () {
            initializeBarcodeEntryDialog();
            setupBucketEvents();
            if (isBucketEmpty()) {
                $j("bucketEntryForm").hide()
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
            if (isBucketEmpty()) {
                $j("bucketEntryForm").hide();
                $j(".actionControls").hide();
            } else {
                $j(".actionControls").show();
            }

            $j('.bucket-checkbox').enableCheckboxRangeSelection({
                checkAllClass:'bucket-checkAll',
                countDisplayClass:'bucket-checkedCount',
                checkboxClass:'bucket-checkbox'});

            $j("#dueDate").datepicker();

            $j("input[name='selectedEntryIds'], .bucket-checkAll").change(function () {
                            if ($j("input[name='selectedEntryIds']:checked").length == 0) {
                                $j(".actionControls").find("input").attr("disabled", "disabled")
                            } else {
                                $j(".actionControls").find("input").removeAttr("disabled");
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
                </div>
            </div>
        </div>
    </stripes:form>
    <stripes:form beanclass="${actionBean.class.name}" id="bucketEntryForm" class="form-horizontal">
        <div class="form-horizontal">
                <stripes:hidden name="selectedBucket" value="${actionBean.selectedBucket}"/>
                        <div class="control-group batch-create">
                            <stripes:label for="workflowSelect" name="Select Workflow" class="control-label"/>
                            <div class="controls">
                                <stripes:select id="workflowSelect" name="selectedWorkflow">
                                    <stripes:option value="">Select a Workflow</stripes:option>
                                    <stripes:options-collection collection="${actionBean.possibleWorkflows}"/>
                                </stripes:select>
                            </div>
                        </div>
                        <div class="control-group batch-append">
                            <stripes:label for="lcsetText" name="Batch Name" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="lcsetText" class="defaultText" name="selectedLcset"
                                              title="Enter if you are adding to an existing batch"/>
                            </div>
                        </div>
                        <div class="control-group batch-create">
                            <stripes:label for="summary" name="Summary" class="control-label"/>
                            <div class="controls">
                                <stripes:text name="summary" class="defaultText"
                                              title="Enter a summary for a new batch ticket" id="summary"
                                              value="${actionBean.summary}"/>
                            </div>
                        </div>

                        <div class="control-group batch-create">
                            <stripes:label for="description" name="Description" class="control-label"/>
                            <div class="controls">
                                <stripes:textarea name="description" class="defaultText"
                                                  title="Enter a description for a new batch ticket"
                                                  id="description" value="${actionBean.description}"/>
                            </div>
                        </div>

                        <div class="control-group batch-create" >
                            <stripes:label for="important" name="Important Information"
                                           class="control-label"/>
                            <div class="controls">
                                <stripes:textarea name="important" class="defaultText"
                                                  title="Enter important info for a new batch ticket"
                                                  id="important"
                                                  value="${actionBean.important}"/>
                            </div>
                        </div>

                        <div class="control-group batch-create">
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
                        <div class="control-group batch-create batch-append">
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
                   title="Use a pasted-in list of tube barcodes to select samples">Choose via list of barcodes...</a>
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
                <th width="300">PDO Name</th>
                <th width="200">PDO Owner</th>
                <th>Batch Name</th>
                <th>Workflow</th>
                <th>Product</th>
                <th>Add-ons</th>
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
                            <stripes:checkbox class="bucket-checkbox" name="selectedEntryIds" value="${entry.bucketEntryId}"/>
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
                    <td class="ellipsis">
                        ${entry.labVessel.latestMaterialType.displayName}
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

