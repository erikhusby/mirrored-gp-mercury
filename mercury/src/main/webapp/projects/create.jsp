<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Edit Research Project" sectionTitle="Edit Project: ${actionBean.researchProject.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                    function () {
                        $j("#projectManagers").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true
                                }
                        );
                        $j("#scientists").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true
                                }
                        );
                        $j("#externalCollaborators").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true
                                }
                        );
                        $j("#broadPIs").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true
                                }
                        );
                    }
            );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form action="/projects/project.action" id="createForm" class="form-horizontal">
        <stripes:hidden name="businessKey" value="${actionBean.researchProject.jiraTicketKey}"/>
        <div class="form-horizontal">
            <div class="control-group">
                <stripes:label for="title" name="Project" class="control-label"/>
                <div class="controls">
                        <stripes:text name="researchProject.title" value="${actionBean.researchProject.title}" id="title" title="Enter in the project name"/>
                </div>
            </div>

            <!-- Synopsis -->
            <div class="control-group">
                <stripes:label for="synopsis" name="Synopsis" class="control-label"/>

                <div class="controls">
                    <stripes:text id="synopsis" name="researchProject.synopsis" class="defaultText"
                                  title="Enter the synopsis of the project" value="${actionBean.researchProject.synopsis}"/>
                </div>
            </div>

            <!-- Project Managers -->
            <div class="control-group">
                <stripes:label for="projectManagers" name="Project Managers" class="control-label"/>

                <div class="controls">
                    <stripes:text id="projectManagers" name="researchProject.projectManagers" class="defaultText"
                                  title="Type text to search to search for Project Managers"
                                  value="${actionBean.researchProject.projectManagers}"/>
                </div>
            </div>

            <!-- Broad PIs -->
            <div class="control-group">
                <stripes:label for="broadPIs" name="Broad PIs" class="control-label"/>

                <div class="controls">
                    <stripes:text id="broadPIs" name="researchProject.broadPIs" class="defaultText"
                                  title="Type text to search to search for Broad PIs"
                                  value="${actionBean.researchProject.broadPIs}"/>
                </div>
            </div>

            <!-- External Collaborators -->
            <div class="control-group">
                <stripes:label for="externalCollaborators" name="External Collaborators" class="control-label"/>
                <div class="controls">
                    <stripes:text id="externalCollaborators" name="researchProject.externalCollaborators" class="defaultText"
                                  title="Type text to search to search for External Collaborators"
                                  value="${actionBean.researchProject.externalCollaborators}"/>
                </div>
            </div>

            <!-- Project Managers -->
            <div class="control-group">
                <stripes:label for="scientists" name="Scientists" class="control-label"/>
                <div class="controls">
                    <stripes:text id="scientists" name="researchProject.scientists" class="defaultText"
                                  title="Type text to search to search for Scientists"
                                  value="${actionBean.researchProject.scientists}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="createdBy" name="createdBy" class="control-label"/>

                <div class="controls">
                    <div class="form-value">
                        ${actionBean.researchProjectCreatorString} on <fmt:formatDate value="${actionBean.researchProject.createdDate}" pattern="MM/dd/yyyy"/>
                    </div>
                </div>
            </div>


            <div class="control-group">
                <stripes:label for="fundingSourcesListString" name="Funding Sources" class="control-label"/>

                <div class="controls">
                    <stripes:text id="fundingSourcesListString" name="researchProject.fundingSourcesListString" class="defaultText"
                                  title="Type text to search to search for Funding Sources"
                                  value="${actionBean.researchProject.fundingSourcesListString}"/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="cohortsListString" name="Sample Cohorts" class="control-label"/>

                <div class="controls">
                    <stripes:text id="cohortsListString" name="researchProject.cohortsListString" class="defaultText"
                                  title="Type text to search to search for Cohorts"
                                  value="${actionBean.researchProject.cohortsListString}"/>
                </div>
            </div>


            <div class="control-group">
                <stripes:label for="irbNotEngaged" name="IRB Not Engaged" class="control-label"/>

                <div class="controls">
                    <stripes:checkbox id="irbNotEngaged" name="researchProject.irbNotEngaged"
                                      class="defaultText" value="${actionBean.researchProject.irbNotEngaged}"/>
                </div>
            </div>
        </div>

        <div class="tableBar">
            Orders
        </div>

        <table id="orderList" class="table simple">
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Order ID</th>
                    <th>Product</th>
                    <th>Status</th>
                    <th>Owner</th>
                    <th>Updated</th>
                    <th>Samples</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.researchProject.productOrders}" var="order">
                    <tr>
                        <td>
                            <stripes:link href="/orders/order.action" event="view">
                                <stripes:param name="orderKey" value="${order.businessKey}"/>
                                ${order.title}
                            </stripes:link>
                        </td>
                        <td>
                            <a class="external" target="JIRA" href="${actionBean.jiraUrl}${order.jiraTicketKey}" class="external" target="JIRA">
                                    ${order.jiraTicketKey}
                            </a>
                        </td>
                        <td>${order.title}</td>
                        <td>${order.orderStatus}</td>
                        <td>${actionBean.fullNameMap[order.modifiedBy]}</td>
                        <td>
                            <fmt:formatDate value="${order.modifiedDate}" pattern="MM/dd/yyyy"/>
                        </td>
                        <td>${order.pdoSampleCount}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
