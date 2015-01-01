<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="List Research Projects" sectionTitle="List Research Projects" showCreate="true">

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

        <style type="text/css">
            /* Convince the browser to always respect specified widths, only flexing the "Name" column. */
            #projectsTable { table-layout: fixed; }

            .columnId { width: 5em; }
            .columnStatus { width: 4em; }
            .columnOwner { width: 10em; }
            .columnUpdated { width: 8em; }
            .columnNumberOfOrders { width: 6em; }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">

    <div class="clearfix"></div>

    <p>
    <table class="table simple" id="projectsTable">
        <thead>
        <tr>
            <th>Name</th>
            <th class="columnId" style="min-width: 180px;">ID</th>
            <th class="columnStatus">Status</th>
            <th class="columnOwner">Owner</th>
            <th class="columnUpdated">Updated</th>
            <th class="columnNumberOfOrders"># of Orders</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${actionBean.allResearchProjects}" var="project">
            <tr>
                <td class="ellipsis">
                    <stripes:link beanclass="${actionBean.class.name}" event="view">
                        <stripes:param name="researchProject" value="${project.businessKey}"/>
                        ${project.title}
                    </stripes:link>
                </td>
                <td class="columnId">
                    <a class="external" target="JIRA" href="${actionBean.jiraUrl(project.jiraTicketKey)}" class="external" target="JIRA" title="${project.jiraTicketKey}">
                            ${project.jiraTicketKey}
                    </a>
                </td>
                <td class="columnStatus">
                    ${project.status}
                </td>
                <td class="columnOwner">
                    ${actionBean.getUserFullName(project.createdBy)}
                </td>
                <td class="columnUpdated">
                    <fmt:formatDate value="${project.modifiedDate}" pattern="${actionBean.dateTimePattern}"/>
                </td>
                <td class="columnNumberOfOrders">
                    ${actionBean.researchProjectCounts.get(project.jiraTicketKey)}
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    </p>

    </stripes:layout-component>
</stripes:layout-render>
