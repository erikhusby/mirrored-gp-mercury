<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Control" sectionTitle="View Control">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form action="/sample/controls.action" id="sampleControlForm" class="form-horizontal">

            <stripes:hidden name="collaboratorParticipantId" value="${actionBean.workingControl.businessKey}"/>

            <div class="actionButtons">
                <stripes:link title="Click to edit ${actionBean.workingControl.businessKey}"
                              beanclass="${actionBean.class.name}" event="edit" class="pull-right">
                    <span class="icon-shopping-cart"></span> <%=CollaboratorControlsActionBean.EDIT_CONTROL%>
                    <stripes:param name="collaboratorParticipantId"
                                   value="${actionBean.workingControl.businessKey}"/>
                </stripes:link>
            </div>
        </stripes:form>

        <stripes:form action="/sample/controls.action" id="sampleControlForm" class="form-horizontal">

            <stripes:hidden name="collaboratorParticipantId" value="${actionBean.workingControl.businessKey}"/>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Collaborator Participant Id</label>

                <div class="controls">
                    <div class="form-value">${actionBean.workingControl.businessKey}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Control Type</label>

                <div class="controls">
                    <div class="form-value">${actionBean.workingControl.type.displayName}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Control State</label>

                <div class="controls">
                    <div class="form-value">${actionBean.workingControl.state.displayName}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Concordance Sample</label>

                <div class="controls">
                    <div class="form-value">
                        <c:if test="${not empty actionBean.workingControl.concordanceMercurySample}">
                            ${actionBean.workingControl.concordanceMercurySample.sampleKey}
                        </c:if>
                    </div>
                </div>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
