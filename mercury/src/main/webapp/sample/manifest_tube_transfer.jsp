<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestTubeTransferActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="" sectionTitle="" showCreate="">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <span>Choose an existing session to complete accessioning</span>
        <stripes:form beanclass="${actionBean.class.name}" id="startNewSessionForm">

        <div id="chooseExistingSession">
            <table id="sessionList" class="table simple">
                <thead>
                <tr>
                    <th></th>
                    <th>Research Project</th>
                    <th>Session Name</th>
                    <th>Creator</th>
                    <th>Creation Date</th>
                    <th>Last Modified By</th>
                    <th>Last Modified Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.availableSessions}" var="closedSession">
                    <tr>
                        <td>
                            <stripes:radio value="${closedSession.manifestSessionId}" name="activeSessionId" />
                        </td>
                        <td>
                                ${closedSession.researchProject.businessKey}
                        </td>
                        <td>
                                ${closedSession.sessionName}
                        </td>
                        <td>
                                ${actionBean.getUserFullName(closedSession.updateData.createdBy)}
                        </td>
                        <td>
                            <fmt:formatDate value="${closedSession.updateData.createdDate}" pattern="${actionBean.datePattern}"/>
                        </td>
                        <td>
                                ${actionBean.getUserFullName(closedSession.updateData.modifiedBy)}
                        </td>
                        <td>
                            <fmt:formatDate value="${closedSession.updateData.modifiedDate}" pattern="${actionBean.datePattern}"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>


        <div id="startNewSession">
                <div class="form-horizontal span6">
                    <div class="control-group">
                        <stripes:label for="source" class="control-label">
                            Step 1 (Source Tube) *
                        </stripes:label>
                        <div class="controls">
                            <stripes:text id="source" name="sourceTube"
                                          class="defaultText input-xlarge"
                                          maxlength="255" title="Enter the specimen number"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="mercurySample" class="control-label">
                            Step 2 (Mercury Sample Key) *
                        </stripes:label>
                        <div class="controls">
                            <stripes:text id="mercurySample" name="targetSample" class="defaultText input-xlarge"
                                          maxlength="255" title="Enter target mercury sample key"/><br/>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label for="mercuryLabVessel" class="control-label">
                            Step 3 (Mercury Lab Vessel) *
                        </stripes:label>
                        <div class="controls">
                            <stripes:text id="mercuryLabVessel" name="targetVessel" class="defaultText input-xlarge"
                                          maxlength="255" title="Enter target mercury lab vessel"/><br/>
                        </div>
                    </div>


                    <div class="control-group">
                        <stripes:label for="mercuryLabVessel" class="control-label">
                            Step 4
                        </stripes:label>
                        <div class="controls">
                            Execute tube transfer.  Informatics has validated that these selections are valid.  Perform
                            the tube transfer and then click the "Record Transfer" button.
                        </div>
                    </div>
                    <div class="actionButtons">
                        <stripes:submit name="recordTransfer" value="Record Transfer" class="btn"/>
                    </div>
                </div>
        </div>
            </stripes:form>


    </stripes:layout-component>
</stripes:layout-render>    