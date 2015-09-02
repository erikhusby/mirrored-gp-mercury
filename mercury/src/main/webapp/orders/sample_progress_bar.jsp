<%@ include file="/resources/layout/taglibs.jsp" %>

<%--@elvariable id="status" type="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus"--%>
<%--@elvariable id="size" type="org.broadinstitute.gpinformatics.athena.presentation.orders.SampleProgressBar.Size"--%>

<stripes:layout-definition>
    <jsp:useBean id="progressBar" class="org.broadinstitute.gpinformatics.athena.presentation.orders.SampleProgressBar" scope="page">
        <jsp:setProperty name="progressBar" property="status" value="${status}"/>
    </jsp:useBean>

    <div class="barFull ${size == 'LARGE' ? 'view' : ''}" title="${progressBar.formatPercentageString(progressBar.status.percentInProgress)}% In Progress">
        <span class="barAbandon"
              title="${progressBar.formatPercentageString(progressBar.status.percentAbandoned)}% Abandoned"
              style="width: ${progressBar.getPercentForProgressBarWidth(progressBar.status.percentAbandoned)}%"> </span>
        <span class="barComplete"
              title="${progressBar.formatPercentageString(progressBar.status.percentCompleted)}% Completed"
              style="width: ${progressBar.getPercentForProgressBarWidth(progressBar.status.percentCompleted)}%"> </span>
    </div>
</stripes:layout-definition>