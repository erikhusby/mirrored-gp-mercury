<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.PickerActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Create Picker CSV"
                       sectionTitle="Create Picker CSV" showCreate="false" dataTablesVersion="1.10">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#labVesselResultsTable').dataTable({
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




                ({
                    "oTableTools": {
                        "sSwfPath": "/Mercury/resources/scripts/DataTables-1.9.4/extras/TableTools/media/swf/copy_csv_xls.swf",
                        "aButtons": [
                            {
                                "sExtends" : "csv",
                                "bHeader" : false,
                                "sFieldBoundary": "",
                                "mColumns": [ 1, 2, 3, 4, 5 ]
                            }
                        ]
                    },
                    "aoColumnDefs" : [
                        { "bVisible": false, "aTargets": [ 0 ] },
                        { "bSortable": false, "aTargets": "no-sort" },
                        { "bSortable": true, "sType": "numeric", "aTargets": "sort-numeric" }
                    ]
                });
            });


        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <p/>
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="searchType" class="control-label">Search Type</stripes:label>
                    <div class="controls">
                        <stripes:select name="searchType" id="searchType">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.presentation.vessel.PickerActionBean.SearchType"
                                    label="displayName"/>
                        </stripes:select>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="pickerIdentifiersText" name="Search Terms" class="control-label"/>
                    <div class="controls">
                        <textarea name="barcodes" id="pickerIdentifiersText" rows="8">${actionBean.barcodes}</textarea>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="search" value="Search" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>

        <c:if test="${!actionBean.hasErrors() && actionBean.resultList != null}">
                <table class="table simple">
                    <thead>
                    <tr>
                        <th>Unique Storage Locations</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.storageLocations}" var="storageLocation">
                        <tr>
                            <td>${storageLocation}</td>
                        </tr>
                    </c:forEach>

                    </tbody>
                </table>

        <c:if test="${not empty actionBean.resultList.resultRows}">
            <div style="margin-top: 50px;">
                <stripes:layout-render name="/columns/configurable_list.jsp"
                                       entityName="${actionBean.entityName}"
                                       sessionKey="${actionBean.sessionKey}"
                                       columnSetName="${actionBean.columnSetName}"
                                       downloadColumnSets="${actionBean.downloadColumnSets}"
                                       resultList="${actionBean.resultList}"
                                       action="${ctxpath}/search/ConfigurableSearch.action"
                                       downloadViewedColumns="false"
                                       isDbSortAllowed="false"
                                       dbSortPath=""
                                       dataTable="true"
                                       loadDatatable="false"
                                       showJumpToEnd="false"
                                        />
            </div>
        </c:if>
            <c:if test="${not empty actionBean.unpickableBarcodes}">
                <div style="margin-top: 50px;">
                    <table class="table simple">
                        <thead>
                        <tr>
                            <th>Vessels that cannot be picked on the XL20</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.unpickableBarcodes}" var="unpicakables">
                            <tr>
                                <td>${unpicakables}</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>

        </c:if>
    </stripes:layout-component>
</stripes:layout-render>
