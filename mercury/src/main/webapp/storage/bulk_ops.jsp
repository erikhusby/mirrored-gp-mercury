<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.BulkStorageOpsActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="SRS Bulk Ops" sectionTitle="SRS Bulk Ops" showCreate="false" dataTablesVersion = "1.10">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            var csrfTokenName="<csrf:tokenname/>";
            var csrfTokenValue = "<csrf:tokenvalue/>";

            /**
             * Shows a fading warning and allows mouse free action to continue
             * type argument matches bootstrap types (primary,secondary,success,danger,warning,info,light,dark)
             */
            var showFadingFeedback = function( type, text ) {
                var fadeId = "msg_" + Math.ceil( Math.random() * 10000 );
                var parentDom = $j("#fadingWarn").append( '<span id="' + fadeId + '" class="alert alert-' + type + '" role="alert">' + text + '</span>');
                $j( "#" + fadeId, $j(parentDom) ).fadeOut( 2000, function() {
                    $j(this).remove();
                });
            };

            /**
             * Assumes all validation complete and ready to make ajax call
             * Returns the added element
             */
            var addBarcode = function(barcode){
                var listElement = $j("#barcodeList");
                var feedbackElement = null;

                // Check for duplicates
                if( listElement.data("barcodeList")[barcode] != undefined ) {
                    showFadingFeedback("warning", "Ignoring duplicate barcode");
                } else {
                    var responseId = "resp_" + Math.ceil( Math.random() * 10000 );
                    listElement.data("barcodeList")[barcode] = barcode;
                    feedbackElement = listElement.append('<li id="' + responseId + '" style="width:100%;padding-bottom: 12px">Processing ' + barcode + ' <img src="/Mercury/images/spinner.gif" width="16px" height="16px"/></li>');
                    feedbackElement = $j( "#" + responseId, $j(feedbackElement) );
                }

                return feedbackElement;
            };

            var doCheckout = function(evt) {
                var txtInput = $j("#txtBarcodeScanInput");
                var barcode = txtInput.val().trim();

                if( barcode != null || barcode.length > 0 ) {
                    txtInput.val("");
                } else {
                    // The input loses focus if tabbed out when empty
                    return true;
                }

                var listElement = addBarcode(barcode);
                // null if validation fails
                if( listElement != null ) {
                    $j("#statusOutput").css("display","block");
                    $j.ajax("/Mercury/vessel/bulkStorageOps.action", {
                        context: listElement,
                        dataType: "html",
                        data: [
                            {name: csrfTokenName, value: csrfTokenValue},
                            {name: "checkOut", value: ""},
                            {name: "barcode", value: barcode}
                        ],
                        complete: function (response, status) {
                            if (status != "success") {
                                $(this).html('<span style="width:100%; padding: 6px 30px 6px 12px" class="alert-danger" role="alert">An error occurred: ' + response.responseText + '</span>');
                            } else {
                                var obj = JSON.parse( response.responseText );
                                $(this).html('<span style="width:100%; padding: 6px 30px 6px 12px" class="alert-' +  obj.feedbackLevel + '" role="alert">' + obj.feedbackMessage + '</span>');
                            }
                        }
                    });
                }
                // Think it's stupid?  Then you're welcome to remove the timeout.
                window.setTimeout(function () { txtInput.focus(); }, 0);
                return true;
            };

            /**
             * Sets up event listeners
             */
            $j(document).ready(function () {
                $j("#txtBarcodeScanInput").change(doCheckout);
                $j("#barcodeList").data("barcodeList", []);
            } );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div class="container-fluid">
        <div class="row-fluid">
            <div class="span4">
                <fieldset>
                    <legend>Scan Each Rack Barcode</legend>
                    <input type="text" style="width:100px;margin-left: 12px" id="txtBarcodeScanInput" name="barcodeScanInput" tabindex="1"/><span id="fadingWarn" style="margin-left: 24px"></span>
                </fieldset>
            </div>
        </div><%--row-fluid--%>
        <div class="row-fluid"><div class="span8" id="statusOutput" style="display:none"><fieldset><legend>Output</legend>
            <ol id="barcodeList"></ol></fieldset>
        </div></div>
        </div><%--container-fluid--%>
    </stripes:layout-component>

</stripes:layout-render>