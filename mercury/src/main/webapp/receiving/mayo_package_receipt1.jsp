<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>
<script type="text/javascript">
    function toReceive() {
        $j(".myclass.relinkMode").css('display','none');
        $j(".myclass.receiveMode").css('display','block');
    }
    function toRelink() {
        $j(".myclass.relinkMode").css('display','block');
        $j(".myclass.receiveMode").css('display','none');
    }
    $j(document).ready(function () {
        toReceive();
    });
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
            div.inputGroup > div.inputRow > div.labelCol {
                display: table-cell;
            }
            div.inputGroup > div.inputRow > div.valueCol {
                display: table-cell;
                padding-left: 10px;
                padding-top: 10px;
            }
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="rackScanForm" class="form-horizontal">
            <div class="inputGroup">
                <div class="inputRow">
                    <div class="labelCol">Package ID</div>
                    <div class="valueCol">
                        <stripes:text id="packageBarcode" name="packageBarcode"/>
                    </div>
                </div>
                <div class="inputRow">
                    <div class="labelCol"></div>
                    <div class="valueCol">
                        <input type="radio" onchange="toReceive()" id="receive" value="receive"
                               name="Receive" style="display:inline;">
                        <input type="radio" onchange="toRelink()" id="relink" value="relink"
                               name="Relink" style="display:inline;">
                    </div>
                <div class="inputRow receiveMode">
                    <div class="labelCol">Number of racks</div>
                    <div class="valueCol">
                        <stripes:text id="rackCount" name="rackCount"/>
                    </div>
                </div>
                <div class="inputRow receiveMode">
                    <div class="labelCol">Rack barcodes</div>
                    <div class="valueCol">
                        <stripes:textarea rows="10" id="rackBarcodeString" name="rackBarcodeString"/>
                    </div>
                </div>
                <div class="inputRow relinkMode">
                    <div class="labelCol">Manifest filename</div>
                    <div class="valueCol">
                        <stripes:text id="filename" name="filename"/>
                    </div>
                </div>
                <div class="inputRow">
                    <div class="labelCol">
                        <stripes:submit id="page1ContinueBtn" name="page1ContinueBtn" value="Continue" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>