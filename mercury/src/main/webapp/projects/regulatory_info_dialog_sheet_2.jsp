<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<c:choose>
    <c:when test="${not empty actionBean.searchResults}">
        <p class="alert alert-block" style="font-weight: bold">Found existing regulatory information in Mercury.</p>
        <stripes:form beanclass="${actionBean.class.name}">
            <input type="hidden" name="addRegulatoryInfoToResearchProject"/>
            <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
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
                            <td><stripes:submit name="${regulatoryInfo.regulatoryInfoId}" disabled="${actionBean.isRegulatoryInfoInResearchProject(regulatoryInfo)}" class="btn">Add</stripes:submit></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </c:when>
    <c:when test="${actionBean.orspSearchResult != null}">
        <p class="alert alert-block" style="font-weight: bold">Found existing regulatory information in ORSP Portal.</p>
        <stripes:form beanclass="${actionBean.class.name}">
            <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
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
                            <input type="hidden" name="regulatoryInfoType" value="${actionBean.orspSearchResult.type}"
                        </td>
                        <td>${actionBean.orspSearchResult.status}</td>
                    </tr>
                    <c:if test="${!actionBean.orspSearchResult.usable}">
                        <td colspan="4">
                            <span style="font-weight: bold; color: red;">
                                WARNING: This ORSP project should not be used due to its status of "${actionBean.orspSearchResult.status}".<br>
                                Please contact <a href="mailto:orsp@broadinstitute.org">orsp@broadinstitute.org</a> for assistance.
                            </span>
                        </td>
                    </c:if>
                </tbody>
            </table>
            <stripes:submit name="addNewRegulatoryInfo" value="Add to Mercury" class="btn btn-primary"/>
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
        // Catch clicks on the table, check that it's an "Add" button, and take the button's "name" as the ID to use.
        var table = $j('#addRegulatoryInfoDialogQueryResults tbody');
        table.click(function (event) {
            var target = event.target;
            if (target.nodeName == "INPUT" &&
                    target.type == "submit" &&
                    target.value == "Add") {
                $j('#regulatoryInfoId').val(target.name);
            }
        });
    })();
</script>