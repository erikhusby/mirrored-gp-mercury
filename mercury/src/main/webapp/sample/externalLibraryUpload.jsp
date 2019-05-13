<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Upload External Library Spreadsheet" sectionTitle="External Library Upload">
    <stripes:layout-component name="extraHead"/>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm">
            <div class="control-group">
                <span>
                    <div style="width: 40%; float: left">
                        <p>Upload an .xls spreadsheet of external libraries or samples.</p>
                        <div class="controls" style="padding: 20px;">
                            <stripes:file name="samplesSpreadsheet" id="samplesSpreadsheet"
                                          title="Choose the .xlsx file to upload."/>
                        </div>
                        <div style="padding-left: 20px;">
                            <span>
                                <stripes:submit name="upload" value="Upload" class="btn btn-primary"
                                                title="Mercury reads the file and creates or updates the samples and tubes."/>
                                &MediumSpace;
                                <stripes:checkbox id="overWriteFlag" name="overWriteFlag"
                                                  title="This must be checked to update existing samples and tubes."/>
                                &MediumSpace;Overwrite previous upload
                            </span>
                        </div>
                    </div>
                    <div style="width: 40%; float: left">
                        <p>Obtain a blank spreadsheet with correct headers and dropdown selections.</p>
                        <div class="controls" style="padding: 20px;">
                            <stripes:submit name="downloadTemplate" value="Download Template" class="btn btn-primary"
                                            title="Click for a blank spreadsheet."/>
                        </div>
                    </div>
                </span>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>