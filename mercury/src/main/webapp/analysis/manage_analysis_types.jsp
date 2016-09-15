<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Analysis Types"
                       sectionTitle="Manage Analysis Types">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#analysisTypeData').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [1, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": true},
                        {"bSortable": true}
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
                        <stripes:submit name="addAnalysisType" value="Add" class="btn btn-primary"/>
                    </div>
                </stripes:form>
            </div>

            <div class="clearfix"></div>

            <stripes:form beanclass="${actionBean.class.name}" id="deleteForm">
                <div class="actionButtons">
                    <stripes:submit name="removeAnalysisTypes" value="Remove Selected" class="btn"/>
                </div>
                <table id="analysisTypeData" class="table simple">
                    <thead>
                    <tr>
                        <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                            <th width="40">
                                <input for="count" type="checkbox" class="checkAll"/><span id="count"
                                                                                           class="checkedCount"></span>
                            </th>
                        </security:authorizeBlock>
                        <th>Name</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.analysisTypeList}" var="analysisType">
                        <tr>
                            <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                                <td><stripes:checkbox
                                        class="shiftCheckbox"
                                        value="${analysisType.businessKey}"
                                        name="businessKeyList"></stripes:checkbox></td>
                            </security:authorizeBlock>
                            <td>${analysisType.name}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>