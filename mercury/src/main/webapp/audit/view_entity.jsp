<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailEntryActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Audited Entity" sectionTitle="Audited Entity" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

            });
        </script>
    </stripes:layout-component>


    <stripes:layout-component name="content">
        <p>${actionBean.displayClassname} at revision ${actionBean.revId}</p>
        <c:if test="${actionBean.auditEntity != null}">
            <c:set var="auditEntity" value="${actionBean.auditEntity}"/>

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
                    <c:forEach items="${auditEntity.fields}" var="entityField">
                        <td>
                            <c:choose>
                                <c:when test="${entityField.valueList != null}">
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
                </tbody>
            </table>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
