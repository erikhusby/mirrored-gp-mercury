<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition" %>
<%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"/>
<%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
<style type="text/css">
    .jstree-anchor {
        /*enable wrapping*/
        white-space : normal !important;
        /*ensure lower nodes move down*/
        height : auto !important;
        /*offset icon width*/
        padding-right : 18px;
        font-size: 11pt;
    }
    label {
        display: inline;
        font-weight: bold;
    }
    input[type="text"].smalltext {
        width: 70px;
        font-size: 9pt;
        padding: 2px 2px;
    }
    input[type='text'].barcode {
        width: 100px;
        font-size: 9pt;
    }

    .top-buffer { margin-top:20px; }
</style>

<div class="row-fluid">
    <strong id="containerInfo">Vessel: ${actionBean.viewVessel.label}, Type: ${actionBean.containerTypeDisplayName}</strong>
    <c:if test="${actionBean.ajaxRequest and actionBean.isContainer()}">
        <a title="Click to Edit Container" class="pull-right"
           href="${ctxpath}/container/container.action?edit=&amp;containerBarcode=${actionBean.containerBarcode}">
            <span class="icon-pencil"></span>Edit Container</a>
    </c:if>
</div>
<stripes:form beanclass="${actionBean.class.name}"
              id="editContainerForm" class="form-horizontal">
    <c:set var="geometry" value="${actionBean.viewVessel.vesselGeometry}"/>
    <c:set var="canRackScan" value="${actionBean.editLayout and
                    actionBean.rackOfTubes.rackType.rackScannable}"/>
    <stripes:hidden name="containerBarcode" value="${actionBean.containerBarcode}"/>
    <input type="hidden" name="<csrf:tokenname/>" value="<csrf:tokenvalue/>"/>
    <%--Do not let the lab get away with hand scanning RackOfTubes that can be scanned by a flatbed--%>
    <c:if test="${canRackScan}">
        <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
        <div class="controls">
            <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                            name="rackScan"/>
        </div>
    </c:if>
    <c:if test="${actionBean.showLayout and not actionBean.isContainer()}"><%-- Simple layout for a cryostraw/slide --%>
        <input type="text"
               id="receptacleTypes[0].barcode" name="receptacleTypes[0].barcode"
               value="${actionBean.mapPositionToVessel[VesselPosition._1_1].label}"
               class="clearable smalltext unique" autocomplete="off"
               readonly><br/>${actionBean.mapPositionToSampleId[VesselPosition._1_1]}
        <input type="hidden"
               id="receptacleTypes[0].position"
               name="receptacleTypes[0].position"
               value="_1_1"/>
    </c:if>
    <c:if test="${actionBean.showLayout and empty actionBean.staticPlate and actionBean.isContainer()}"><%-- Layout for racks of tubes only --%>
        <table style="border-collapse: collapse; border: 1px solid black;">
            <c:forEach items="${geometry.rowNames}" var="rowName" varStatus="rowStatus">
                <c:if test="${rowStatus.first}">
                    <tr>
                        <td style="border: 1px solid black;"> </td>
                        <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                            <td style="text-align:center;border: 1px solid black;">${columnName}</td>
                        </c:forEach>
                    </tr>
                </c:if>
                <tr>
                    <td style="text-align:center; padding: 2px 8px 2px 8px; border: 1px solid black;">${rowName}</td>
                    <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                        <c:set var="receptacleIndex"
                               value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                        <td style="text-align:center; vertical-align: top; border: 1px solid #888888; width: 80px;">
                            <c:if test="${empty rowName}">${geometry.vesselPositions[receptacleIndex]}</c:if>
                            <input type="text"
                                   id="receptacleTypes[${receptacleIndex}].barcode"
                                   name="receptacleTypes[${receptacleIndex}].barcode"
                                   value="${actionBean.mapPositionToVessel[geometry.vesselPositions[receptacleIndex]].label}"
                                   class="clearable smalltext unique" autocomplete="off"
                                   <c:if test="${actionBean.editLayout}">placeholder="barcode"</c:if>
                                   <c:if test="${not actionBean.editLayout or canRackScan}">readonly</c:if>/><c:if test="${not empty rowName}"><br/>${actionBean.mapPositionToSampleId[geometry.vesselPositions[receptacleIndex]]}</c:if>
                            <input type="hidden"
                                   id="receptacleTypes[${receptacleIndex}].position"
                                   name="receptacleTypes[${receptacleIndex}].position"
                                   value="${geometry.vesselPositions[receptacleIndex]}"/>
                        </td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </table>
        <c:if test="${actionBean.editLayout}">
            <div class="control-group top-buffer">
                <div class="controls">
                    <stripes:submit id="saveLayout" name="save" value="Update Layout" class="btn btn-primary"/>
                    <stripes:submit id="cancelUpdateLayout" name="cancel" value="Cancel" class="btn"/>
                </div>
            </div>
        </c:if>
    </c:if>
    <c:if test="${!actionBean.editLayout}">
        <fieldset>
            <legend>Manage Storage Location</legend>
            <div class="control-group">
                <stripes:label for="storageName" class="control-label"/>
                <div class="controls">
                    <stripes:hidden id="storageId" name="storageId"/>
                    <stripes:hidden id="containerBarcode" name="containerBarcode"/>
                    <%-- input type="hidden" name="csrf:tokenname" value="csrf:tokenvalue" Relocated above - remove this from (hopefully) develop merge conflict --%>
                    <enhance:out escapeXml='false'><stripes:text id="storageName" name="storageName" value="${actionBean.locationTrail}" readonly="true" style="width:${empty actionBean.locationTrail ? 200 : actionBean.locationTrail.length() * 8}px"/></enhance:out>
                    <c:if test="${not empty actionBean.staticPlate or (actionBean.showLayout && !actionBean.editLayout)}">
                        <stripes:submit name="browse" id="browse" value="Browse"
                                        class="btn"/>
                        <stripes:submit id="saveStorageLocation" name="saveLocation" value="Save To Location"
                                        class="btn btn-primary"/>
                    </c:if>
                </div>
            </div>
            <c:if test="${not empty actionBean.storageLocation and (not empty actionBean.staticPlate or actionBean.showLayout)
                          and !actionBean.editLayout}">
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit id="removeFromStorage" name="removeContainerLoc" value="Remove From Storage"
                                        class="btn btn-danger"/>
                    </div>
                </div>
            </c:if>
        </fieldset>
    </c:if>
</stripes:form>
