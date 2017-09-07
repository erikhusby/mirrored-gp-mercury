<%@ page contentType="text/html;charset=UTF-8" language="java" %><%@
        include file="/resources/layout/taglibs.jsp" %>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ResultParamsActionBean"--%><%--
This Stripes layout displays a dynamically generated UI snippet to display in a modal dialog
  to choose custom parameter options for a result column --%>

    <p>Column Name: <input type="text" name="userColumnName" id="userColumnName" style="width: 180px"></p>
        <c:forEach items="${actionBean.resultParams.paramInputs.values()}" var="inputParam" varStatus="row">
            <c:choose>
                <c:when test="${inputParam.type eq 'TEXT'}">
                    <p>${inputParam.label} <input name="${inputParam.name}" id="${inputParam.name}" type="text"></p>
                </c:when>
                <c:when test="${inputParam.type eq 'CHECKBOX'}">
                    <p>${inputParam.label} <input name="${inputParam.name}" id="${inputParam.name}" value="${inputParam.name}" <c:if test="${inputParam.name eq inputParam.defaultSingleValue}">checked="true"</c:if> type="checkbox"></p>
                </c:when>
                <c:when test="${inputParam.type eq 'CHECKBOX_GROUP'}">
                    <p>${inputParam.label}<br/>
                    <c:forEach items="${inputParam.optionItems}" var="option" varStatus="loop">
                        <input name="${inputParam.name}" id="${inputParam.name}_${loop.index}" value="${option.code}" <c:if test="${option.code eq inputParam.defaultValues}">checked="true"</c:if> type="checkbox">${option.label}
                    </c:forEach></p>
                </c:when>
                <c:when test="${inputParam.type eq 'RADIO'}">
                    <p>${inputParam.label}<br/>
                    <c:forEach
                            items="${inputParam.optionItems}"
                            var="option"
                            varStatus="loop"><c:if test="loop.index > 1"><br/></c:if> <input name="${inputParam.name}" id="${inputParam.name}" value="${option.code}" <c:if test="${option.code eq inputParam.defaultSingleValue}">checked="true"</c:if> type="radio">${option.label}
                    </c:forEach></p>
                </c:when>
                <c:when test="${inputParam.type eq 'PICKLIST'}">
                    <p>${inputParam.label}<br/>
                    <select name="${inputParam.name}" id="${inputParam.name}" size="16">
                        <c:forEach items="${inputParam.optionItems}" var="option">
                            <option value="${option.code}" <c:if test="${option.code eq inputParam.defaultSingleValue}">selected="true"</c:if> >${option.label}</option>
                        </c:forEach>
                    </select></p>
                </c:when>
                <c:when test="${inputParam.type eq 'MULTI_PICKLIST'}">
                    <p>${inputParam.label}<br/>
                    <select name="${inputParam.name}" id="${inputParam.name}" multiple="true" size="16">
                        <c:forEach items="${inputParam.optionItems}" var="option">
                            <option value="${option.code}" <c:if test="${ inputParam.defaultValues.contains(option.code)}">selected="true"</c:if> >${option.label}</option>
                        </c:forEach>
                    </select></p>
                </c:when>
            </c:choose>
        </c:forEach>


