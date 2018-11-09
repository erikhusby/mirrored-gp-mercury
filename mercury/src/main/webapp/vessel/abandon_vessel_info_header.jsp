<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="abandonedVessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.abandonedvessels"--%>
    <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
    <input type="hidden" name="vesselLabel" id="vesselLabel"  value="${actionBean.barcode}">
    <div id="headerId" class="fourcolumn" style="padding: 0">
        <div>Vessel Type: ${vessel.type.name}</div>
        <div>Vessel Label: ${vessel.label}</div>
        <div>Create Date: <fmt:formatDate value="${vessel.createdOn}" pattern="${bean.datePattern}"/></div>
        <div>Abandon Reason: ${bean.abandonReason}</div>
        <div>Abandon Date: ${bean.abandonDate}</div>
    </div>
</stripes:layout-definition>
