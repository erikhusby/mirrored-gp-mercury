<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>
<stripes:layout-render name="/layout.jsp"

<c:choose>
    <c:when test="${actionBean.scanErrors != null}">

        <div id="scanErrorsBlock" class="alert alert-error">
            ${actionBean.scanErrors}
        </div>
    </c:when>
    <c:otherwise>
        <div id="scanMessagesBlock" class="alert alert-success" >
            ${actionBean.scanMessages}
        </div>
    </c:otherwise>
</c:choose>

<fieldset width="300px">
    <legend>Scan Summary</legend>
    <div style="margin-left: 20px">
    <p>Samples successfully scanned: ${actionBean.statusValues.samplesSuccessfullyScanned}
    <p>Samples eligible in manifest: ${actionBean.statusValues.samplesEligibleInManifest}
    <p>Samples in manifest: ${actionBean.statusValues.samplesInManifest}
    </div>
</fieldset>
