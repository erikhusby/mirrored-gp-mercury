<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <script type="text/javascript">
        function showPlasticHistoryVisualizer(batchKey) {
            $j('#plasticViewDiv').load('${ctxpath}/view/plasticHistoryView.action?batchKey=' + batchKey);
            $j('#plasticViewDiv').show();
        }
    </script>
    <%--@elvariable id="batches" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>

    <table id="sampleListView" class="table simple">
        <thead>
        <tr>
            <th width="30">Vessel History</th>
            <th>Batch Name</th>
            <th>JIRA ID</th>
            <th>Is Active</th>
            <th>Create Date</th>
            <th>Latest Event</th>
            <th>Event Location</th>
            <th>Event User</th>
            <th>Event Date</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${batches}" var="batch">
            <tr>
                <td>
                    <a href="javascript:showPlasticHistoryVisualizer('${batch.businessKey}')">
                        <img width="30" height="30" name="" title="show plastic history view"
                             src="${ctxpath}/images/plate.png"/>
                    </a>
                </td>
                <td>
                        ${batch.businessKey}
                </td>
                <td>
                    <stripes:link href="${batch.jiraTicket.browserUrl}">

                        ${batch.jiraTicket.ticketName}
                    </stripes:link>
                </td>
                <td>
                        ${batch.active}
                </td>
                <td>
                    <fmt:formatDate value="${batch.createdOn}" pattern="MM/dd/yyyy"/>
                </td>
                <td>
                        ${batch.latestEvent.labEventType.name}
                </td>
                <td>
                        ${batch.latestEvent.eventLocation}
                </td>
                <td>
                        ${bean.getUserFullName(batch.latestEvent.eventOperator)}
                </td>
                <td>
                    <fmt:formatDate value="${batch.latestEvent.eventDate}" pattern="MM/dd/yyyy"/>
                </td>

            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div id="plasticViewDiv" style="display:none"></div>
</stripes:layout-definition>