<%@ page contentType="text/html;charset=UTF-8" %><%@
        include file="/resources/layout/taglibs.jsp" %>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ResultParamsActionBean"--%><%--
This Stripes layout displays a dynamically generated UI snippet to display in a modal dialog
  to choose custom parameter options for a result column --%>
    <c:if test="${actionBean.paramType eq 'SEARCH_TERM'}"><p>Column Name: <input type="text" name="userColumnName" id="userColumnName" style="width: 180px" value="${actionBean.resultParamValues.userColumnName}"></p></c:if>
        <c:forEach items="${actionBean.resultParamConfig.paramInputs.values()}" var="inputParam" varStatus="row">
            <c:set value="${inputParam.name}" var="paramName"/>
            <c:set value="${actionBean.resultParamValues.getSingleValue(paramName)}" var="paramValue"/>
            <c:set value="${actionBean.resultParamValues.getMultiValues(paramName)}" var="paramValues" />
            <c:choose>
                <c:when test="${inputParam.type eq 'TEXT'}">
                    <p>${inputParam.label} <input name="${paramName}" id="${paramName}" type="text" value="${paramValue}"></p>
                </c:when>
                <c:when test="${inputParam.type eq 'CHECKBOX'}">
                    <p>${inputParam.label} <input name="${inputParam.name}" id="${inputParam.name}" value="${inputParam.name}" <c:if test="${inputParam.name eq paramValue}">checked="true"</c:if> type="checkbox"></p>
                </c:when>
                <c:when test="${inputParam.type eq 'CHECKBOX_GROUP'}">
                    <p>${inputParam.label}<br/>
                    <c:forEach items="${inputParam.optionItems}" var="option" varStatus="loop">
                        <input name="${inputParam.name}" id="${inputParam.name}_${loop.index}" value="${option.code}" <c:if test="${ actionBean.resultParamValues.containsValue(inputParam.name, option.code) }">checked="true"</c:if> type="checkbox">${option.label}
                    </c:forEach></p>
                </c:when>
                <c:when test="${inputParam.type eq 'RADIO'}"><!-- -->
                    <p>${inputParam.label}<br/>
                    <c:forEach
                            items="${inputParam.optionItems}" var="option" varStatus="loop"><c:if test="loop.index > 1"><br/></c:if> <input name="${inputParam.name}" id="${inputParam.name}_${loop.index}" value="${option.code}" <c:if test="${option.code eq paramValue}">checked="true"</c:if> type="radio">${option.label}
                     </c:forEach></p>
                </c:when>
                <c:when test="${inputParam.type eq 'PICKLIST'}">
                    <c:if test="${paramValue eq '' }"><c:set value="${inputParam.defaultSingleValue}" var="paramValue"/></c:if>
                    <p>${inputParam.label}<br/>
                    <select name="${inputParam.name}" id="${inputParam.name}" size="16">
                        <c:forEach items="${inputParam.optionItems}" var="option">
                            <option value="${option.code}" <c:if test="${option.code eq paramValue}">selected="true"</c:if> >${option.label}</option>
                        </c:forEach>
                    </select></p>
                </c:when>
                <c:when test="${inputParam.type eq 'MULTI_PICKLIST'}">
                    <p>${inputParam.label}<br/>
                    <select name="${inputParam.name}" id="${inputParam.name}" multiple="true" size="16">
                        <c:forEach items="${inputParam.optionItems}" var="option">
                            <option value="${option.code}" <c:if test="${ actionBean.resultParamValues.containsValue(inputParam.name, option.code) }">selected="true"</c:if> >${option.label}</option>
                        </c:forEach>
                    </select></p>
                </c:when>
            </c:choose>
        </c:forEach>


