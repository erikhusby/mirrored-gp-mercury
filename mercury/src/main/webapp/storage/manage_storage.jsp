<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageManagerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create Storage" sectionTitle="Create Storage">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
        <script type="text/javascript">
            $j(document).ready(function () {

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
                                //TODO Handle successful results
                            }
                        },
                        error: function(results){
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
                <stripes:form beanclass="${actionBean.class.name}" id="createStorageForm" class="form-horizontal">
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
                                                <td id="${rowName}${colName}"
                                                    class="${actionBean.hasSample(rowName, colName) ? 'sample' : ''}">
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
                </stripes:form>
            </c:if>
        </div>

    </stripes:layout-component>
</stripes:layout-render>