<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.WorkflowActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="View Workflow: ${actionBean.viewWorkflow.name}"
                       sectionTitle="View Workflow: ${actionBean.viewWorkflow.name}"
                       businessKeyValue="${actionBean.viewWorkflow.name}">

    <stripes:layout-component name="content">

        <div class="form-horizontal span7">

            <div class="view-control-group control-group">
                <label class="control-label label-form">Name</label>
                <div class="controls">
                    <div class="form-value">${actionBean.viewWorkflow.name}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Version</label>
                <div class="controls">
                    <div class="form-value">${actionBean.viewWorkflow.effectiveVersion.version}</div>
                </div>
            </div>

            <div class="view-control-group control-group">
                <label class="control-label label-form">Effective Date</label>
                <div class="controls">
                    <div class="form-value">${actionBean.viewWorkflowDto.effectiveDate}</div>
                </div>
            </div>

        <c:choose>
            <c:when test="${actionBean.workflowImage != null}">
                    <img src="${actionBean.workflowImage}"  class="hidden-print img-rounded workflow-image"
                         alt="Image for Workflow: ${actionBean.viewWorkflow.name} Version: ${actionBean.viewWorkflow.effectiveVersion.version}"/>
            </c:when>
            <c:otherwise>
                <div class="help-block btn-warning">
                    No image available for Workflow: ${actionBean.viewWorkflow.name} Version: ${actionBean.viewWorkflow.effectiveVersion.version}
                </div>
            </c:otherwise>
        </c:choose>

    </stripes:layout-component>
</stripes:layout-render>
