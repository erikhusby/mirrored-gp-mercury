<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailEntryActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Audit Trail Entry" sectionTitle="Audit Trail Entry" showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

            });
        </script>
    </stripes:layout-component>

    <%-- Each audit trail entry has a table consisting of header row, prev entity row, and entity row. --%>
    <%-- When previous entity fields is null it's an entity add, when current entity fields is null it's a delete. --%>

    <stripes:layout-component name="content">
        <c:forEach items="${actionBean.auditTrailEntries}" var="auditTrailEntry">
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
            <tr>
                <c:set var="auditEntity" value="${auditTrailEntry.previousEntity}"/>
                <c:set var="displayStringWhenNoFields" value="(Newly created entity)"/>
                <%@ include file="view_trail_entry_row.jsp" %>
            </tr>
            <tr>
                <c:set var="auditEntity" value="${auditTrailEntry.entity}"/>
                <c:set var="displayStringWhenNoFields" value="(Entity was deleted)"/>
                <%@ include file="view_trail_entry_row.jsp" %>
            </tr>
            </tbody>
        </table></p>
        </c:forEach>

    </stripes:layout-component>
</stripes:layout-render>
