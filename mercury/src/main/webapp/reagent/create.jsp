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

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}"
                       sectionTitle="${actionBean.submitString} ${actionBean.reagentDesign.designName}">

    <stripes:layout-component name="extraHead">

    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="businessKey" value="${actionBean.reagentDesign.businessKey}"/>
            <stripes:hidden name="submitString" value="${actionBean.submitString}"/>

            <div class="control-group">
                <stripes:label for="designName" name="Name *" class="control-label"/>
                <div class="controls">
                            <stripes:text id="designName" readonly="${! actionBean.creating}"
                                          name="reagentDesign.designName"
                                          class="defaultText"
                                          title="Enter the name of the reagent design"/>

                </div>
            </div>

            <div class="control-group">
                <stripes:label for="targetSetName" name="Target Set Name" class="control-label"/>
                <div class="controls">
                    <stripes:text id="targetSetName" name="reagentDesign.targetSetName" class="defaultText"
                                  title="Enter the name of the target set."/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="manufacturersName" name="Manufacturer" class="control-label"/>
                <div class="controls">
                    <stripes:text id="manufacturersName" name="reagentDesign.manufacturersName" class="defaultText"
                                  title="Enter the name of the manufacturer."/>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="reagentType" name="Reagent Type *" class="control-label"/>
                <div class="controls">
                    <stripes:select name="reagentDesign.reagentType"  disabled="${!actionBean.creating}" id="reagentType">
                        <stripes:option value="">Select a Reagent Type</stripes:option>
                        <stripes:options-enumeration
                                enum="org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType"/>
                    </stripes:select>
                    <stripes:hidden name="reagentDesign.reagentType" value="${actionBean.reagentDesign.reagentType}"/>
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
                            <stripes:submit name="save" value="${saveAction}"/>
                        </div>
                        <div class="offset">
                            <c:choose>
                                <c:when test="${actionBean.creating}">
                                    <stripes:link beanclass="${actionBean.class.name}"
                                                  event="list">Cancel</stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link beanclass="${actionBean.class.name}" event="view">
                                        <stripes:param name="businessKey"
                                                       value="${actionBean.reagentDesign.businessKey}"/>
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
