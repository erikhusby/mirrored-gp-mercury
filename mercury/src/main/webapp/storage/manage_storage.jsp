<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageManagerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Storage" sectionTitle="Manage Storage">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
        <script type="text/javascript">
            $j(document).ready(function () {

                /**
                 * When freezer selection changes, query to grab its children
                 */
                $("#freezerSelection").change(function () {
                    var label = $("#freezerSelection").val();
                    var formData = new FormData();
                    formData.append("freezerSearchKey", label);
                    formData.append("freezerSearch", label);
                    $j.ajax({
                        url: "${ctxpath}/storage/storageManager.action",
                        type: 'POST',
                        data: formData,
                        async: true,
                        success: function (results) {
                            $j("#slotSearchSubmit").removeAttr("disabled");
                            if( results.startsWith("Failure")) {
                                $j("#slotSearchError").text(results);
                            } else {
                                // Results is html frag in location_select.jsp
                                $('#manageStorageForm').append(results);
                            }
                        },
                        error: function(results){
                            //TODO Handle errors
                            $j("#slotSearchSubmit").removeAttr("disabled");
                            $j("#rackScanError").text("A server error occurred");
                        },
                        cache: false,
                        datatype: "text",
                        processData: false,
                        contentType: false
                    });
                });

                /*
                 * User is searching for a slot, query to find its rack and that racks last known locations
                 */
                $("#slotSearchSubmit").click(function (e) {
                    e.preventDefault();
                    var formData = new FormData();
                    formData.append("slotSearchKey", $j('#slotSearchKey').val());
                    formData.append("slotBarcodeSearch", $j('#slotSearchSubmit').val());
                    $j("#slotSearchSubmit").attr("disabled","disabled");
                    $j.ajax({
                        url: "${ctxpath}/storage/storageManager.action",
                        type: 'POST',
                        data: formData,
                        async: true,
                        success: function (results) {
                            $j("#slotSearchSubmit").removeAttr("disabled");
                            if( results.startsWith("Failure")) {
                                $j("#slotSearchError").text(results);
                            } else {
                                console.log(results);
                                // SlotSearchResult object
//                                var freezerData = JSON.parse(results);
                                $("#searchResultsAjax").html(results);
//                                $('#rackSelection').val(freezerData.rack.label);
//                                $('#freezerSelection').val(freezerData.locationTrail[0].label);
//                                $.each(freezerData.locationTrail.slice(1), function (idx, location) {
//
//                                    $('#freezerSelection').val(freezerData.locationTrail[0].label);
//                                });
                            }
                        },
                        error: function(results){
                            //TODO Handle errors
                            $j("#slotSearchSubmit").removeAttr("disabled");
                            $j("#rackScanError").text("A server error occurred");
                        },
                        cache: false,
                        datatype: "text",
                        processData: false,
                        contentType: false
                    });
                });
            });
        </script>
        <style type="text/css">
            .platemap {
                width: auto;
            }

            .platemap td {
                width: 35px;
            }

            .platemapcontainer {
                padding-top: 15px;
            }

            .sample {
                background-color: #d9edf7
            }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <%--Enter Vessel Barcode or Scan In if Rack--%>
        <stripes:form beanclass="${actionBean.class.name}" id="createStorageForm" class="form-horizontal">
            <stripes:hidden id="vesselBarcode" name="vesselBarcode" value=''/>
            <%--<input type="hidden" id="rackMap" name="rackMap" value="${actionBean.rackMap}">--%>
            <div class="control-group">
                <stripes:label for="vesselBarcode" name="Barcode" class="control-label"/>
                <div class="controls">
                    <stripes:text id="vesselBarcode" name="searchKey"/>
                    <stripes:submit name="vesselBarcodeSearch" value="Find" class="btn btn-primary"/>
                </div>
            </div>
            <div><h4>Rack Scan</h4></div>
            <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
            <stripes:submit name="fireRackScan" value="Scan" class="btn btn-primary"/>
        </stripes:form>

        <div id="searchResults">
            <c:if test="${actionBean.resultsAvailable}">
                <stripes:form beanclass="${actionBean.class.name}" id="manageStorageForm" class="form-horizontal">
                    <div id="platemap" class="platemapcontainer">
                        <div class="row">
                            <div class="span5">
                                <table class="platemap table table-bordered table-condensed">
                                    <tbody>
                                    <tr>
                                        <th colspan="${actionBean.vesselGeometry.columnCount + 1}"
                                            style="text-align: center">
                                            <stripes:label for="containerBarcode" class="control-label"/>
                                            <stripes:text id="containerBarcode" name="containerBarcode"/>
                                        </th>
                                    </tr>
                                    <tr>
                                        <th class="fit" ></th>
                                        <c:forEach items="${actionBean.vesselGeometry.columnNames}"
                                                   var="colName">
                                            <th class="fit">${colName}</th>
                                        </c:forEach>
                                    </tr>
                                    <c:forEach items="${actionBean.vesselGeometry.rowNames}"
                                               var="rowName">
                                        <tr>
                                            <th>${rowName}</th>
                                            <c:forEach items="${actionBean.vesselGeometry.columnNames}"
                                                       var="colName">
                                                <c:set var="wellName" value="${rowName.concat(colName)}" />
                                                <td id="$wellName"
                                                    class="${actionBean.rackScan.containsKey(wellName) ? 'sample' : ''}">
                                                </td>
                                            </c:forEach>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                    <div class="control-group">
                        <div id="slotSearchError" style="color:red"></div>
                        <stripes:label for="slotSearchKey" name="Slot" class="control-label"/>
                        <div class="controls">
                            <stripes:text id="slotSearchKey" name="slotSearchKey"/>
                            <stripes:submit id="slotSearchSubmit" name="slotBarcodeSearch" value="Find" class="btn btn-primary"/>
                        </div>
                    </div>
                    <div id="searchResultsAjax">
                        <div class="control-group">
                            <stripes:label for="rackSelect" class="control-label">Rack</stripes:label>
                            <div class="controls">
                                <stripes:select name="rackSelection" id="rackSelection">
                                    <c:forEach items="${actionBean.rackLocations}" var="entry">
                                        <option value="${entry.label}">${entry.label}</option>
                                    </c:forEach>
                                </stripes:select>
                            </div>
                        </div>
                        <div class="control-group">
                            <stripes:label for="freezerSelect" class="control-label">Freezer</stripes:label>
                            <div class="controls">
                                <stripes:select name="freezerSelection" id="freezerSelection">
                                    <c:forEach items="${actionBean.freezerLocations}" var="entry">
                                        <option value="${entry.label}">${entry.label}</option>
                                    </c:forEach>
                                </stripes:select>
                            </div>
                        </div>
                    </div>
                    <stripes:submit name="store" value="Store" class="btn btn-primary"/>
                </stripes:form>
            </c:if>
        </div>

    </stripes:layout-component>
</stripes:layout-render>