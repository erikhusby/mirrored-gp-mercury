<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receive By Sample Scan" sectionTitle="Receive DDP Samples">

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
                <stripes:label for="ddpBarcodes" name="Manufacturer Barcodes" class="control-label"/>
                <div class="controls">
                    <stripes:textarea id="ddpBarcodes" name="ddpBarcodes"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit value="Search" id="scanBtn" class="btn btn-primary"
                                    name="findByDdpScan"/>
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
                            <span id="count" class="sample-checkedCount"></span>
                        </th>
                        <th>Barcode</th>
                        <th>Collaborator Sample ID</th>
                        <th>Collaborator Participant ID</th>
                        <th>Gender</th>
                        <th>Material Type</th>
                        <th>Sample Collection</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.ddpKitInfos}" var="sampleData" varStatus="rowStatus">
                        <c:set var="receptacleIndex" value="${rowStatus.index}"/>
                        <tr class="sample-row">
                            <td>
                                    <stripes:checkbox class="sample-checkbox" name="selectedSampleIds"
                                                      value="${sampleData.manufacturerBarcode}"/>
                            <td>${sampleData.manufacturerBarcode}
                                <stripes:hidden name="ddpKitInfos[${receptacleIndex}].manufacturerBarcode" value="${sampleData.manufacturerBarcode}"/>
                            </td>
                            <td>${sampleData.collaboratorSampleId}
                                <stripes:hidden name="ddpKitInfos[${receptacleIndex}].collaboratorSampleId" value="${sampleData.collaboratorSampleId}"/>
                            </td>
                            <td>${sampleData.collaboratorParticipantId}
                                <stripes:hidden name="ddpKitInfos[${receptacleIndex}].collaboratorParticipantId" value="${sampleData.collaboratorParticipantId}"/>
                            </td>
                            <td>${sampleData.gender}
                                <stripes:hidden name="ddpKitInfos[${receptacleIndex}].gender" value="${sampleData.gender}"/>
                            </td>
                            <td>${sampleData.materialInfo}
                                <stripes:hidden name="ddpKitInfos[${receptacleIndex}].materialInfo" value="${sampleData.materialInfo}"/>
                            </td>
                            <td>${sampleData.sampleCollectionBarcode}
                                <stripes:hidden name="ddpKitInfos[${receptacleIndex}].sampleCollectionBarcode" value="${sampleData.sampleCollectionBarcode}"/>
                            </td>
                            <stripes:hidden name="ddpKitInfos[${receptacleIndex}].receptacleName" value="${sampleData.receptacleName}"/>
                            <stripes:hidden name="ddpKitInfos[${receptacleIndex}].organismClassificationId" value="${sampleData.organismClassificationId}"/>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <br/>
                <stripes:submit id="receiveToMercury" name="receiveDdpSampleToMercury" value="Receive To Mercury"
                                class="btn btn-primary"/>
            </stripes:form>
        </c:if>
    </stripes:layout-component>

</stripes:layout-render>