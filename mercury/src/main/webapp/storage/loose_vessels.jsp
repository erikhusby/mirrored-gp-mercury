<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%-- This element cycles itself within '#replaceMeWithStorageContents' element --%>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"/>
<style type="text/css">
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

<p id="looseMessages">
    <stripes:errors/>
    <stripes:messages/>
</p>
<c:set var="vesselCount"
       value="${actionBean.looseTubes.size()}"/>
<div class="row-fluid">
    <h4 id="containerInfo">(${vesselCount}) Loose Vessels<br/>${actionBean.locationTrail}</h4>
</div>

<stripes:form beanclass="${actionBean.class.name}"
              id="looseVesselForm" class="form-horizontal">
    <stripes:hidden id="storageId" name="storageId"/>
    <input type="hidden" name="<csrf:tokenname/>" value="<csrf:tokenvalue/>"/>

    <table style="border-collapse: collapse; border: 1px solid black;width:450px;"><%-- 3 columns to a row --%><c:set var="col" value="${1}"/>
        <c:forEach items="${actionBean.looseTubes}" var="vessel" varStatus="vesselStatus">
            <c:if test="${ col == 1 }"><tr></c:if>
            <td style="padding-left:20px; vertical-align: top; border: 1px solid #888888; width: 80px;">&nbsp;<input type="checkbox" name="selectedLooseVessels" value="${vessel.label}" style="float: unset;"/>&nbsp;${vessel.label}<br/>${actionBean.mapBarcodeToSampleId[vessel.label]}</td>
            <c:set var="col" value="${col + 1}"/>
            <c:if test="${ col == 4 }"></tr><c:set var="col" value="${1}"/></c:if>
        </c:forEach>
        <c:if test="${ vesselCount > 0 and col > 1 and col < 4 }">
        <c:forEach begin="${col}" end="3"><td style="text-align:center; vertical-align: top; border: 1px solid #888888; width: 80px;">&nbsp;</td></c:forEach></tr></c:if>
    </table>
    <c:if test="${!actionBean.editLayout}">
        <fieldset>
            <legend>Manage Storage Location</legend>
            <div class="control-group">
                <stripes:label for="storageName" class="control-label"/>
                <div class="controls">
                    <enhance:out escapeXml="false"><stripes:text id="storageName" name="storageName" value="${actionBean.locationTrail}" readonly="true" style="width:${empty actionBean.locationTrail ? 200 : actionBean.locationTrail.length() * 8}px"/></enhance:out>
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
                        <input type="button" id="removeFromStorage" value="Remove From Storage"
                                        class="btn btn-danger" onclick="removeLooseVessels();"/>
                    </div>
                </div>
            </c:if>
        </fieldset>
    </c:if>
</stripes:form>


