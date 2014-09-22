<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<c:set var="session" value="${actionBean.selectedSession}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="${session.researchProject.businessKey}: Buick Sample Accessioning: ${session.sessionName}"
                       sectionTitle="${session.researchProject.businessKey}: Buick Sample Accessioning: ${session.sessionName}" showCreate="false">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <fieldset>
            <legend>Scan Summary</legend>
            Samples successfully scanned: ${actionBean.statusValues.samplesSuccessfullyScanned}
            Samples eligible in manifest: ${actionBean.statusValues.samplesEligibleInManifest}
            Samples in manifest: ${actionBean.statusValues.samplesInManifest}
        </fieldset>

        <stripes:form beanclass="${actionBean.class.name}" id="accessionSampleForm">
            <div class="form-horizontal span6">
                <div class="control-group">
                    <stripes:label for="accessionSource" class="control-label">
                        Scan or input specimen number *
                    </stripes:label>
                    <div class="controls">
                        <stripes:text id="accessionSource" name="accessionSource"
                                      class="defaultText input-xlarge"
                                      maxlength="255" title="Enter the clinical sample ID"/>
                    </div>
                </div>
            </div>

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>    