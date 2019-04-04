<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Receive By External" sectionTitle="Receive External Tubes">

    <stripes:layout-component name="extraHead">

        <style type="text/css">
            .form-inline > * {
                margin:5px 3px !important;
            }

            .form-inline > .left-spacer {
                margin:5px 10px 2px 50px !important;
            }
        </style>

        <script src="${ctxpath}/resources/scripts/receive_external_shared.js"></script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div class="row">
            <div class="alert" id="error-dialog">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                <ul id="error-messages">
                </ul>
            </div>
        </div>

        <stripes:form beanclass="${actionBean.class.name}"
                      id="kitInfoForm" onsubmit="return false;">
            <c:set var="isMatrixTubes" value="${false}" scope="request"/>
            <jsp:include page="receive_ext_kit_info.jsp"/>
            <fieldset class="form-inline">
                <legend>Scan To Receive</legend>
                <div class="control-group">
                    <label class="control-label">What type of barcode is on the tube?</label>
                </div>

                <stripes:select name="barcodeType" id="barcodeType">
                    <stripes:options-enumeration
                            enum="org.broadinstitute.gpinformatics.mercury.presentation.receiving.ReceiveExternalActionBean.TubeLabelType"
                            label="displayName"/>
                </stripes:select>
                <stripes:text name="idText" id="idText" class="input-medium"/>

                <label class="control-label left-spacer">Next Open Position</label>
                <stripes:text name="nextOpenPosition" id="nextOpenPosition" readonly="true" class="input-mini"/>

                <label class="control-label">Current Vessel</label>
                <stripes:text name="currentRack" id="currentRack" class="input-medium"/>
                <stripes:button name="createContainer" id="createContainer" value="Create Container"/>
            </fieldset>

        </stripes:form>

        <a href="javascript:void(0)" id="delete">Remove Last Scanned Sample</a>
        <stripes:form beanclass="${actionBean.class.name}"
                      id="sampleSubmitForm" class="form-horizontal">
            <table id="samplesTable" class="table simple">
                <thead>
                <tr>
                    <th>Scanned Collaborator ID</th>
                    <th>Scan Type</th>  <%-- Hidden Column --%>
                    <th>Volume (uL)</th>
                    <th>Container</th>
                    <th>Position</th>
                    <th>Organism</th>
                    <th>Organism Id</th> <%-- Hidden Column --%>
                    <th>Receptacle Type</th>
                    <th>Label Format</th>
                    <th>Material Type</th>
                    <th>Original Material Type</th>
                    <th>Format Type</th>
                </tr>
                </thead>
                <tbody>
                </tbody>
            </table>
            <stripes:submit id="saveKit" name="saveKit" value="Save Kit"
                            class="btn btn-primary"/>
        </stripes:form>
    </stripes:layout-component>

</stripes:layout-render>