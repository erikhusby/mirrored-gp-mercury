<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.SampleVesselActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Upload Samples" sectionTitle="Upload Samples">
    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="samplesSpreadsheet" class="control-label">
                        Samples Spreadsheet
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="samplesSpreadsheet" id="samplesSpreadsheet"/>
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