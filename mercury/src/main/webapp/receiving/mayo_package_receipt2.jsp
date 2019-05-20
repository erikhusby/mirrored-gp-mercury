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


            <!-- User entered values that go into the RCT ticket. -->
            <table style="padding-top: 20px;" cellpadding="5">
                <tr>
                    <td>Shipment Condition</td>
                    <td><stripes:textarea style="min-width: 50em" rows="2" id="shipmentCondition" name="shipmentCondition"/></td>
                </tr>
                <tr>
                    <td>Reason to Quarantine</td>
                    <td><stripes:select id="quarantineReason" class="multiEditSelect" name="quarantineReason">
                        <stripes:option value="" label=""/>
                        <stripes:option value="Package damage" label="Package damage"/>
                        <stripes:option value="Unreadable barcode" label="UnreaLocal Courier"/>
                    </stripes:select>
                    </td>
                </tr>
                <tr>
                    <td>Tracking number</td>
                    <td><stripes:text style="min-width: 40em" id="shippingAcknowledgement" name="shippingAcknowledgement"/></td>
                </tr>
                <tr>
                    <td>Delivery Method</td>
                    <td><stripes:select id="deliveryMethod" class="multiEditSelect" name="deliveryMethod">
                        <stripes:option value="None" label="None"/>
                        <stripes:option value="FedEx" label="FedEx"/>
                        <stripes:option value="Local Courier" label="Local Courier"/>
                    </stripes:select>
                    </td>
                </tr>
            </table>

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