<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean"/>

<jsp:include page="<%= ManifestAccessioningActionBean.SCAN_SAMPLE_RESULTS_PAGE%>"/>


<div id="chooseExistingSession">
    <table id="sessionList" class="table simple">
        <thead>
        <tr>
            <th>Errors</th>
            <th>Date</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${actionBean.manifestErrors}" var="event">
            <tr>
                <td>
                        ${event.message}
                </td>
                <td>
                        ${event.updateData.createdDate}
                </td>
            </tr>

        </c:forEach>
        </tbody>
    </table>
</div>


<stripes:form beanclass="${actionBean.class.name}" partial="true">
    <div class="actionButtons">
        <stripes:submit name="<%= ManifestAccessioningActionBean.CLOSE_SESSION_ACTION %>" value="Complete Session" class="btn"/>
        <stripes:link beanclass="${actionBean.class.name}">
            Exit Session
        </stripes:link>
    </div>
</stripes:form>
