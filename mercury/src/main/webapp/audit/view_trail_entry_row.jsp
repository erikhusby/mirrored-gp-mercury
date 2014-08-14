<%-- This jsp gets inserted as a subroutine for view_trail_entry.jsp and view_entity.jsp --%>

<td>${auditEntity.revId}</td>
<c:if test="${auditEntity.fields == null}">
    <td colspan="0" align="left">${displayStringWhenNoFields}</td>
</c:if>
<c:if test="${auditEntity.fields != null}">
    <c:forEach items="${auditEntity.fields}" var="field">
        <td>
            <c:choose>
                <c:when test="${field.valueList != null}">
                    <%-- Puts the space-delimited list of values or references in one table cell. --%>
                    <c:forEach items="${field.valueList}" var="fieldItem">
                        <c:choose>
                            <c:when test="${fieldItem.referenceClassname != null && fieldItem.value != null}">
                                <%-- displays the link to another entity --%>
                                <stripes:link beanclass="${actionBean.class.name}" event="viewEntity">
                                    <stripes:param name="revId" value="${auditEntity.revId}"/>
                                    <stripes:param name="displayClassname" value="${fieldItem.displayClassname}"/>
                                    <stripes:param name="entityId" value="${fieldItem.value}"/>
                                    ${fieldItem.value}
                                </stripes:link>
                            </c:when>
                            <c:otherwise>
                                ${fieldItem.value}
                            </c:otherwise>
                        </c:choose>
                        &MediumSpace;
                    </c:forEach>
                    <c:if test="${fn:length(field.valueList) == 0}">
                        null
                    </c:if>
                </c:when>
                <c:when test="${field.referenceClassname != null && field.value != null}">
                    <%-- displays the link to another entity --%>
                    <stripes:link beanclass="${actionBean.class.name}" event="viewEntity">
                        <stripes:param name="revId" value="${auditEntity.revId}"/>
                        <stripes:param name="displayClassname" value="${field.displayClassname}"/>
                        <stripes:param name="entityId" value="${field.value}"/>
                        ${field.value}
                    </stripes:link>
                </c:when>
                <c:when test="field.value == null">
                    null
                </c:when>
                <c:otherwise>
                    ${field.value}
                </c:otherwise>
            </c:choose>
        </td>
    </c:forEach>
</c:if>
