<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="List Research Projects" sectionTitle="List Research Projects"
        createTitle="<%=ResearchProjectActionBean.CREATE_PROJECT%>">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('.shiftCheckbox').enableCheckboxRangeSelection();
                $j('#projectsTable').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[4,'desc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},           // Name
                        {"bSortable": true, "sType": "title-jira"},     // ID
                        {"bSortable": true},                            // Status
                        {"bSortable": true},                            // Owner
                        {"bSortable": true, "sType": "date"},           // Updated
                        {"bSortable": true, "sType": "numeric"}         // Count
                    ]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

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
                    <a class="external" target="JIRA" href="${actionBean.jiraUrl(project.jiraTicketKey)}" class="external" target="JIRA" title="${project.jiraTicketKey}">
                            ${project.jiraTicketKey}
                    </a>
                </td>
                <td>
                    ${project.status}
                </td>
                <td>
                    ${actionBean.getUserFullName(project.createdBy)}
                </td>
                <td>
                    <fmt:formatDate value="${project.modifiedDate}" pattern="${actionBean.dateTimePattern}"/>
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