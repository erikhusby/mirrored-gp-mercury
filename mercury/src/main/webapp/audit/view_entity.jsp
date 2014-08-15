<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailEntryActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Audited Entity" sectionTitle="Audited Entity" showCreate="false">

    <stripes:layout-component name="extraHead"/>

    <stripes:layout-component name="content">
        <p>${actionBean.displayClassname} at revision ${actionBean.revId}</p>
        <c:if test="${actionBean.auditEntity != null}">
            <c:set var="auditEntity" value="${actionBean.auditEntity}"/>
            <c:set var="auditEntryType" value=""/>

            <table class="table simple">
                <thead>
                <tr>
                    <c:forEach items="${auditEntity.fields}" var="entityField">
                        <th>${entityField.fieldName}</th>
                    </c:forEach>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <%@ include file="view_trail_entry_row.jsp" %>
                </tr>
                </tbody>
            </table>
        </c:if>
        <c:if test="${actionBean.auditEntity == null}">
            <p>Entity is null.</p>
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>
