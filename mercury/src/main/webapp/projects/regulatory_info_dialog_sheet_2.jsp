<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<c:choose>
    <c:when test="${actionBean.searchResults.isEmpty()}">
        <p>No regulatory information found in Mercury</p>
    </c:when>

    <c:otherwise>
        <p>Found existing regulatory information. Choose one to use or create a new one of a different type.</p>
        <stripes:form beanclass="${actionBean.class.name}">
            <input type="hidden" name="addRegulatoryInfoToResearchProject"/>
            <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
            <input type="hidden" id="regulatoryInfoId" name="regulatoryInfoId">
            <table id="addRegulatoryInfoDialogQueryResults" class="table simple">
                <thead>
                <th style="width:10em">Identifier</th>
                <th>Protocol Title</th>
                <th style="width:17em">Type</th>
                <th style="width:9em"></th>
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
        <hr>
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