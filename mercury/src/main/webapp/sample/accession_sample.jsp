<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<c:set var="session" value="${actionBean.selectedSession}"/>
<stripes:layout-render name="/layout.jsp"
                       pageTitle="${session.researchProject.businessKey}: Buick Sample Accessioning: ${session.sessionName}"
                       sectionTitle="${session.researchProject.businessKey}: Buick Sample Accessioning: ${session.sessionName}"
                       showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {
                $j("#accessionSourceText").blur(function () {
                    if ($j(this).val() != '') {
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
            });

            function performAccessionScan() {
                $j.ajax({
                    url: '${ctxpath}/sample/accessioning.action',
                    data: {
                        '<%= ManifestAccessioningActionBean.SCAN_ACCESSION_SOURCE_ACTION %>': '',
                        '<%= ManifestAccessioningActionBean.SELECTED_SESSION_ID %>': '${actionBean.selectedSessionId}',
                        accessionSource: $j("#accessionSourceText").val()
                    },
                    datatype: 'html',
                    success: function (html) {
                        $j('#scanResults').html(html);
                        $j('#accessionSourceText').val('');
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
        </script>

    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div id="previewSessionCloseDialog" title="Preview Manifest Session Close" style="width:600px; display:none;">
        </div>

        <div id="scanResults" width="300px">
            <jsp:include page="<%= ManifestAccessioningActionBean.SCAN_SAMPLE_RESULTS_PAGE%>"/>
        </div>
        <stripes:form beanclass="${actionBean.class.name}" id="accessionSampleForm">
            <stripes:hidden name="selectedSessionId" id="selectedSessionId"/>
            <div class="form-horizontal span6">
                <div class="control-group">
                    <label class="control-label" for="accessionSourceText">Scan or input specimen number *</label>
                    <div class="controls">
                        <input type="text" class="input-xlarge" name="accessionSource" maxlength="255"
                               placeholder="Enter the clinical sample ID" id="accessionSourceText">
                    </div>
                </div>
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