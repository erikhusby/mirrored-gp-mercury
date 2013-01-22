<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.WorkflowViewActionBean"/>

<c:forEach items="${actionBean.productWorkflowDefVersionMap}" var="workflow">
    <table class="simple workflow table" cellpadding="0" cellspacing="0">
        <thead>
        <tr>
            <th colspan="20">
                    ${workflow.key} ${workflow.value.version}
            </th>
        </tr>
        </thead>
        <tbody>

        <c:forEach items="${workflow.value.workflowProcessDefs}" var="process">
            <tr>
                <th colspan="20" bgcolor="#999999">
                    <b>${process.name}</b>
                </th>
            </tr>

            <tr>
                <c:forEach items="${process.effectiveVersion.workflowStepDefs}" var="step">
                    <td width="100">${step.name}</td>
                </c:forEach>
            </tr>
            <tr>
                <c:forEach items="${process.effectiveVersion.workflowStepDefs}" var="step">
                    <td class="${actionBean.getStepClass(step)}"
                        width="100">${actionBean.getLastEventForStep(step).eventDate}</td>
                </c:forEach>
            </tr>
        </c:forEach>


        </tbody>
    </table>
</c:forEach>