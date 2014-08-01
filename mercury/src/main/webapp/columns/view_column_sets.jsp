<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:layout-definition>
    <%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"--%>
    <!-- Allow user to choose column sets -->
    <label>Column view set: </label>
    <select name="columnSetName" id="columnSetName">
        <c:forEach items="${actionBean.viewColumnSets}" var="columnSet">
            <c:set var="optionValue" value="${columnSet.level}|${columnSet.name}"/>
            <option value="${optionValue}"
            <c:if test="${actionBean.columnSetName == optionValue}">selected</c:if>
            >
            ${columnSet.level} - ${columnSet.name}
            </option>
        </c:forEach>
    </select>
</stripes:layout-definition>
