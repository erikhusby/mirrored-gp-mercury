<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Mercury Control" sectionTitle="View Mercury Control">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form action="/resources/sample/controls.action" id="sampleControlForm" class="form-horizontal">

            <stripes:hidden name="sampleCollaboratorId" value="${actionBean.workingControl.collaboratorSampleId}"/>

            <div class="actionButtons">
                <stripes:link title="Click to edit ${actionBean.workingControl.collaboratorSampleId}"
                              beanclass="${actionBean.class.name}" event="edit" class="pull-right">
                    <span class="icon-shopping-cart"></span> <%=CollaboratorControlsActionBean.EDIT_CONTROL%>
                    <stripes:param name="sampleCollaboratorId"
                                   value="${actionBean.workingControl.collaboratorSampleId}"/>
                </stripes:link>
            </div>
        </stripes:form>

        <stripes:form action="/resources/sample/controls.action" id="sampleControlForm" class="form-horizontal">

            <stripes:hidden name="sampleCollaboratorId" value="${actionBean.workingControl.collaboratorSampleId}"/>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Collaborator Sample Id</label>

                <div class="controls">
                    <div class="form-value">${actionBean.workingControl.collaboratorSampleId}</div>
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


        </stripes:form>


    </stripes:layout-component>
</stripes:layout-render>
