<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Research Project" sectionTitle="View Project: ${actionBean.researchProject.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#orderList').dataTable({
                    "oTableTools": ttExportDefines
                })
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <p>
            <stripes:link title="Click to edit ${actionBean.researchProject.title}" beanclass="${actionBean.class.name}" event="edit" class="pull-right">
                <span class="icon-home"></span> <%=ResearchProjectActionBean.EDIT_PROJECT%>
                <stripes:param name="businessKey" value="${actionBean.researchProject.businessKey}"/>
            </stripes:link>
        </p>

        <div class="form-horizontal">
            <div class="control-group view-control-group">
                <label class="control-label label-form">Project</label>

                <div class="controls">
                    <div class="form-value">${actionBean.researchProject.title}</div>
                    (<stripes:link target="tableau" href="${actionBean.tableauLink}" class="external">Pass Report</stripes:link>)
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">ID</label>

                <div class="controls">
                    <div class="form-value">
                        <c:if test="${actionBean.researchProject.jiraTicketKey != null}">
                            <stripes:link target="JIRA"
                                          href="${actionBean.jiraUrl}${actionBean.researchProject.jiraTicketKey}"
                                          class="external">
                                ${actionBean.researchProject.jiraTicketKey}
                            </stripes:link>
                        </c:if>
                    </div>
                </div>
            </div>

            <!-- Synopsis -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Synopsis</label>

                <div class="controls">
                    <div class="form-value">${actionBean.researchProject.synopsis}</div>
                </div>
            </div>

            <!-- Project Managers -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Project Managers</label>

                <div class="controls">
                    <div class="form-value">${actionBean.managersListString}</div>
                </div>
            </div>

            <!-- Broad PIs -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Broad PIs</label>

                <div class="controls">
                    <div class="form-value">${actionBean.broadPIsListString}</div>
                </div>
            </div>

            <!-- External Collaborators -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">External Collaborators</label>

                <div class="controls">
                    <div class="form-value">${actionBean.externalCollaboratorsListString}</div>
                </div>
            </div>

            <!-- Project Managers -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Scientists</label>

                <div class="controls">
                    <div class="form-value">${actionBean.scientistsListString}</div>
                </div>
            </div>


            <div class="control-group view-control-group">
                <label class="control-label label-form">Created by</label>

                <div class="controls">
                    <div class="form-value">
                        ${actionBean.researchProjectCreatorString} on <fmt:formatDate value="${actionBean.researchProject.createdDate}" pattern="MM/dd/yyyy"/>
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
                        <c:forEach items="${actionBean.researchProject.irbNumbers}" var="irb">
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
                            <c:when test="${actionBean.researchProject.irbNotEngaged}">
                                <img src="${ctxpath}/images/check.png" alt="yes" title="yes"/>
                            </c:when>
                            <c:otherwise>
                                No
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
                            <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" event="view">
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

    </stripes:layout-component>
</stripes:layout-render>
