<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.WorkflowActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Workflows" sectionTitle="List Workflows" showCreate="false">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#workflowList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "title-string"},
                        {"bSortable": true},
                        {"bSortable": true}]
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="clearfix"></div>

        <table id="workflowList" class="table simple">
            <thead>
            <tr>
                <th>Name</th>
                <th>Version</th>
                <th>Effective Date</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.allWorkflows}" var="workflowDto">
                <tr>
                    <td>
                        <stripes:link beanclass="${actionBean.class.name}" event="view" title="${workflowDto.workflowDef.name}">
                            <stripes:param name="workflowDtoId" value="${workflowDto.id}"/>
                            ${workflowDto.workflowDef.name}
                        </stripes:link>
                    </td>
                    <td>${workflowDto.workflowDef.effectiveVersion.version}</td>
                    <td>${workflowDto.effectiveDate}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
