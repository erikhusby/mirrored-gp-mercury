<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>
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

        <stripes:form beanclass="${actionBean.class.name}" id="saveForm" class="form-horizontal">
            <!-- The hidden variables to pass back to the action bean. -->
            <input type="hidden" name="packageBarcode" value="${actionBean.packageBarcode}"/>
            <input type="hidden" name="rackCount" value="${actionBean.rackCount}"/>
            <input type="hidden" name="rackBarcodeString" value="${actionBean.rackBarcodeString}"/>
            <input type="hidden" name="filename" value="${actionBean.filename}"/>

            <!-- Shows the manifest filename and button to download it. -->
            <c:if test="${!empty actionBean.filename}">
                <div>
                    <span>Found manifest file: ${actionBean.filename}</span>
                    <span style="padding-left: 15px;">
                        <stripes:submit id="downloadBtn" name="downloadBtn" value="Download" class="btn btn-primary"
                                        title="Downloads the manifest file."/>
                    </span>
                </div>
            </c:if>

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
                    <input type="hidden" name="rackBarcodes[${item.index}]" value="${barcode}"/>
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
                    <stripes:submit id="receiveBtn" name="receiveBtn" value="Receive" class="btn btn-primary"
                                    title="Receives the package."/>
                    <span style="margin-left: 20px;">
                        <stripes:submit id="cancelBtn" name="cancelBtn" value="Cancel" class="btn btn-primary"/>
                    </span>
                </span>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>