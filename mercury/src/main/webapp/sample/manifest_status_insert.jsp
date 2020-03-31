<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<c:choose>
    <c:when test="${actionBean.scanErrors != null}">

        <div id="scanErrorsBlock" class="alert alert-error">
                ${actionBean.scanErrors}
        </div>
    </c:when>
    <c:otherwise>
        <c:if test="${actionBean.scanMessages != null}">
            <div id="scanMessagesBlock" class="alert alert-success">
                <c:if test="${actionBean.scanMessages.size() > 0}"><c:forEach items="${actionBean.scanMessages}" var="message">* ${message}<BR></c:forEach></c:if>
            </div>
        </c:if>
    </c:otherwise>
</c:choose>

<fieldset width="300px">
    <legend>Scan Summary</legend>
    <div style="margin-left: 20px">
        <p>Samples successfully scanned:
        <div id="successfulScan">${actionBean.statusValues.samplesSuccessfullyScanned}</div></p>
        <p>Samples eligible for accessioning in manifest:
        <div id="eligibleForAccessioning">${actionBean.statusValues.samplesEligibleForAccessioningInManifest}</div></p>
        <p>Samples quarantined: ${actionBean.statusValues.samplesQuarantined}</p>
        <p>Samples in manifest: <div>${actionBean.statusValues.samplesInManifest}</div></p>
    </div>
</fieldset>
