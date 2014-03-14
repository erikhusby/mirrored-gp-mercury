<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
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
            });

            function resetRegulatoryInfoDialog() {
                $j('#regulatoryInfoQuery').val('');
                $j('#addRegulatoryInfoDialogQueryResults tbody').empty();
                $j('#regulatoryInfoType option').prop('disabled', false);
                $j('#addRegulatoryInfoDialogSheet2').hide();
                $j('#addRegulatoryInfoDialogSheet1').show();
            }

            function searchRegulatoryInfo(event) {
                event.preventDefault();
                $j.ajax({
                    url: '${ctxpath}/projects/project.action?regulatoryInfoQuery=&q=' + $j('#regulatoryInfoQuery').val(),
                    dataType: 'json',
                    success: showRegulatoryInfo
                });
                $j('#addRegulatoryInfoDialogSheet1').hide();
            }

            function showRegulatoryInfo(infos) {
                $j('#addRegulatoryInfoDialogSheet2').show();
                var table = $j('#addRegulatoryInfoDialogQueryResults tbody');

                for (var i = 0; i < infos.length; i++) {
                    var info = infos[i];
                    var addButton = $j('<input type="submit" value="Add" class="btn">');
                    addButton.click(function() { $j('#regulatoryInfoId').val(info.id); });
                    var row = $j('<tr/>');
                    row.append($j('<td/>').append(info.identifier));
                    row.append($j('<td/>').append(info.alias));
                    row.append($j('<td/>').append(info.type));
                    row.append($j('<td/>').append(addButton));
                    table.append(row);

                    $j('#regulatoryInfoType option:contains(' + info.type + ')').prop('disabled', true);
                }

                // pre-populate new regulatory information form
                $j('#identifierDisplay').text($j('#regulatoryInfoQuery').val());
                $j('#identifier').val($j('#regulatoryInfoQuery').val());
            }
        </script>

        <style type="text/css">
            .barFull { height: 10px; width:80px; background-color: white; border-color: #a9a9a9; border-style: solid; border-width: thin; }
            .barComplete { height: 10px; float:left; background-color: #c4eec0; }
            .barAbandon { height: 10px; float:left; background-color: #eed6e1; }
        </style>
    </stripes:layout-component>

    <stripes:layout-component name="content">

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

        <div id="addRegulatoryInfoDialog" title="Add Regulatory Information for ${actionBean.editResearchProject.title} (${actionBean.editResearchProject.businessKey})" class="form-horizontal">
            <div id="addRegulatoryInfoDialogSheet1">
                <p>Enter the protocol or determination number to see if the regulatory information is already known to Mercury.</p>
                <form id="regulatoryInfoSearchForm">
                    <div class="control-group">
                        <stripes:label for="regulatoryInfoQuery" class="control-label">Identifier</stripes:label>
                        <div class="controls">
                            <input id="regulatoryInfoQuery" type="text" name="q" required>
                            <button id="regulatoryInfoSearchButton" class="btn btn-primary">Search</button>
                        </div>
                    </div>
                </form>
            </div>
            <div id="addRegulatoryInfoDialogSheet2">
                <p>Found existing regulatory information. Choose one to use or create a new one of a different type.</p>
                <stripes:form action="project.action">
                    <stripes:hidden name="addRegulatoryInfoToResearchProject"/>
                    <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
                    <input type="hidden" id="regulatoryInfoId" name="regulatoryInfoId">
                    <table id="addRegulatoryInfoDialogQueryResults" class="table simple">
                        <thead>
                        <th style="width:10em">Identifier</th>
                        <th>Alias</th>
                        <th style="width:17em">Type</th>
                        <th style="width:9em"></th>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
                </stripes:form>
                <hr>
                <p>Fill in the details below to add new regulatory information to Mercury and this research project.</p>
                <stripes:form id="regulatoryInfoCreateForm" beanclass="${actionBean.class.name}" class="form-horizontal">
                    <stripes:hidden name="addNewRegulatoryInfo"/>
                    <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>
                    <div class="control-group view-control-group">
                        <label class="control-label">Identifier</label>

                        <div class="controls">
                            <div id="identifierDisplay" class="form-value">${actionBean.q}</div>
                            <input type="hidden" id="identifier" name="identifier"/>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label for="regulatoryInfoType" class="control-label">Type</stripes:label>
                        <div class="controls">
                            <stripes:select id="regulatoryInfoType" name="regulatoryInfoType">
                                <stripes:options-enumeration enum="org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo.Type" label="name"/>
                            </stripes:select>
                        </div>
                    </div>

                    <div class="control-group">
                        <stripes:label for="alias" class="control-label"/>
                        <div class="controls">
                            <input type="text" name="alias" required>
                        </div>
                    </div>

                    <div class="control-group">
                        <div class="controls">
                            <stripes:submit name="add" value="Add" class="btn btn-primary"/>
                        </div>
                    </div>
                </stripes:form>
            </div>
        </div>

        <div style="clear:both;">
            <h4 style="display:inline">Regulatory Information for ${actionBean.editResearchProject.title}</h4>
            <a href="#" id="addRegulatoryInfo" class="pull-right"><i class="icon-plus"></i>Add Regulatory Information</a>
        </div>

        <table class="table simple">
            <thead>
                <th style="width:10em">Identifier</th>
                <th>Alias</th>
                <th style="width:25em">Type</th>
                <th style="width:5em"></th>
                <th style="width:9em"></th>
            </thead>
            <tbody>
                <tr>
                    <td>13423</td>
                    <td>Manhattan Project reference consent</td>
                    <td>IRB Protocol #</td>
                    <td style="text-align:center"><a href="#">Edit...</a></td>
                    <td style="text-align:center"><button class="btn">Remove</button></td>
                </tr>
                <tr>
                    <td>34423</td>
                    <td>Project common reference consent</td>
                    <td>ORSP "Not Engaged" Determination #</td>
                    <td style="text-align:center"><a href="#">Edit...</a></td>
                    <td style="text-align:center"><button class="btn">Remove</button></td>
                </tr>
            </tbody>
        </table>

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
