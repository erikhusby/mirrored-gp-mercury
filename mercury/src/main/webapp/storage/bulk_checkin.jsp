<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.BulkStorageOpsActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Bulk Check-In" sectionTitle="SRS Bulk Check-In" showCreate="false">

    <stripes:layout-component name="extraHead">
        <link rel="stylesheet"
              href="${ctxpath}/resources/scripts/jsTree/themes/default/style.min.css"/>
        <script src="${ctxpath}/resources/scripts/jsTree/jstree.min.js"></script>
        <style>
            .jstree-anchor {
                /*enable wrapping*/
                white-space : normal !important;
                /*ensure lower nodes move down*/
                height : auto !important;
                /*offset icon width*/
                padding-right : 18px;
                font-size: 11pt;
            }
            .show-loading-icon {
                background: url("${ctxpath}/resources/scripts/jsTree/themes/default/throbber.gif") center center no-repeat;
            }
            .storageList {
                display: inline-block;
                border-style: solid;
                border-color: #2a62bc;
                border-width: thin;
                width: 90%;
                padding: 4px;
                min-height: 20px;
                margin-left: 16px;
                margin-right: 16px;
            }
            .removal {
                color: red;
                cursor: pointer;
                font-size: large;
                font-weight: bold;
            }
        </style>

        <script type="text/javascript">

            /**
             * Shows a fading warning and allows mouse-free action to continue
             * type argument matches bootstrap types (primary,secondary,success,danger,warning,info,light,dark)
             */
            var showFadingFeedback = function( type, text ) {
                var fadeId = "msg_" + Math.ceil( Math.random() * 10000 );
                var parentDom = $j("#fadingWarn").append( '<span id="' + fadeId + '" class="alert alert-' + type + '" role="alert">' + text + '</span>');
                $j( "#" + fadeId, $j(parentDom) ).fadeOut( 3000, function() {
                    $j(this).remove();
                });
            };

            /**
             * Show modal overlay.  level:  Error, Info,
             */
            var showAlertDialog = function(level, content) {
                var theDialog = $j("#dialog-message");
                theDialog.attr("title", level);
                var theOutput = $j("#dialog-message span");
                theOutput.text(content);
                theOutput.attr("class", "alert-" + level.toLowerCase());
                theDialog.dialog("open");
            };

            /**
             * Assumes all validation complete and ready to make ajax call
             * Returns the added element or null if additional validation (duplicate barcode) fails
             */
            var addBarcodeFeedbackElement = function(barcode){
                var feedbackListElement = $j("#feedbackList");
                var feedbackElement = null;

                // Check for duplicates
                if( feedbackListElement.data("barcodeList")[barcode] != undefined ) {
                    showFadingFeedback("warning", "Ignoring duplicate barcode");
                } else {
                    var responseId;
                    do {
                        // 10,000 should be good, but check for duplicates anyways
                        responseId = "resp_" + Math.ceil( Math.random() * 10000 );
                    } while ( $j( "#" + responseId, $j(feedbackElement) ).length > 0 );
                    feedbackListElement.data("barcodeList")[barcode] = barcode;
                    feedbackElement = feedbackListElement.append('<li id="' + responseId + '" style="width:100%;padding-bottom: 12px">Processing ' + barcode + ' <img src="/Mercury/images/spinner.gif" width="16px" height="16px"/></li>');
                    feedbackElement = $j( "#" + responseId, $j(feedbackElement) );
                }

                return feedbackElement;
            };

            /**
             * Append/remove proposed storage location from list
             */
            var processTreeSelection = function( theNode, isSelect ){
                var listElement = $j("#storageNames");
                var nodeId = theNode.node.id;
                // Tree API selects on every click - remove first
                $j( "#storage_" + nodeId ).remove();

                if( isSelect ) {
                    listElement.append("<div id='storage_" + nodeId + "' data-storage-id='" + nodeId + "'><span class='removal' onclick='removeSelection(this)'>&times;</span> " + findLocationTrail(theNode) + "</div>");
                }
            };

            /**
             * Allow user to delete selected locations
             * */
            var removeSelection = function( child ){
                $j( child ).parent().remove();
            };

            /**
             * User has (hopefully) selected one or more locations at the INIT stage
             * Submit to validate capacity is available and present READY stage
             * */
            var submitSelections = function(){
                var idElements = $j("#storageNames > div");
                if( idElements.length == 0 ) {
                    showAlertDialog("Info", "No locations selected");
                    return;
                }
                var ids = [];
                $j.each(idElements, function(idx){
                    ids.push($j(this).data("storageId"));
                });

                $j("#proposedLocationIds").val( ids.join(',') );
                $j("#formValidate").submit();
            };

            /**
             * Perform the check-in asynchronously using validated locations displayed att he READY stage
             * Queue up the status results and remove the checkin location from the valid list if successful
             */
            var doVesselAction = function(evt) {
                var txtInput = $j(evt.delegateTarget);
                var barcode = txtInput.val().trim();

                if( barcode != null || barcode.length > 0 ) {
                    txtInput.val("");
                } else {
                    // The input loses focus if tabbed out when empty
                    return true;
                }

                // Next location from list
                var locElements = $j("#availableList").children();
                if( locElements.length == 0 ) {
                    showAlertDialog("Info", "No remaining selected locations.");
                    return false;
                }
                var locElement = $j( locElements[0] );
                var storageLocationId = locElement.data("storageLocationId");

                var formData = [];
                formData.push({name: "checkIn", value: ""});
                formData.push({name: "barcode", value: barcode});
                formData.push({name: "storageLocationId", value: storageLocationId});
                formData.push({name: "<csrf:tokenname/>", value: "<csrf:tokenvalue/>" });

                var feedbackElement = addBarcodeFeedbackElement(barcode);

                // null if pre-validation fails
                if( feedbackElement != null ) {
                    $j("#statusOutput").css("display","block");
                    $j.ajax("/Mercury/vessel/bulkStorageOps.action", {
                        context: feedbackElement,
                        dataType: "html",
                        data: formData,
                        complete: function (response, status) {
                            if (status != "success") {
                                $(this).html('<span style="width:100%; padding: 6px 30px 6px 12px" class="alert-danger" role="alert">An error occurred: ' + response.responseText + '</span>');
                            } else {
                                var obj = JSON.parse( response.responseText );
                                $(this).html('<span style="width:100%; padding: 6px 30px 6px 12px" class="alert-' +  obj.feedbackLevel + '" role="alert">' + obj.feedbackMessage + '</span>');
                                if( obj.feedbackLevel == 'success') {
                                    locElement.remove();
                                }
                            }
                        }
                    });
                }
                // Think it's stupid?  Then you're welcome to remove the timeout.
                window.setTimeout(function () { txtInput.focus(); }, 0);
                return true;
            };

            /**
             * Use the location tree parent hierarchy to build a path to the location on the client side
             * Only valid at the INIT stage
             */
            var findLocationTrail = function( theNode ){
                var treeInstance = theNode.instance;
                var locationTrail = theNode.node.text;
                var parentNodes = theNode.node.parents;
                // Ignore last node (# = tree's root node)
                for( var i = 0; i < parentNodes.length - 1; i++ ) {
                    locationTrail = treeInstance.get_node(parentNodes[i]).text + " > " + locationTrail;
                }
                return theNode.node.original.type + ":  " + locationTrail;
            };

            /**
             * Set up the location selection tree at the INIT stage
             */
            var initializeJsTreeNav = function() {
                $j('#ajax-jstree').jstree({
                    plugins: ["types", "wholerow"],
                    types : {
                        FREEZER: {
                        },
                        SHELF: {
                        },
                        BOX: {
                        },
                        SECTION: {
                        },
                        GAUGERACK: {
                        },
                        SLOT: {
                        },
                        LOOSE: {
                        }
                    },
                    validStorageTypes: [ 'GAUGERACK', 'LOOSE' /* TBD?: 'SHELF', 'BOX', 'SLOT' */ ],
                    core : {
                        data : {
                            url : "/Mercury/storage/storage.action?loadTreeAjax=",
                            data : function (node) {
                                if (node.id == "#") {
                                    return {"id": node.id};
                                } else {
                                    return {"id": node.data.storageLocationId};
                                }
                            },
                            dataType : "json",
                            error: function (data) {
                                console.log("Error occurred when loading tree");
                                console.log(data);
                                showAlertDialog("Error", "Error building storage hierarchy.");
                            }
                        }
                    },
                    multiple: true
                }).bind("select_node.jstree", function ( evt, theNode ) {
                    var treeInstance = theNode.instance;
                    // Only some types are selectable
                    if ( $j.inArray(theNode.node.original.type, treeInstance.settings.validStorageTypes) < 0 ) {
                        treeInstance.deselect_node( theNode.node, false );
                    } else {
                        processTreeSelection( theNode, true );
                    }
                }).bind("deselect_node.jstree", function ( evt, theNode ) {
                    processTreeSelection( theNode, false );

                });
            };

            /**
             * Sets up shared functionality
             */
            $j(document).ready(function () {
                $j( "#dialog-message" ).dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: {
                        Ok: function() {
                            $j( this ).dialog( "close" );
                        }
                    }
                });
            } );
            <c:if test="${actionBean.checkInPhase eq 'INIT'}">
            /**
             * Sets up event listeners for location selection (INIT stage)
             */
            $j(document).ready(function () {
                initializeJsTreeNav();
                $j("#btnValidateLocations").click(submitSelections);
            } );
            </c:if>
            <c:if test="${actionBean.checkInPhase eq 'READY'}">
            /**
             * Sets up event listeners for checkout (READY stage)
             */
            $j(document).ready(function () {
                $j("#txtBarcodeScan").change(doVesselAction);
                $j("#feedbackList").data("barcodeList", []);
            } );
            </c:if>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
        <div class="row-fluid">
            <div class="span6">

                <c:if test="${actionBean.checkInPhase eq 'INIT'}">
                    <form id="formValidate" action="bulkStorageOps.action?validateCheckIn=" method="post"><input type="hidden" name="proposedLocationIds" id="proposedLocationIds"/></form>
                    <fieldset>
                        <legend><h5>Check-In: Select/Deselect Proposed Location(s)</h5></legend>
                        <div id="ajax-jstree"/>
                    </fieldset>
                </c:if>
                <c:if test="${actionBean.checkInPhase eq 'READY'}">
                    <div class="control-group" style="margin-left:24px">
                        Barcode:&nbsp;<input type="text" style="width:140px;margin-left: 12px" id="txtBarcodeScan" name="barcodeScan" tabindex="1"/><span id="fadingWarn" style="margin-left: 24px"></span>
                    </div>
                    <fieldset><legend>Available selected locations:</legend>
                        <ol id="availableList">
                            <c:forEach var="loc" varStatus="row" items="${actionBean.validLocations}">
                                <li id="available_${loc.storageLocationId}" data-storage-location-id="${loc.storageLocationId}">${actionBean.storageLocPaths[loc.storageLocationId]}</li>
                            </c:forEach>
                        </ol></fieldset>
                </c:if>
            </div>
                <c:if test="${actionBean.checkInPhase eq 'INIT'}">
                <div class="span6">
                    <fieldset>
                    <legend><h5>Proposed Location(s)</h5></legend>
                    <table border="0" width="100%"><tr><td width="70%"><div id="storageNames" class="storageList"></div></td><td valign="top" width="30%"><input type="button" id="btnValidateLocations" value="Validate Selected Locations"/></td></tr></table>
                    </fieldset>
                </div>
                </c:if>
                <c:if test="${actionBean.checkInPhase eq 'READY'}">
                    <div class="span6" id="statusOutput" style="display:none">
                    <fieldset><legend>Action Outcome(s)</legend>
                        <ol id="feedbackList"></ol></fieldset>
                    </div>
                </c:if>
        </div>
        </div><%--container-fluid--%>

        <div id="dialog-message" title="Error"><p><span class="alert-error" style="float:left; margin:0 7px 50px 0;"></span></p></div>
    </stripes:layout-component>

</stripes:layout-render>