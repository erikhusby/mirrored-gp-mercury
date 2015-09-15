<%@ include file="/resources/layout/taglibs.jsp" %>

<%--
  - Snippet for showing a progress bar of sample status for a PDO.
  -
  - @param status      a ProductOrderCompletionStatus object for the PDO
  - @param extraStyle  any extra styles to add to the main container div, e.g. "view"
  --%>

<%--@elvariable id="status" type="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus"--%>

<stripes:layout-definition>
    <div class="barFull ${extraStyle}" title="${status.percentInProgressDisplay}% In Progress">
        <span class="barAbandon"
              title="${status.percentAbandonedDisplay}% Abandoned"
              style="width: ${status.percentAbandoned * 100}%"> </span>
        <span class="barComplete"
              title="${status.percentCompletedDisplay}% Completed"
              style="width: ${status.percentCompleted * 100}%"> </span>
    </div>
</stripes:layout-definition>