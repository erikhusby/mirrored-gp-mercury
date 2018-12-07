<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%-- This JSP fragment supports recursing over nested tables --%>
<%--@elvariable id="tableName" type="java.lang.String"--%>
<%--@elvariable id="nestedTable" type="org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList.ResultList"--%>
<table class="table simple dataTable">
    <c:if test="${not empty tableName}">
        <tr>
            <th colspan="${nestedTable.headers.size()}">${tableName}</th>
        </tr>
    </c:if>
    <tr>
        <c:forEach items="${nestedTable.headers}" var="nestedHeader">
            <th>${nestedHeader.viewHeader}</th>
        </c:forEach>
    </tr>
    <c:forEach items="${nestedTable.resultRows}" var="nestRow">
        <tr>
            <c:forEach items="${nestRow.renderableCells}" var="nestCell" varStatus="nestRowStatus">
                <td style="vertical-align: top"><div><enhance:out escapeXml="false">${nestCell}</enhance:out></div>
                    <c:if test="${not empty nestRow.cellNestedTables[nestRowStatus.index]}">
                        <c:set var="nestedTable" value="${nestRow.cellNestedTables[nestRowStatus.index]}" scope="request"/>
                        <c:set var="tableName" value="${null}" scope="request"/>
                        <jsp:include page="nested_table.jsp"/>
                    </c:if>
                </td>
            </c:forEach>
        </tr>
    </c:forEach>
</table>
