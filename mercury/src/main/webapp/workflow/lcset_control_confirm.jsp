<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LcsetActionBean" %>
<%--
This fragment is used by lcset_controls.jsp to re-use code when displaying the confirm controls labels
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LcsetActionBean"--%>
<%--@elvariable id="batchName" type="java.lang.String"--%>

Click Confirm to make the following changes:<br/>
<c:if test="${not empty actionBean.controlBarcodes}">
    Add the following controls to the Lab Batch: ${batchName}
    <c:forEach items="${actionBean.controlBarcodes}" var="controlBarcode" varStatus="loop">
        <div>
                ${controlBarcode}
            <input type="hidden" name="controlBarcodes[${loop.index}]" value="${controlBarcode}"/>
        </div>
    </c:forEach>
</c:if>
<c:if test="${not empty actionBean.addBarcodes}">
    Add the following samples to the Lab Batch: ${batchName}
    <c:forEach items="${actionBean.addBarcodes}" var="addBarcode" varStatus="loop">
        <div>
                ${addBarcode}
            <input type="hidden" name="addBarcodes[${loop.index}]" value="${addBarcode}"/>
        </div>
    </c:forEach>
</c:if>
<c:if test="${not empty actionBean.removeBarcodes}">
    Remove the following samples from the Lab Batch: ${batchName}, and place them in the bucket:
    <c:forEach items="${actionBean.removeBarcodes}" var="removeBarcode" varStatus="loop">
        <div>
                ${removeBarcode}
            <input type="hidden" name="removeBarcodes[${loop.index}]" value="${removeBarcode}"/>
        </div>
    </c:forEach>
</c:if>