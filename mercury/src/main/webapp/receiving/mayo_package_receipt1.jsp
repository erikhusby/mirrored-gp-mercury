<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>
<script type="text/javascript">
    function receive() {
        $j(".relinkMode").css('display', 'none');
        $j(".receiveMode").css('display', 'block');
    };
    function relink() {
        $j(".relinkMode").css('display', 'block');
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
            }
            div.inputGroup > div.inputRow > div.controls {
                display: table-cell;
                padding-top: 10px;
            }
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
            <div style="padding: 20px;">
                <span>
                    <input type="radio" id="receiveId" onchange="receive()" name="mode"
                           style="display:inline;" checked="checked">
                    <label for="receiveId" style="display:inline;">Receive</label>
                </span>
                <span style="padding-left: 20px;">
                    <input type="radio" id="relinkId" onchange="relink()" name="mode"
                           style="display:inline;">
                    <label for="relinkId" style="display:inline;">Link to manifest</label>
                </span>
            </div>
            <div class="inputGroup">
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
                    <div class="control-group controls">
                        <stripes:text id="rackCount" name="rackCount"/>
                    </div>
                </div>
                <div class="inputRow receiveMode">
                    <div class="firstCol">Rack barcodes</div>
                    <div class="control-group controls">
                        <stripes:textarea rows="5" id="rackBarcodeString" name="rackBarcodeString"/>
                    </div>
                </div>
                <div class="inputRow relinkMode" style="display: none;">
                    <div class="firstCol">Manifest filename</div>
                    <div class="control-group controls">
                        <stripes:text id="filename" name="filename"/>
                    </div>
                </div>
            </div>
            <div style="padding-top: 20px;">
                <stripes:submit id="page1ContinueBtn" name="page1ContinueBtn" value="Continue" class="btn btn-primary"/>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>