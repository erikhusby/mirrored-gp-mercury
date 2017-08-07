<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageLocationActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Edit Storage" sectionTitle="Edit Storage: ${actionBean.storageLocation.label}">
    <stripes:layout-component name="content">
        <div class="container-fluid">
            <stripes:form beanclass="${actionBean.class.name}" class="form-horizontal">
                <c:set var="storageId" value="${actionBean.storageId}"/>
                <stripes:hidden name="storageId" value="${actionBean.storageId}"/>
                <c:forEach items="${actionBean.mapIdToStorageLocation}" var="entry" varStatus="rowStatus">
                    <div class="control-group">
                        <stripes:label for="${entry.value.label}" class="control-label" >
                            ${entry.value.label}
                        </stripes:label>
                        <div class="controls">
                            <input type="text" id="${entry.value.label}"
                                   name="mapIdToStorageLocation[${entry.key}].barcode" placeholder="barcode"
                                   value="${entry.value.barcode}"/>
                        </div>
                    </div>
                </c:forEach>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit id="saveStorageBarcodes" name="saveStorageBarcodes" value="Save"
                                        class="btn btn-primary"/>
                    </div>
                </div>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>