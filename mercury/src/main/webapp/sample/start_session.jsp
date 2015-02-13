<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession.SessionStatus.*" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="List Open Manifest Sessions" sectionTitle="List Open Manifest Sessions" showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {
                $j("#researchProjectKey").tokenInput(
                        // Call into the centralized AJAX RP completion on the ResearchProjectActionBean.  The selected
                        // RP key value will be POSTed into the ProjectTokenInput in ManifestAccessioningActionBean on
                        // form submission.
                        "${ctxpath}/projects/project.action?projectAutocomplete=", {
                            hintText: "Type a Research Project key or title",
                            prePopulate: ${actionBean.ensureStringResult(actionBean.projectTokenInput.completeData)},
                            resultsFormatter: formatInput,
                            tokenDelimiter: "${actionBean.projectTokenInput.separator}",
                            tokenLimit: 1,
                            preventDuplicates: true,
                            autoSelectFirstResult: true
                        }
                );

                $j('#sessionList').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [6, 'desc']
                    ],
                    "asStripeClasses": [ '' ],
                    "aoColumns": [
                        {"bSortable": true}, // Session Name
                        {"bSortable": true}, // Session Status
                        {"bSortable": true}, // Research Project
                        {"bSortable": true}, // Created By
                        {"bSortable": true, "sType": "date"}, // Creation Time
                        {"bSortable": true}, // Last Modified By
                        {"bSortable": true, "sType": "date"} // Last Modified Time
                    ]
                }).fnSetFilteringDelay(300);
            });
            function formatInput(item) {
                var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }


        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <c:set var="pendingData" value="<%= ManifestSession.SessionStatus.PENDING_SAMPLE_INFO%>"/>
        <div id="chooseExistingSession">
            <table id="sessionList" class="table simple">
                <thead>
                <tr>
                    <th>Session Name</th>
                    <th>Session Status</th>
                    <th>Research Project</th>
                    <th>Created By</th>
                    <th>Created Time</th>
                    <th>Last Modified By</th>
                    <th>Last Modified Time</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.openSessions}" var="openSession">
                    <tr>
                        <td>
                            <c:choose>
                                <c:when test="${openSession.status == pendingData}">
                                    ${openSession.sessionName}
                                </c:when>
                                <c:otherwise>
                                    <stripes:link beanclass="${actionBean.class.name}" event="loadSession">
                                        <stripes:param name="selectedSessionId"
                                                       value="${openSession.manifestSessionId}"/>
                                        ${openSession.sessionName}
                                    </stripes:link>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td style="font-weight: bold">
                                ${openSession.status}
                        </td>
                        <td>
                                ${openSession.researchProject.businessKey}
                        </td>
                        <td>
                                ${actionBean.getUserFullName(openSession.updateData.createdBy)}
                        </td>
                        <td>
                            <fmt:formatDate value="${openSession.updateData.createdDate}"
                                            pattern="${actionBean.dateTimePattern}"/>
                        </td>
                        <td>
                                ${actionBean.getUserFullName(openSession.updateData.modifiedBy)}
                        </td>
                        <td>
                            <fmt:formatDate value="${openSession.updateData.modifiedDate}"
                                            pattern="${actionBean.dateTimePattern}"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>

        <span>Or start a new session</span>

        <div id="startNewSession">
            <stripes:form beanclass="${actionBean.class.name}" id="startNewSessionForm">
                <div class="form-horizontal span6">
                    <div class="control-group">
                        <stripes:label for="researchProjectKey" class="control-label">
                            Research Project *
                        </stripes:label>
                        <div class="controls">
                            <stripes:text id="researchProjectKey" name="projectTokenInput.listOfKeys"
                                          class="defaultText input-xlarge"
                                          maxlength="255" title="Enter the JIRA ticket of the Research Project"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="manifestFile" class="control-label">Manifest File *</stripes:label>

                        <div class="controls">
                            <stripes:file id="manifestFile" name="manifestFile" title="Manifest File"/><br/>
                        </div>
                    </div>
                    <div class="actionButtons">
                        <stripes:submit name="uploadManifest" value="Upload manifest" class="btn"/>
                    </div>
                </div>
            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
