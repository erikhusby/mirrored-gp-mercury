<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.SRSBatchActionBean"/>
<c:set var="stage" scope="page" value="${actionBean.stage}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Batch Management" sectionTitle="SRS Batch Management"
                       showCreate="false" dataTablesVersion="1.10">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            /** Expand and collapse the SRS selection section */
            let toggleVisible = function (id, img) {
                let div = $j('#' + id);
                div.toggle();
                if (div.css("display") == 'none') {
                    img.src = "/Mercury/images/plus.gif";
                } else {
                    img.src = "/Mercury/images/minus.gif";
                }
            };

            let atLeastOneChecked = function (name, container) {
                var checkboxes = $j(":checkbox", container);
                for (var i = 0; i < checkboxes.length; ++i) {
                    if (checkboxes[i].name === name && checkboxes[i].checked) {
                        return true;
                    }
                }
                alert('You must check at least one sample to remove');
                return false;
            };

            let removeOne = function (event) {
                let removeForm = $j('#formRemove');
                $j('#removeInputValues', removeForm).val(event.data);
                removeForm.submit();
            };

            let saveNewBatch = function () {
                let dlg = $j("#newNameOverlay");
                let feedback = $j("#newNameError");
                let newBatchName = $j("#newBatchName").val();
                if (newBatchName.trim().length == 0) {
                    feedback.text("Batch name required");
                    feedback.show();
                    return;
                }
                dlg.dialog("close");
                // Let the server sort it out
                $j("#formNewBatch").submit();
            };

            $j(document).ready(function () {

                // May not be present depending on what stage we're in
                let listElement = $j('#vesselList');
                if (listElement.length > 0) {
                    listElement.dataTable({
                        rowReorder: true,
                        "order": [[3, "asc"]],
                        "columns": [
                            {"orderable": false, "searchable": false},  // Selected checkboxes
                            {"orderable": true}, // Sample
                            {"orderable": true},  // Barcode
                            {"orderable": true},  // Storage Location
                            {"orderable": false, "searchable": false, "width": "5%"} // Delete button
                        ]
                    });
                    $j.each($j('img', listElement), function (i, item) {
                        if (item.id.startsWith("removeOneIcon")) {
                            var link = $j(item);
                            link.click(link.data("value"), removeOne);
                        }
                    });
                    $j('#btnDeleteChecked').click(function () {
                        if (!atLeastOneChecked("selectedLabels", listElement)) {
                            return;
                        }
                        var checkboxes = $j(":checkbox", listElement);
                        var idList = "";
                        for (var i = 0; i < checkboxes.length; ++i) {
                            if (checkboxes[i].name === "selectedLabels" && checkboxes[i].checked) {
                                idList = idList + " " + checkboxes[i].value;
                            }
                        }
                        var removeForm = $j('#formRemove');
                        $j('#removeInputValues', removeForm).val(idList);
                        removeForm.submit();

                    });
                }

                $j("#btnNewBatch").click(
                    function () {
                        let feedback = $j("#newNameError");
                        feedback.text("");
                        feedback.hide();
                        $j("#newNameOverlay").dialog("open");
                    }
                );

                $j("#newNameOverlay").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: [
                        {
                            text: "Save",
                            click: saveNewBatch
                        },
                        {
                            text: "Cancel",
                            click: function () {
                                $j(this).dialog("close");
                            }
                        }
                    ]
                });

            }); // onReady
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid"><stripes:form id="formBatch" name="formBatch" action="/storage/srs.action">
        <fieldset>
            <c:if test="${stage eq 'CHOOSING'}"><c:set var="expandImage" scope="page"
                                                       value="/Mercury/images/minus.gif"/></c:if>
            <c:if test="${stage ne 'CHOOSING'}"><c:set var="expandImage" scope="page" value="/Mercury/images/plus.gif"/></c:if>
            <legend>View SRS Batches <img id="savedSearchesPlus" src="${expandImage}"
                                          onclick="toggleVisible('batchSelectDiv', this);"
                                          alt="Expand / Contract SRS Batch Pick"></legend>
            <div class="span12" style="display: ${actionBean.displayAttribChoosing}" id="batchSelectDiv">
                Batches: <select id="lbBatches" name="batchId">
                <option value="">Choose An SRS Batch</option>
                <c:forEach items="${actionBean.batchSelectionList}" var="item" varStatus="row">
                    <c:if test="${item.active}">
                        <option value="${item.batchId}">${item.batchName}</option>
                    </c:if>
                    <c:if test="${not item.active}">
                        <option value="${item.batchId}"
                                style="background-color: #f8b9b7;font-style: italic">${item.batchName}</option>
                    </c:if>
                </c:forEach></select> <stripes:submit id="btnViewBatch" name="view" value="View SRS Batch"/> <input
                    type="button"
                    style="margin-left: 40px" id="btnNewBatch" value="Create New SRS Batch"/> <input type="submit"
                                                                                                     style="margin-left: 40px"
                                                                                                     id="btnToggleBatchStatus"
                                                                                                     name="evtToggleBatchStatus"
                                                                                                     value="Close/Re-Open SRS Batch"/>
            </div>
        </fieldset>
        </div>
        <hr style="color: #b9b9b9; margin: 6px 0 6px 0"/>
        <div class="row-fluid" style="display: ${actionBean.displayAttribEditing}">
            <div class="span3">
                <h4>${actionBean.labBatch.batchName}<c:if test="${not actionBean.isBatchActive}"> (inactive)</c:if></h4>
                <h5>Created <fmt:formatDate value="${actionBean.labBatch.createdOn}"
                                            pattern="${actionBean.dateTimePattern}"/></h5>
            </div>
            <c:if test="${stage eq 'EDITING' and actionBean.isBatchActive}"><input type="hidden" name="batchId"
                                                                                   value="${actionBean.labBatch.labBatchId}"/>
                <div class="span9">
                    <label for="txtAddValues" style="padding-top: 10px">Samples/Barcodes to Add</label>
                    <textarea name="inputValues" id="txtAddValues"></textarea> <stripes:submit
                        name="evtAddBarcodes" id="btnAddBarcodes" value="Add Barcodes"/> <stripes:submit
                        name="evtAddSamples" id="btnAddSamples" value="Add Samples"/>
                </div>
            </c:if>
        </div>
    </stripes:form>
        <div class="row-fluid" style="display: ${actionBean.displayAttribEditing}">
            <div class="span12">
                <table id="vesselList" class="table simple">
                    <thead>
                    <tr>
                        <th><c:if test="${actionBean.isBatchActive}"><input for="count" type="checkbox" class="checkAll"
                                                                            id="checkAllTop">
                            <div id="count" class="checkedCount">0</div>
                        </c:if>
                        </th>
                        <th>Sample ID</th>
                        <th>Barcode</th>
                        <th>Storage Location</th>
                        <th>&nbsp;</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.labBatch.labBatchStartingVessels}" var="sv" varStatus="row">
                        <tr>
                            <td><c:if test="${actionBean.isBatchActive}"><input name="selectedLabels"
                                                                                value="${sv.labVessel.label}"
                                                                                class="shiftCheckbox"
                                                                                type="checkbox"></c:if></td>
                            <td><c:forEach items="${sv.labVessel.mercurySamples}"
                                           var="sample">${sample.sampleKey} </c:forEach></td>
                            <td>${sv.labVessel.label}</td>
                            <td>
                                <c:if test="${sv.labVessel.storageLocation ne null}">${sv.labVessel.storageLocation.buildLocationTrail()}</c:if>
                                <c:if test="${sv.labVessel.storageLocation eq null}">(not in storage)</c:if>
                            </td>
                            <td><c:if test="${actionBean.isBatchActive}"><img id="removeOneIcon[${row.index}]"
                                                                              src="${ctxpath}/images/error.png"
                                                                              alt="Remove"
                                                                              data-value="${sv.labVessel.label}"/></c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <c:if test="${actionBean.isBatchActive}">
                    <form id="formRemove" name="formRemove" action="${ctxpath}/storage/srs.action?evtRemove="
                          method="post">
                        <input type="button" id="btnDeleteChecked" value="Delete Checked"/>
                        <input type="hidden" name="batchId" id="batchIdRemove" value="${actionBean.batchId}"/>
                        <input type="hidden" name="inputValues" id="removeInputValues" value=""/>
                    </form>
                </c:if>
            </div>
        </div><%--container-fluid--%>

        <div id="newNameOverlay" title="Create New Batch">
            <div id="newNameError" class="alert-error" style="text-align:center;display:none"></div>
            <label for="newBatchName" style="padding-top: 10px">Batch Name</label>
            <form id="formNewBatch" name="formNewBatch" action="/Mercury/storage/srs.action"><input type="text"
                                                                                                    id="newBatchName"
                                                                                                    name="batchName"/><input
                    type="hidden" name="save" value=""/></form>
        </div>

    </stripes:layout-component>

</stripes:layout-render>