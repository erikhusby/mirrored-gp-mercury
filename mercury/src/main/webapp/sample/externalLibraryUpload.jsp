<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Upload External Library Spreadsheet" sectionTitle="External Library Upload">
    <stripes:layout-component name="extraHead"/>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm">
            <h5>Instantiate samples/libraries via spreadsheet upload.
                &MediumSpace; &MediumSpace;
                <stripes:submit name="downloadTemplate" value="View Template" class="btn btn-primary"
                                title="Click for a blank spreadsheet with valid headers and dropdowns showing all valid choices."/>
            </h5>
            <div class="control-group">
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
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>