<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<c:set var="session" value="${actionBean.selectedSession}"/>
<stripes:layout-render name="/layout.jsp"
                       pageTitle="${session.researchProject.businessKey}: Sample Accessioning: ${session.sessionName}"
                       sectionTitle="${session.researchProject.businessKey}: Sample Accessioning: ${session.sessionName}"
                       showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {
                $j("#accessionSourceText").blur(function () {
                    if (!($j(this).val() == '' || $j("#accessionTubeText").val() == '')) {
                        performAccessionScan();
                    }
                });

                $j("#accessionTubeText").blur(function () {
                    if ($j(this).val() != '' && $j("#accessionSourceText").val() != '') {
                        performAccessionScan();
                    }
                });

                $j("#previewSessionCloseDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    width: 600,
                    position: {my: "center top", at: "center top", of: window}
                });

                $j('#previewSessionClose').click(function (event) {
                    event.preventDefault();
                    showPreviewSessionCloseDialog();
                    $j('#previewSessionCloseDialog').dialog("open");
                });

                $j("#associateReceiptDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    width: 600,
                    position: {my: "center top", at: "center top", of: window}
                });

                $j('#findReceipt').click(function (event) {
                    event.preventDefault();
                    showAssociateReceiptDialog();
                    $j("#associateReceiptDialog").dialog("open");
                });

                // Prevent posting the form for an enter key press in the accession source field.  Also
                // blur out of the accession source field so an enter key press essentially behaves the
                // same as a blurring tab.
                $j('#accessionSourceText').keydown(function(event) {
                    if (event.which == 13) {
                        event.preventDefault();
                        $j(this).blur();
                        return false;
                    }
                });
                // Prevent posting the form for an enter key press in the accession source field.  Also
                // blur out of the accession source field so an enter key press essentially behaves the
                // same as a blurring tab.
                $j('#accessionTubeText').keydown(function(event) {
                    if (event.which == 13) {
                        event.preventDefault();
                        $j(this).blur();
                        return false;
                    }
                });
            });

            function performAccessionScan() {
                $j.ajax({
                    url: '${ctxpath}/sample/accessioning.action',
                    data: {
                        '<%= ManifestAccessioningActionBean.SCAN_ACCESSION_SOURCE_ACTION %>': '',
                        '<%= ManifestAccessioningActionBean.SELECTED_SESSION_ID %>': '${actionBean.selectedSessionId}',
                        accessionSource: $j("#accessionSourceText").val()
                        <c:if test="${actionBean.selectedSession.fromSampleKit}">
                        ,
                        accessionTube: $j("#accessionTubeText").val()
                        </c:if>
                    },
                    datatype: 'html',
                    success: function (html) {
                        $j('#scanResults').html(html);
                        $j('#accessionSourceText').val('');
                        <c:if test="${actionBean.selectedSession.fromSampleKit}">
                        $j('#accessionTubeText').val('');
                        </c:if>
                        $j('#accessionSourceText').focus();
                    }
                });
            }

            function showPreviewSessionCloseDialog() {
                $j('#previewSessionCloseDialog').html('');

                $j.ajax({
                    url: '${ctxpath}/sample/accessioning.action',
                    data: {
                        '<%= ManifestAccessioningActionBean.PREVIEW_SESSION_CLOSE_ACTION %>': '',
                        '<%= ManifestAccessioningActionBean.SELECTED_SESSION_ID %>': '${actionBean.selectedSessionId}'
                    },
                    datatype: 'html',
                    success: function (html) {
                        var dialog = $j('#previewSessionCloseDialog');
                        dialog.html(html);
                    }
                });
            }

            function showAssociateReceiptDialog() {
                $j('#associateReceiptDialog').html('');

                $j.ajax({
                    url: '${ctxpath}/sample/accessioning.action',
                    data: {
                        '<%= ManifestAccessioningActionBean.FIND_RECEIPT_ACTION %>': '',
                        '<%= ManifestAccessioningActionBean.SELECTED_SESSION_ID %>': '${actionBean.selectedSessionId}',
                        'receiptKey': $j('#receiptKeyField').val()
                    },
                    datatype: 'html',
                    success: function (html) {
                        var dialog = $j('associateReceiptDialog');
                        dialog.html(html);
                    }
                });
            }
        </script>

    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div id="previewSessionCloseDialog" title="Preview Manifest Session Close" style="width:600px; display:none;">
        </div>

        <div id="associateReceiptDialog" title="Find and Associate Receipt Ticket" style="width:600px; display:none">
        </div>

        <div id="scanResults" width="300px">
            <jsp:include page="<%= ManifestAccessioningActionBean.SCAN_SAMPLE_RESULTS_PAGE%>"/>
        </div>
        <stripes:form beanclass="${actionBean.class.name}" id="accessionSampleForm">
            <stripes:hidden name="selectedSessionId" id="selectedSessionId"/>
            <div class="form-horizontal span6">
                <div class="control-group">
                    <label class="control-label" for="receiptKeyField">Receipt identifier</label>
                    <div class="controls">
                        <stripes:text name="selectedSession.receiptTicket" id="receiptKeyField"/>
                        <stripes:submit id="findReceipt"
                                        name="<%= ManifestAccessioningActionBean.FIND_RECEIPT_ACTION %>"
                                        value="Update receipt" class="btn"/>
                    </div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="accessionSourceText">Scan or input specimen number *</label>
                    <div class="controls">
                        <c:set var="sourcePlaceholderText" value="Enter the clinical sample ID"/>
                        <c:if test="${actionBean.selectedSession.fromSampleKit}">
                            <c:set var="sourcePlaceholderText" value="Enter the Broad sample ID"/>
                        </c:if>

                        <input type="text" class="input-xlarge" name="accessionSource" maxlength="255"
                               placeholder="${sourcePlaceholderText}" id="accessionSourceText" tabindex="1">
                    </div>
                </div>
                <c:if test="${actionBean.selectedSession.fromSampleKit}">
                    <div class="control-group">
                        <label class="control-label" for="accessionTubeText">Scan or input tube barcode *</label>

                        <div class="controls">
                            <input type="text" class="input-xlarge" name="accessionTube" maxlength="255"
                                   placeholder="Enter the 2d barcode" id="accessionTubeText" tabindex="2">
                        </div>
                    </div>
                </c:if>
                <div class="actionButtons">
                <stripes:submit id="previewSessionClose"
                                    name="<%= ManifestAccessioningActionBean.PREVIEW_SESSION_CLOSE_ACTION %>"
                                    value="Submit Session" class="btn"/>
                    <stripes:link beanclass="${actionBean.class.name}">
                        Exit Session
                    </stripes:link>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>    