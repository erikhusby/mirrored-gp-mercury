<%@ include file="/resources/layout/taglibs.jsp" %>

<c:choose>
    <c:when test="${fn:length(actionBean.editOrder.regulatoryInfos) ne 0}">
        <c:forEach var="regulatoryInfo" items="${actionBean.editOrder.regulatoryInfos}">
            ${regulatoryInfo.displayText}<br/>
        </c:forEach>
    </c:when>

    <c:otherwise>
        <c:choose><c:when test="${actionBean.editOrder.canSkipRegulatoryRequirements()}">
            Regulatory information not entered because: ${actionBean.editOrder.skipRegulatoryReason}
        </c:when>
            <c:otherwise>
                No regulatory information entered.
            </c:otherwise></c:choose>
    </c:otherwise>
</c:choose>
