<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Research Project"
                       sectionTitle="View Project: ${actionBean.editResearchProject.title}"
                       businessKeyValue="${actionBean.editResearchProject.businessKey}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j( "#tabs" ).tabs();
                $j('#addRegulatoryInfoDialog').dialog({
                    autoOpen: false,
                    height: 500,
                    width: 700,
                    modal: true
                });

                $j('#addRegulatoryInfo').click(function(event) {
                    event.preventDefault();
                    resetRegulatoryInfoDialog();
                    $j('#addRegulatoryInfoDialog').dialog("open");
                });

                $j('#regulatoryInfoSearchForm').submit(searchRegulatoryInfo);

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

            function resetRegulatoryInfoDialog() {
                $j('#regulatoryInfoQuery').val('');
                $j('#addRegulatoryInfoDialogSheet2').html('');
                $j('#addRegulatoryInfoDialogSheet1').show();
            }

            function searchRegulatoryInfo(event) {
                event.preventDefault();
                $j.ajax({
                    url: '${ctxpath}/projects/project.action',
                    data: {
                        '<%= ResearchProjectActionBean.REGULATORY_INFO_QUERY_ACTION %>': '',
                        researchProject: '${actionBean.editResearchProject.businessKey}',
                        q: $j('#regulatoryInfoQuery').val()
                    },
                    dataType: 'html',
                    success: function(html) {
                        $j('#addRegulatoryInfoDialogSheet2').html(html);
                    }
                });
                $j('#addRegulatoryInfoDialogSheet1').hide();
            }

            function openRegulatoryInfoEditDialog(regulatoryInfoId) {
                $j('#addRegulatoryInfoDialogSheet2').html('');
                $j('#addRegulatoryInfoDialogSheet1').hide();
                $j('#addRegulatoryInfoDialog').dialog("open");

                $j.ajax({
                    url: '${ctxpath}/projects/project.action',
                    data: {
                        '<%= ResearchProjectActionBean.VIEW_REGULATORY_INFO_ACTION %>': '',
                        regulatoryInfoId: regulatoryInfoId,
                        researchProject: '${actionBean.editResearchProject.businessKey}'
                    },
                    dataType: 'html',
                    success: function(html) {
                        $j('#addRegulatoryInfoDialogSheet2').html(html);
                    }
                });
                return false;
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
                <stripes:select id="collaboratorId" name="selectedCollaborator" onchange="updateEmailField()">
                    <stripes:option  label="Choose a Collaborator" value=""/>

                    <c:if test="${not empty actionBean.externalCollaboratorList.tokenObjects}">
                        <optgroup label="Project Collaborators">
                            <stripes:options-collection collection="${actionBean.externalCollaboratorList.tokenObjects}"
                                                        value="userId" label="fullName"/>
                        </optgroup>
                    </c:if>

                    <optgroup label="Other">
                        <stripes:option id="emailId" label="Email Address" value=""/>
                    </optgroup>
                </stripes:select>
                <stripes:text class="defaultText" style="display:none;margin-left:4px;width:240px;" id="emailTextId"
                              name="specifiedCollaborator" maxlength="250"/>

                <p style="clear:both">
                    <label for="collaborationMessage">Optional message to send to collaborator</label>
                </p>

                <textarea id="collaborationMessageId" name="message" class="controlledText" cols="80" rows="4"> </textarea>
            </div>

            <!-- Hidden fields that are needed for operating on the current research project -->
            <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.businessKey}"/>

            <div class="form-horizontal span6">
                <div class="control-group view-control-group">
                    <label class="control-label label-form">Project</label>

                    <div class="controls">
                        <div class="form-value">
                            <div style="float: left">
                                    ${actionBean.editResearchProject.title}
                            </div>

                            <security:authorizeBlock roles="<%= roles(Developer) %>">
                                <c:if test="${actionBean.validCollaborationPortal}">
                                    <c:choose>
                                        <c:when test="${actionBean.invitationPending}">
                                            <div class="notificationText">
                                                <stripes:link style="font-size:x-small;" href="${actionBean.collaborationData.viewCollaborationUrl}">
                                                    Collaboration Portal
                                                </stripes:link>
                                                invitation sent to ${actionBean.getUserFullName(actionBean.collaborationData.collaboratorId)}, expires on
                                                <fmt:formatDate value="${actionBean.collaborationData.expirationDate}" pattern="${actionBean.datePattern}"/>
                                                (<stripes:link beanclass="${actionBean.class.name}" style="font-size: x-small; font-weight: normal;">
                                                <stripes:param name="researchProject" value="${actionBean.researchProject}"/>
                                                <stripes:param name="resendInvitation" value=""/>
                                                Resend Invitation
                                            </stripes:link>)
                                            </div>
                                        </c:when>
                                        <c:when test="${actionBean.collaborationData != null}">
                                            <div class="notificationText">
                                                <stripes:link style="font-size:x-small;" href="${actionBean.collaborationData.viewCollaborationUrl}">
                                                    Collaborating on Portal
                                                </stripes:link>
                                                with ${actionBean.getUserFullName(actionBean.collaborationData.collaboratorId)}
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div style="float:left">
                                                <stripes:hidden id="dialogAction" name="" value=""/>
                                                <stripes:hidden id="selectedCollaborator" name="selectedCollaborator" value=""/>
                                                <stripes:hidden id="specifiedCollaborator" name="specifiedCollaborator" value=""/>
                                                <stripes:hidden id="collaborationMessage" name="collaborationMessage" value=""/>

                                                <security:authorizeBlock roles="<%= roles(Developer, PM) %>">
                                                    <stripes:button name="collaborate" value="Begin Collaboration" class="btn-mini"
                                                                    style="margin-left: 10px;" onclick="showBeginCollaboration()"/>
                                                </security:authorizeBlock>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                            </security:authorizeBlock>
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
        </stripes:form>

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

        <div id="addRegulatoryInfoDialog" title="Add Regulatory Information for ${actionBean.editResearchProject.title} (${actionBean.editResearchProject.businessKey})" class="form-horizontal">

            <div id="addRegulatoryInfoDialogSheet1">
                <p>Enter the IRB Protocol or ORSP Determination number to see if the regulatory information is already known to Mercury.</p>
                <stripes:form id="regulatoryInfoSearchForm" beanclass="${actionBean.class.name}">
                    <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
                    <div class="control-group">
                        <stripes:label for="regulatoryInfoQuery" class="control-label">Identifier</stripes:label>
                        <div class="controls">
                            <input id="regulatoryInfoQuery" type="text" name="q" required>
                            <button id="regulatoryInfoSearchButton" class="btn btn-primary">Search</button>
                        </div>
                    </div>
                </stripes:form>
            </div>

            <div id="addRegulatoryInfoDialogSheet2"></div>
        </div>

        <div style="clear:both;" class="tableBar">
            <h4 style="display:inline">Regulatory Information for ${actionBean.editResearchProject.title}</h4>
            <a href="#" id="addRegulatoryInfo" class="pull-right"><i class="icon-plus"></i>Add Regulatory Information</a>
        </div>

        <stripes:form beanclass="${actionBean.class.name}">
            <input type="hidden" name="<%= ResearchProjectActionBean.REMOVE_REGULATORY_INFO_ACTION %>">
            <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
            <input type="hidden" id="removeRegulatoryInfoId" name="regulatoryInfoId">
            <table class="table simple">
                <thead>
                        <th style="width:10em">Identifier</th>
                        <th>Protocol Title</th>
                        <th style="width:25em">Type</th>
                        <th style="width:5em"></th>
                        <th style="width:9em"></th>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.editResearchProject.regulatoryInfos}" var="regulatoryInfo">
                        <tr>
                            <td>${regulatoryInfo.identifier}</td>
                            <td>${regulatoryInfo.name}</td>
                            <td>${regulatoryInfo.type.name}</td>
                            <td style="text-align:center"><a href="#" onclick="return openRegulatoryInfoEditDialog(${regulatoryInfo.regulatoryInfoId});">Edit...</a></td>
                            <td style="text-align:center"><stripes:submit name="remove" onclick="$j('#removeRegulatoryInfoId').val(${regulatoryInfo.regulatoryInfoId});" disabled="${actionBean.isRegulatoryInfoInProductOrdersForThisResearchProject(regulatoryInfo)}" class="btn">Remove</stripes:submit></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </stripes:form>

        <div id="tabs" class="simpletab">
            <ul>
                <li><a href="#ordersTab">Orders</a></li>
                <li><stripes:link beanclass="${actionBean.class.name}" event="viewSubmissions">Submission Requests
                        <stripes:param name="researchProject" value="${actionBean.researchProject}"/>
                    </stripes:link></li>
            </ul>

            <div id="ordersTab">
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
            </div>
        </div>



    </stripes:layout-component>
</stripes:layout-render>
