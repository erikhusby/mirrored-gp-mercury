<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.Developer" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.BillingManager" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.PDM" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}: ${actionBean.editResearchProject.title}"
                       sectionTitle="${actionBean.submitString}: ${actionBean.editResearchProject.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                    function () {
                        $j("#projectManagers").tokenInput(
                            "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                hintText: "Type a Project Manager name",
                                <enhance:out escapeXml="false">
                                prePopulate: ${actionBean.ensureStringResult(actionBean.projectManagerList.completeData)},
                                </enhance:out>
                                tokenDelimiter: "${actionBean.projectManagerList.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                        );

                        $j("#scientists").tokenInput(
                            "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                hintText: "Type a Scientist name",
                                <enhance:out escapeXml="false">
                                prePopulate: ${actionBean.ensureStringResult(actionBean.scientistList.completeData)},
                                </enhance:out>
                                tokenDelimiter: "${actionBean.scientistList.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                        );

                        $j("#externalCollaborators").tokenInput(
                            "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                hintText: "Type a Collaborator name",
                                <enhance:out escapeXml="false">
                                prePopulate: ${actionBean.ensureStringResult(actionBean.externalCollaboratorList.completeData)},
                                </enhance:out>
                                tokenDelimiter: "${actionBean.externalCollaboratorList.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                        );

                        $j("#broadPIs").tokenInput(
                            "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                hintText: "Type a Broad PI",
                                <enhance:out escapeXml="false">
                                prePopulate: ${actionBean.ensureStringResult(actionBean.broadPiList.completeData)},
                                </enhance:out>
                                tokenDelimiter: "${actionBean.broadPiList.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                        );

                        $j("#otherUsers").tokenInput(
                            "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                hintText: "Enter a user name",
                                <enhance:out escapeXml="false">
                                prePopulate: ${actionBean.ensureStringResult(actionBean.otherUserList.completeData)},
                                </enhance:out>
                                tokenDelimiter: "${actionBean.otherUserList.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                        );

                        $j("#fundingSources").tokenInput(
                            "${ctxpath}/projects/project.action?fundingAutocomplete=", {
                                <enhance:out escapeXml="false">
                                prePopulate: ${actionBean.ensureStringResult(actionBean.fundingSourceList.completeData)},
                                </enhance:out>
                                tokenDelimiter: "${actionBean.fundingSourceList.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                        );

                        $j("#cohorts").tokenInput(
                            "${ctxpath}/projects/project.action?cohortAutocomplete=", {
                                hintText: "Type a Sample Cohort name",
                                <enhance:out escapeXml="false">
                                prePopulate: ${actionBean.ensureStringResult(actionBean.cohortsList.completeData)},
                                </enhance:out>
                                tokenDelimiter: "${actionBean.cohortsList.separator}",
                                preventDuplicates: true,
                                resultsFormatter: formatInput,
                                autoSelectFirstResult: true
                            }
                        );

                        $j("#parentResearchProject").tokenInput(
                                "${ctxpath}/projects/project.action?projectHierarchyAwareAutocomplete=&researchProject=${actionBean.editResearchProject.businessKey}", {
                                    hintText: "Type a project name",
                                    <enhance:out escapeXml="false">
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.projectTokenInput.completeData)},
                                    </enhance:out>
                                    resultsFormatter: formatInput,
                                    tokenLimit: 1,
                                    autoSelectFirstResult: true
                                }
                        );
                    }
            );

            function formatInput(item) {
                var extraCount = (item.extraCount == undefined) ? "" : item.extraCount;
                return "<li>" + item.dropdownItem + extraCount + '</li>';
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal" focus="editResearchProject.title">
            <stripes:hidden name="submitString"/>
            <stripes:hidden name="researchProject" value="${actionBean.editResearchProject.jiraTicketKey}"/>

            <div class="row">
                <div class="form-horizontal span7" >
                    <div class="control-group">
                        <stripes:label for="title" class="control-label">Name *</stripes:label>
                        <div class="controls">
                            <c:choose>
                                <c:when test="${actionBean.creating}">
                                    <stripes:text name="editResearchProject.title"
                                                  value="${actionBean.editResearchProject.title}"
                                                  id="title" class="defaultText input-create-form"
                                                  title="Enter the project name" maxlength="255"/>
                                </c:when>
                                <c:otherwise>
                                    <stripes:hidden name="editResearchProject.title" value="${actionBean.editResearchProject.title}" />
                                    ${actionBean.editResearchProject.title}
                                </c:otherwise>
                            </c:choose>

                        </div>
                    </div>

                    <!-- Synopsis -->
                    <div class="control-group">
                        <stripes:label for="synopsis" class="control-label">Synopsis *</stripes:label>

                        <div class="controls">
                            <stripes:textarea id="synopsis" rows="5" cols="100" name="editResearchProject.synopsis" class="defaultText textarea input-create-form"
                                          title="Enter the synopsis of the project" value="${actionBean.editResearchProject.synopsis}"/>
                        </div>
                    </div>


                    <c:choose>
                        <c:when test="${actionBean.creating}">
                            <div class="control-group">
                                <label class="control-label" for="regulatoryDesignation">Regulatory Designation *</label>
                                <div class="controls">
                                    <stripes:select id="regulatoryDesignation" name="editResearchProject.regulatoryDesignation">
                                        <stripes:option value="">Select One</stripes:option>
                                        <stripes:options-enumeration enum="org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject.RegulatoryDesignation" label="description"/>
                                    </stripes:select>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="control-group view-control-group">
                                <label class="control-label label-form">Regulatory Designation</label>
                                <div class="controls">
                                    <div class="form-value" id="regulatoryDesignation">${actionBean.editResearchProject.regulatoryDesignationDescription}</div>
                                    <stripes:hidden name="editResearchProject.regulatoryDesignation" value="${actionBean.editResearchProject.regulatoryDesignation}"/>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>


                    <div class="control-group">
                        <stripes:label for="researchProject" class="control-label">
                            Parent Research Project
                        </stripes:label>
                        <div class="controls">
                            <stripes:text
                                    id="parentResearchProject" name="projectTokenInput.listOfKeys"
                                    class="defaultText"
                                    title="Enter the parent research project"/>
                        </div>
                    </div>

                    <!-- Subproject view only. -->
                    <div class="control-group">
                        <label class="control-label">Subprojects</label>

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

                    <c:if test="${!actionBean.creating}">
                        <div class="control-group">
                            <stripes:label for="createdBy" class="control-label">Created By</stripes:label>

                            <div class="controls">
                                <div class="form-value">
                                        ${actionBean.getUserFullName(actionBean.editResearchProject.createdBy)}
                                    on <fmt:formatDate value="${actionBean.editResearchProject.createdDate}"
                                                       pattern="${actionBean.datePattern}"/>
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
                        <label class="control-label">Irb Numbers</label>

                        <div class="controls">
                            <div class="form-value">
                                <c:forEach items="${actionBean.editResearchProject.irbNumbers}" var="irb">
                                        ${irb}
                                </c:forEach>
                            </div>
                        </div>
                    </div>

                    <div class="control-group">
                        <label class="control-label">IRB Not Engaged</label>
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

                    <div class="control-group">
                        <label class="control-label">IRB Notes</label>

                        <div class="controls">
                            <div class="form-value">${actionBean.editResearchProject.irbNotes}</div>
                        </div>
                    </div>
                </div>


                <div class="form-horizontal span5">
                    <fieldset>
                        <legend><h4>Project Users</h4></legend>

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

                        <!-- Other -->
                        <div class="control-group">
                            <stripes:label for="otherUsers" class="control-label">Other Users</stripes:label>
                            <div class="controls">
                                <stripes:text id="otherUsers" name="otherUserList.listOfKeys" />
                            </div>
                        </div>
                    </fieldset>

                    <fieldset>
                        <legend><h4>Pipeline Analysis</h4></legend>

                        <div class="control-group">
                            <stripes:label for="accessControlEnabled" class="control-label">Access Control</stripes:label>
                            <div class="controls">
                                <stripes:checkbox name="editResearchProject.accessControlEnabled"
                                                  id="accessControlEnabled" style="margin-top: 10px;"/>
                                <stripes:label for="accessControlEnabled" class="control-label" style="width:auto;">Restrict to Project Users</stripes:label>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="ui-outputlabel control-label" for="sequenceAligner">Sequence Aligner</label>
                            <div class="controls">
                                <stripes:select id="sequenceAligner" name="editResearchProject.sequenceAlignerKey">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.sequenceAligners}" label="displayName" value="businessKey"/>
                                </stripes:select>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="ui-outputlabel control-label" for="referenceSequence">Reference Sequence</label>
                            <div class="controls">
                                <stripes:select id="referenceSequence" name="editResearchProject.referenceSequenceKey">
                                    <stripes:option value="">Select One</stripes:option>
                                    <stripes:options-collection collection="${actionBean.referenceSequences}" label="displayName" value="businessKey"/>
                                </stripes:select>
                            </div>
                        </div>
                    </fieldset>
                    <fieldset>
                        <legend><h4>Data Submission</h4></legend>
                        <div class="control-group">
                            <stripes:label for="submissionSite" class="control-label">Default Repository</stripes:label>
                            <div class="controls">
                                <stripes:select id="submissionSite" name="editResearchProject.submissionRepositoryName"
                                                disabled="${!actionBean.submissionServiceAvailable}">
                                    <stripes:option value="">Choose...</stripes:option>
                                    <stripes:options-collection label="description" value="name"
                                                                collection="${actionBean.activeRepositories}"/>
                                </stripes:select>
                            </div>
                        </div>
                    </fieldset>
                    <security:authorizeBlock roles="<%= roles(Developer, BillingManager, PDM) %>">
                        <fieldset>
                            <legend><h4>Default Billing Triggers</h4></legend>
                            <div class="control-group">
                                <stripes:label for="billingTriggers" class="control-label"/>
                                <div class="controls">
                                    <stripes:select multiple="true" style="width: auto;" id="billingTrigger"
                                                    name="editResearchProject.defaultBillingTriggers"
                                                    value="${actionBean.editResearchProject.billingTriggers}"
                                                    title="What can trigger billing.">
                                        <stripes:options-enumeration label="displayName"
                                                                     enum="org.broadinstitute.gpinformatics.athena.entity.project.BillingTrigger"/>
                                    </stripes:select>
                                </div>
                            </div>
                        </fieldset>
                    </security:authorizeBlock>
                </div>

            <div class="row">
                <div class="form-horizontal span12" >
                    <div class="control-group">
                        <div class="controls">
                            <div class="row-fluid">
                                <div class="span1">
                                    <stripes:submit name="save" value="Save" disabled="${!actionBean.canSave}" class="btn btn-primary"/>
                                </div>
                                <div class="span1">
                                    <c:choose>
                                        <c:when test="${actionBean.creating}">
                                            <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:link beanclass="${actionBean.class.name}" event="view">
                                                <stripes:param name="researchProject" value="${actionBean.researchProject}"/>
                                                Cancel
                                            </stripes:link>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
