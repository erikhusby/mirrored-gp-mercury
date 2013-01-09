<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Edit Research Project" sectionTitle="Edit Project: ${actionBean.editResearchProject.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                    function () {
                        $j("#projectManagers").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    <c:if test="${actionBean.projectManagerCompleteData != null && actionBean.projectManagerCompleteData != ''}">
                                        prePopulate: ${actionBean.projectManagerCompleteData},
                                    </c:if>
                                    preventDuplicates: true
                                }
                        );

                        $j("#scientists").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    <c:if test="${actionBean.scientistCompleteData != null && actionBean.scientistCompleteData != ''}">
                                        prePopulate: ${actionBean.scientistCompleteData},
                                    </c:if>
                                    preventDuplicates: true
                                }
                        );

                        $j("#externalCollaborators").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    <c:if test="${actionBean.externalCollaboratorCompleteData != null && actionBean.externalCollaboratorCompleteData != ''}">
                                        prePopulate: ${actionBean.externalCollaboratorCompleteData},
                                    </c:if>
                                    preventDuplicates: true
                                }
                        );

                        $j("#broadPIs").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    <c:if test="${actionBean.broadPICompleteData != null && actionBean.broadPICompleteData != ''}">
                                        prePopulate: ${actionBean.broadPICompleteData},
                                    </c:if>
                                    preventDuplicates: true
                                }
                        );

                        $j("#fundingSources").tokenInput(
                                "${ctxpath}/projects/project.action?fundingAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    <c:if test="${actionBean.fundingSourcesCompleteData != null && actionBean.fundingSourcesCompleteData != ''}">
                                        prePopulate: ${actionBean.fundingSourcesCompleteData},
                                    </c:if>
                                    preventDuplicates: true
                                }
                        );

                        $j("#cohorts").tokenInput(
                                "${ctxpath}/projects/project.action?cohortAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 2,
                                    <c:if test="${actionBean.cohortsCompleteData != null && actionBean.cohortsCompleteData != ''}">
                                        prePopulate: ${actionBean.cohortsCompleteData},
                                    </c:if>
                                    preventDuplicates: true
                                }
                        );

                        $j("#irbs").tokenInput(
                                "${ctxpath}/projects/project.action?irbAutocomplete=", {
                                    searchDelay: 500,
                                    minChars: 1,
                                    <c:if test="${actionBean.irbsCompleteData != null && actionBean.irbsCompleteData != ''}">
                                        prePopulate: ${actionBean.irbsCompleteData},
                                    </c:if>
                                    preventDuplicates: true
                                }
                        );
                    }
            );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="title" class="control-label">Project *</stripes:label>
                    <div class="controls">
                            <stripes:text name="editResearchProject.title" value="${actionBean.editResearchProject.title}"
                                          id="title"  class="defaultText" title="Enter in the project name"/>
                    </div>
                </div>

                <!-- Synopsis -->
                <div class="control-group">
                    <stripes:label for="synopsis" class="control-label">Synopsis *</stripes:label>

                    <div class="controls">
                        <stripes:text id="synopsis" name="editResearchProject.synopsis" class="defaultText" style="width:390"
                                      title="Enter the synopsis of the project" value="${actionBean.editResearchProject.synopsis}"/>
                    </div>
                </div>

                <!-- Project Managers -->
                <div class="control-group">
                    <stripes:label for="projectManagers" class="control-label">Project Managers *</stripes:label>

                    <div class="controls">
                        <stripes:text id="projectManagers" name="projectManagerList.listOfKeys" />
                    </div>
                </div>

                <!-- Broad PIs -->
                <div class="control-group">
                    <stripes:label for="broadPIs" class="control-label">Broad PIs</stripes:label>

                    <div class="controls">
                        <stripes:text id="broadPIs" name="broadPiList.listOfKeys" />
                    </div>
                </div>

                <!-- External Collaborators -->
                <div class="control-group">
                    <stripes:label for="externalCollaborators" class="control-label">External Collaborators</stripes:label>
                    <div class="controls">
                        <stripes:text id="externalCollaborators" name="externalCollaboratorList.listOfKeys" />
                    </div>
                </div>

                <!-- Scientists -->
                <div class="control-group">
                    <stripes:label for="scientists" class="control-label">Scientists</stripes:label>
                    <div class="controls">
                        <stripes:text id="scientists" name="scientistList.listOfKeys" />
                    </div>
                </div>

                <c:if test="${actionBean.researchProjectCreatorString != null}">
                    <div class="control-group">
                        <stripes:label for="createdBy" class="control-label">Created By</stripes:label>

                        <div class="controls">
                            <div class="form-value">
                                    ${actionBean.researchProjectCreatorString} on <fmt:formatDate
                                    value="${actionBean.editResearchProject.createdDate}"/>
                            </div>
                        </div>
                    </div>
                </c:if>

                <div class="control-group">
                    <stripes:label for="fundingSources" class="control-label">Funding Sources</stripes:label>

                    <div class="controls">
                        <stripes:text id="fundingSources" name="fundingSourceList.listOfKeys" />
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="cohorts" class="control-label">Sample Cohorts</stripes:label>

                    <div class="controls">
                        <stripes:text id="cohorts" name="cohortsList.listOfKeys" />
                    </div>
                </div>


                <div class="control-group">
                    <stripes:label for="irbs" class="control-label">IRB/IACUC Numbers</stripes:label>

                    <div class="controls">
                        <stripes:text id="irbs" name="irbList" />
                        <p>
                            <stripes:checkbox id="irbNotEngaged" name="irbList"/>&nbsp;<stripes:label for="irbNotEngaged" style="display:inline;">IRB Not Engaged</stripes:label>
                        </p>
                    </div>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <div class="row-fluid">
                        <div class="span1">
                            <stripes:submit name="save" value="Save"/>
                        </div>
                        <div class="span1">
                            <c:choose>
                                <c:when test="${actionBean.creating}">
                                    <stripes:link href="${ctxpath}/projects/product.action?list=">Cancel</stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link beanclass="${actionBean.class.name}" event="view">
                                        <stripes:param name="researchProject" value="${actionBean.editResearchProject.businessKey}"/>
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
                    <c:forEach items="${actionBean.editResearchProject.productOrders}" var="order">
                        <tr>
                            <td>
                                <stripes:link href="/orders/order.action" event="view">
                                    <stripes:param name="businessKey" value="${order.businessKey}"/>
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
