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
                    performAccessionScan();
                });


                $j("#preCloseDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    width: 600
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
                    }
                });
            }

        </script>

    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div id="preCloseDialog"  style="width:600px;display:none;"></div>


        <div id="scanResults" width="300px">

            <%--<fieldset width="300px">--%>
                <%--<legend>Scan Summary</legend>--%>
                <%--<div style="margin-left: 20px">--%>
                    <%--<p>Samples successfully scanned: ${actionBean.statusValues.samplesSuccessfullyScanned}--%>
                    <%--<p>Samples eligible in manifest: ${actionBean.statusValues.samplesEligibleInManifest}--%>
                    <%--<p>Samples in manifest: ${actionBean.statusValues.samplesInManifest}--%>
                <%--</div>--%>
            <%--</fieldset>--%>
            <jsp:include page="<%= ManifestAccessioningActionBean.SCAN_SAMPLE_RESULTS_PAGE%>" />
        </div>
        <stripes:form beanclass="${actionBean.class.name}" id="accessionSampleForm">
            <stripes:hidden name="selectedSessionId" id="selectedSessionId"/>
            <div class="form-horizontal span6">
                <div class="control-group">
                    <stripes:label for="accessionSource" class="control-label">
                        Scan or input specimen number *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="accessionSourceText" name="accessionSource"
                                      class="defaultText input-xlarge"
                                      maxlength="255" title="Enter the clinical sample ID"/>
                        <a href="javascript:performAccessionScan()">Scan</a>
                    </div>
                </div>
                <div class="actionButtons">
                    <stripes:submit name="<%= ManifestAccessioningActionBean.PREVIEW_SESSION_CLOSE_ACTION %>" value="Submit Session" class="btn"/>
                    <stripes:link beanclass="${actionBean.class.name}">
                        Exit Session
                    </stripes:link>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>    