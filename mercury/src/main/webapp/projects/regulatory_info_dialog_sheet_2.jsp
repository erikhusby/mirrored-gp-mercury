<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.RegulatoryInfoActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="d-stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.RegulatoryInfoActionBean"/>

<c:choose>
    <c:when test="${not empty actionBean.searchResults}">
        <p class="alert alert-block" style="font-weight: bold">Found existing regulatory information in Mercury.</p>
        <stripes:form id="regulatoryInfoForm" beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.RegulatoryInfoActionBean">
            <%-- Hidden action is needed because the form is being submitted via AJAX and form serialization doesn't include submit buttons. --%>
            <input type="hidden" name="<%= RegulatoryInfoActionBean.ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION %>"/>
            <stripes:hidden name="researchProjectKey" value="${actionBean.researchProject.jiraTicketKey}"/>
            <input type="hidden" id="regulatoryInfoId" name="regulatoryInfoId">
            <table id="addRegulatoryInfoDialogQueryResults" class="table simple">
                <thead>
                    <tr>
                        <th style="width:10em">Identifier</th>
                        <th>Protocol Title</th>
                        <th style="width:17em">Type</th>
                        <th style="width:9em"></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.searchResults}" var="regulatoryInfo">
                        <tr>
                            <td>${regulatoryInfo.identifier}</td>
                            <td>${regulatoryInfo.name}</td>
                            <td>${regulatoryInfo.type.name}</td>
                            <td><d-stripes:submit name="<%= RegulatoryInfoActionBean.ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION %>" regulatoryInfoId="${regulatoryInfo.regulatoryInfoId}" disabled="${actionBean.isRegulatoryInfoInResearchProject(regulatoryInfo) || regulatoryInfo.userEdit}" class="btn">Add</d-stripes:submit></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </c:when>
    <c:when test="${actionBean.orspSearchResult != null}">
        <p class="alert alert-block" style="font-weight: bold">Found existing regulatory information in ORSP Portal.</p>
        <stripes:form id="regulatoryInfoForm" beanclass="${actionBean.class.name}">
            <stripes:hidden name="researchProjectKey" value="${actionBean.researchProject.jiraTicketKey}"/>
            <input type="hidden" id="regulatoryInfoId" name="regulatoryInfoId">
            <table class="table simple">
                <thead>
                    <tr>
                        <th style="width:10em">Identifier</th>
                        <th>Protocol Title</th>
                        <th style="width:17em">Type</th>
                        <th style="width:9em">ORSP Status</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>
                            ${actionBean.orspSearchResult.projectKey}
                            <input type="hidden" name="regulatoryInfoIdentifier" value="${actionBean.orspSearchResult.projectKey}">
                        </td>
                        <td>
                            ${actionBean.orspSearchResult.name}
                            <input type="hidden" name="regulatoryInfoAlias" value="${actionBean.orspSearchResult.name}">
                        </td>
                        <td>
                            ${actionBean.orspSearchResult.type.name}
                            <input type="hidden" name="regulatoryInfoType" value="${actionBean.orspSearchResult.type}">
                        </td>
                        <td>${actionBean.orspSearchResult.status}</td>
                    </tr>
                    <c:if test="${!actionBean.orspSearchResult.usable}">
                        <tr>
                            <td colspan="4">
                            <span style="font-weight: bold; color: red;">
                                WARNING: This ORSP project should not be used due to its status of "${actionBean.orspSearchResult.status}".<br>
                                Please contact <a href="mailto:orsp@broadinstitute.org">orsp@broadinstitute.org</a> for assistance.
                            </span>
                            </td>
                        </tr>
                    </c:if>
                    <c:if test="${actionBean.orspSearchResult.consentGroup}">
                        <tr>
                            <td colspan="4">
                                <span style=" font-weight: bold; color: red;">
                                    WARNING:  Consent Groups IDs are not evidence of IRB or ORSP approval; <BR>
                                    instead please use the ORSP ID associated with your IRB approval or ORSP Not Engaged/Not Human Subject Research Determination.  <BR>
                                    Contact <a href="mailto:orsp@broadinstitute.org">orsp@broadinstitute.org</a> for further assistance as needed.
                                </span>
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
            <stripes:submit name="<%= RegulatoryInfoActionBean.ADD_NEW_REGULATORY_INFO_ACTION %>" value="Add to Mercury" class="btn btn-primary"/>
            <%-- Hidden action is needed because the form is being submitted via AJAX and form serialization doesn't include submit buttons. --%>
            <input type="hidden" name="<%= RegulatoryInfoActionBean.ADD_NEW_REGULATORY_INFO_ACTION %>">
        </stripes:form>
    </c:when>
    <c:otherwise>
        <p class="alert alert-block" style="font-weight: bold">No results found in Mercury or the ORSP Portal.</p>
    </c:otherwise>
</c:choose>

<c:if test="${actionBean.addRegulatoryInfoAllowed}">
    <jsp:include page="regulatory_info_form.jsp"/>
</c:if>

<script type="text/javascript">
    (function() {
        // Catch clicks on the form, check that it's an "Add" button, and take the button's "name" as the ID to use.
        $j('#regulatoryInfoForm').click(function (event) {
            var target = event.target;
            if (target.nodeName == 'INPUT' &&
                    target.type == 'submit' &&
                    target.name == '<%= RegulatoryInfoActionBean.ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION %>') {
                $j('#regulatoryInfoId').val($j(target).attr('regulatoryInfoId'));
            }
        });

        $j('#regulatoryInfoForm').submit(function (event) {
            event.preventDefault();
            $j.ajax({
                url: $j(this).attr('action'),
                method: $j(this).attr('method'),
                data: $j(this).serialize(),
                success: successfulAddCallback,
                error: handleRegInfoAjaxError
            })
        });
    })();
</script>