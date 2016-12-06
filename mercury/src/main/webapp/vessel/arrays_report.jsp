<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.vessel.ArraysReportActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.ArraysReportActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Arrays Reports" sectionTitle="Arrays Reports">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function scannerCheckboxVisible() {
                var select = $j("#report");
                var div = $j("#includeScannerNameDiv");
                if (select.val() == '<%=ArraysReportActionBean.Report.SUMMARY%>') {
                    div.show();
                } else {
                    div.hide();
                }
            }
            $j(document).ready(function () {
                $j("#report").change(function () {
                    scannerCheckboxVisible();
                });
                scannerCheckboxVisible($j("#report"));
            });
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="report" class="control-label">Report</stripes:label>
                    <div class="controls">
                        <stripes:select name="report" id="report">
                            <stripes:options-enumeration
                                    enum="org.broadinstitute.gpinformatics.mercury.presentation.vessel.ArraysReportActionBean.Report"
                                    label="displayName"/>
                        </stripes:select>
                    </div>
                </div>
                <div class="control-group" id="includeScannerNameDiv">
                    <stripes:label for="includeScannerName" class="control-label">Include scanner name (slower)</stripes:label>
                    <div class="controls">
                        <input type="checkbox" name="includeScannerName">
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="pdoBusinessKeys"
                            class="control-label">Product Orders (from same Research Project)</stripes:label>
                    <div class="controls">
                        <stripes:textarea rows="2" name="pdoBusinessKeys"/>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="chipWellBarcodes" class="control-label">Chip Well Barcodes</stripes:label>
                    <div class="controls">
                        <stripes:textarea rows="10" name="chipWellBarcodes"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="download" value="Download" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>