<%@ include file="/resources/layout/taglibs.jsp" %>

<!-- Recursive Stripes component that will display the full tree of child projects based on the supplied parameter childProjects. -->

<%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
<%--@elvariable id="childProjects" type="java.util.List<org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject>"--%>

<stripes:layout-definition>
        <ul style="list-style:none;">
            <c:forEach items="${childProjects}" var="childProject">
                <c:set var="showDetails" value="${fn:length(childProject.childProjects) > 0}" />
                <li>
                    <c:if test="${showDetails}"><details></c:if>

                    <summary>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"
                                      event="view" title="Click to view this project">
                            <stripes:param name="researchProject" value="${childProject.businessKey}"/>
                            ${childProject.title}
                        </stripes:link>
                        (<stripes:link target="JIRA" href="${bean.jiraUrl(childProject.jiraTicketKey)}" title="Click to view this project in Jira" class="external">
                            ${childProject.businessKey}
                        </stripes:link>)</summary>

                        <stripes:layout-render name="/projects/treeview_component.jsp"
                                               childProjects="${childProject.childProjects}"
                                               bean="${bean}" />

                        <c:if test="${showDetails}"></details></c:if>
                </li>
            </c:forEach>
        </ul>

</stripes:layout-definition>