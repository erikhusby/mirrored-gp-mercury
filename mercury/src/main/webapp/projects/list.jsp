<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="List research projects" sectionTitle="List research projects">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('.shiftCheckbox').enableCheckboxRangeSelection();
                $j('#projectsTable').dataTable( {  })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

    <p>
        <stripes:link title="New Research Project" href="${ctxpath}/projects/project.action?create" class="pull-right">New research project</stripes:link>
    </p>

    <div class="clearfix"></div>

    <p>
    <table class="table simple" id="projectsTable" width="100%">
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
                        ${project.title}
                </td>
                <td>
                    <stripes:link href="/projects/project.action" event="view">
                        <stripes:param name="businessKey" value="${project.businessKey}"/>
                        ${project.businessKey}
                    </stripes:link>
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
            </tr>
        </c:forEach>
        </tbody>
    </table>
    </p>

    </stripes:layout-component>
</stripes:layout-render>