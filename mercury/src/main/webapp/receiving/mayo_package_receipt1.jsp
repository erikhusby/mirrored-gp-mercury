<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>
<script type="text/javascript">
    function receiveIt() {
        $j(".relinkMode").css('display', 'none');
        $j(".receiveMode").css('display', 'block');
        $j(".pkgIdInput").show();
        $j("#linkPkgBtn").hide();
        $j("#updateBtn").hide();
    };
    function relinkIt() {
        $j(".relinkMode").css('display', 'block');
        $j(".receiveMode").css('display', 'none');
        $j(".pkgIdInput").show();
        $j("#linkPkgBtn").show();
        $j("#updateBtn").hide();
    };
    function updateIt() {
        $j(".relinkMode").css('display', 'block');
        $j(".receiveMode").css('display', 'none');
        $j(".pkgIdInput").hide();
        $j("#linkPkgBtn").hide();
        $j("#updateBtn").show();
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
                padding-top: 15px;
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
                    <label for="selectReceive" style="display:inline; vertical-align: middle;">Receive</label>
                </span>
                <span style="padding-left: 20px;">
                    <input type="radio" id="selectLink" onchange="relinkIt()" name="mode" style="display:inline;">
                    <label for="selectLink" style="display:inline;">Link pacakge to manifest</label>
                </span>
                <span style="padding-left: 20px;">
                    <input type="radio" id="selectUpdate" onchange="updateIt()" name="mode" style="display:inline;">
                    <label for="selectUpdate" style="display:inline;">Update metadata from manifest</label>
                </span>
            </div>
            <div class="inputGroup pkgIdInput">
                <div class="inputRow">
                    <div class="firstCol">Package ID</div>
                    <div class="control-group controls">
                        <stripes:text id="packageBarcode" name="packageBarcode"/>
                    </div>
                </div>
            </div>
            <div class="inputGroup">
                <div class="inputRow receiveMode">
                    <div class="firstCol">Number of racks</div>
                    <div class="controls">
                        <stripes:text id="rackCount" name="rackCount"/>
                    </div>
                </div>
                <div class="inputRow receiveMode">
                    <div class="firstCol">Rack barcodes</div>
                    <div class="controls">
                        <stripes:textarea rows="5" id="rackBarcodeString" name="rackBarcodeString"
                                          title="For unreadable barcodes type in short identifiers such as 1, 2, 3 or a, b, c"/>
                    </div>
                </div>
                <div class="inputRow relinkMode" style="display: none;">
                    <div class="firstCol">Manifest filename</div>
                    <div class="controls">
                        <stripes:text id="filename" name="filename"/>
                    </div>
                </div>
            </div>
            <div style="padding-top: 20px;">
                <stripes:submit id="continueBtn" name="continueBtn" value="Continue" class="btn btn-primary receiveMode"/>
                <stripes:submit id="linkPkgBtn" name="linkPkgBtn" value="Link Package" class="btn btn-primary" style="display: none;"/>
                <stripes:submit id="updateMetadataBtn" name="updateMetadataBtn" value="Update Metadata" class="btn btn-primary" style="display: none;"/>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>