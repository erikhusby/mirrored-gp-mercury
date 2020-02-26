<%@ page contentType="text/html;charset=UTF-8"%><%@ include file="/resources/layout/taglibs.jsp" %><%--
***** This is the recursive JSP snippet which drills down into storage tiers **** --%>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageAllocationActionBean"/>
<ul style="margin-bottom: 0px">
<c:forEach items="${nodeList.entrySet()}" var="children">
    <li><div style="display:inline-block;width:100%"><div style="display:inline-block;font-weight: bold;width:140px">${actionBean.getLocName(children.key)}</div><c:if test="${children.value.isEmpty()}">
    <c:set var="usageVals" scope="page" value="${actionBean.getCapacityUsage(children.key)}"/>
    <c:choose>
        <c:when test="${ usageVals == null }">(Capacity Usage N/A) </c:when>
        <c:otherwise>
            <c:set value="${actionBean.getCapacityPercentage(children.key)}" scope="page" var="pct"/>
            <div style="display:inline-block;width:300px">${ usageVals.left } slots - ${ usageVals.middle } locations used out of ${ usageVals.right } available
                <c:if test="${pct <= 100}"> (${pct}%)</c:if>
                <c:if test="${pct > 100}"><span style="font-weight: bold;color: red"> (${pct}%)</span><c:set value="${100}" scope="page" var="pct"/></c:if>
            </div>
            <div style="display:inline-block;background-color: #ff8080;width:${250 * pct / 100}px">&nbsp;</div><div style="display:inline-block;background-color: #80ff80;width:${250 * ( 100 - pct ) / 100}px" >&nbsp;</div></div></c:otherwise>
    </c:choose>
    </c:if></div></li>
    <c:if test="${not children.value.isEmpty()}"><li>
    <c:set var="nodeList" scope="request" value="${children.value}"/>
    <jsp:include page="freezer_space_nodes.jsp"/></li>
    </c:if>
</c:forEach>
</ul>
