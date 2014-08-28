<%--
This jsp gets inserted as a subroutine for view_trail_entry.jsp and view_entity.jsp.
It displays a single value/reference, a list of them, or a map of them.

@param auditEntity  contains the fields and revId
@param actionBean   is the target of the link
--%>

<c:if test="${auditEntity.fields != null}">
    <c:forEach items="${auditEntity.fields}" var="field">
        <td>
            <c:choose>

                <%-- Displays the map as a space-delimited list of pairs, all in one table cell. --%>
                <c:when test="${field.entityFieldMap != null}">
                    <c:forEach items="${field.entityFieldMap}" var="mapItem">
                        <c:set var="fieldItem" value="${mapItem.value}"/>
                        ${mapItem.key}&MediumSpace;=&MediumSpace;
                        <%@ include file="link_or_value.jsp" %>
                        <br/>
                    </c:forEach>
                    <c:if test="${fn:length(field.entityFieldMap) == 0}">
                        null
                    </c:if>
                </c:when>

                <%-- Displays the list as space-delimited references/values, all in one table cell. --%>
                <c:when test="${field.entityFieldList != null}">
                    <c:forEach items="${field.entityFieldList}" var="fieldItem">
                        <%@ include file="link_or_value.jsp" %>
                        &MediumSpace;
                    </c:forEach>
                    <c:if test="${fn:length(field.entityFieldList) == 0}">
                        null
                    </c:if>
                </c:when>

                <%-- Displays the single space-delimited reference/value in one table cell. --%>
                <c:otherwise>
                    <c:set var="fieldItem" value="${field}"/>
                    <%@ include file="link_or_value.jsp" %>
                </c:otherwise>

            </c:choose>
        </td>
    </c:forEach>
</c:if>
