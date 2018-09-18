<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean"/>

<script type="text/javascript">
    $j(document).ready(function () {
        $(".control-group").removeClass("control-group");
        $(".control-label").removeClass("control-label");
        $(".controls").removeClass("controls");
        $j("#vesselBarcode").attr("value", $("#vesselLabel").val());
        $j("#accordion").accordion({collapsible: true, active: false, heightStyle: "content", autoHeight: false});
    });
</script>

<stripes:layout-render name="/layout.jsp" pageTitle="External Library Upload" sectionTitle="External Library Upload">
    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <div class="controls">
                        <div style="float: left; width: 25%;">
                            <stripes:select id="spreadsheetType" name="spreadsheetType">
                                <stripes:option value="">Select a Spreadsheet Type</stripes:option>
                                <stripes:options-enumeration
                                        enum="org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean.SpreadsheetType"
                                        label="displayName"/>
                            </stripes:select>
                        </div>
                        <div style="float: right; width: 75%;">
                            <stripes:submit name="downloadTemplate" value="View Template" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:file name="samplesSpreadsheet" id="samplesSpreadsheet"/>
                    </div>
                    <div style="margin-left: 180px; width: auto;">
                        <stripes:checkbox id="overWriteFlag" name="overWriteFlag"/>
                        <stripes:label for="overWriteFlag">
                            Overwrite previous upload
                        </stripes:label>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="uploadSamples" value="Upload Samples" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>