<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestTubeTransferActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="" sectionTitle="" showCreate="">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#sessionList').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [2, 'desc']
                    ],
                    "asStripeClasses": [ '' ],
                    "aoColumns": [
                        {"bSortable": true}, // RP Key
                        {"bSortable": true}, // Name
                        {"bSortable": true}, // Creator
                        {"bSortable": true, "sType": "date"}, // Creation Date
                        {"bSortable": true}, // Modified by
                        {"bSortable": true, "sType": "date"} // Modified Date
                    ] // Modified Date
                }).fnSetFilteringDelay(300);

                $j('#sessionList').on('click', 'tbody tr', function(event) {
                    $(this).addClass('highlighted').siblings().removeClass('highlighted');
                    var rowSessionId = $(this).children().find('input[name=sessionId]').val();
                    $j("#activeSessionId").val(rowSessionId);
                });

                var activeSession = $j("#activeSessionId").val();

                $("tr").filter(function(index){
                    return $('input[type="hidden"]').val() == activeSession;
                }).addClass('highlighted').siblings().removeClass('highlighted');

            });
        </script>

    </stripes:layout-component>

    <stripes:layout-component name="content">
        <span>Choose an existing session to complete accessioning</span>
        <stripes:form beanclass="${actionBean.class.name}" id="startNewSessionForm">
            <stripes:hidden name="activeSessionId" id="activeSessionId" />
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
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.availableSessions}" var="closedSession">
                    <tr>
                        <td name="researchProjectColumn" width="120px">
                                ${closedSession.researchProject.businessKey}
                        </td>
                        <td name="sessionNameColumn">
                                ${closedSession.sessionName}<stripes:hidden name="sessionId" value="${closedSession.manifestSessionId}" />
                        </td>
                        <td name="createdByColumn">
                                ${actionBean.getUserFullName(closedSession.updateData.createdBy)}
                        </td>
                        <td name="createdDateColumn" width="80px">
                            <fmt:formatDate value="${closedSession.updateData.createdDate}" pattern="${actionBean.datePattern}"/>
                        </td>
                        <td name="modifiedByColumn">
                                ${actionBean.getUserFullName(closedSession.updateData.modifiedBy)}
                        </td>
                        <td name="modifiedDateColumn" width="80px">
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