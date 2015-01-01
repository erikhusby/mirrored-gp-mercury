<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailEntryActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Audit Trail Entry" showCreate="false" sectionTitle="Audit Trail Entry">

    <stripes:layout-component name="extraHead"/>

    <%--
    Each audit trail entry is represented by an html table having 2 or 3 rows: header row,
    previous entity row (but no row if it's a created entity), and changed entity row.
    --%>

    <stripes:layout-component name="content">
        <c:choose>
            <c:when test="${fn:length(actionBean.auditTrailEntries) < 2}">
                <p>${actionBean.displayClassname} entity changed at revision ${actionBean.revId}</p>
            </c:when>
            <c:otherwise>
                <p>${fn:length(actionBean.auditTrailEntries)} ${actionBean.displayClassname} entites changed at revision ${actionBean.revId}</p>
            </c:otherwise>
        </c:choose>

        <c:forEach items="${actionBean.auditTrailEntries}" var="auditTrailEntry">
            <c:set var="auditEntryType"
                   value='${(auditTrailEntry.previousEntity.fields == null) ? "(Created)" : (auditTrailEntry.entity.fields == null ? "(Deleted)" : "(Modified)")}'/>

            <p/>
            <table class="table simple">
                <thead>
                <tr>
                    <th>RevId</th>
                    <c:forEach items="${auditTrailEntry.columnNames}" var="columnName">
                        <th>${columnName}</th>
                    </c:forEach>
                </tr>
                </thead>
                <tbody>

                <%-- The previous entity is shown if not null --%>
                <c:set var="auditEntity" value="${auditTrailEntry.previousEntity}"/>
                <c:if test="${auditEntity.fields != null}">
                    <tr>
                        <%-- The revId column --%>
                        <td>${auditEntity.revId}</td>
                        <%-- The entity columns --%>
                        <%@ include file="view_trail_entry_row.jsp" %>
                    </tr>
                </c:if>

                <%-- The changed entity is always shown --%>
                <c:set var="auditEntity" value="${auditTrailEntry.entity}"/>
                <tr>
                    <%-- The revId along with the type of change. --%>
                    <td>${auditEntity.revId} ${auditEntryType}</td>
                    <%-- The entity columns.  A deleted entity may only have the entity id field. --%>
                    <%@ include file="view_trail_entry_row.jsp" %>
                </tr>

                </tbody>
            </table>
        </c:forEach>

    </stripes:layout-component>
</stripes:layout-render>
