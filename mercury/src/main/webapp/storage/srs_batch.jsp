<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.SRSBatchActionBean"/>
<c:set var="stage" scope="page" value="${actionBean.stage}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Batch Management" sectionTitle="SRS Batch Management" showCreate="true" dataTablesVersion="1.10">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            atLeastOneChecked = function (name, container) {
                var checkboxes = $j( ":checkbox", container );
                for (var i = 0; i < checkboxes.length; ++i) {
                    if (checkboxes[i].name === name && checkboxes[i].checked) {
                        return true;
                    }
                }
                alert('You must check at least one sample to remove');
                return false;
            };

            removeOne = function (event) {
                var removeForm = $j('#formRemove');
                $j( '#removeInputValues', removeForm ).val(event.data);
                removeForm.submit();
            };

            $j(document).ready(function() {
                // May not be present depending on what stage we're in
                var listElement = $j('#vesselList');
                if( listElement.length > 0 ) {
                    listElement.dataTable({
                        rowReorder: true,
                        "order": [[ 3, "asc" ]],
                        "columns": [
                            { "orderable": false, "searchable": false },  // Selected checkboxes
                            { "orderable": true }, // Sample
                            { "orderable": true },  // Barcode
                            { "orderable": true },  // Storage Location
                            { "orderable": false, "searchable": false, "width": "5%" } // Delete button
                        ]
                    });
                    $j.each( $j('img', listElement), function( i, item ) {
                        if(item.id.startsWith("removeOneIcon")) {
                            var link = $j(item);
                            link.click(link.data("value"), removeOne);
                        }
                    });
                    $j('#btnDeleteChecked').click(function() {
                        if( !atLeastOneChecked("selectedLabels", listElement)){
                            return;
                        }
                        var checkboxes = $j( ":checkbox", listElement );
                        var idList = "";
                        for (var i = 0; i < checkboxes.length; ++i) {
                            if (checkboxes[i].name === "selectedLabels" && checkboxes[i].checked) {
                                idList = idList + " " + checkboxes[i].value;
                            }
                        }
                        var removeForm = $j('#formRemove');
                        $j( '#removeInputValues', removeForm ).val(idList);
                        removeForm.submit();

                    });
                }
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
        <div class="row-fluid">
            <stripes:form id="formBatch" name="formBatch" action="/storage/srs.action">
                <c:choose>

                    <c:when test = "${stage eq 'SEARCHING'}">
                        <div class="span12">
                        <label for="batchName" style="padding-top: 10px">Batch Name</label>
                        <stripes:text name="batchName" id="txtBatchName"/>  <stripes:submit name="evtSearch" id="btnSearch" value="Search"/>
                        </div>
                    </c:when>

                    <c:when test="${stage eq 'EDITING'}">
                        <div class="span3">
                        <h4>SRS Batch: ${actionBean.labBatch.batchName}</h4>
                        Created <fmt:formatDate value="${actionBean.labBatch.createdOn}"
                                                pattern="${actionBean.dateTimePattern}"/><stripes:hidden name="batchId"/>
                        </div><div class="span9">
                        <label for="txtAddValues" style="padding-top: 10px">Samples/Barcodes to Add</label>
                        <textarea name="inputValues" id="txtAddValues"></textarea> <stripes:submit name="evtAddBarcodes" id="btnAddBarcodes" value="Add Barcodes"/> <stripes:submit name="evtAddSamples" id="btnAddSamples" value="Add Samples"/>
                    </div>
                    </c:when>

                    <c:when test = "${stage eq 'CREATING'}">
                        <div class="span12">
                        <label for="batchName" style="padding-top: 10px">Batch Name</label>
                        <stripes:text name="batchName" id="txtBatchName"/> <stripes:submit name="save" id="btnSave" value="Save"/>
                        </div>
                    </c:when>

                    <c:otherwise>
                        Error: Unknown stage: ${stage}
                    </c:otherwise>
                </c:choose>
            </stripes:form>
        </div>
        <hr style="color: #b9b9b9; margin: 6px 0 6px 0"/>
        <c:if test="${stage eq 'EDITING'}">
            <table id="vesselList" class="table simple">
                <thead>
                <tr>
                    <th><input for="count" type="checkbox" class="checkAll" id="checkAllTop"><div id="count" class="checkedCount">0</div></th>
                    <th>Sample ID</th>
                    <th>Barcode</th>
                    <th>Storage Location</th>
                    <th>&nbsp;</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.labBatch.labBatchStartingVessels}" var="sv" varStatus="row">
                    <tr>
                        <td><input name="selectedLabels" value="${sv.labVessel.label}" class="shiftCheckbox" type="checkbox"></td>
                        <td><c:forEach items="${sv.labVessel.mercurySamples}" var="sample">${sample.sampleKey} </c:forEach></td>
                        <td>${sv.labVessel.label}</td>
                        <td>
                            <c:if test="${sv.labVessel.storageLocation ne null}">${sv.labVessel.storageLocation.buildLocationTrail()}</c:if>
                            <c:if test="${sv.labVessel.storageLocation eq null}">(not in storage)</c:if>
                        </td>
                    <td> <img id="removeOneIcon[${row.index}]" src="${ctxpath}/images/error.png" alt="Remove" data-value="${sv.labVessel.label}"/> </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <form id="formRemove" name="formRemove" action="${ctxpath}/storage/srs.action?evtRemove=" method="post">
                <input type="button" id="btnDeleteChecked" value="Delete Checked"/>
                <input type="hidden" name="batchId" id="batchIdRemove" value="${actionBean.batchId}"/>
                <input type="hidden" name="<csrf:tokenname/>" value="<csrf:tokenvalue/>"/>
                <input type="hidden" name="inputValues" id="removeInputValues" value=""/>
            </form>
        </c:if> <%-- Show table of vessels for editing --%>
        </div><%--container-fluid--%>

    </stripes:layout-component>

</stripes:layout-render>