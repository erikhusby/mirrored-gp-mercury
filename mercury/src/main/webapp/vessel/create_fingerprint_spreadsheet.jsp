<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.FingerprintingSpreadsheetActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Create Fingerprint Spreadsheet for Plate"
                       sectionTitle="Create Fingerprint Spreadsheet for Plate" showCreate="false">

    <stripes:layout-component name="content">
        <p/>
        <div class="control-group">
            <stripes:form beanclass="${actionBean.class.name}" id="enterBarcodeForm" class="form-horizontal">
                <div class="controls">
                    <stripes:label for="barcodeTextbox" name="Enter plate barcode" class="control-label"/>
                    &MediumSpace;<stripes:text id="barcodeTextbox" name="plateBarcode" size="50"/>
                    &MediumSpace;<stripes:submit id="barcodeSubmit" name="barcodeSubmit" class="btn btn-primary" value="Submit"/>
                </div>
            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
