<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestTubeTransferActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Post Accessioning Sample Tube Transfer"
                       sectionTitle="Post Accessioning Sample Tube Transfer" showCreate="false">
<stripes:layout-component name="extraHead">
    <script type="text/javascript">

        $j(document).ready(function () {

            $j('#sessionList').dataTable({
                "oTableTools": ttExportDefines,
                "aaSorting": [
                    [9, 'desc']
                ],
                "asStripeClasses": [ '' ],
                "aoColumns": [
                    {"bSortable": true}, // Radio Button
                    {"bSortable": true}, // RP Key
                    {"bSortable": true}, // Name
                    {"bSortable": true}, // # Tubes Transferred
                    {"bSortable": true}, // # Total Tubes for transfer
                    {"bsortable": false}, // # tubes quarantined
                    {"bSortable": true}, // Created By
                    {"bSortable": true, "sType": "date"}, // Creation Date
                    {"bSortable": true}, // Last Modified By
                    {"bSortable": true, "sType": "date"} // Modified Date
                ]
            }).fnSetFilteringDelay(300);

            $j("#sourceTube").blur(function () {
                if ($j("#sourceTube").val()) {
                    clearErrorsAndMessages();
                    validateSource();
                }
            });
            $j("#mercurySample").blur(function () {
                if ($j("#mercurySample").val()) {
                    clearErrorsAndMessages();
                    validateSample();
                }
            });
            $j("#mercuryLabVessel").blur(function () {
                if ($j("#mercuryLabVessel").val()) {
                    clearErrorsAndMessages();
                    validateVessel();
                }
            });

            $j("#sourceTube").focus();

            // If the user tabs off the submit button, reset the focus loop on the first #sourceTube field.
            $j("#recordTransfer").blur(function () {
               $j("#sourceTube").focus();
            });

            // Prevent posting the form for an enter key press in the input text fields.  Also
            // blur out of the current text field so an enter key press essentially behaves the
            // same as a blurring tab.
            $j('#sourceTube, #mercurySample, #mercuryLabVessel').keydown(function(event) {
                if (event.which == 13) {
                    event.preventDefault();

                    // Do not do any tabindex traversal if the current input is blank.
                    if ($j(this).val()) {

                        // Traverse to the next input in tabindex order.
                        var next_idx = parseInt($j(':focus').attr('tabindex'), 10) + 1;
                        var $next_input = $j('form [tabindex=' + next_idx + ']');
                        if ($next_input.length)
                            $next_input.focus();
                        else
                            $j('form [tabindex]:first').focus();
                    }
                    return false;
                }
            });
        });

        // Clear out global errors and warnings after blurring one of the sample id or vessel input fields.
        function clearErrorsAndMessages() {
            $j(".alert, .alert-error").removeClass("alert alert-error").empty();
        }

        /**
         * Makes an Ajax call to the action bean to execute the validation on the entered source sample
         */
        function validateSource() {
            // Select the selected radio button.
            var activeSession = $j("input[name='activeSessionId']:checked").val();

            $j.ajax({
                url: "${ctxpath}/sample/manifestTubeTransfer.action",
                data: {
                    scanSource: '',
                    activeSessionId: activeSession,
                    sourceTube: $j("#sourceTube").val()
                },
                dataType: 'text',
                success: updateScanResults
            })
        }

        /**
         * Makes an Ajax call to the action bean in order to execute the validation on the entered target sample name
         */
        function validateSample() {
            var activeSession = $j("input[name='activeSessionId']:checked").val();

            $j.ajax({
                url: "${ctxpath}/sample/manifestTubeTransfer.action",
                data: {
                    scanTargetSample: '',
                    activeSessionId: activeSession,
                    targetSample: $j("#mercurySample").val()
                },
                dataType: 'text',
                success: updateScanResults
            })
        }

        /**
         * Makes an Ajax call to the action bean in order to execute the validation on the entered lab vessel.
         * While the intention of this is to validate the Vessel, part of that validation is to ensure that the given
         * target sample is actually affiliated with the given vessel.  Since the user is instructed to enter the
         * sample before the vessel, we should have both at this time.
         */
        function validateVessel() {
            var activeSession = $j("input[name='activeSessionId']:checked").val();

            $j.ajax({
                url: "${ctxpath}/sample/manifestTubeTransfer.action",
                data: {
                    scanTargetVessel: '',
                    activeSessionId: activeSession,
                    targetSample: $j("#mercurySample").val(),
                    targetVessel: $j("#mercuryLabVessel").val()
                },
                dataType: 'text',
                success: updateScanResults
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
    <span>Choose an existing session to record a tube transfer</span>
    <stripes:form beanclass="${actionBean.class.name}" id="startNewSessionForm">
        <div id="chooseExistingSession">
            <table id="sessionList" class="table simple">
                <thead>
                <tr>
                    <th></th>
                    <th>Research Project</th>
                    <th>Session Name</th>
                    <th># Tubes Transferred</th>
                    <th># Tubes Available for Transfer</th>
                    <th># Tubes Quarantined</th>
                    <th>Creator</th>
                    <th>Creation Date</th>
                    <th>Last Modified By</th>
                    <th>Last Modified Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.closedSessions}" var="closedSession">
                    <tr>
                        <td name="selection">
                            <stripes:radio name="activeSessionId" value="${closedSession.manifestSessionId}" />
                        </td>
                        <td name="researchProjectColumn" width="120px">
                                ${closedSession.researchProject.businessKey} - ${closedSession.researchProject.name}
                        </td>
                        <td name="sessionNameColumn">
                                ${closedSession.sessionName}
                            <input type="hidden" name="sessionId" value="${closedSession.manifestSessionId}"/>
                        </td>
                        <td name="tubesXferred">
                            ${closedSession.numberOfTubesTransferred}
                        </td>
                        <td name="tubesForXfer">
                            ${closedSession.numberOfTubesAvailableForTransfer}
                        </td>
                        <td name="quarantinedTubes">
                            ${closedSession.numberOfQuarantinedRecords}
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
                    <label class="control-label" for="sourceTube">Step 1</label>
                    <div class="controls">
                        <input type="text" id="sourceTube" name="sourceTube" class="input-xlarge"
                               maxlength="255" value="" placeholder="Scan collaborator tube" tabindex="1"/>
                    </div>
                </div>

                <div class="control-group">
                    <label class="control-label" for="mercurySample">Step 2</label>
                    <div class="controls">
                        <input type="text" id="mercurySample" name="targetSample" class="input-xlarge"
                                      maxlength="255" value="" placeholder="Scan target tube linear barcode" tabindex="2"/><br/>
                    </div>
                </div>

                <div class="control-group">
                    <label class="control-label" for="mercuryLabVessel">Step 3</label>
                    <div class="controls">
                        <input type="text" id="mercuryLabVessel" name="targetVessel" class="input-xlarge"
                               maxlength="255" value="" placeholder="Scan target tube 2D barcode" tabindex="3"/><br/>
                    </div>
                </div>

                <div class="control-group">
                    <label class="control-label" for="mercuryLabVessel">Step 4</label>
                    <div class="controls">
                        Execute tube transfer. Informatics has validated that these selections are valid. Perform
                        the tube transfer and then click the "Record Transfer" button.
                    </div>
                </div>

                <div class="actionButtons">
                    <stripes:submit id="recordTransfer" name="recordTransfer" value="Record Transfer" class="btn" tabindex="4"/>
                </div>
            </div>

            <div id="scanResults" class="help-block span4"></div>
        </div>
    </stripes:form>


</stripes:layout-component>
</stripes:layout-render>    