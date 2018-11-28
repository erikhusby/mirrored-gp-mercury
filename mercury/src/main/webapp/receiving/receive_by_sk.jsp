<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receive By SK-ID" sectionTitle="Receive By SK-ID">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#samplesTable').DataTable({
                    "oTableTools": {
                        "aButtons": ["copy", "csv"]
                    },
                    "aoColumns": [
                        {"bSortable": false},
                        {"bSortable": true} ,
                        {"bSortable": true} ,
                        {"bSortable": true} ,
                        {"bSortable": true}
                    ]
                });

                $j('.sample-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'sample-checkAll',
                    countDisplayClass:'sample-checkedCount',
                    checkboxClass:'sample-checkbox'});

            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}"
                      id="rackScanForm" class="form-horizontal">
            <div class="control-group">
                <stripes:label for="rackBarcode" name="Sample Kit Barcode" class="control-label"/>
                <div class="controls">
                    <input type="text" id="rackBarcode" autocomplete="off" name="rackBarcode" value="${actionBean.rackBarcode}"
                           class="clearable barcode unique" required="" aria-required="true">
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit value="Search" id="scanBtn" class="btn btn-primary"
                                    name="findSkId"/>
                </div>
            </div>
        </stripes:form>
        <c:if test="${actionBean.showLayout}">
            <stripes:form beanclass="${actionBean.class.name}"
                          id="showScanForm" class="form-horizontal">
                <stripes:hidden name="rackBarcode" value="${actionBean.rackBarcode}"/>
                <stripes:hidden name="isPlate" value="${actionBean.sampleKitInfo.plate}"/>
                <table id="samplesTable" class="sample-checkbox table simple">
                    <thead>
                    <tr>
                        <th width="30px">
                            <input type="checkbox" class="sample-checkAll" title="Check All"/>
                            <span id="count" class="samples-checkedCount"></span>
                        </th>
                        <th>Sample Info</th>
                        <th>Sample Kit</th>
                        <th>Status</th>
                        <th>Material Type</th>
                        <th>Original Material Type</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.sampleRows}" var="sampleData">
                        <tr class="sample-row">
                            <td>
                                <stripes:checkbox class="sample-checkbox" name="selectedSampleIds"
                                                  value="${sampleData.sampleId}"/>
                            </td>
                            <td>${sampleData.sampleId}</td>
                            <td>${sampleData.sampleKitId}</td>
                            <td>${sampleData.sampleStatus}</td>
                            <td>${sampleData.materialType}</td>
                            <td>${sampleData.originalMaterialType}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <br/>
                <stripes:submit id="receiveToBsp" name="receiveBySkToBsp" value="Receive To BSP"
                                class="btn btn-primary"/>
            </stripes:form>
        </c:if>
    </stripes:layout-component>

</stripes:layout-render>