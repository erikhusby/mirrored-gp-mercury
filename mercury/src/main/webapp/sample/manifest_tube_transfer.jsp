<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestTubeTransferActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="" sectionTitle="" showCreate="">
<stripes:layout-component name="extraHead">
    <script type="text/javascript">

        var performValidation = false;

        $j(document).ready(function () {

            $j('#sessionList').dataTable({
                "oTableTools": ttExportDefines,
                "aaSorting": [
                    [2, 'desc']
                ],
                "asStripeClasses": [ '' ],
                "aoColumns": [
                    {"bSortable": true}, // Rado Button
                    {"bSortable": true}, // RP Key
                    {"bSortable": true}, // Name
                    {"bSortable": true}, // Creator
                    {"bSortable": true, "sType": "date"}, // Creation Date
                    {"bSortable": true}, // Modified by
                    {"bSortable": true, "sType": "date"} // Modified Date
                ] // Modified Date
            }).fnSetFilteringDelay(300);

            $j('#sessionList').on('click', 'tbody tr', function (event) {
                $(this).addClass('highlighted').siblings().removeClass('highlighted');
                var rowSessionId = $(this).children().find('input[name=sessionId]').val();
                $j("#activeSessionId").val(rowSessionId);
            });

            var activeSession = $j("#activeSessionId").val();

//            $j('input[type="hidden"]').filter(function(index) {
//                return $(this).val() == activeSession;
//            }).parentsUntil("tr").addClass('highlighted').siblings().removeClass('highlighted');
//
//            $("tr").filter(function (index) {
//                return $('input[type="hidden"]').val() == activeSession;
//            }).addClass('highlighted').siblings().removeClass('highlighted');

            $j("#source").blur(function () {
                if ($j("#source").val()) {
                    validateSource();
                    performValidation = false;
                }
            });
            $j("#mercurySample").blur(function () {
                if ($j("#mercurySample").val()) {
                    validateSample();
                    performValidation = false;
                }
            });
            $j("#mercuryLabVessel").blur(function () {
                if ($j("#mercuryLabVessel").val()) {
                    validateVessel();
                    performValidation = false;
                }
            });
        });

        function validateSource() {
            var activeSession = $j("#activeSessionId").val();

            $j.ajax({
                url: "${ctxpath}/sample/manifestTubeTransfer.action",
                data: {
                    scanSource: '',
                    activeSessionId: activeSession,
                    sourceTube: $j("#source").val()
                },
                dataType: 'text',
                success: updateScanResults,
                error: function (jqXHR, textStatus, errorThrown) {
                    var message = "An error occurred attempting to validate the source tube.  Please try " +
                            "again or create a Jira ticket: " + textStatus;

                    updateScanResults(message);
                }
            })
        }

        function validateSample() {

            $j.ajax({
                url: "${ctxpath}/sample/manifestTubeTransfer.action",
                data: {
                    scanTargetSample: '',
                    targetSample: $j("#mercurySample").val()
                },
                dataType: 'text',
                success: updateScanResults,
                error: function (jqXHR, textStatus, errorThrown) {
                    var message = "An error occurred attempting to validate the target sample.  Please try " +
                            "again or create a Jira ticket: " + textStatus;

                    updateScanResults(message);
                }
            })
        }

        function validateVessel() {
            var activeSession = $j("#activeSessionId").val();

            $j.ajax({
                url: "${ctxpath}/sample/manifestTubeTransfer.action",
                data: {
                    scanTargetVessel: '',
                    targetSample: $j("#mercurySample").val(),
                    targetVessel: $j("#mercuryLabVessel").val()
                },
                dataType: 'text',
                success: updateScanResults,
                error: function (jqXHR, textStatus, errorThrown) {
                    var message = "An error occurred attempting to validate the target vessel and sample.  " +
                            "Please try again or create a Jira ticket: " + textStatus;

                    updateScanResults(message);
                }
            })
        }

        function updateScanResults(resultsMessage) {

            $j("#scanResults").empty();

            var message = "Scan Successful";
            if (resultsMessage) {
                $j("#scanResults").append('<div class="alert alert-error" style="font-size: 14px;margin-left:20%;margin-right:20%">' + resultsMessage + "</div>");
            } else {
                $j("#scanResults").append('<div class="alert alert-success" style="font-size: 14px;margin-left:20%;margin-right:20%">' + message + "</div>");
            }
        }
    </script>

</stripes:layout-component>

<stripes:layout-component name="content">
    <span>Choose an existing session to complete accessioning</span>
    <stripes:form beanclass="${actionBean.class.name}" id="startNewSessionForm">
        <stripes:hidden name="activeSessionId" id="activeSessionId"/>
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
                        <td name="selection">
                            <stripes:radio name="activeSessionId" value="${closedSession.manifestSessionId}" />
                        </td>
                        <td name="researchProjectColumn" width="120px">
                                ${closedSession.researchProject.businessKey}
                        </td>
                        <td name="sessionNameColumn">
                                ${closedSession.sessionName}
                            <input type="hidden" name="sessionId" value="${closedSession.manifestSessionId}"/>
                        </td>
                        <td name="createdByColumn">
                                ${actionBean.getUserFullName(closedSession.updateData.createdBy)}
                        </td>
                        <td name="createdDateColumn" width="80px">
                            <fmt:formatDate value="${closedSession.updateData.createdDate}"
                                            pattern="${actionBean.datePattern}"/>
                        </td>
                        <td name="modifiedByColumn">
                                ${actionBean.getUserFullName(closedSession.updateData.modifiedBy)}
                        </td>
                        <td name="modifiedDateColumn" width="80px">
                            <fmt:formatDate value="${closedSession.updateData.modifiedDate}"
                                            pattern="${actionBean.datePattern}"/>
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
                        Step 1
                        (Source sample) *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="source" name="sourceTube"
                                      class="defaultText input-xlarge"
                                      maxlength="255" value="" tabindex="1"/>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="mercurySample" class="control-label">
                        Step 2 (Sample key) *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="mercurySample" name="targetSample" class="defaultText input-xlarge"
                                      maxlength="255" value="" tabindex="2"/><br/>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="mercuryLabVessel" class="control-label">
                        Step 3 (Lab vessel) *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="mercuryLabVessel" name="targetVessel" class="defaultText input-xlarge"
                                      maxlength="255" value="" tabindex="3"/><br/>
                    </div>
                </div>


                <div class="control-group">
                    <stripes:label for="mercuryLabVessel" class="control-label">
                        Step 4
                    </stripes:label>
                    <div class="controls">
                        Execute tube transfer. Informatics has validated that these selections are valid. Perform
                        the tube transfer and then click the "Record Transfer" button.
                    </div>
                </div>
                <div class="actionButtons">
                    <stripes:submit name="recordTransfer" value="Record Transfer" class="btn" tabindex="4"/>
                </div>
            </div>

            <div id="scanResults" class="help-block span4"></div>
        </div>
    </stripes:form>


</stripes:layout-component>
</stripes:layout-render>    