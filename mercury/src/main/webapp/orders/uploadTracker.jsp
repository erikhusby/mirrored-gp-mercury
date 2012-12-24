<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.UploadTrackerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Upload Tracker" sectionTitle="Upload Billing Tracker Spreadsheet">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#productOrderList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[1,'asc']],
                    "aoColumns": [
                        {"bSortable": false},                   // checkbox
                        {"bSortable": true, "sType": "html"},   // Name
                        {"bSortable": true, "sType": "html"},   // ID
                        {"bSortable": true},                    // Product
                        {"bSortable": true},                    // Product Family
                        {"bSortable": false},                   // Status
                        {"bSortable": true},                    // Research Project
                        {"bSortable": true},                    // Owner
                        {"bSortable": true, "sType": "date"},   // Updated
                        {"bSortable": false},                   // Count
                        {"bSortable": false},                   // Billing Session ID
                        {"bSortable": false}]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">

            <input type="hidden" name="previewFilePath" value="${actionBean.previewFilePath}"/>

            <p class="help-block">
                Choose an updated billing tracker spreadsheet to import and then click preview. You will be presented with
                a summary preview to review the data to be imported and if all looks good, you can click upload to
                enter all information into the billing ledger.
            </p>

            <div class="control-group">
                <stripes:label for="trackerFile" class="control-label">Tracker File</stripes:label>

                <div class="controls">
                    <stripes:file id="trackerFile" name="trackerFile" title="Tracker File"/><br/>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <stripes:submit name="preview" value="Preview Tracker"/>
                </div>
            </div>

            <c:if test="${actionBean.isPreview}">
                <div class="borderHeader">
                    Summary of Tracker
                    <stripes:submit class="actionButtons" style="margin-left:20px;" name="upload" value="Do Upload"/>
                </div>

                <table id="uploadPreviewTable" class="table simple">
                    <thead>
                        <tr>
                            <th>Order Id</th>
                            <th>Product Part Number</th>
                            <th>Price Item Name</th>
                            <th>Total New Charges</th>
                            <th>Total New Credits</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${actionBean.previewData}" var="data">
                            <tr>
                                <td>${data.order}</td>
                                <td>${data.partNumber}</td>
                                <td>${data.priceItemName}</td>
                                <td>${data.newCharges}</td>
                                <td>${data.newCredits}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:if>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
