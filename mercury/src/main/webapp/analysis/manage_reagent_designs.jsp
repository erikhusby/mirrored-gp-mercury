<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Reagent Designs"
                       sectionTitle="Manage Reagent Designs">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#reagentDesignData').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [1, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true}
                    ]
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div id="reagentDataId">
            <div id="add" class="form-horizontal span4">
                <stripes:form beanclass="${actionBean.class.name}" id="addForm">
                    <div class="control-group">
                        <stripes:label for="newName" class="control-label">Name * </stripes:label>
                        <div class="controls">
                            <stripes:text name="newName"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="reagentType" class="control-label">Reagent Type * </stripes:label>
                        <div class="controls">
                            <stripes:select name="selectedReagentType" id="reagentType">
                                <stripes:option value="">Select a Reagent Type</stripes:option>
                                <stripes:options-enumeration
                                        enum="org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType"/>
                            </stripes:select>
                        </div>
                    </div>

                    <div class="controls">
                        <stripes:submit name="addReagentDesign" value="Add" class="btn btn-primary"/>
                    </div>
                </stripes:form>
            </div>

            <div class="clearfix"></div>

            <stripes:form beanclass="${actionBean.class.name}" id="deleteForm">
                <div class="actionButtons">
                    <stripes:submit name="removeReagentDesigns" value="Remove Selected" class="btn"/>
                </div>
                <table id="reagentDesignData" class="table simple">
                    <thead>
                    <tr>
                        <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                            <th width="40">
                                <input for="count" type="checkbox" class="checkAll"/><span id="count"
                                                                                           class="checkedCount"></span>
                            </th>
                        </security:authorizeBlock>
                        <th>Name</th>
                        <th width="90">Reagent Type</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.reagentDesignList}" var="reagentDesign">
                        <tr>
                            <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                                <td><stripes:checkbox
                                        class="shiftCheckbox"
                                        value="${reagentDesign.businessKey}"
                                        name="businessKeyList"></stripes:checkbox></td>
                            </security:authorizeBlock>
                            <td>${reagentDesign.name}</td>
                            <td>${reagentDesign.reagentType}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>