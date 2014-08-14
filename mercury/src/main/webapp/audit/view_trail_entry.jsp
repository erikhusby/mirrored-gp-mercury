<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailEntryActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Audit Trail Entry" sectionTitle="Audit Trail Entry" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

            });
        </script>
    </stripes:layout-component>


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

            <c:set var="auditEntity" value="${auditTrailEntry.previousEntity}"/>
            <c:choose>
                <%-- It is an entity addition when previousEntity is null. --%>
                <c:when test="${auditEntity == null}">
                    <tr>
                        <td>(new entity)</td>
                    </tr>
                </c:when>
                <c:otherwise>
                    <tr>
                        <td>${auditEntity.revId}</td>
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
                                                        <stripes:param name="entityClassname" value="${fieldItem.referenceClassname}"/>
                                                        <stripes:param name="entityId" value="${fieldItem.value}"/>
                                                        ${fieldItem.value}&MediumSpace;
                                                    </stripes:link>
                                                </c:when>
                                                <c:otherwise>
                                                    ${fieldItem.value}&MediumSpace;
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>
                                    </c:when>
                                    <c:when test="${field.referenceClassname != null && field.value != null}">
                                        <%-- displays the link to another entity --%>
                                        <stripes:link beanclass="${actionBean.class.name}" event="viewEntity">
                                            <stripes:param name="revId" value="${auditEntity.revId}"/>
                                            <stripes:param name="entityClassname" value="${field.referenceClassname}"/>
                                            <stripes:param name="entityId" value="${field.value}"/>
                                            ${field.value}
                                        </stripes:link>
                                    </c:when>
                                    <c:otherwise>
                                        ${field.value}
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </c:forEach>
                    </tr>
                </c:otherwise>
            </c:choose>

            </tbody>
        </table></p>
        </c:forEach>

    </stripes:layout-component>
</stripes:layout-render>
