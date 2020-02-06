<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.SRSBatchActionBean"/>
<c:set var="stage" scope="page" value="${actionBean.stage}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Batch Management" sectionTitle="SRS Batch Management"
                       showCreate="false" dataTablesVersion="1.10">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

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
                var removeForm = $j('#formRemove');
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
        <div class="row-fluid">
            <div class="span12">
                Batches: <select id="lbBatches" name="batchId">
                <option value="">Choose An SRS Batch</option>
                <c:forEach items="${actionBean.batchSelectionList}" var="item" varStatus="row">
                    <option value="${item.batchId}">${item.batchName}</option>
                </c:forEach></select> <stripes:submit id="btnViewBatch" name="view" value="View SRS Batch"/> <input
                    style="margin-left: 40px" type="button" id="btnNewBatch" value="Create New SRS Batch"/>
            </div>
        </div>
        <hr style="color: #b9b9b9; margin: 6px 0 6px 0"/>
        <div class="row-fluid">
            <div class="span3">
                <h4>SRS Batch: ${actionBean.labBatch.batchName}</h4>
                Created <fmt:formatDate value="${actionBean.labBatch.createdOn}"
                                        pattern="${actionBean.dateTimePattern}"/>
            </div>
            <c:if test="${stage eq 'EDITING' and actionBean.labBatch.active}"><input type="hidden" name="batchId"
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
        <div class="row-fluid">
            <div class="span12">
                <table id="vesselList" class="table simple">
                    <thead>
                    <tr>
                        <th><input for="count" type="checkbox" class="checkAll" id="checkAllTop">
                            <div id="count" class="checkedCount">0</div>
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
                            <td><input name="selectedLabels" value="${sv.labVessel.label}" class="shiftCheckbox"
                                       type="checkbox"></td>
                            <td><c:forEach items="${sv.labVessel.mercurySamples}"
                                           var="sample">${sample.sampleKey} </c:forEach></td>
                            <td>${sv.labVessel.label}</td>
                            <td>
                                <c:if test="${sv.labVessel.storageLocation ne null}">${sv.labVessel.storageLocation.buildLocationTrail()}</c:if>
                                <c:if test="${sv.labVessel.storageLocation eq null}">(not in storage)</c:if>
                            </td>
                            <td><img id="removeOneIcon[${row.index}]" src="${ctxpath}/images/error.png" alt="Remove"
                                     data-value="${sv.labVessel.label}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <form id="formRemove" name="formRemove" action="${ctxpath}/storage/srs.action?evtRemove=" method="post">
                    <input type="button" id="btnDeleteChecked" value="Delete Checked"/>
                    <input type="hidden" name="batchId" id="batchIdRemove" value="${actionBean.batchId}"/>
                    <input type="hidden" name="inputValues" id="removeInputValues" value=""/>
                </form>
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