<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Research Project"
                       sectionTitle="View Project: ${actionBean.editResearchProject.title}"
                       businessKeyValue="${actionBean.editResearchProject.businessKey}">
    <stripes:layout-component name="extraHead">
        <style type="text/css">
            .extraSpace {
                height: calc(100vh - 100px);
            }
            .extraSpace > .ui-tabs-panel {
                height: auto;
                min-height: 100%;
            }
        </style>
        <script type="text/javascript">
            function getParameterByName(name) {
                name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
                var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
                    results = regex.exec(location.search);
                return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
            }
            var paramterValue = getParameterByName("rpSelectedTab");
            var activeTab = parseInt(paramterValue == undefined ? 0 : paramterValue) + 1;

            $j(document).ready(function () {
                $j("#tabs").tabs({
                    active: activeTab,
                    activate: function () {
                        this.scrollIntoView({block: "start", behavior: "smooth"});
                    }
                });
                if (${! actionBean.validateViewOrPostSubmissions(true)}) {
                    var index = $j("#tabs ul").find("[href='#submissionsTab']").closest("li").index();
                    $j("#tabs").tabs("disable", index);
                }

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

            // $(window).load(...) is used here because the submissions.jsp dom is not ready
            // when $j(document).ready() is called.
            $(window).load(function(){
                if (activeTab) {
                    $j("#tabs ul li:nth-child(" + activeTab + ") a").trigger('click');
                }
            });

            function showBeginCollaboration() {
                $j("#dialogAction").attr("name", "beginCollaboration");
                // Make sure email field's show/hide state is correct.
                updateEmailField();
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
                                <% // This sets  up the hidden fields with the values from the dialog. %>
                                $j("#confirmOkButton").attr("disabled", "disabled");
                                $j("#selectedCollaborator").attr("value", $j("#collaboratorId").val());
                                $j("#specifiedCollaborator").attr("value", $j("#emailTextId").val());
                                $j("#collaborationMessage").attr("value", $j("#collaborationMessageId").val());
                                $j("#collaborationQuoteId").attr("value", $j("#collaborationQuoteIdId").val());
                                $j("#sampleKitShipping").attr("value", $j("#sampleKitShippingId").val());
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
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="projectForm" class="form-horizontal">

            <div id="confirmDialog" style="width:600px;display:none;">
                <p style="margin-bottom: 10px">
                    Begin a collaboration on the Portal with this Research Project.
                </p>
                <div class="form-horizontal">
                    <div class="control-group">
                        <%-- Hardcoded values to override the settings from control-label so this dialog looks OK. --%>
                        <%-- Someday we should add classes to generally handle forms in dialogs. --%>
                        <stripes:label style="width:80px" class="control-label" for="collaboratorId">Collaborator *</stripes:label>
                        <div class="controls" style="margin-left:90px">
                            <stripes:select id="collaboratorId" name="selectedCollaborator" onchange="updateEmailField()">
                                <stripes:option label="Choose a Collaborator" value=""/>
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
                            <stripes:text class="defaultText" id="emailTextId"
                                          name="specifiedCollaborator" maxlength="250"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label style="width:80px" class="control-label" for="collaborationQuoteIdId">Quote *</stripes:label>
                        <div class="controls" style="margin-left:90px">
                            <stripes:text id="collaborationQuoteIdId" name="collaborationQuoteId" class="defaultText" title="Enter the Quote ID"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label style="width:80px" class="control-label" for="sampleKitShippingId">Send Kits To *</stripes:label>
                        <div class="controls" style="margin-left:90px">
                            <stripes:select id="sampleKitShippingId" name="sampleKitRecipient">
                                <stripes:options-enumeration label="displayName"
                                        enum="org.broadinstitute.gpinformatics.athena.boundary.projects.SampleKitRecipient"/>
                            </stripes:select>
                        </div>
                    </div>

                    <p style="clear:both">
                        <label for="collaborationMessageId">Optional message to send to collaborator</label>
                    </p>

                    <textarea id="collaborationMessageId" name="message" class="controlledText" cols="80" rows="4"> </textarea>
                </div>
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

                            <c:if test="${actionBean.validCollaborationPortal}">
                                <c:choose>
                                    <c:when test="${actionBean.invitationPending}">
                                        <div class="notificationText">
                                            <stripes:link style="font-size:x-small;" href="${actionBean.collaborationData.viewCollaborationUrl}">
                                                Collaboration Portal
                                            </stripes:link>
                                            invitation sent to ${actionBean.getUserFullName(actionBean.collaborationData.collaboratorId)}, expires on
                                            <fmt:formatDate value="${actionBean.collaborationData.expirationDate}" pattern="${actionBean.datePattern}"/>
                                            <c:if test="${actionBean.canBeginCollaborations}">
                                                (<stripes:link beanclass="${actionBean.class.name}" style="font-size: x-small; font-weight: normal;">
                                                    <stripes:param name="researchProject" value="${actionBean.researchProject}"/>
                                                    <stripes:param name="resendInvitation" value=""/>
                                                    Resend Invitation
                                                </stripes:link>)
                                            </c:if>
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
                                        <c:if test="${actionBean.canBeginCollaborations}">
                                            <div style="float:left">
                                                <stripes:hidden id="dialogAction" name="" value=""/>
                                                <stripes:hidden id="selectedCollaborator" name="selectedCollaborator" value=""/>
                                                <stripes:hidden id="specifiedCollaborator" name="specifiedCollaborator" value=""/>
                                                <stripes:hidden id="collaborationMessage" name="collaborationMessage" value=""/>
                                                <stripes:hidden id="collaborationQuoteId" name="collaborationQuoteId" value=""/>
                                                <stripes:hidden id="sampleKitShipping" name="sampleKitRecipient" value=""/>

                                                <stripes:button name="collaborate" value="Begin Collaboration" class="btn-mini"
                                                                style="margin-left: 10px;" onclick="showBeginCollaboration()"/>
                                            </div>
                                        </c:if>
                                    </c:otherwise>
                                </c:choose>
                            </c:if>
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
                                              class="external"
                                              id="rpId">
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
                    <label class="control-label label-form">Regulatory Designation</label>
                    <div class="controls">
                        <div class="form-value" id="regulatoryDesignation">${actionBean.editResearchProject.regulatoryDesignationDescription}</div>
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

            <fieldset>
                <legend><h4>Data Submission</h4></legend>
                <div class="control-group view-control-group">
                    <label class="control-label label-form">Submission Repository</label>

                    <div class="controls">
                        <div class="form-value">
                            <c:if test="${actionBean.submissionRepository != null}">
                                ${actionBean.submissionRepository.description}
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
        <div>
            <h5>${actionBean.complianceStatement}</h5>
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

        <%-- extraSpace class adds about a page worth of blank at the bottom of the page so the --%>
        <%-- submission entries aren't hidden when the ajax call returns --%>

        <div id="tabs" class="simpletab extraSpace">
            <ul>
                <li><a href="#ordersTab" title="View Product Orders">Orders</a></li>
                <li><a href="#submissionsTab" title="${actionBean.submissionTabHelpText}">Submission Requests</a></li>
            </ul>

            <div id="ordersTab">

            <stripes:link title="Create product order with research project ${actionBean.editResearchProject.title}"
                          beanclass="<%=ProductOrderActionBean.class.getName()%>" event="create" class="pull-right">
                <stripes:param name="researchProjectKey" value="${actionBean.editResearchProject.businessKey}"/>
                <i class="icon-plus"></i>
                Add New Product Order
            </stripes:link>

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
                            <c:choose>

                                <%-- draft PDO --%>
                                <c:when test="${order.draft}">
                                    <span title="DRAFT">&#160;</span>
                                </c:when>
                                <c:otherwise>
                                    <a class="external" target="JIRA" href="${actionBean.jiraUrl(order.jiraTicketKey)}"
                                       class="external" target="JIRA">
                                            ${order.jiraTicketKey}
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>${order.product.name}</td>
                        <td>${order.orderStatus}</td>
                        <td>${actionBean.getUserFullName(order.createdBy)}</td>
                        <td>
                            <fmt:formatDate value="${order.modifiedDate}" pattern="${actionBean.datePattern}"/>
                        </td>
                        <td align="center">
                            <stripes:layout-render name="/orders/sample_progress_bar.jsp" status="${actionBean.progressFetcher.getStatus(order.businessKey)}"/>
                        </td>
                        <td>${actionBean.progressFetcher.getNumberOfSamples(order.businessKey)}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
            </div>
            <div id="submissionsTab">
                <a name="#submissionsTab"></a>
                <input type="hidden" name="_sourcePage" value="<%request.getServletPath();%>"/>
                <stripes:layout-render name="<%=ResearchProjectActionBean.PROJECT_SUBMISSIONS_PAGE%>"
                                       event="viewSubmissions"
                                       submissionsTabSelector="a[href = '#submissionsTab']"
                                       researchProject="${actionBean.editResearchProject.businessKey}"/>
            </div>
        </div>



    </stripes:layout-component>
</stripes:layout-render>
