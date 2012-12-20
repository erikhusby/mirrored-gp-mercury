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
                                    preventDuplicates: true,
                                    prePopulate: ${actionBean.projectManagerCompleteData}
                                }
                        );

                        $j("#scientists").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true,
                                    prePopulate: ${actionBean.scientistCompleteData}
                                }
                        );

                        $j("#externalCollaborators").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true,
                                    prePopulate: ${actionBean.externalCollaboratorCompleteData}
                                }
                        );

                        $j("#broadPIs").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true,
                                    prePopulate: ${actionBean.broadPICompleteData}
                                }
                        );

                        $j("#fundingSources").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true,
                                    prePopulate: ${actionBean.fundingSourcesCompleteData}
                                }
                        );

                        $j("#cohorts").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true,
                                    prePopulate: ${actionBean.cohortsCompleteData}
                                }
                        );

                        $j("#irbs").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    preventDuplicates: true,
                                    prePopulate: ${actionBean.irbsCompleteData}
                                }
                        );
                    }
            );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" id="createForm" class="form-horizontal">
        <stripes:hidden name="businessKey" value="${actionBean.researchProject.jiraTicketKey}"/>
        <div class="form-horizontal">
            <div class="control-group">
                <stripes:label for="title" class="control-label">Project *</stripes:label>
                <div class="controls">
                        <stripes:text name="researchProject.title" id="title" title="Enter in the project name"/>
                </div>
            </div>

            <!-- Synopsis -->
            <div class="control-group">
                <stripes:label for="synopsis" class="control-label">Synopsis *</stripes:label>

                <div class="controls">
                    <stripes:text id="synopsis" name="researchProject.synopsis" class="defaultText" style="width:390"
                                  title="Enter the synopsis of the project" value="${actionBean.researchProject.synopsis}"/>
                </div>
            </div>

            <!-- Project Managers -->
            <div class="control-group">
                <stripes:label for="projectManagers" class="control-label">Project Managers *</stripes:label>

                <div class="controls">
                    <stripes:text id="projectManagers" name="researchProject.projectManagers" />
                </div>
            </div>

            <!-- Broad PIs -->
            <div class="control-group">
                <stripes:label for="broadPIs" class="control-label">Broad PIs</stripes:label>

                <div class="controls">
                    <stripes:text id="broadPIs" name="researchProject.broadPIs" />
                </div>
            </div>

            <!-- External Collaborators -->
            <div class="control-group">
                <stripes:label for="externalCollaborators" class="control-label">External Collaborators</stripes:label>
                <div class="controls">
                    <stripes:text id="externalCollaborators" name="researchProject.externalCollaborators" />
                </div>
            </div>

            <!-- Scientists -->
            <div class="control-group">
                <stripes:label for="scientists" class="control-label">Scientists</stripes:label>
                <div class="controls">
                    <stripes:text id="scientists" name="researchProject.scientists" />
                </div>
            </div>

            <c:if test="${actionBean.researchProjectCreatorString} ne null">
                <div class="control-group">
                    <stripes:label for="createdBy" class="control-label">Created By</stripes:label>

                    <div class="controls">
                        <div class="form-value">
                                ${actionBean.researchProjectCreatorString} on <fmt:formatDate
                                value="${actionBean.researchProject.createdDate}" pattern="MM/dd/yyyy"/>
                        </div>
                    </div>
                </div>
            </c:if>

            <div class="control-group">
                <stripes:label for="fundingSources" class="control-label">Funding Sources</stripes:label>

                <div class="controls">
                    <stripes:text id="fundingSources" name="researchProject.fundingSourcesListString" />
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="cohorts" class="control-label">Sample Cohorts</stripes:label>

                <div class="controls">
                    <stripes:text id="cohorts" name="researchProject.cohortsListString" />
                </div>
            </div>


            <div class="control-group">
                <stripes:label for="irbs" class="control-label">IRB/IACUC Numbers</stripes:label>

                <div class="controls">
                    <stripes:text id="irbs" name="researchProject.irbs" />
                    <p>
                        <stripes:checkbox id="irbNotEngaged" name="researchProject.irbNotEngaged"/>&nbsp;<stripes:label for="irbNotEngaged" name="IRB Not Engaged" style="display:inline;"/>
                    </p>
                </div>
            </div>
        </div>

        <div class="control-group">
            <div class="controls">
                <div class="row-fluid">
                    <div class="span2">
                        <stripes:submit name="save" value="Save"/>
                    </div>
                    <div class="offset">
                        <c:choose>
                            <c:when test="${actionBean.creating}">
                                <stripes:link href="${ctxpath}/projects/product.action?list=">Cancel</stripes:link>
                            </c:when>
                            <c:otherwise>
                                <stripes:link href="${ctxpath}/projects/project.action?view=">
                                    <stripes:param name="businessKey" value="${actionBean.researchProject.businessKey}"/>
                                    Cancel
                                </stripes:link>
                            </c:otherwise>
                        </c:choose>
                    </div>
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
