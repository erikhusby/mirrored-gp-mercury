<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean" %>
<%--
  This page allows the user to record a manual transfer, i.e. a transfer not done on a liquid handling deck with
  messaging.
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manual Transfer" sectionTitle="Manual Transfer">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
        <style type="text/css">
            label {
                display: inline;
                font-weight: bold;
            }
            input[type="text"].smalltext {
                width: 70px;
                font-size: 12px;
                padding: 2px 2px;
            }
            input[type='text'].barcode {
                width: 100px;
                font-size: 12px;
            }
            input[type='text'].date {
                width: 70px;
                font-size: 12px;
            }

            #analyzebtnid .ui-icon-waiting {
                background-image: url("${ctxpath}/images/spinner.gif");
                background-position: 0 center;
            }
        </style>

        <script src="${ctxpath}/resources/scripts/jsPlumb-2.1.4.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/cherryPick.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.validate-1.14.0.min.js"></script>
        <script type="text/javascript">
            $j(document).ready(function () {
                $j.validator.addMethod("unique", function(value, element) {
                    var parentForm = $j(element).closest('form');
                    var timeRepeated = 0;
                    if (value != '') {
                        $j(parentForm.find(':text.unique')).each(function () {
                            if ($j(this).val() === value) {
                                timeRepeated++;
                            }
                        });
                    }
                    return timeRepeated === 1 || timeRepeated === 0;
                }, "* Duplicate");
                $j.validator.classRuleSettings.unique = { unique: true };
                $j("#transferForm").validate();

                //video: {width: {min: 1280}, height: {min: 720}}
                var camHeight = 720;//$(window).height() - 60;
                var camWidth = 1280;//$(window).width() - 60;//
                var camInitialized = false;
                var barcodeCounter = 0;
                $j("#camera_overlay").dialog({
                    title: "Camera",
                    autoOpen: false,
                    height: camHeight + 115,
                    width: camWidth + 40,
                    modal: true,
                    buttons: {
                        "Record": {
                            text: "Record",
                            id: "recordbtnid",
                            click : function() {
                                $j('#video').show();
                                $j('#camera_alert').hide();
                                $j('#canvas').hide();
                                $j('#video').get(0).play();
                                $j('#snapbtnid').button('enable');
                                $j('#analyzebtnid').button('disable');
                                $j('#addbtnid').button('disable');
                            }
                        },
                        "Snap": {
                            text: "Snap",
                            id: "snapbtnid",
                            click: function () {
                                $j('#video').get(0).pause();
                                $j('#snapbtnid').button('disable');
                                $j('#analyzebtnid').button('enable');
                            }
                        },
                        "Analyze": {
                            text: "Analyze",
                            id: "analyzebtnid",
                            click: function () {
                                $j('#snapbtnid').button('disable');
                                var video = document.getElementById('video');
                                var canvas = document.getElementById('canvas');
                                var context = canvas.getContext('2d');
                                context.drawImage(video, 0, 0, camWidth, camHeight);
                                var scale = 4;
                                var tempCanvas = document.createElement("canvas");
                                tempCanvas.width = video.videoWidth * scale;
                                tempCanvas.height = video.videoHeight * scale;
                                tempCanvas.getContext('2d')
                                        .drawImage(video, 0, 0, tempCanvas.width, tempCanvas.height);

                                var canvasData = tempCanvas.toDataURL("image/png");
                                var data = new FormData();
                                data.append("imageFile", canvasData);
                                var eventClass = $j('input[name=\"eventClass[0]\"').val();
                                console.log(eventClass);
                                data.append("eventClass", eventClass);
                                $j('#canvas').data(null);
                                $j.ajax({
                                    url: '/Mercury/labevent/manualtransfer.action?decodeImage',
                                    data: data,
                                    cache: false,
                                    contentType: false,
                                    processData: false,
                                    datatype: "application/json",
                                    type: 'POST',
                                    beforeSend: function() {
                                        $j('#analyzebtnid').button('disable');
                                        $j('#analyzebtnid').button('option', 'icons', { primary: 'ui-icon-waiting' } );
                                    },
                                    success: function (data) {
                                        console.log(data);
                                        var canvas = document.getElementById('canvas');
                                        var context = canvas.getContext('2d');
                                        $j('#video').hide();
                                        $j('#canvas').show();
                                        if (data.hasErrors) {
                                            cameraAlert(data.errorMessage);

                                        }
                                        if (data.decodeBitmapBase64 !== null) {
                                            var img = new window.Image();
                                            img.addEventListener("load", function () {
                                                $j('#video').hide();
                                                $j('#canvas').show();
                                                context.drawImage(img, 0, 0, camWidth, camHeight);
                                                if (data.count > 0) {
                                                    $j('#addbtnid').button('enable');
                                                    $j('#canvas').data(data);
                                                }
                                            });
                                            img.setAttribute("src", data.decodeBitmapBase64);
                                        }
                                    },
                                    error: function (req, textstatus, msg) {
                                        cameraAlert("An error occured when attempting to decode barcodes");
                                        console.log(req);
                                        console.log(msg);
                                    },
                                    complete: function () {
                                        $j('#analyzebtnid').button('option', 'icons', { primary: null} );
                                    }
                                });
                            }
                        },
                        "Add Barcodes": {
                            text: "Add",
                            id: "addbtnid",
                            click: function () {
                                var data = $j('#canvas').data();
                                console.log(data);
                                var eventClass = $j('input[name=\"eventClass[0]\"').val();
                                if (eventClass === 'PlateTransferEventType') {
                                    $(data.transfers).each(function (idx, transfer) {
                                        console.log(transfer);
                                        $('#srcRcpBcd0_' + (barcodeCounter + transfer.index - 1)).val(transfer.sourceTubeBarcode);
                                        $('#destRcpBcd0_' + (barcodeCounter + transfer.index - 1)).val(transfer.destinationTubeBarcode);
                                    });
                                } else if (eventClass === 'PlateEventType') {
                                    $(data.decodedBarcodes).each(function (idx, ciBarcode) {
                                        $('#destRcpBcd0_' + (barcodeCounter + idx)).val(ciBarcode.label);
                                    });
                                }
                                barcodeCounter = barcodeCounter + data.transfers.length;
                                $j(this).dialog("close");
                            }
                        },
                        "Cancel": function() {
                            $j(this).dialog("close");
                        }
                    },
                    open: function(){
                        $j('#camera_alert').hide();
                        $j('#analyzebtnid').button('disable');
                        $j('#addbtnid').button('disable');
                        if (!camInitialized && initializeCamera()) {
                            var video = $j('<video id="video" width="' + camWidth + '" height="' + camHeight + '" autoplay></video>');
                            $j("#camera_overlay").append(video);
                            var canvas = $j('<canvas id="canvas" width="' + camWidth + '" height="' + camHeight + '"></canvas>').hide();
                            $j("#camera_overlay").append(canvas);
                        }
                    }
                });
                function initializeCamera() {
                    var hdConstraints = {
                        video: {width: {min: 1280}, height: {min: 720}}
                    };
                    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                        navigator.mediaDevices.getUserMedia(hdConstraints).then(function (stream) {
                            var video = $j('<video id="video" width="' + camWidth + '" height="' + camHeight + '" autoplay></video>');
                            $j("#camera_overlay").append(video);
                            var canvas = $j('<canvas id="canvas" width="' + camWidth + '" height="' + camHeight + '"></canvas>').hide();
                            $j("#camera_overlay").append(canvas);
                            video[0].src = window.URL.createObjectURL(stream);
                            video[0].play();
                            camInitialized = true;
                            return true;
                        }).catch(function (err) {
                            console.log(err);
                            cameraAlert("Failed to initialize camera, is it turned on? If not, start and reload page.");
                            camInitialized = false;
                            return false;
                        });
                    }
                }

                function cameraAlert(msg) {
                    $j('#camera_alert_msg').text(msg);
                    $j('#camera_alert').show();
                    $j('#video').hide();
                    $j('#canvas').hide();
                }

                function displayCamera() {
                    $j("#camera_overlay").dialog("open");
                }

                <c:if test="${actionBean.useWebCam}">
                    displayCamera();
                    $j('#displayCamera').click(function(event) {
                        event.preventDefault();
                        displayCamera();
                    });
                </c:if>
            });

            // Some scanners send carriage return, we don't want this to submit the form
            $j(document).on("keypress", ":input:not(textarea)", function(event) {
                return event.keyCode != 13;
            });

            /*
             * When the 'transfer' button is clicked, ensure reagents with expiration dates have valid dates.
             */
            function verifyReagents() {
                var failFound = false;      // Whether found expired reagents.
                var reagentExpirationElements = $j("input[id*='rgtExp']:visible"); // Search for 'input' objects with id starting with 'rgtExp'

                if (reagentExpirationElements.length > 0) {
                    var userApproved = false;   // Whether user decides to continue with expired reagents.

                    // Loop through elements found with a visible reagent expiration date field.
                    reagentExpirationElements.each(function (index) {
                        // Only provide a prompt if the date is expired and the user hasn't been prompted already.
                        if (!failFound && isDatePast(this.value)) {
                            failFound = true;
                            userApproved = confirm("A reagent is expired. A BQMS ticket is required to be made if an expired reagent is used.");
                        }
                    });

                    // return true if no expiration found or if expiration found and user confirmed to continue.
                    return !failFound || userApproved ;
                }
                // If no reagent expiration elements are found, then return true.
                return true;
            }

            /* Date passed in is expected to be in the format of mm-dd-yyyy or mm/dd/yyyy. */
            function isDatePast(dateText) {
                var today = new Date();
                // If a slash is found, use that as delimiter otherwise use a dash.
                var delimUsed = dateText.indexOf("/") > 0 ? "/" : "-";
                var inputDate = dateText.split(delimUsed);
                // Note that we subtract one from month because of weird date indexing by Date() class.
                inputDate = new Date(inputDate[2], (inputDate[0] - 1), inputDate[1], 0, 0, 0, 0);
                today = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0, 0);
                return inputDate < today;
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <c:if test="${empty actionBean.workflowStepDef}">
            <stripes:form beanclass="${actionBean.class.name}" id="eventForm">
                <stripes:select name="stationEvents[0].eventType" id="eventType">
                    <stripes:options-collection collection="${actionBean.manualEventTypes}" label="name" value="name"/>
                </stripes:select>
                <stripes:submit name="chooseEventType" value="Choose Event Type" class="btn btn-primary"/>
            </stripes:form>
        </c:if>

        <div id="camera_overlay">
            <div id="camera_alert">
                <span class="ui-icon ui-icon-alert"></span>
                <strong>Alert:</strong><p id="camera_alert_msg">Sample ui-state-error style.</p>
            </div>
        </div>
        <c:choose>
            <c:when test="${actionBean.parseLimsFile}">
                <stripes:form beanclass="${actionBean.class.name}" id="transferForm">
                    <h5>LIMS File</h5>
                    <stripes:hidden name="stationEvents[0].eventType" value="${actionBean.stationEvents[0].eventType}"/>
                    <div class="controls">
                        <stripes:select name="limsFileType" id="limsFileType">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.control.vessel.LimsFileType"
                                    label="displayName"/>
                        </stripes:select>
                    </div>
                    <div class="control-group">
                        <div class="controls">
                            <stripes:file name="limsUploadFile" id="limsUploadFile"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <div class="controls">
                            <stripes:submit name="parseLimsFile" value="Parse File" class="btn btn-primary"/>
                            <stripes:submit value="Go To Manual Transfers" name="skipLimsFile" id="SkipLimsFile" class="btn"/>
                        </div>
                    </div>
                </stripes:form>
            </c:when>
            <c:otherwise>
                <stripes:form beanclass="${actionBean.class.name}" id="transferForm" onsubmit="return verifyReagents();">
                    <%-- See https://code.google.com/p/chromium/issues/detail?id=468153 --%>
                    <div style="display: none;">
                        <input type="text" id="PreventChromeAutocomplete" name="PreventChromeAutocomplete" autocomplete="address-level4" />
                    </div>
                    <%-- Can't use stripes:text because the value in the request takes precedence over the value set in the action bean. --%>
                    <c:if test="${not empty actionBean.stationEvents}">
                        ${empty actionBean.workflowStepDef ? '' : actionBean.workflowStepDef.instructions}
                        <c:if test="${actionBean.manualTransferDetails.useWebCam}">
                            <br/>
                            <input id="displayCamera" type="submit" class="btn btn-primary" value="Display Camera">
                        </c:if>
                        <input type="hidden" name="workflowProcessName" value="${actionBean.workflowProcessName}"/>
                        <input type="hidden" name="workflowStepName" value="${actionBean.workflowStepName}"/>
                        <input type="hidden" name="workflowEffectiveDate" value="${actionBean.workflowEffectiveDate}"/>
                        <input type="hidden" name="batchName" value="${actionBean.batchName}"/>
                        <input type="hidden" name="anchorIndex" value="${actionBean.anchorIndex}"/>
                        <%-- Set by transfer_plate.jsp --%>
                        <input type="hidden" name="scanIndex" value="">
                        <%-- Set by transfer_plate.jsp --%>
                        <input type="hidden" name="scanSource" value="">

                        <c:choose>
                            <c:when test="${not empty actionBean.manualTransferDetails.machineNames}">
                                <stripes:label for="station">Machine </stripes:label>
                                <stripes:select name="stationEvents[0].station" id="station">
                                    <stripes:options-collection collection="${actionBean.manualTransferDetails.machineNames}"/>
                                </stripes:select>
                            </c:when>
                            <c:otherwise>
                                <stripes:hidden name="stationEvents[0].station" value="${actionBean.stationEvents[0].station}"/>
                            </c:otherwise>
                        </c:choose>

                        <c:if test="${not empty actionBean.stationEvents[0].reagent}">
                            <h5>Reagents</h5>
                            Expiration date format is mm/dd/yyyy.
                            <c:set var="prevReagentType" value=""/>
                            <c:forEach items="${actionBean.stationEvents[0].reagent}" var="reagent" varStatus="loop">
                                <%--@elvariable id="reagent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType"--%>
                                <c:if test="${reagent.kitType != prevReagentType && !loop.first}">
                                    </div>
                                </c:if>
                                <c:if test="${reagent.kitType != prevReagentType}">
                                    <div class="control-group">
                                    <label for="rgtType${loop.index}">Type </label>${reagent.kitType}
                                </c:if>
                                <input type="hidden" name="stationEvents[0].reagent[${loop.index}].kitType"
                                        value="${reagent.kitType}"/>
                                <label for="rgtBcd${loop.index}">Barcode </label>
                                <input type="text" id="rgtBcd${loop.index}" name="stationEvents[0].reagent[${loop.index}].barcode"
                                        value="${reagent.barcode}" class="barcode" autocomplete="off"/>
                                <c:if test="${(reagent.kitType != prevReagentType && not empty actionBean.manualTransferDetails.mapReagentNameToRequirements[reagent.kitType]
                                            && actionBean.manualTransferDetails.mapReagentNameToRequirements[reagent.kitType].expirationDateIncluded)}">
                                    <label for="rgtExp${loop.index}">Expiration </label>
                                    <input type="text" id="rgtExp${loop.index}" name="stationEvents[0].reagent[${loop.index}].expiration"
                                            value="${reagent.expiration}" class="date" autocomplete="off"/>
                                </c:if>
                                <c:set var="prevReagentType" value="${reagent.kitType}"/>
                            </c:forEach>
                            </div>
                        </c:if>
                        <c:forEach items="${actionBean.stationEvents}" var="stationEvent" varStatus="stationEventStatus">
                            <input type="hidden" name="stationEvents[${stationEventStatus.index}].eventType"
                                    value="${actionBean.stationEvents[stationEventStatus.index].eventType}"/>
                            <input type="hidden" name="eventClass[${stationEventStatus.index}]"
                                            value="${actionBean.stationEvents[0].class.simpleName}"/>
                            <c:if test="${fn:length(actionBean.stationEvents) > 1}">
                                ${stationEventStatus.index + 1}
                                <input type="hidden" name="stationEvents[${stationEventStatus.index}].metadata[0].name" value="MessageNum"/>
                                <input type="hidden" name="stationEvents[${stationEventStatus.index}].metadata[0].value" value="${stationEventStatus.index + 1}"/>
                            </c:if>
                            <c:choose>
                                <c:when test="${stationEvent.class.simpleName == 'PlateTransferEventType' or stationEvent.class.simpleName == 'PlateEventType' or stationEvent.class.simpleName == 'PlateCherryPickEvent'}">
                                    <c:set var="plateTransfer" value="${stationEvent}"/>
                                    <%--@elvariable id="plateTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType"--%>
                                    <c:if test="${stationEvent.class.simpleName == 'PlateTransferEventType' or stationEvent.class.simpleName == 'PlateCherryPickEvent'}">

                                        <c:if test="${empty actionBean.manualTransferDetails.secondaryEvent or not stationEventStatus.last}">
                                            <h4>Plate Transfer</h4>
                                            <h5>Source</h5>

                                            <c:set var="stationEvent" value="${stationEvent}" scope="request"/>
                                            <c:set var="plate" value="${plateTransfer.sourcePlate}" scope="request"/>
                                            <c:set var="positionMap" value="${plateTransfer.sourcePositionMap}" scope="request"/>
                                            <c:set var="stationEventIndex" value="${stationEventStatus.index}" scope="request"/>
                                            <c:set var="vesselTypeGeometry" value="${actionBean.manualTransferDetails.sourceVesselTypeGeometry}" scope="request"/>
                                            <c:set var="section" value="${actionBean.manualTransferDetails.sourceSection}" scope="request"/>
                                            <c:set var="source" value="${true}" scope="request"/>
                                            <c:set var="tableName" value="sourceTable" scope="request"/>
                                            <c:set var="transferType" value="${actionBean.stationEvents[stationEventStatus.index].eventType}"/>

                                            <c:choose>
                                                <c:when test="${ stationEvent.class.simpleName.equals('PlateCherryPickEvent')}">
                                                    <jsp:include page="transfer_plate_cherry_pick.jsp"/>
                                                </c:when>
                                                <c:otherwise>
                                                    <jsp:include page="transfer_plate.jsp"/>
                                                </c:otherwise>
                                            </c:choose>

                                        </c:if>

                                        <h5>Destination</h5>
                                    </c:if>
                                    <c:set var="stationEvent" value="${stationEvent}" scope="request"/>
                                    <c:set var="plate" value="${plateTransfer.plate}" scope="request"/>
                                    <c:set var="positionMap" value="${plateTransfer.positionMap}" scope="request"/>
                                    <c:set var="stationEventIndex" value="${stationEventStatus.index}" scope="request"/>
                                    <c:set var="vesselTypeGeometry" value="${actionBean.manualTransferDetails.targetVesselTypeGeometry}" scope="request"/>
                                    <c:set var="eventType" value="${stationEvent.eventType}" scope="request"/>
                                    <c:set var="section" value="${actionBean.manualTransferDetails.targetSection}" scope="request"/>
                                    <c:set var="source" value="${false}" scope="request"/>

                                    <c:choose>
                                        <c:when test = "${eventType.equals('StripTubeBTransfer')}">
                                            <jsp:include page="transfer_plate_strip_tube.jsp"/>
                                        </c:when>
                                        <c:when test="${stationEvent.class.simpleName.equals('PlateCherryPickEvent')}">
                                            <jsp:include page="transfer_plate_cherry_pick.jsp"/>
                                        </c:when>
                                        <c:otherwise>
                                            <jsp:include page="transfer_plate.jsp"/>
                                        </c:otherwise>
                                    </c:choose>

                                </c:when> <%-- end PlateTransferEventType or PlateEventType--%>

                                <c:when test="${stationEvent.class.simpleName == 'ReceptacleTransferEventType'}">
                                    <c:set var="receptacleTransfer" value="${stationEvent}"/>
                                    <%--@elvariable id="receptacleTransfer" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType"--%>
                                    <h4>Tube Transfer</h4>

                                    <div class="control-group">
                                    <h5>Source</h5>
                                        <c:choose>
                                            <c:when test="${not empty actionBean.manualTransferDetails.sourceVesselTypeGeometriesString}">
                                                <stripes:label for="sourceReceptacleType">Type </stripes:label>
                                                <stripes:select name="stationEvents[${stationEventStatus.index}].sourceReceptacle.receptacleType"
                                                                id="sourceReceptacleType">
                                                    <stripes:options-collection collection="${actionBean.manualTransferDetails.sourceVesselTypeGeometriesString}"/>
                                                </stripes:select>
                                            </c:when>
                                            <c:otherwise>
                                                <label>Type</label>
                                                ${receptacleTransfer.sourceReceptacle.receptacleType}
                                                <input type="hidden" name="stationEvents[${stationEventStatus.index}].sourceReceptacle.receptacleType"
                                                       value="${receptacleTransfer.sourceReceptacle.receptacleType}"/>
                                            </c:otherwise>
                                        </c:choose>
                                    <label for="srcRcpBcd${stationEventStatus.index}">Barcode</label>
                                    <input type="text" id="srcRcpBcd${stationEventStatus.index}" autocomplete="off"
                                            name="stationEvents[${stationEventStatus.index}].sourceReceptacle.barcode"
                                            value="${receptacleTransfer.sourceReceptacle.barcode}"
                                            class="clearable barcode unique" required/>
                                    <label for="srcRcpVol${stationEventStatus.index}">Volume</label>
                                    <input type="text" id="srcRcpVol${stationEventStatus.index}" autocomplete="off"
                                            name="stationEvents[${stationEventStatus.index}].sourceReceptacle.volume"
                                            value="${receptacleTransfer.sourceReceptacle.volume}" class="clearable barcode"/> ul
                                    </div>
                                    <div class="control-group">
                                        <h5>Destination</h5>
                                        <c:choose>
                                            <c:when test="${not empty actionBean.manualTransferDetails.targetVesselTypeGeometriesString}">
                                                <stripes:label for="targetReceptacleType">Type </stripes:label>
                                                <stripes:select name="stationEvents[${stationEventStatus.index}].receptacle.receptacleType"
                                                                id="targetReceptacleType">
                                                    <stripes:options-collection collection="${actionBean.manualTransferDetails.targetVesselTypeGeometriesString}"/>
                                                </stripes:select>
                                            </c:when>
                                            <c:otherwise>
                                                <label>Type</label>
                                                ${receptacleTransfer.receptacle.receptacleType}
                                                <input type="hidden" name="stationEvents[${stationEventStatus.index}].receptacle.receptacleType"
                                                       value="${receptacleTransfer.receptacle.receptacleType}"/>
                                                <!-- todo jmt material type? -->
                                            </c:otherwise>
                                        </c:choose>
                                        <label for="destRcpBcd${stationEventStatus.index}">Barcode</label>
                                        <input type="text" id="destRcpBcd${stationEventStatus.index}" autocomplete="off"
                                                name="stationEvents[${stationEventStatus.index}].receptacle.barcode"
                                                value="${receptacleTransfer.receptacle.barcode}"
                                                class="clearable barcode unique" required/>
                                        <label for="destRcpVol${stationEventStatus.index}">Volume</label>
                                        <input type="text" id="destRcpVol${stationEventStatus.index}" autocomplete="off"
                                                name="stationEvents[${stationEventStatus.index}].receptacle.volume"
                                                value="${receptacleTransfer.receptacle.volume}" class="clearable barcode"/> ul
                                    </div>
                                </c:when> <%-- end ReceptacleTransferEventType --%>

                                <c:when test="${stationEvent.class.simpleName == 'ReceptacleEventType'}">
                                    <c:set var="receptacleEvent" value="${stationEvent}"/>
                                    <%--@elvariable id="receptacleEvent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType"--%>
                                    <h4>Tube Event</h4>
                                    <div class="control-group">
                                        <label for="destRcpBcd${stationEventStatus.index}">
                                            ${fn:containsIgnoreCase(receptacleEvent.receptacle.receptacleType, "matrix") ? '2D ' : ''}Barcode
                                        </label>
                                        <input type="text" id="destRcpBcd${stationEventStatus.index}" autocomplete="off"
                                                name="stationEvents[${stationEventStatus.index}].receptacle.barcode"
                                                value="${receptacleEvent.receptacle.barcode}"
                                                class="clearable barcode unique" required/>
                                    </div>
                                </c:when> <%-- end ReceptacleEventType --%>
                            </c:choose>
                        </c:forEach>

                        <c:if test="${stationEvent.class.simpleName.equals('PlateCherryPickEvent')}">
                            <input type="button" value="Add Cherry Picks" id="PreviewButton" name="PreviewButton" class="btn btn-primary" >
                        </c:if>

                        <stripes:submit name="fetchExisting" value="Validate Barcodes" class="btn"/>
                        <stripes:submit name="transfer" value="${actionBean.manualTransferDetails.buttonValue}"
                                class="btn btn-primary"/>
                        <%-- todo jmt why does this require server roundtrip? --%>
                        <c:if test="${stationEvent.class.simpleName.equals('PlateCherryPickEvent')}">
                            <stripes:submit value="Clear Cherry Picks" id="ClearConnectionsButton" name="ClearConnectionsButton"  class="btn"/>
                        </c:if>
                        <input type="button" onclick="$('.clearable').each(function (){$(this).val('');});" value="Clear non-reagent fields" class="btn">

                        <div id="cherryPickSourceElements">
                            <c:forEach items="${actionBean.stationEvents}" var="stationEvent" varStatus="stationEventStatus">
                                <c:if test="${stationEvent.class.simpleName == 'PlateCherryPickEvent'}">
                                    <c:set var="plateCherryPickEvent" value="${stationEvent}"/>
                                    <%--@elvariable id="plateCherryPickEvent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent"--%>
                                    <c:forEach items="${plateCherryPickEvent.source}" var="sourceElement" varStatus="sourceStatus">
                                        <c:set var="namePrefix" value="stationEvents[${stationEventStatus.index}].source[${sourceStatus.index}]"/>
                                        <div class="sourceElements">
                                            <input type="text" readonly name="${namePrefix}.barcode" value="${sourceElement.barcode}"/>
                                            <input type="text" readonly name="${namePrefix}.well" value="${sourceElement.well}"/>->
                                            <input type="text" readonly name="${namePrefix}.destinationBarcode" value="${sourceElement.destinationBarcode}"/>
                                            <input type="text" readonly name="${namePrefix}.destinationWell" value="${sourceElement.destinationWell}"/>
                                        </div>
                                    </c:forEach>
                                </c:if>
                            </c:forEach>
                        </div>
                    </c:if>
                </stripes:form>
            </c:otherwise>
        </c:choose>
        <c:if test="${not empty actionBean.batchName}">
            <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean">
                <stripes:param name="batchName" value="${actionBean.batchName}"/>
                <stripes:param name="anchorIndex" value="${actionBean.anchorIndex}"/>
                Return to Batch Workflow page
            </stripes:link>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
