<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="edu.mit.broad.gap.portal.web.stripes.workspace.DataDownloadActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="List research projects" sectionTitle="List research projects">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('.shiftCheckbox').enableCheckboxRangeSelection();
                $j('#projectsTable').dataTable({
                    "aaSorting":[],
                    "bFilter":true
                });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

    <p>
        <stripes:link title="New Research Project" href="/project/project.action?create" class="pull-right"/>
    </p>

    <table class="simple" id="studies" width="100%">
        <thead>
        <tr>
            <th width="*">Name</th>
            <th style="text-align: center;">ID</th>
            <th style="text-align: center;">Status</th>
            <th>Owner</th>
            <th>Updated</th>
            <th style="text-align: center;"># of Orders</th>
        </tr>
        </thead>
        <tbody>
            <c:forEach items="${actionBean.researchProjectData.values}" var="project">
                <td>
                    ${project.title}
                </td>
                <td>
                        ${project.businessKey}
                </td>
                <td>
                        ${project.jiraTicketKey}
                </td>
                <td>
                        ${project.status}
                </td>
                <td>
                        ${project.createdBy}
                </td>
                <td>
                        ${project.modifiedDate}
                </td>
                <td>
                        ${actionBean.researchProjectCounts.get(project.jiraTicketKey)}
                </td>
            </c:forEach>
        </tbody>
    </table>


    </stripes:layout-component>
</stripes:layout-render>