<%--
JSP to allow upload of fingerprinting run.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadFingerprintingRunActionBean"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Fingerprinting Run Upload"
                       sectionTitle="Fingerprinting Run Upload" showCreate="true">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="fingerprintingForm">
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="runFile" class="control-label">
                        Fingerprinting Run File
                    </stripes:label>
                    <div class="controls">
                        <stripes:file name="runFile" id="runFile"/>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="upload" value="Upload Run" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
