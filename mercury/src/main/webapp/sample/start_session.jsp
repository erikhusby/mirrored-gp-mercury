<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="" sectionTitle="" showCreate="false">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <span>Choose an existing session to complete accessioning</span>

        <div id="chooseExistingSession">
            <table id="sessionList" class="table simple">
                <thead>
                <tr>
                    <th>Research Project</th>
                    <th>Session Name</th>
                    <th>Creator</th>
                    <th>Creation Date</th>
                    <th>Last Modified By</th>
                    <th>Last Modified Date</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.openSessions}" var="openSession">
                    <tr>
                        <td>
                                ${openSession.researchProject.businessKey}
                        </td>
                        <td>
                                ${openSession.sessionName}
                        </td>
                        <td>
                                ${actionBean.getUserFullName(openSession.updateData.createdBy)}
                        </td>
                        <td>
                            <fmt:formatDate value="${openSession.updateData.createdDate}" pattern="${actionBean.datePattern}"/>
                        </td>
                        <td>
                                ${actionBean.getUserFullName(openSession.updateData.modifiedBy)}
                        </td>
                        <td>
                            <fmt:formatDate value="${openSession.updateData.modifiedDate}" pattern="${actionBean.datePattern}"/>
                        </td>
                        <td>
                            <stripes:link beanclass="${actionBean.class.name}" event="viewUpload">
                                <stripes:param name="selectedSessionId" value="${openSession.manifestSessionId}"/>
                                Load
                            </stripes:link>

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
                            <stripes:text id="researchProjectKey" name="researchProjectKey"
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