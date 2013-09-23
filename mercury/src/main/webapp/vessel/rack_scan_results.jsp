<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:messages />
<stripes:errors />

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean"--%>

<div class="rackScanResults">
    <c:forEach items="${actionBean.matrixPositions}" var="position">
        Position: ${position} &#160; &#160; Barcode: ${actionBean.rackScan[position]} <br />
    </c:forEach>
</div>

