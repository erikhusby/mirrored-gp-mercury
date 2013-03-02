<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Edit Research Project" sectionTitle="Edit Research Project: ${actionBean.editResearchProject.title}">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                    function () {
                        $j("#projectManagers").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    hintText: "Type a Project Manager name",
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.projectManagerList.completeData)},
                                    preventDuplicates: true,
                                    resultsFormatter: formatInput
                                }
                        );

                        $j("#scientists").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    hintText: "Type a Scientist name",
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.scientistList.completeData)},
                                    preventDuplicates: true,
                                    resultsFormatter: formatInput
                                }
                        );

                        $j("#externalCollaborators").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    hintText: "Type a Collaborator name",
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.externalCollaboratorList.completeData)},
                                    preventDuplicates: true,
                                    resultsFormatter: formatInput
                                }
                        );

                        $j("#broadPIs").tokenInput(
                                "${ctxpath}/projects/project.action?usersAutocomplete=", {
                                    hintText: "Type a Broad PI",
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.broadPiList.completeData)},
                                    preventDuplicates: true,
                                    resultsFormatter: formatInput
                                }
                        );

                        $j("#fundingSources").tokenInput(
                                "${ctxpath}/projects/project.action?fundingAutocomplete=", {
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.fundingSourceList.completeData)},
                                    preventDuplicates: true,
                                    resultsFormatter: formatInput
                                }
                        );

                        $j("#cohorts").tokenInput(
                                "${ctxpath}/projects/project.action?cohortAutocomplete=", {
                                    hintText: "Type a Sample Cohort name",
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.cohortsList.completeData)},
                                    preventDuplicates: true,
                                    resultsFormatter: formatInput
                                }
                        );

                        $j("#irbs").tokenInput(
                                "${ctxpath}/projects/project.action?irbAutocomplete=", {
                                    hintText: "Type an IRB Number",
                                    prePopulate: ${actionBean.ensureStringResult(actionBean.irbsCompleteData)},
                                    preventDuplicates: true
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
            <div class="form-horizontal">
                <div class="control-group">
                    <stripes:label for="title" class="control-label">Name *</stripes:label>
                    <div class="controls">
                            <stripes:text name="editResearchProject.title" value="${actionBean.editResearchProject.title}"
                                          id="title"  class="defaultText input-xxlarge" title="Enter the project name"  maxlength="255"/>
                    </div>
                </div>

                <!-- Synopsis -->
                <div class="control-group">
                    <stripes:label for="synopsis" class="control-label">Synopsis *</stripes:label>

                    <div class="controls">
                        <stripes:textarea id="synopsis" rows="5" cols="100" name="editResearchProject.synopsis" class="defaultText textarea input-xxlarge"
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

                <c:if test="${!actionBean.creating}">
                    <div class="control-group">
                        <stripes:label for="createdBy" class="control-label">Created By</stripes:label>

                        <div class="controls">
                            <div class="form-value">
                                    ${actionBean.getUserFullName(actionBean.editResearchProject.createdBy)}
                                on <fmt:formatDate value="${actionBean.editResearchProject.createdDate}"/>
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
                        <stripes:text id="irbs" name="irbList" title="Enter the IRB Number" class="defaultText"/>
                        <p>
                            <stripes:checkbox id="irbNotEngaged" name="editResearchProject.irbNotEngaged"/>&nbsp;<stripes:label for="irbNotEngaged" style="display:inline;">IRB Not Engaged</stripes:label>
                        </p>
                    </div>
                </div>

                <div class="control-group">
                    <label id="j_idt130" class="ui-outputlabel control-label" for="irbNotes">IRB Notes</label>
                    <div class="controls">
                        <stripes:text id="irbNotes" class="defaultText input-xxlarge" title="Enter notes about the above IRBs" name="editResearchProject.irbNotes" value="${actionBean.editResearchProject.irbNotes}" maxlength="255" />
                    </div>
                </div>

            </div>

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

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
