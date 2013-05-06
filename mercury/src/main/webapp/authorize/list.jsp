<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizePageActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Page Authorization" sectionTitle="List Page Authorization" createTitle="Create Page Authorization">

    <stripes:layout-component name="content">

        <div class="clearfix"></div>

        <table class="table simple" id="projectsTable">
            <thead>
                <tr>
                    <th width="400">Page Path</th>
                    <th>Authorized Roles</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.allPageAuthorizations}" var="pageAuth">
                    <tr>
                        <td>
                            <stripes:link beanclass="${actionBean.class.name}" event="edit">
                                <stripes:param name="pageAuthorization.pagePath" value="${pageAuth.pagePath}"/>
                                ${pageAuth.pagePath}
                            </stripes:link>
                        </td>
                        <td>
                            ${pageAuth.displayRoles}
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

    </stripes:layout-component>
</stripes:layout-render>