<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<c:set var="session" value="${actionBean.selectedSession}"/>
<jsp:include page="<%= ManifestAccessioningActionBean.SCAN_SAMPLE_RESULTS_PAGE%>"/>

<div id="chooseExistingSession">
    <%-- Manifest errors --%>
    <c:choose>
        <c:when test="${not empty actionBean.selectedSession.manifestEvents}">
            <table id="errorList" class="table simple">
                <thead>
                <tr>
                    <th>Errors</th>
                    <th>Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.selectedSession.manifestEvents}" var="event">
                <tr>
                    <td>
                            ${event.message}
                    </td>
                    <td>
                            ${event.updateData.createdDate}
                    </td>
                </tr>
                </c:forEach>
            </table>
        </c:when>
        <c:otherwise>
            <div id="no_manifest_errors">
                No manifest errors found.
            </div>
        </c:otherwise>
    </c:choose>

    <%-- ManifestStatus error messages, these will become quarantine ManifestEvents if the user proceeds to close
         the session without resolving them. --%>
    <c:choose>
        <c:when test="${not empty actionBean.statusValues.errorMessages}">
            <div id="">
                If the following messages are not resolved before clicking Complete Session, these samples will
                be quarantined and a deviation will need to be created before any further work will be possible
                with these samples.
            </div>
            <table id="messageList" class="table simple">
                <thead>
                <tr>
                    <th>Messages</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.statusValues.errorMessages}" var="message">
                <tr>
                    <td>
                            ${message}
                    </td>
                </tr>
                </c:forEach>
            </table>
        </c:when>
        <c:otherwise>
            <div id="no_manifest_status_warnings">
                No warnings found.
            </div>
        </c:otherwise>
    </c:choose>

</div>

<stripes:form beanclass="${actionBean.class.name}" partial="true">
    <div class="actionButtons">
        <stripes:submit name="<%= ManifestAccessioningActionBean.CLOSE_SESSION_ACTION %>"
                        value="Complete Session" class="btn"/>
        <stripes:link beanclass="${actionBean.class.name}">
            Exit Session
        </stripes:link>
    </div>
</stripes:form>
