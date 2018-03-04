<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>
<c:set var="session" value="${actionBean.selectedSession}"/>

<stripes:form beanclass="${actionBean.class.name}">
    <stripes:hidden name="selectedSessionId" id="selectedSessionId"/>
    <input type="hidden" name="<csrf:tokenname/>" value="<csrf:tokenvalue/>"/>
    <div class="form-horizontal span6">
        <c:set var="hasErrors" value="${actionBean.scanErrors!=null}"/>
        <c:choose>
            <c:when test="${!hasErrors}">
                Is this the correct Receipt Ticket?

                <div class="view-control-group control-group">
                    <stripes:label name="receiptLabel" id="receiptKeyLabel" for="receiptJiraKey"
                                   class="control-label">Receipt Key </stripes:label>
                    <div class="controls">
                        <div class="form-value">
                            <a target="JIRA" href="${actionBean.jiraUrl(actionBean.receiptKey)}"
                               class="external" target="JIRA" id="receiptJiraKey">${actionBean.receiptKey}</a>
                        </div>
                    </div>
                </div>
                <div class="view-control-group control-group">
                    <stripes:label name="shipmentIdentifier" id="receiptKeyLabel"
                                   class="control-label">Shipment Identifier </stripes:label>
                    <div class="controls">
                        <div class="form-value"> ${actionBean.receiptSummary} </div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-error">${actionBean.scanErrors}</div>
            </c:otherwise>
        </c:choose>

        <div class="actionButtons">
            <c:if test="${!hasErrors}">
                <stripes:submit name="<%= ManifestAccessioningActionBean.ASSOCIATE_RECEIPT_ACTION%>"
                                value="Yes" class="dialog-button btn"/>
                <stripes:hidden name="receiptKey" id="receiptKeyId"/>
            </c:if>

            <stripes:submit name="<%= ManifestAccessioningActionBean.LOAD_SESSION_ACTION%>"
                            value="Cancel" class="dialog-button btn"/>
            <stripes:hidden name="selectedSessionId" value="${actionBean.selectedSessionId}"/>

        </div>
    </div>
</stripes:form>

