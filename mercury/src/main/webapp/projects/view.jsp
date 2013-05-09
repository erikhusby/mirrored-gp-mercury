<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Research Project" sectionTitle="View Project: ${actionBean.editResearchProject.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#orderList').dataTable({
                    "oTableTools": ttExportDefines
                })
            });
        </script>

        <style type="text/css">
            .barFull { height: 10px; width:80px; background-color: white; border-color: #a9a9a9; border-style: solid; border-width: thin; }
            .barComplete { height: 10px; float:left; background-color: #c4eec0; }
            .barAbandon { height: 10px; float:left; background-color: #eed6e1; }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <p>
            <stripes:link title="Click to edit ${actionBean.editResearchProject.title}" beanclass="${actionBean.class.name}" event="edit" class="pull-right">
                <span class="icon-briefcase"></span> <%=ResearchProjectActionBean.EDIT_PROJECT%>
                <stripes:param name="researchProject" value="${actionBean.editResearchProject.businessKey}"/>
            </stripes:link>
        </p>

        <div class="form-horizontal">
            <div class="control-group view-control-group">
                <label class="control-label label-form">Project</label>

                <div class="controls">
                    <div class="form-value">
                        ${actionBean.editResearchProject.title}
                    </div>
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">ID</label>

                <div class="controls">
                    <div class="form-value">
                        <c:if test="${actionBean.editResearchProject.jiraTicketKey != null}">
                            <stripes:link target="JIRA"
                                          href="${actionBean.jiraUrl(actionBean.editResearchProject.jiraTicketKey)}"
                                          class="external">
                                ${actionBean.editResearchProject.jiraTicketKey}
                            </stripes:link>
                        </c:if>
                    </div>
                </div>
            </div>

            <!-- Synopsis -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Synopsis</label>

                <div class="controls">
                    <div class="form-value">${actionBean.editResearchProject.synopsis}</div>
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">Access Control</label>
                <div class="controls">
                    <div class="form-value">
                        <c:choose>
                            <c:when test="${actionBean.editResearchProject.accessControlEnabled}">
                                Restricted to Project Users
                            </c:when>
                            <c:otherwise>
                                Open to All Users
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- Project Managers -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Project Managers</label>

                <div class="controls">
                    <div class="form-value">${actionBean.getUserListString(actionBean.editResearchProject.projectManagers)}</div>
                </div>
            </div>

            <!-- Broad PIs -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Broad PIs</label>

                <div class="controls">
                    <div class="form-value">${actionBean.getUserListString(actionBean.editResearchProject.broadPIs)}</div>
                </div>
            </div>

            <!-- External Collaborators -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">External Collaborators</label>

                <div class="controls">
                    <div class="form-value">${actionBean.getUserListString(actionBean.editResearchProject.externalCollaborators)}</div>
                </div>
            </div>

            <!-- Scientists -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Scientists</label>

                <div class="controls">
                    <div class="form-value">${actionBean.getUserListString(actionBean.editResearchProject.scientists)}</div>
                </div>
            </div>

            <!-- Other -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Other Users</label>

                <div class="controls">
                    <div class="form-value">${actionBean.getUserListString(actionBean.editResearchProject.other)}</div>
                </div>
            </div>


            <div class="control-group view-control-group">
                <label class="control-label label-form">Created by</label>

                <div class="controls">
                    <div class="form-value">
                        ${actionBean.getUserFullName(actionBean.editResearchProject.createdBy)}
                            on <fmt:formatDate value="${actionBean.editResearchProject.createdDate}"/>
                    </div>
                </div>
            </div>


            <div class="control-group view-control-group">
                <label class="control-label label-form">Funding Sources</label>

                <div class="controls">
                    <div class="form-value">${actionBean.fundingSourcesListString}</div>
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">Sample Cohorts</label>

                <div class="controls">
                    <div class="form-value">${actionBean.cohortsListString}</div>
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">Irb Numbers</label>

                <div class="controls">
                    <div class="form-value">
                        <c:forEach items="${actionBean.editResearchProject.irbNumbers}" var="irb">
                                ${irb}
                        </c:forEach>
                    </div>
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">IRB Not Engaged</label>
                <div class="controls">
                    <div class="form-value">
                        <c:choose>
                            <c:when test="${actionBean.editResearchProject.irbNotEngaged}">
                                <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                            </c:when>
                            <c:otherwise>
                                No
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">IRB Notes</label>

                <div class="controls">
                    <div class="form-value">${actionBean.editResearchProject.irbNotes}</div>
                </div>
            </div>
        </div>


        <div class="tableBar" style="margin-bottom: 10px;">
            Orders

            <stripes:link title="Create product with research project ${actionBean.editResearchProject.title}"
                          beanclass="<%=ProductOrderActionBean.class.getName()%>" event="create" class="pull-right">
                <span class="icon-tags"></span> <%=ProductOrderActionBean.CREATE_ORDER%>
                <stripes:param name="researchProjectKey" value="${actionBean.editResearchProject.businessKey}"/>
            </stripes:link>
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
                    <th width="80">%&nbsp;Complete</th>
                    <th>Sample Count</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${actionBean.editResearchProject.productOrders}" var="order">
                    <tr>
                        <td>
                            <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" event="view">
                                <stripes:param name="productOrder" value="${order.businessKey}"/>
                                ${order.title}
                            </stripes:link>
                        </td>
                        <td>
                            <a class="external" target="JIRA" href="${actionBean.jiraUrl(order.jiraTicketKey)}" class="external" target="JIRA">
                                    ${order.jiraTicketKey}
                            </a>
                        </td>
                        <td>${order.title}</td>
                        <td>${order.orderStatus}</td>
                        <td>${actionBean.getUserFullName(order.modifiedBy)}</td>
                        <td>
                            <fmt:formatDate value="${order.modifiedDate}" pattern="MM/dd/yyyy"/>
                        </td>
                        <td align="center">
                            <div class="barFull" title="${actionBean.progressFetcher.getPercentInProgress(order.businessKey)}% In Progress">
                                <span class="barAbandon"
                                      title="${actionBean.progressFetcher.getPercentAbandoned(order.businessKey)}% Abandoned"
                                      style="width: ${actionBean.progressFetcher.getPercentAbandoned(order.businessKey)}%"> </span>
                                <span class="barComplete"
                                      title="${actionBean.progressFetcher.getPercentCompleted(order.businessKey)}% Completed"
                                      style="width: ${actionBean.progressFetcher.getPercentCompleted(order.businessKey)}%"> </span>
                            </div>
                        </td>
                        <td>${actionBean.progressFetcher.getNumberOfSamples(order.businessKey)}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

    </stripes:layout-component>
</stripes:layout-render>
