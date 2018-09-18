<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceivingActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receive By Sample Scan And Link" sectionTitle="Receive By Sample Scan And Link">

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
                <c:if test="${not empty actionBean.mapSampleToCollaborator}">
                    <c:forEach items="${actionBean.mapSampleToCollaborator}" var="entry">
                        <stripes:hidden name="mapSampleToCollaborator[${entry.key}]" value="${entry.value}"/>
                    </c:forEach>
                </c:if>
                <c:if test="${not empty actionBean.sampleCollaboratorRows}">
                    <c:forEach items="${actionBean.sampleCollaboratorRows}" var="row"  varStatus="rowStatus">
                        <c:forEach items="${row}" var="entry">
                            <stripes:hidden
                                    name="sampleCollaboratorRows[${rowStatus.index}][${entry.key}]"
                                    value="${entry.value}"/>
                        </c:forEach>
                    </c:forEach>
                </c:if>
                <stripes:label for="sampleId" name="Sample Id" class="control-label"/>
                <div class="controls">
                    <input type="text" id="sampleId" autocomplete="off" name="sampleIds" value="${actionBean.rackBarcode}"
                           class="clearable barcode unique" required="" aria-required="true">
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="collaboratorSampleId" name="Collaborator Sample Id" class="control-label"/>
                <div class="controls">
                    <input type="text" id="collaboratorSampleId" autocomplete="off" name="collaboratorSampleId"
                           class="clearable barcode unique" required="" aria-required="true">
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit value="Add" id="scanBtn" class="btn btn-primary"
                                    name="findCollaborator"/>
                </div>
            </div>
        </stripes:form>
        <c:if test="${not empty actionBean.sampleCollaboratorRows}">
            <stripes:form beanclass="${actionBean.class.name}"
                          id="showScanForm" class="form-horizontal">
                <c:if test="${not empty actionBean.mapSampleToCollaborator}">
                    <c:forEach items="${actionBean.mapSampleToCollaborator}" var="entry">
                        <stripes:hidden name="mapSampleToCollaborator[${entry.key}]" value="${entry.value}"/>
                    </c:forEach>
                </c:if>
                <table id="samplesTable" class="sample-checkbox table simple">
                    <thead>
                    <tr>
                        <th width="30px">
                            <input type="checkbox" class="sample-checkAll" title="Check All"/>
                            <span id="count" class="samples-checkedCount"></span>
                        </th>
                        <th>Sample Info</th>
                        <th>Collaborator Sample ID</th>
                        <th>Sample Kit</th>
                        <th>Status</th>
                        <th>Material Type</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.sampleCollaboratorRows}" var="sampleData" varStatus="status">
                        <tr class="sample-row">
                            <td>
                                <stripes:checkbox class="sample-checkbox" name="selectedSampleIds"
                                                  value="${sampleData['SAMPLE_ID']}"/>
                                <stripes:hidden name="allSampleIds[${status.index}]" value="${sampleData['SAMPLE_ID']}"/>
                            </td>
                            <td>
                                    ${sampleData['SAMPLE_ID']}
                                    <stripes:hidden name="sampleCollaboratorRows[${status.index}]['SAMPLE_ID']"
                                                    value="${sampleData['SAMPLE_ID']}"/>
                            </td>
                            <td>
                                    ${sampleData['COLLABORATOR_SAMPLE_ID']}
                                    <stripes:hidden name="sampleCollaboratorRows[${status.index}]['COLLABORATOR_SAMPLE_ID']"
                                                        value="${sampleData['COLLABORATOR_SAMPLE_ID']}"/>
                            </td>
                            <td>
                                    ${sampleData['SAMPLE_KIT']}
                                    <stripes:hidden name="sampleCollaboratorRows[${status.index}]['SAMPLE_KIT']"
                                                        value="${sampleData['SAMPLE_KIT']}"/>
                            </td>
                            <td>
                                    ${sampleData['SAMPLE_STATUS']}
                                    <stripes:hidden name="sampleCollaboratorRows[${status.index}]['SAMPLE_STATUS']"
                                                        value="${sampleData['SAMPLE_STATUS']}"/>
                            </td>
                            <td>
                                    ${sampleData['ORIGINAL_MATERIAL_TYPE']}
                                    <stripes:hidden name="sampleCollaboratorRows[${status.index}]['ORIGINAL_MATERIAL_TYPE']"
                                                        value="${sampleData['ORIGINAL_MATERIAL_TYPE']}"/>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit id="receiveByScanAndLink" name="receiveByScanAndLink" value="Receive To BSP"
                                        class="btn btn-primary"/>
                    </div>
                </div>
            </stripes:form>
        </c:if>
    </stripes:layout-component>

</stripes:layout-render>