<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Package Receipt" sectionTitle="Mayo Package Receipt">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function showIt() {
                $j("#manifestContents").show();
                $j("#showBtn").hide();
                $j("#hideBtn").show();
            }
            function hideIt() {
                $j("#manifestContents").hide();
                $j("#showBtn").show();
                $j("#hideBtn").hide();
            }
        </script>
    </stripes:layout-component>

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
            }
            div.inputGroup > div.inputRow > div.secondCol {
                display: table-cell;
                padding-left: 10px;
                padding-top: 10px;
            }
            text, textarea, .firstCol, .secondCol, span, select, option, p {
                font-size: 12px;
                font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            }
        </style>

        <!-- The manifest file in a grid. -->
        <c:if test="${!actionBean.getManifestCellGrid().isEmpty()}">
            <div>
                <span>Found manifest file: ${actionBean.filename}</span>
                <span style="padding-left: 15px;">
                    <button id="showBtn" onclick="showIt()">Show</button>
                    <button id="hideBtn" onclick="hideIt()" style="display: none;">Hide</button>
                </span>
            </div>
            <div id="manifestContents" style="padding-top: 10px; display: none";>
                <table id="manifestCellGrid" border="2">
                    <tbody>
                    <c:forEach items="${actionBean.getManifestCellGrid()}" var="manifestRow">
                        <tr>
                            <c:forEach items="${manifestRow}" var="manifestColumn">
                                <td align="center">${manifestColumn}</td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>

        <stripes:form beanclass="${actionBean.class.name}" id="saveForm" class="form-horizontal">
            <!-- The hidden variables to pass back to the action bean. -->
            <stripes:hidden name="packageBarcode" value="${actionBean.packageBarcode}"/>
            <stripes:hidden name="rackCount" value="${actionBean.rackCount}"/>
            <stripes:hidden name="rackBarcodeString" value="${actionBean.rackBarcodeString}"/>
            <stripes:hidden name="filename" value="${actionBean.filename}"/>
            <stripes:hidden name="manifestSessionId" value="${actionBean.manifestSessionId}"/>

            <div class="inputGroup">
                <div class="inputRow">
                    <div class="firstCol">Shipment Conditions</div>
                    <div class="secondCol">
                        <stripes:textarea style="min-width: 50em" rows="2" id="shipmentCondition" name="shipmentCondition"/>
                    </div>
                </div>
                <div class="inputRow">
                    <div class="firstCol">Tracking number</div>
                    <div class="secondCol">
                        <stripes:text id="trackingNumber" name="trackingNumber"/>
                    </div>
                </div>
                <div class="inputRow">
                    <div class="firstCol">Delivery Method</div>
                    <div class="secondCol">
                        <stripes:select id="deliveryMethod" class="multiEditSelect" name="deliveryMethod">
                            <stripes:option value="None" label="None"/>
                            <stripes:option value="FedEx" label="FedEx"/>
                            <stripes:option value="Local Courier" label="Local Courier"/>
                        </stripes:select>
                    </div>
                </div>
                <div style="padding-top: 20px;">
                    <p>To quarantine a rack, select a reason:</p>
                </div>
                <c:forEach items="${actionBean.rackBarcodes}" var="barcode" varStatus="item">
                    <stripes:hidden name="rackBarcodes[${item.index}]" value="${barcode}"/>
                    <div class="inputRow">
                        <div class="firstCol" style="float: right;">${barcode}</div>
                        <div class="secondCol">
                            <stripes:select id="quarantine_${item.index}" name="quarantineBarcodeAndReason['${barcode}']"
                                            style="vertical-align: bottom;">
                                <stripes:option value=""/>
                                <stripes:options-collection collection="${actionBean.rackReasons}"/>
                            </stripes:select>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <div style="padding-top: 20px;">
                <span>
                    <stripes:submit id="saveBtn" name="saveBtn" value="Receive" class="btn btn-primary"
                                    title="Receives the package."/>
                    <span style="margin-left: 20px;">
                        <stripes:submit id="cancelBtn" name="cancelBtn" value="Cancel" class="btn btn-primary"/>
                    </span>
                </span>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>