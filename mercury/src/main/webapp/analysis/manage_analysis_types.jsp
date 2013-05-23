<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.Role.*" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.DB" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Analysis Types"
                       sectionTitle="Manage Analysis Types">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#analysisTypesData').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [1, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": false},
                        {"bSortable": true},
                        {"bSortable": false},
                        {"bSortable": false}
                    ]
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div id="analysisTypesId">
            <div id="add" class="form-horizontal span4">
                <stripes:form beanclass="${actionBean.class.name}" id="addForm">
                    <div class="control-group">
                        <stripes:label for="newName" class="control-label">Name * </stripes:label>
                        <div class="controls">
                            <stripes:text name="newName"/>
                        </div>
                    </div>

                    <div class="controls">
                        <stripes:submit name="AddAnalysisTypes" value="Add"/>
                    </div>
                </stripes:form>
            </div>

            <table id="analysisTypesData" class="table simple">
                <thead>
                <tr>
                    <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                        <th></th>
                    </security:authorizeBlock>
                    <th>Name</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.analysisTypeList}" var="analysisType">
                    <tr>
                        <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                            <td><stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"
                                    event="RemoveAnalysisType">
                                <stripes:param name="businessKey" value="${analysisType.businessKey}"/>
                                X</stripes:link></td>
                        </security:authorizeBlock>
                        <td>${analysisType.name}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>