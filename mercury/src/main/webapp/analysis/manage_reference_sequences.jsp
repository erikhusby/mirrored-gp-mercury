<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.Role.*" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.DB" %>
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
                        <stripes:submit name="AddReferenceSequence" value="Add"/>
                    </div>
                </stripes:form>
            </div>

            <table id="refSeqData" class="table simple">
                <thead>
                <tr>
                    <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                        <th></th>
                    </security:authorizeBlock>
                    <th>Name</th>
                    <th>Version</th>
                    <th>Current</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.referenceSequenceList}" var="refSeq">
                    <tr>
                        <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                            <td><stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"
                                    event="RemoveReferenceSequence">
                                <stripes:param name="businessKey" value="${refSeq.businessKey}"/>
                                X</stripes:link></td>
                        </security:authorizeBlock>
                        <td>${refSeq.name}</td>
                        <td>${refSeq.version}</td>
                        <td>${refSeq.current}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>