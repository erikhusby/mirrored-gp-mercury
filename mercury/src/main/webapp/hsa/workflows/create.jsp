<%@ include file="/resources/layout/taglibs.jsp" %>

<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2013 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.FiniteStateMachineActionBean"/>

<stripes:layout-render name="/layout.jsp"
                       pageTitle="${actionBean.submitString}: ${actionBean.editFiniteStateMachine.stateMachineName}"
                       sectionTitle="${actionBean.submitString}: ${actionBean.editFiniteStateMachine.stateMachineName}">

    <stripes:layout-component name="extraHead">

    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="finiteStateMachineKey" value="${actionBean.editFiniteStateMachine.finiteStateMachineId}"/>
            <stripes:hidden name="submitString" value="${actionBean.submitString}"/>

            <div class="control-group">
                <stripes:label for="stateMachineName" class="control-label">
                    Name *
                </stripes:label>
                <div class="controls">
                    <stripes:text id="stateMachineName" readonly="${!actionBean.creating}"
                                  name="editFiniteStateMachine.stateMachineName"
                                  class="defaultText"
                                  title="Enter the name of the state machine"/>`

                </div>
            </div>

            <div class="control-group">
                <stripes:label for="runName" class="control-label">
                    Illumina Run Name *
                </stripes:label>
                <div class="controls">
                    <stripes:text id="runName" name="runName" class="defaultText"
                                  title="Enter the name of the Illumina Sequencing Run."/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="sampleIds" class="control-label">
                    Filter Samples
                </stripes:label>
                <div class="controls">
                    <stripes:textarea id="sampleIds" name="sampleIds" title="Demultiplex/Align only certain samples."/>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <div class="row-fluid">
                        <div class="span2">
                            <c:choose>
                                <c:when test="${actionBean.creating}">
                                    <c:set var="saveAction" value="Create"/>
                                </c:when>
                                <c:otherwise>
                                    <c:set var="saveAction" value="Update"/>
                                </c:otherwise>
                            </c:choose>
                            <stripes:submit name="save" class="btn btn-primary" value="${saveAction}"/>
                        </div>
                        <div class="offset">
                            <c:choose>
                                <c:when test="${actionBean.creating}">
                                    <stripes:link beanclass="${actionBean.class.name}"
                                                  event="list">Cancel</stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link beanclass="${actionBean.class.name}" event="view">
                                        <stripes:param name="finiteStateMachineKey" value="${actionBean.finiteStateMachineKey}"/>
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
