<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receive Ext. Matrix" sectionTitle="Receive By External Matrix Tubes">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>

        <style type="text/css">
            .form-inline > * {
                margin:5px 3px !important;
            }

            .form-inline > .left-spacer {
                margin:5px 10px 2px 50px !important;
            }

            label {
                display: inline;
                font-weight: bold;
            }
            input[type="text"].smalltext {
                width: 70px;
                font-size: 12px;
                padding: 2px 2px;
            }
            input[type='text'].barcode {
                width: 100px;
                font-size: 12px;
            }
        </style>
        <script src="${ctxpath}/resources/scripts/receive_external_shared.js"></script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}"
                      id="kitInfoForm">
            <c:set var="isMatrixTubes" value="${true}" scope="request"/>
            <jsp:include page="receive_ext_kit_info.jsp"/>
            <fieldset class="form-inline">
                <legend>Scan To Receive</legend>
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                                        name="rackScanExternal"/>
                    </div>
                </div>
            </fieldset>

        </stripes:form>

        <c:if test="${actionBean.showLayout}">
            <stripes:form beanclass="${actionBean.class.name}" id="sampleDataForm">
                <table id="matrixSamplesTable" class="table simple">
                    <thead>
                    <tr>
                        <th>Position</th>
                        <th>Barcode</th>
                        <th>Organism</th>
                        <th>Material Type</th>
                        <th>Original Material Type</th>
                        <th>Format Type</th>
                    </tr>
                    </thead>
                    <tbody>
                    </tbody>
                    <c:forEach items="${actionBean.externalSamplesRequest.externalSampleContents}" var="item" varStatus="index">
                        <tr class="sample-row">
                            <td>${item.position}
                                <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].position" value="${item.position}"/>
                            </td>
                            <td>${item.barcode}
                                <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].barcode" value="${item.barcode}"/>
                            </td>
                            <td>${item.organism}
                                <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].organismId" value="${item.organismId}"/>
                                <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].organism" value="${item.organism}"/>
                            </td>
                            <td>${item.materialType}
                                <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].materialType" value="${item.materialType}"/>
                            </td>
                            <td>${item.originalMaterialType}
                                <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].originalMaterialType" value="${item.originalMaterialType}"/>
                            </td>
                            <td>${item.formatType}
                                <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].formatType" value="${item.formatType}"/>
                            </td>
                            <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].labelFormat" value="${item.labelFormat}"/>
                            <stripes:hidden name="externalSamplesRequest.externalSampleContents[${index.index}].receptacleType" value="${item.receptacleType}"/>
                        </tr>
                    </c:forEach>
                </table>
                <stripes:hidden name="externalSamplesRequest.shippingMethodType" value="${actionBean.externalSamplesRequest.shippingMethodType}"/>
                <stripes:hidden name="externalSamplesRequest.shippingNotes" value="${actionBean.externalSamplesRequest.shippingNotes}"/>
                <stripes:hidden name="externalSamplesRequest.conditionShipment" value="${actionBean.externalSamplesRequest.conditionShipment}"/>
                <stripes:hidden name="externalSamplesRequest.collectionId" value="${actionBean.externalSamplesRequest.collectionId}"/>
                <stripes:hidden name="externalSamplesRequest.siteId" value="${actionBean.externalSamplesRequest.siteId}"/>
                <stripes:hidden name="showLayout" value="${actionBean.showLayout}"/>

                <stripes:label for="manifestFile" class="control-label">
                    Manifest Spreadsheet
                </stripes:label>
                <div class="controls">
                    <stripes:file name="manifestFile" id="manifestFile"/>
                </div>
                <stripes:submit id="saveMatrixKit" name="saveMatrixKit" value="Save Kit"
                                class="btn btn-primary"/>
            </stripes:form>
        </c:if>

    </stripes:layout-component>

</stripes:layout-render>