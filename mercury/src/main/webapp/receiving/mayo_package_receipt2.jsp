<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Mayo Package Receipt" sectionTitle="Mayo Package Receipt">

    <script type="text/javascript">

        function showHideFunction() {
            if ($j('#manifestContents').style.display === 'none') {
                $j('#manifestContents').css('display', 'block');
            } else {
                $j('#manifestContents').css('display', 'none');
            }
        }

        $j(document).ready(function () {

        });

    </script>

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


        <!-- Shows the manifest file contents. -->
        <c:if test="${!actionBean.getManifestCellGrid().isEmpty()}">
            <p>Found matching manifest file: ${actionBean.filename}</p>

            <button onclick="showHideFunction()">Show/Hide Manifest Content</button>

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
                    <div class="labelCol">Shipment Conditions</div>
                    <div class="valueCol">
                        <stripes:textarea style="min-width: 50em" rows="2" id="shipmentCondition" name="shipmentCondition"/>
                    </div>
                </div>
                <div class="inputRow">
                    <div class="labelCol">Tracking number</div>
                    <div class="valueCol">
                        <stripes:text style="min-width: 40em" id="shippingAcknowledgement" name="shippingAcknowledgement"/>
                    </div>
                </div>
                <div class="inputRow">
                    <div class="labelCol">Delivery Method</div>
                    <div class="valueCol">
                        <stripes:select id="deliveryMethod" class="multiEditSelect" name="deliveryMethod">
                            <stripes:option value="None" label="None"/>
                            <stripes:option value="FedEx" label="FedEx"/>
                            <stripes:option value="Local Courier" label="Local Courier"/>
                        </stripes:select>
                    </div>
                </div>
                <c:forEach items="${actionBean.rackBarcodes}" var="barcode" varStatus="item">
                    <stripes:hidden id="quarantineBarcode_${item.index}" name="quarantineBarcode[${item.index}]" value="${barcode}"/>
                    <div class="inputRow">
                        <div class="labelCol">Quarantine rack ${barcode}</div>
                        <div class="valueCol">
                            <stripes:select id="quarantineReason_${item.index}" class="multiEditSelect"
                                            name="quarantineReason[${item.index}]">
                                <stripes:option value="" label=""/>
                                <stripes:option value="Package damage" label="Package damage"/>
                                <stripes:option value="Unreadable barcode" label="Unreadable barcode"/>
                            </stripes:select>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <div style="padding-top: 20px;">
                <span>
                    <stripes:submit id="receivePkgBtn" name="receivePkgBtn" value="Receive" class="btn btn-primary"
                                    title="Receives the package."/>
                    <span style="margin-left: 20px;">
                        <stripes:submit id="cancelBtn" name="cancelBtn" value="Cancel" class="btn btn-primary"/>
                    </span>
                </span>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>