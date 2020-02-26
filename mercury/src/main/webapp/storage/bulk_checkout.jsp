<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.BulkStorageOpsActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Bulk Check-Out" sectionTitle="SRS Bulk Check-Out" showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            /**
             * Shows a fading warning and allows mouse-free action to continue
             * Type argument matches bootstrap types (primary,secondary,success,danger,warning,info,light,dark)
             */
            var showFadingFeedback = function( type, text ) {
                var fadeId = "msg_" + Math.ceil( Math.random() * 10000 );
                var parentDom = $j("#fadingWarn").append( '<span id="' + fadeId + '" class="alert alert-' + type + '" role="alert">' + text + '</span>');
                $j( "#" + fadeId, $j(parentDom) ).fadeOut( 3000, function() {
                    $j(this).remove();
                });
            };

            /**
             * Assumes all initial validation complete and ready to make ajax call
             * Returns the added element or null if additional validation (duplicate barcode) fails
             */
            var addBarcodeFeedbackElement = function(barcode){
                var listElement = $j("#barcodeList");
                var responseId = "resp_" + barcode;
                var feedbackElement = $j("#" + responseId, listElement );

                // Check for duplicates
                if( feedbackElement.length > 0 && feedbackElement.data("itemStatus") != "init" ) {
                    showFadingFeedback("warning", "Ignoring duplicate barcode");
                    return null;
                }
                var feedback = 'Processing ' + barcode + ' <img src="/Mercury/images/spinner.gif" width="16px" height="16px"/>';
                if( feedbackElement.length > 0 ) {
                    feedbackElement.html(feedback);
                } else {
                    listElement.append('<li id="' + responseId + '" data-item-status="processing" style="width:100%;padding-bottom: 12px">' + feedback + '</li>');
                    feedbackElement = $j("#" + responseId, listElement );
                }
                feedbackElement.data("itemStatus", "processing");
                return feedbackElement;
            };

            /**
             * Perform the checkout asynchronously and queue up the result status
             * */
            var doVesselAction = function(evt) {
                var txtInput = $j(evt.delegateTarget);
                var barcode = txtInput.val().trim();

                if( barcode != null || barcode.length > 0 ) {
                    txtInput.val("");
                } else {
                    // Allow input to lose focus if tabbed out when empty
                    return true;
                }

                var formData = [];
                formData.push({name: "checkOut", value: ""});
                formData.push({name: "barcode", value: barcode});
                formData.push({name: "<csrf:tokenname/>", value: "<csrf:tokenvalue/>" });

                // Null if pre-validation fails
                var listElement = addBarcodeFeedbackElement(barcode);
                if( listElement != null ) {
                    $j.ajax("/Mercury/storage/bulkStorageOps.action", {
                        context: listElement,
                        dataType: "html",
                        data: formData,
                        complete: function (response, status) {
                            this.data("itemStatus", status);
                            if (status != "success") {
                                this.html('<span style="width:100%; padding: 6px 30px 6px 12px" class="alert-danger" role="alert">An error occurred: ' + response.responseText + '</span>');
                            } else {
                                var obj = JSON.parse( response.responseText );
                                this.html('<span style="width:100%; padding: 6px 30px 6px 12px" class="alert-' +  obj.status + '" role="alert">' + obj.feedbackMsg + '</span>');
                            }
                        }
                    });
                }
                // Think it's stupid?  Then you're welcome to remove the timeout.
                window.setTimeout(function () { txtInput.focus(); }, 0);
                return true;
            };

            /**
             * Sets up event listeners, etc.
             */
            $j(document).ready(function () {
                $j("#txtBarcodeScan").change(doVesselAction);
            } );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
        <div class="row-fluid">
            <div class="span6" id="actionTabs">
                <div>
                    <fieldset>
                        <legend><h5>Check-Out: Scan Each Rack Barcode</h5></legend>
                        <input type="text" style="width:140px;margin-left: 12px" id="txtBarcodeScan" name="barcodeScan" tabindex="1"/><span id="fadingWarn" style="margin-left: 24px"></span>
                    </fieldset>
                </div>
            </div>

        </div><%--row-fluid--%>
        <div class="row-fluid"><div class="span8" id="statusOutput"><fieldset><legend>Outcome(s)</legend>
            <ol id="barcodeList">
                <c:forEach var="mapEntry" items="${actionBean.vesselsCheckOutStatus.entrySet()}">
                <li id="resp_${mapEntry.key}" data-item-status="init" style="width:100%;padding-bottom: 12px"><span style="width:100%; padding: 6px 30px 6px 12px" class="alert-info" role="alert">${mapEntry.value}</span></li>
                </c:forEach>
            </ol></fieldset>
        </div></div>
        </div><%--container-fluid--%>
    </stripes:layout-component>

</stripes:layout-render>