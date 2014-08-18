<%--
 This jsp get inserted as a subroutine.
 It displays an EntityField as either a single value or a reference.

@param actionBean   is the target of the link
@param fieldItem    is an EntitiyField to display
@param auditEntity  contains the revId
--%>

<c:choose>
    <c:when test="${fieldItem.canonicalClassname != null && fieldItem.value != null}">
        <stripes:link beanclass="${actionBean.class.name}" event="viewEntity">
            <stripes:param name="revId" value="${auditEntity.revId}"/>
            <stripes:param name="canonicalClassname" value="${fieldItem.canonicalClassname}"/>
            <stripes:param name="entityId" value="${fieldItem.value}"/>
            ${fieldItem.value}
        </stripes:link>
    </c:when>
    <c:otherwise>
        ${fieldItem.value}
    </c:otherwise>
</c:choose>
