<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>
<c:set var="session" value="${actionBean.selectedSession}"/>


<script type="text/javascript">
    $j(document).ready(function () {
    });

</script>

<stripes:form beanclass="${actionBean.class.name}">
    <stripes:hidden name="selectedSessionId" id="selectedSessionId"/>
    <div class="form-horizontal span6">
        <c:if test="${actionBean.receiptKey != null}">
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
                <stripes:label name="shipmentIdentifier" id="receiptKeyLabel" class="control-label">Shipment Identifier </stripes:label>
                <div class="controls">
                    <div class="form-value"> ${actionBean.receiptSummary} </div>
                </div>
            </div>
        </c:if>

        <div class="actionButtons">
            <c:if test="${actionBean.receiptKey != null}">
                <stripes:submit name="<%= ManifestAccessioningActionBean.ASSOCIATE_RECEIPT_ACTION%>"
                                value="Yes" class="btn" />
                <stripes:hidden name="receiptKey" id="receiptKeyId" />
            </c:if>
            <stripes:link beanclass="${actionBean.class.name}">
                Cancel...
            </stripes:link>
        </div>
    </div>
</stripes:form>

