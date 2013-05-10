<%@ include file="/resources/layout/taglibs.jsp" %>

<!-- Recursive Stripes component that will display the full tree of child projects based on the supplied parameter childProjects. -->

<stripes:layout-definition>
    <c:if test="${(childProjects != null)}">
        <stripes:useActionBean var="projectLinkBean"
                               beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" />

        <ul>
            <c:forEach items="${childProjects}" var="childProject">
                <c:set var="showDetails" value="${(childProject.childProjects != null) && fn:length(childProject.childProjects) > 0}" />
                <li>
                    <c:if test="${showDetails}"><details></c:if>

                    <summary>
                        <stripes:link beanclass="${projectLinkBean.class.name}" event="view" title="Click to view this project">
                            <stripes:param name="researchProject" value="${childProject.businessKey}"/>
                            ${childProject.title}
                        </stripes:link>
                        (<stripes:link target="JIRA" href="${bean.jiraUrl(childProject.jiraTicketKey)}" title="Click to view this project in LabOpsJira" class="external">
                            ${childProject.businessKey}
                        </stripes:link>)</summary>

                        <stripes:layout-render name="/projects/treeview_component.jsp"
                                               childProjects="${childProject.childProjects}"
                                               bean="${bean}" />

                        <c:if test="${showDetails}"></details></c:if>
                </li>
            </c:forEach>
        </ul>

    </c:if>

</stripes:layout-definition>