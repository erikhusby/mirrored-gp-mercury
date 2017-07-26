<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
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
        padding-right : 24px;
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

    .top-buffer { margin-top:20px; }
</style>
<script src="${ctxpath}/resources/scripts/storage-location-ajax.js"></script>

<div id="storage_location_overlay">
    <div class="alert" id="error-dialog-ajax">
        <button type="button" class="close" data-dismiss="alert">&times;</button>
        <span id="error-text-ajax">defaul error message.</span>
    </div>
    <div class="control-group">
        <div class="control">
            <input type="text" id="searchTermAjax" name="searchTerm" placeholder="storage barcode"/>
            <input type="submit" value="Find" id="searchTermAjaxSubmit"/>
        </div>
    </div>

    <div id="ajax-jstree"></div>
</div>
<div class="row-fluid">
    <strong>Container ${actionBean.viewVessel.label} Type: ${actionBean.containerTypeDisplayName}</strong>
    <c:if test="${actionBean.ajaxRequest}">
        <a title="Click to Edit Container" class="pull-right"
           href="${ctxpath}/container/container.action?edit=&amp;containerBarcode=${actionBean.containerBarcode}">
            <span class="icon-pencil"></span>Edit Container</a>
    </c:if>
</div>
<stripes:form beanclass="${actionBean.class.name}"
              id="editContainerForm" class="form-horizontal">
    <c:set var="geometry" value="${actionBean.viewVessel.vesselGeometry}"/>
    <c:set var="canRackScan" value="${actionBean.editLayout and
                    actionBean.rackOfTubes.rackType.displayName.startsWith('Matrix96')}"/>
    <stripes:hidden name="containerBarcode" value="${actionBean.containerBarcode}"/>
    <%--Do not let the lab get away with hand scanning RackOfTubes that can be scanned by a flatbed--%>
    <c:if test="${canRackScan}">
        <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
        <div class="controls">
            <stripes:submit value="Scan" id="scanBtn" class="btn btn-primary"
                            name="rackScan"/>
        </div>
    </c:if>
    <table>
        <c:forEach items="${geometry.rowNames}" var="rowName" varStatus="rowStatus">
            <c:if test="${rowStatus.first}">
                <tr>
                    <td></td>
                    <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                        <td>${columnName}</td>
                    </c:forEach>
                </tr>
            </c:if>
            <tr>
                <td>${rowName}</td>
                <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                    <c:set var="receptacleIndex"
                           value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                    <td align="right">
                        <c:if test="${empty rowName}">${geometry.vesselPositions[receptacleIndex]}</c:if>
                        <input type="text"
                               name="receptacleTypes[${receptacleIndex}].barcode"
                               value="${actionBean.mapPositionToVessel[geometry.vesselPositions[receptacleIndex]].label}"
                               class="clearable smalltext unique" autocomplete="off"
                               <c:if test="${actionBean.editLayout}">placeholder="barcode"</c:if>
                               <c:if test="${not actionBean.editLayout or canRackScan}">readonly</c:if>/>
                        <input type="hidden"
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
                <stripes:submit name="save" value="Update Layout" class="btn btn-primary"/>
                <stripes:submit name="cancel" value="Cancel" class="btn"/>
            </div>
        </div>
    </c:if>
    <fieldset>
        <legend>Manage Storage Location</legend>
        <div class="control-group">
            <stripes:label for="storageName" class="control-label"/>
            <div class="controls">
                <stripes:hidden id="storageId" name="storageId"/>
                <stripes:hidden id="containerBarcode" name="containerBarcode"/>
                <stripes:text id="storageName" name="storageName" value="${actionBean.locationTrail}" readonly="true"/>
                <stripes:submit name="browse" id="browse" value="Browse"
                                class="btn"/>
                <stripes:submit name="saveLocation" value="Save To Location"
                                class="btn btn-primary"/>
            </div>
        </div>
        <c:if test="${not empty actionBean.storageLocation}">
            <div class="control-group">
                <div class="controls">
                    <stripes:submit name="removeLocation" value="Remove From Storage"
                                    class="btn btn-danger"/>
                </div>
            </div>
        </c:if>
    </fieldset>
</stripes:form>
