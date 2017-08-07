<%@ page contentType="text/html;charset=UTF-8" language="java" %><%@
        include file="/resources/layout/taglibs.jsp" %>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ResultParamsActionBean"--%><%--
This Stripes layout displays a dynamically generated UI snippet to display in a modal dialog
  to choose custom parameter options for a result column --%>

    <script type="text/javascript">
    </script>
    <p>Column Name: <input type="text" name="userColumnName" id="userColumnName" style="width: 180px"></p>
        <c:forEach items="${actionBean.resultParams.paramInputs.values()}" var="inputParam" varStatus="row">
            <c:choose>
                <c:when test="${inputParam.type eq 'TEXT'}">
                    <p>${inputParam.label} <input name="${inputParam.name}" id="${inputParam.name}" type="text"></p>
                </c:when>
                <c:when test="${inputParam.type eq 'CHECKBOX'}">
                    <p>${inputParam.label} <input name="${inputParam.name}" id="${inputParam.name}" value="${inputParam.name}" type="checkbox"></p>
                </c:when>
                <c:when test="${inputParam.type eq 'CHECKBOX_GROUP'}">
                    <p>${inputParam.label}<br/>
                    <c:forEach items="${inputParam.optionItems}" var="option" varStatus="index">
                        <input name="${inputParam.name}" id="${param.name}" value="${option.code}" type="checkbox">${option.label}
                    </c:forEach></p>
                </c:when>
                <c:when test="${inputParam.type eq 'RADIO'}">
                    <p>${inputParam.label}<br/>
                    <c:forEach items="${inputParam.optionItems}" var="option" varStatus="index">
                        <c:if test="index > 1"><br/></c:if> <input name="${inputParam.name}" id="${inputParam.name}" value="${option.code}" type="radio">${option.label}
                    </c:forEach></p>
                </c:when>
                <c:when test="${inputParam.type eq 'PICKLIST'}">
                    <p>${inputParam.label}<br/>
                    <select name="${inputParam.name}" id="${param.name}" size="16">
                        <c:forEach items="${inputParam.optionItems}" var="option" varStatus="index">
                            <option value="${option.code}" >${option.label}</option>
                        </c:forEach>
                    </select></p>
                </c:when>
                <c:when test="${inputParam.type eq 'MULTI_PICKLIST'}">
                    <p>${inputParam.label}<br/>
                    <select name="${inputParam.name}" id="${inputParam.name}" multiple="true" size="16">
                        <c:forEach items="${inputParam.optionItems}" var="option" varStatus="index">
                            <option value="${option.code}" >${option.label}</option>
                        </c:forEach>
                    </select></p>
                </c:when>
            </c:choose>
        </c:forEach>


