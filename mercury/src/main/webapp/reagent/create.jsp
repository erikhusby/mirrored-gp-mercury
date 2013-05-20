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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentDesignActionBean"/>

<stripes:layout-render name="/layout.jsp"
                       pageTitle="${actionBean.submitString}"
                       sectionTitle="${actionBean.submitString}: ${actionBean.editReagentDesign.designName}">

    <stripes:layout-component name="extraHead"></stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="reagentDesign" value="${actionBean.editReagentDesign.businessKey}"/>
            <stripes:hidden name="submitString" value="${actionBean.submitString}"/>

            <div class="control-group">
                <stripes:label for="designName" class="control-label">
                    Name *
                </stripes:label>
                <div class="controls">
                            <stripes:text id="designName" readonly="${!actionBean.creating}"
                                          name="editReagentDesign.designName"
                                          class="defaultText"
                                          title="Enter the name of the reagent design"/>`

                </div>
            </div>

            <div class="control-group">
                <stripes:label for="targetSetName" class="control-label">
                    Target Set Name *
                </stripes:label>
                <div class="controls">
                    <stripes:text id="targetSetName" name="editReagentDesign.targetSetName" class="defaultText"
                                  title="Enter the name of the target set."/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="manufacturersName" class="control-label">
                    Manufacturer
                </stripes:label>
                <div class="controls">
                    <stripes:text id="manufacturersName" name="editReagentDesign.manufacturersName" class="defaultText"
                                  title="Enter the name of the manufacturer."/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="reagentType" class="control-label">
                    Reagent Type *
                </stripes:label>
                <div class="controls">
                    <stripes:select name="editReagentDesign.reagentType"  disabled="${!actionBean.creating}" id="reagentType">
                        <stripes:option value="">Select a Reagent Type</stripes:option>
                        <stripes:options-enumeration
                                enum="org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType"/>
                    </stripes:select>
                    <stripes:hidden name="editReagentDesign.reagentType" value="${actionBean.editReagentDesign.reagentType}"/>
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
                                        <stripes:param name="reagentDesign" value="${actionBean.reagentDesign}"/>
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
