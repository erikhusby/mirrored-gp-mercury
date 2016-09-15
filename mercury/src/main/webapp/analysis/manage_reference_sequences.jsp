<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Reference Sequences"
                       sectionTitle="Manage Reference Sequences">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#refSeqData').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [1, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": false},
                        {"bSortable": true}
                    ]
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div id="refSeqId">
            <div id="add" class="form-horizontal span4">
                <stripes:form beanclass="${actionBean.class.name}" id="addForm">
                    <div class="control-group">
                        <stripes:label for="newName" class="control-label">Name * </stripes:label>
                        <div class="controls">
                            <stripes:text name="newName"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label for="newVersion" class="control-label">Version * </stripes:label>
                        <div class="controls">
                            <stripes:text name="newVersion"/>
                        </div>
                    </div>

                    <div class="controls">
                        <stripes:submit name="addReferenceSequence" value="Add" class="btn btn-primary"/>
                    </div>
                </stripes:form>
            </div>

            <div class="clearfix"></div>

            <stripes:form beanclass="${actionBean.class.name}" id="deleteForm">
                <div class="actionButtons">
                    <stripes:submit name="removeReferenceSequences" value="Remove Selected" class="btn"/>
                </div>

                <table id="refSeqData" class="table simple">
                    <thead>
                    <tr>
                        <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                            <th width="40">
                                <input for="count" type="checkbox" class="checkAll"/><span id="count"
                                                                                           class="checkedCount"></span>
                            </th>
                        </security:authorizeBlock>
                        <th>Name</th>
                        <th width="25">Version</th>
                        <th width="25">Current</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.referenceSequenceList}" var="refSeq">
                        <tr>
                            <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                                <td><stripes:checkbox
                                        class="shiftCheckbox"
                                        value="${refSeq.businessKey}"
                                        name="businessKeyList"></stripes:checkbox></td>
                            </security:authorizeBlock>
                            <td>${refSeq.name}</td>
                            <td>${refSeq.version}</td>
                            <td>
                                <c:if test="${refSeq.current}">
                                    <stripes:image name="" title="Yes" src="/images/check.png"/>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>