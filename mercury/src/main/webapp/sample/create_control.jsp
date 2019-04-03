<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.submitString}"
                       sectionTitle="${actionBean.submitString}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="createForm">
            <div class="form-horizontal span6">
                <stripes:hidden name="collaboratorParticipantId"/>
                <stripes:hidden name="submitString"/>

                <c:choose>
                    <c:when test="${actionBean.creating}">
                        <div class="control-group">
                            <stripes:label for="controlName" class="control-label">
                                Collaborator Participant ID *
                            </stripes:label>
                            <div class="controls">
                                <stripes:text name="workingControl.collaboratorParticipantId" id="controlName"
                                              class="defaultText input-xlarge"
                                              title="Enter a Collaborator Participant Id"/>
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="view-control-group control-group">
                            <label class="control-label label-form">Collaborator Participant Id</label>

                            <div class="controls">
                                <div class="form-value">${actionBean.workingControl.businessKey}</div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>


                <c:choose>
                    <c:when test="${actionBean.creating}">

                        <div class="control-group">
                            <stripes:label for="controlType" class="control-label">
                                Control Type *
                            </stripes:label>
                            <div class="controls">
                                <stripes:radio value="${actionBean.positiveTypeValue}" id="positiveValue"
                                               name="createControlType"/>
                                    ${actionBean.positiveTypeValue}
                                <stripes:radio value="${actionBean.negativeTypeValue}" id="negativeValue"
                                               name="createControlType"/>
                                    ${actionBean.negativeTypeValue}
                            </div>
                        </div>

                    </c:when>
                    <c:otherwise>

                        <div class="view-control-group control-group">
                            <label class="control-label label-form">Control Type</label>

                            <div class="controls">
                                <div class="form-value">${actionBean.workingControl.type.displayName}</div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div class="control-group">
                    <stripes:label for="concordanceSmId" class="control-label">
                        Aliquot SM-ID for concordance
                    </stripes:label>
                    <div class="controls">
                        <input type="text" name="concordanceSmId" id="concordanceSmId"
                                          value="${actionBean.concordanceSmId}"/>
                    </div>
                </div>

                <c:if test="${not actionBean.creating}">
                    <div class="control-group">
                        <stripes:label for="controlState" class="control-label">
                            Make Control Inactive
                        </stripes:label>
                        <div class="controls">
                            <stripes:checkbox name="editControlInactiveState" id="controlState"
                                              value="${actionBean.workingControl.active}"/>
                        </div>
                    </div>
                </c:if>

                <div class="control-group">
                    <div class="control-label">&nbsp;</div>
                    <div class="controls actionButtons">
                        <stripes:submit name="save" value="Save"
                                        style="margin-right: 10px;" class="btn btn-primary"/>
                        <c:choose>
                            <c:when test="${actionBean.creating}">
                                <stripes:link beanclass="${actionBean.class.name}" event="list">Cancel</stripes:link>
                            </c:when>
                            <c:otherwise>
                                <stripes:link beanclass="${actionBean.class.name}" event="view">
                                    <stripes:param name="collaboratorParticipantId"
                                                   value="${actionBean.workingControl.businessKey}"/>
                                    Cancel
                                </stripes:link>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
