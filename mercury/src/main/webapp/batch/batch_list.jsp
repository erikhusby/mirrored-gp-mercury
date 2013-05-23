<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <script type="text/javascript">

        $(document).ready(function () {

            if (${showCheckboxes}) {
                $j('#batchListView').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [2, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": false},
                        {"bSortable": false},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true, "sType": "date"},
                        {"bSortable": true, "sType": "date"}
                    ]
                });
            }
            else {
                $j('#batchListView').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [
                        [1, 'asc']
                    ],
                    "aoColumns": [
                        {"bSortable": false},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true, "sType": "date"},
                        {"bSortable": true, "sType": "date"}
                    ]
                });
            }

            $j('.batch-checkbox').enableCheckboxRangeSelection({
                checkAllClass: 'batch-checkAll',
                countDisplayClass: 'batch-checkedCount',
                checkboxClass: 'batch-checkbox'});
        });
        function showPlasticHistoryVisualizer(batchKey) {
            $j('#plasticViewDiv').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
            $j('#plasticViewDiv').show();

            // Dynamically created table needs ajax load to allow calling into script in plastic_history_list.jsp
            $j.ajax({
                url: "${ctxpath}/view/plasticHistoryView.action?batchKey=" + batchKey,
                dataType: 'html',
                success: function (plasticViewHtml) {
                    $j('#plasticViewDiv').html(plasticViewHtml);
                    $j('#plasticViewDiv').show();
                    plasticHistoryListRedraw();
                }
            });
        }

    </script>
    <%--@elvariable id="batches" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>
    <%--@elvariable id="showCheckboxes" type="java.lang.Boolean"--%>

    <table id="batchListView" class="table simple">
        <thead>
        <tr>
            <c:if test="${showCheckboxes}">
                <th width="40">
                    <input type="checkbox" class="batch-checkAll"/><span id="count" class="batch-checkedCount"></span>
                </th>
            </c:if>
            <th width="30">Vessel History</th>
            <th>Batch Name</th>
            <th>JIRA ID</th>
            <th>Is Active</th>
            <th>Latest Event</th>
            <th>Event Location</th>
            <th>Event User</th>
            <th>Event Date</th>
            <th>Create Date</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${batches}" var="batch">
            <tr>
                <c:if test="${showCheckboxes}">
                    <td>

                        <stripes:checkbox class="batch-checkbox" name="selectedBatchLabels"
                                          value="${batch.businessKeyList}"/>

                    </td>
                </c:if>
                <td>
                    <a href="javascript:showPlasticHistoryVisualizer('${batch.businessKeyList}')">
                        <img width="30" height="30" name="" title="show plastic history view"
                             src="${ctxpath}/images/plate.png"/>
                    </a>
                </td>
                <td>
                    <a href="${ctxpath}/search/all.action?search=&searchKey=${batch.businessKeyList}">
                            ${batch.businessKeyList}
                    </a>
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
                        ${batch.latestEvent.labEventType.name}
                </td>
                <td>
                        ${batch.latestEvent.eventLocation}
                </td>
                <td>
                        ${bean.getUserFullName(batch.latestEvent.eventOperator)}
                </td>
                <td>
                    <fmt:formatDate value="${batch.latestEvent.eventDate}" pattern="${bean.dateTimePattern}"/>
                </td>
                <td>
                    <fmt:formatDate value="${batch.createdOn}" pattern="${bean.dateTimePattern}"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div id="plasticViewDiv" style="display:none"></div>
</stripes:layout-definition>