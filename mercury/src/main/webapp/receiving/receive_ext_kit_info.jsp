<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>


<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean"--%>
<%--@elvariable id="isMatrixTubes" type="java.lang.Boolean"--%>

<fieldset class="form-horizontal">
    <legend>Kit Creation Info</legend>
    <div class="control-group">
        <stripes:label for="group" name="Group" class="control-label"/>
        <div class="controls">
            <stripes:select name="selectedGroup" id="group">
                <stripes:options-collection collection="${actionBean.mapGroupToCollection.keySet()}"
                                            label="groupName" value="groupName"/>
            </stripes:select>
        </div>
    </div>

    <div id="collectionDiv">
        <jsp:include page="collection_select.jsp"/>
    </div>

    <div id="siteDiv">
        <jsp:include page="site_select.jsp"/>
    </div>

    <div class="control-group">
        <stripes:label for="shippingMethodType" name="shippingMethodType" class="control-label"/>
        <div class="controls">
            <stripes:select name="externalSamplesRequest.shippingMethodType" id="shippingMethodType">
                <stripes:options-enumeration
                        enum="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean.ShippingType"
                        label="displayName"/>
            </stripes:select>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="trackingNumber" name="Tracking Number" class="control-label"/>
        <div class="controls">
            <stripes:text name="externalSamplesRequest.trackingNumber" id="trackingNumber" />
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="shippingNotes" name="Shipping Notes" class="control-label"/>
        <div class="controls">
            <stripes:text name="externalSamplesRequest.shippingNotes" id="shippingNotes" />
        </div>
    </div>

    <div id="organismDiv">
        <jsp:include page="organism_select.jsp"/>
    </div>


    <div class="control-group">
        <stripes:label for="barcodedTubeType" name="barcodedTubeType" class="control-label"/>
        <div class="controls">
            <stripes:select name="externalSamplesRequest.receptacleType" id="receptacleType">
                <c:choose>
                    <c:when test="${isMatrixTubes}">
                        <c:forEach var="tubeType" items="${actionBean.matrixTubeTypes}" varStatus="idx">
                            <option value="${tubeType.getAutomationName()}">
                                    ${tubeType.getDisplayName()}
                            </option>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <stripes:options-enumeration
                                enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube.BarcodedTubeType"
                                label="displayName"/>
                    </c:otherwise>
                </c:choose>

            </stripes:select>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="labelFormat" name="labelFormat" class="control-label"/>
        <div class="controls">
            <stripes:select name="externalSamplesRequest.labelFormat" id="labelFormat">
                <option value="">Select One:</option>
            </stripes:select>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="materialType" name="materialType" class="control-label"/>
        <div class="controls">
            <stripes:select name="externalSamplesRequest.materialType" id="materialType">
                <stripes:options-collection collection="${MaterialType.getBspMaterialTypes()}"
                                            label="displayName" value="displayName"/>
            </stripes:select>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="originalMaterialType" name="originalMaterialType" class="control-label"/>
        <div class="controls">
            <stripes:select name="externalSamplesRequest.originalMaterialType" id="originalMaterialType">
                <stripes:options-collection collection="${MaterialType.getBspMaterialTypes()}"
                                            label="displayName" value="displayName"/>
            </stripes:select>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="formatType" name="formatType" class="control-label"/>
        <div class="controls">
            <select id="formatType" name="externalSamplesRequest.formatType">
                <option value="">Select One:</option>
                <option value="FROZEN">FROZEN</option>
                <option value="UNSPECIFIED">UNSPECIFIED</option>
            </select>
        </div>
    </div>

</fieldset>