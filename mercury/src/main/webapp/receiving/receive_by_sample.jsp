<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receive By Sample Scan" sectionTitle="Receive By Sample Scan">

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
                        {"bSortable": true},
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
                      id="sampleInputForm" class="form-horizontal">
            <div class="control-group">
                <stripes:label for="sampleIds" name="Sample Ids" class="control-label"/>
                <div class="controls">
                    <stripes:textarea id="sampleIds" name="sampleIds"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit value="Search" id="scanBtn" class="btn btn-primary"
                                    name="findBySampleScan"/>
                </div>
            </div>
        </stripes:form>
        <c:if test="${actionBean.showLayout}">
            <stripes:form beanclass="${actionBean.class.name}"
                          id="showScanForm" class="form-horizontal">
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
                            <td>${sampleData.sampleId}</td>
                            <td>${sampleData.sampleKitId}</td>
                            <td>${sampleData.sampleStatus}</td>
                            <td>${sampleData.materialType}</td>
                            <td>${sampleData.originalMaterialType}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <stripes:submit id="receiveToBsp" name="receiveBySampleToBsp" value="Receive To BSP"
                                class="btn btn-primary"/>
            </stripes:form>
        </c:if>
    </stripes:layout-component>

</stripes:layout-render>