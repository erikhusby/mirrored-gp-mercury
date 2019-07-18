<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>
<script type="text/javascript">
    function receiveIt() {
        $j(".updateMode").css('display', 'none');
        $j(".receiveMode").css('display', 'block');
    };
    function updateIt() {
        $j(".updateMode").css('display', 'block');
        $j(".receiveMode").css('display', 'none');
    };
</script>

<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Package Receipt" sectionTitle="Mayo Package Receipt">
    <stripes:layout-component name="content">
        <style type="text/css">
            div.inputGroup {
                display: table;
            }
            div.inputGroup > div.inputRow {
                display: table-row;
            }
            div.inputGroup > div.inputRow > div.firstCol {
                display: table-cell;
                width: 10em;
                vertical-align: middle;
                padding-top: 10px;
                padding-right: 20px;            }
            div.inputGroup > div.inputRow > div.controls {
                display: table-cell;
                vertical-align: middle;
                padding-top: 15px;
            }
            text, textarea, .firstCol, .controls {
                font-size: 12px;
                font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            }
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
            <div style="padding: 20px;">
                <span>
                    <input type="radio" id="selectReceive" onchange="receiveIt()" name="mode" checked="checked"
                           style="display:inline; vertical-align: middle;">
                    <label for="selectReceive" style="display:inline; vertical-align: middle;">Receive package</label>
                </span>
                <span style="padding-left: 20px;">
                    <input type="radio" id="selectUpdate" onchange="updateIt()" name="mode" style="display:inline;">
                    <label for="selectUpdate" style="display:inline;">Update manifest</label>
                </span>
            </div>
            <div class="inputGroup" title="The package barcode, starting with 'PKG'">
                <div class="inputRow">
                    <div class="firstCol">Package ID</div>
                    <div class="control-group controls">
                        <stripes:text id="packageBarcode" name="packageBarcode"/>
                    </div>
                </div>
            </div>
            <div class="inputGroup receiveMode">
                <div class="inputRow receiveMode">
                    <div class="firstCol">Number of racks</div>
                    <div class="controls">
                        <stripes:text id="rackCount" name="rackCount"/>
                    </div>
                </div>
                <div class="inputRow receiveMode" title="For unreadable barcodes type in short identifiers such as 1, 2, 3 or a, b, c">
                    <div class="firstCol">Rack barcodes</div>
                    <div class="controls">
                        <stripes:textarea rows="5" id="rackBarcodeString" name="rackBarcodeString"/>
                    </div>
                </div>
            </div>
            <div class="inputGroup">
                <div class="inputRow" title="Full path to the file including folder names, slashes, and the file suffix.
Leave this blank if you want Mercury to search for the file.">
                    <div class="firstCol">Manifest filename</div>
                    <div class="controls">
                        <stripes:text id="filename" name="filename"/>
                    </div>
                </div>
                <div class="inputRow" title="Check this to allow an existing package receipt to be updated with new values.">
                    <div class="firstCol">
                        <div style="float: left;">Allow Update</div>
                        <div style="float: right;">
                            <stripes:checkbox style="" id="allowUpdate" name="allowUpdate"/>
                        </div>
                    </div>
                </div>
            </div>
            <div style="padding-top: 20px;">
                <stripes:submit id="continueBtn" name="continueBtn" value="Continue" class="btn btn-primary receiveMode"/>
                <stripes:submit id="updateManifestBtn" name="updateManifestBtn" value="Update Manifest" class="btn btn-primary updateMode" style="display: none;"/>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>