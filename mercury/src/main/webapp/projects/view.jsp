<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Research Project"
                       sectionTitle="View Project: ${actionBean.editResearchProject.title}"
                       businessKeyValue="${actionBean.editResearchProject.businessKey}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#orderList').dataTable({
                    "oTableTools": ttExportDefines
                });

                setupDialogs();
            });

            function showBeginCollaboration() {
                $j("#dialogAction").attr("name", "beginCollaboration");
                $j("#confirmDialog").dialog("open");
            }

            function updateEmailField() {
                if ($j("#collaboratorId :selected").text() == $j("#emailId").text()) {
                    $j("#emailTextId").show();
                } else {
                    $j("#emailTextId").hide();
                }
            }

            function setupDialogs() {
                $j("#confirmDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    width: 600,
                    buttons: [
                        {
                            id: "confirmOkButton",
                            text: "OK",
                            click: function () {
                                $j(this).dialog("close");
                                $j("#confirmOkButton").attr("disabled", "disabled");
                                $j("#selectedCollaborator").attr("value", $j("#collaboratorId").val());
                                $j("#specifiedCollaborator").attr("value", $j("#emailTextId").val());
                                $j("#collaborationMessage").attr("value", $j("#collaborationMessageId").val());
                                $j("#projectForm").submit();
                            }
                        },
                        {
                            text: "Cancel",
                            click: function () {
                                $j(this).dialog("close");
                            }
                        }
                    ]
                });
            }
        </script>

        <style type="text/css">
            .barFull { height: 10px; width:80px; background-color: white; border-color: #a9a9a9; border-style: solid; border-width: thin; }
            .barComplete { height: 10px; float:left; background-color: #c4eec0; }
            .barAbandon { height: 10px; float:left; background-color: #eed6e1; }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="projectForm" class="form-horizontal">

            <div id="confirmDialog" style="width:600px;display:none;">
                <p>
                    Publish this Research Project on the Collaboration Portal. If the collaborator is not set up on the
                    Portal an invitation will be sent.
                </p>

                <label style="float:left;margin-right:10px;width:auto;" for="collaboratorId">Collaborator *</label>
                <stripes:select id="collaboratorId" name="collaborator" onchange="updateEmailField()">
                    <stripes:option  label="Choose a Collaborator" value=""/>
                    <optgroup label="Project Collaborators">
                        <stripes:options-collection collection="${actionBean.externalCollaboratorList.tokenObjects}"
                                                    value="userId" label="fullName"/>
                    </optgroup>

                    <optgroup label="Other">
                        <stripes:option id="emailId" label="Email Address" value=""/>
                    </optgroup>
                </stripes:select>
                <stripes:text class="defaultText" style="display:none;margin-left:4px;width:240px;" id="emailTextId"
                              name="collaboratorEmail" maxlength="250"/>

                <p style="clear:both">
                    <label for="collaborationMessage">Optional message to send to collaborator</label>
                </p>

                <textarea id="collaborationMessageId" name="message" class="controlledText" cols="80" rows="4"> </textarea>
            </div>

        <!-- The action buttons and form wrap any special buttons that are needed for the UI -->
            <div class="actionButtons">

                <!-- Hidden fields that are needed for operating on the current research project -->
                <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.businessKey}"/>

                <c:choose>
                    <c:when test="${actionBean.invitationPending}">
                        <div class="notificationText">
                            Collaboration Portal invitation sent to ${actionBean.editResearchProject.invitationEmail}, expires on
                            <fmt:formatDate value="${actionBean.invitationExpirationDate}" pattern="${actionBean.datePattern}"/>
                        </div>
                        <div style="margin-left: 5px; float:left;">
                            <stripes:link beanclass="${actionBean.class.name}" style="font-size: small; font-weight: normal;">
                                <stripes:param name="researchProject" value="${actionBean.researchProject}"/>
                                <stripes:param name="resendInvitation" value=""/>
                                Resend Invitation
                            </stripes:link>
                        </div>
                    </c:when>
                    <c:when test="${actionBean.editResearchProject.collaborationStarted}">
                        <div class="notificationText">Collaborating on Portal with ${actionBean.collaboratingWith}</div>
                    </c:when>
                    <c:otherwise>
                        <stripes:hidden id="dialogAction" name="" value=""/>
                        <stripes:hidden id="selectedCollaborator" name="selectedCollaborator" value=""/>
                        <stripes:hidden id="specifiedCollaborator" name="specifiedCollaborator" value=""/>
                        <stripes:hidden id="collaborationMessage" name="collaborationMessage" value=""/>

                        <security:authorizeBlock roles="<%= roles(Developer, PM) %>">
                            <stripes:button name="collaborate" value="Begin Collaboration" class="btn"
                                            onclick="showBeginCollaboration()"/>
                        </security:authorizeBlock>
                    </c:otherwise>
                </c:choose>
            </div>
        </stripes:form>

        <div style="clear: both"/>

        <div class="form-horizontal span7">
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

            <!-- Parent Project -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Parent Project</label>

                <div class="controls">
                    <div class="form-value">
                        <c:if test="${actionBean.editResearchProject.parentResearchProject != null}">
                            <stripes:link beanclass="${actionBean.class.name}" event="view">
                                <stripes:param name="researchProject" value="${actionBean.editResearchProject.parentResearchProject.businessKey}"/>
                                ${actionBean.editResearchProject.parentResearchProject.title}
                            </stripes:link>

                        </c:if>
                    </div>
                </div>
            </div>

            <!-- Subproject -->
            <div class="control-group view-control-group">
                <label class="control-label label-form">Subprojects</label>

                <div class="controls">
                    <div class="form-value">
                        <div style="margin-left: -24px;">
                        <stripes:layout-render name="/projects/treeview_component.jsp"
                                               childProjects="${actionBean.editResearchProject.childProjects}"
                                               bean="${actionBean}" />
                        </div>
                    </div>
                </div>
            </div>

            <div class="control-group view-control-group">
                <label class="control-label label-form">Created by</label>

                <div class="controls">
                    <div class="form-value">
                        ${actionBean.getUserFullName(actionBean.editResearchProject.createdBy)}
                            on <fmt:formatDate value="${actionBean.editResearchProject.createdDate}" pattern="${actionBean.datePattern}"/>
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

        <div class="form-horizontal span5">
            <fieldset>
                <legend><h4>Project Users</h4></legend>

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
            </fieldset>

            <fieldset>
                <legend><h4>Pipeline Analysis</h4></legend>

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

                <div class="control-group view-control-group">
                    <label class="control-label label-form">Sequence Aligner</label>

                    <div class="controls">
                        <div class="form-value">
                            <c:if test="${!empty actionBean.editResearchProject.sequenceAlignerKey}">
                                ${(actionBean.getSequenceAligner(actionBean.editResearchProject.sequenceAlignerKey)).displayName}
                            </c:if>
                        </div>
                    </div>
                </div>

                <div class="control-group view-control-group">
                    <label class="control-label label-form">Reference Sequence</label>

                    <div class="controls">
                        <div class="form-value">
                            <c:if test="${!empty actionBean.editResearchProject.referenceSequenceKey}">
                                ${(actionBean.getReferenceSequence(actionBean.editResearchProject.referenceSequenceKey)).displayName}
                            </c:if>
                        </div>
                    </div>
                </div>
            </fieldset>
        </div>

        <div class="tableBar" style="clear:both;">
            <h4 style="display:inline">Orders</h4>

            <stripes:link title="Create product with research project ${actionBean.editResearchProject.title}"
                          beanclass="<%=ProductOrderActionBean.class.getName()%>" event="create" class="pull-right">
                <stripes:param name="researchProjectKey" value="${actionBean.editResearchProject.businessKey}"/>
                <i class="icon-plus"></i>
                Add New Product Order
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
                        <td>${order.product.name}</td>
                        <td>${order.orderStatus}</td>
                        <td>${actionBean.getUserFullName(order.modifiedBy)}</td>
                        <td>
                            <fmt:formatDate value="${order.modifiedDate}" pattern="${actionBean.datePattern}"/>
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
