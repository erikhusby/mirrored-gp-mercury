<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailEntryActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Audit Trail Entry" showCreate="false"
                       sectionTitle="Audit Trail Entry">

    <stripes:layout-component name="extraHead"/>

    <%--
    Each audit trail entry is represented by an html table having 2 or 3 rows: header row,
    previous entity row (but no row if it's a created entity), and entity at version row.
    --%>

    <stripes:layout-component name="content">
        <c:choose>
            <c:when test="${fn:length(actionBean.auditTrailEntries) < 2}">
                <p>${actionBean.displayClassname} changes at revision ${actionBean.revId}</p>
            </c:when>
            <c:otherwise>
        <p>${fn:length(actionBean.auditTrailEntries)} ${actionBean.displayClassname} changes at revision ${actionBean.revId}</p>
            </c:otherwise>
        </c:choose>

        <c:forEach items="${actionBean.auditTrailEntries}" var="auditTrailEntry">
            <c:choose>
                <c:when test="${auditTrailEntry.previousEntity.fields == null}">
                    <c:set var="auditEntryType" value="(Created)"/>
                </c:when>
                <c:when test="${auditTrailEntry.entity.fields == null}">
                    <c:set var="auditEntryType" value="(Deleted)"/>
                </c:when>
                <c:otherwise>
                    <c:set var="auditEntryType" value="(Modified)"/>
                </c:otherwise>
            </c:choose>

            <p><table class="table simple">
            <thead>
            <tr>
                <th>RevId</th>
                <c:forEach items="${auditTrailEntry.columnNames}" var="columnName">
                    <th>${columnName}</th>
                </c:forEach>
            </tr>
            </thead>
            <tbody>
            <c:if test="${auditTrailEntry.previousEntity.fields != null}">
                <tr>
                    <!-- the revId column -->
                    <td>${auditEntity.revId}</td>
                    <!-- the entity columns -->
                    <c:set var="auditEntity" value="${auditTrailEntry.previousEntity}"/>
                    <%@ include file="view_trail_entry_row.jsp" %>
                </tr>
            </c:if>
            <tr>
                <!-- the revId along with the type of change. -->
                <td>${auditEntity.revId}  ${auditEntryType}</td>
                <!-- the entity columns -->
                <c:set var="auditEntity" value="${auditTrailEntry.entity}"/>
                <%@ include file="view_trail_entry_row.jsp" %>
            </tr>
            </tbody>
        </table></p>
        </c:forEach>

    </stripes:layout-component>
</stripes:layout-render>
