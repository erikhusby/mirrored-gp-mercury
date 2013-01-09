<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="List research projects" sectionTitle="List research projects">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('.shiftCheckbox').enableCheckboxRangeSelection();
                $j('#projectsTable').dataTable({
                    "oTableTools": ttExportDefines
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

    <p>
        <stripes:link beanclass="${actionBean.class.name}" event="create" title="<%=ResearchProjectActionBean.CREATE_PROJECT%>" class="pull-right">
            <span class="icon-briefcase"></span> <%=ResearchProjectActionBean.CREATE_PROJECT%></stripes:link>
    </p>

    <div class="clearfix"></div>

    <p>
    <table class="table simple" id="projectsTable">
        <thead>
        <tr>
            <th width="*">Name</th>
            <th>ID</th>
            <th>Status</th>
            <th>Owner</th>
            <th>Updated</th>
            <th># of Orders</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${actionBean.allResearchProjects}" var="project">
            <tr>
                <td>
                    <stripes:link beanclass="${actionBean.class.name}" event="view">
                        <stripes:param name="researchProject" value="${project.businessKey}"/>
                        ${project.title}
                    </stripes:link>
                </td>
                <td>
                    <a class="external" target="JIRA" href="${actionBean.jiraUrl}${project.jiraTicketKey}" class="external" target="JIRA">
                            ${project.jiraTicketKey}
                    </a>
                </td>
                <td>
                    ${project.status}
                </td>
                <td>
                    ${actionBean.fullNameMap[project.createdBy]}
                </td>
                <td>
                    <fmt:formatDate value="${project.modifiedDate}" pattern="MM/dd/yyyy HH:mm"/>
                </td>
                <td>
                    ${actionBean.researchProjectCounts.get(project.jiraTicketKey)}
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    </p>

    </stripes:layout-component>
</stripes:layout-render>