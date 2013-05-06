<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>

    <%-- Where in the hosting page to set the id the user chooses --%>
    <%--@elvariable id="listItems" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>

    <script type="text/javascript">

        function plasticHistoryListRedraw() {
            var eventDateIdx = 9;

            var table = $j('#plasticHistoryList').dataTable({"bDestroy":"true",
                "oTableTools":ttExportDefines,
                "aaSorting":[
                    [eventDateIdx, 'asc']
                ],
                "aoColumns":[
                    {"bSortable":true, "sType":"html"},
                    //Label
                    {"bSortable":true, "sType":"numeric"},
                    //Sample count
                    {"bSortable":true},
                    //Type
                    {"bSortable":true, "sType":"numeric"},
                    //Pdo count
                    {"bSortable":true, "sType":"numeric"},
                    //Index count
                    {"bSortable":true, "sType":"html"},
                    //Lab batch count
                    {"bSortable":true},
                    //Latest event
                    {"bSortable":true},
                    //Event location
                    {"bSortable":true},
                    //Event user
                    {"bSortable":true, "sType":"date"},
                    //Event date
                    {"bSortable":true, "sType":"date"}     //Creation date
                ]
            });
            includeAdvancedFilter(table, '#plasticHistoryList');
        }

    </script>
    <table id="plasticHistoryList" class="table simple">
        <thead>
        <tr>
            <th>Label</th>
            <th width="80">Sample Count</th>
            <th>Type</th>
            <th width="30">PDO Count</th>
            <th width="30">Index Count</th>
            <th width="70">Lab Batch</th>
            <th width="100">Latest Event</th>
            <th width="120">Event Location</th>
            <th>Event User</th>
            <th width="120">Event Date</th>
            <th width="60">Creation Date</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${listItems}" var="listItem">
            <tr>
                <td>
                    <a href="${ctxpath}/search/all.action?search=&searchKey=${listItem.label}">
                            ${listItem.label}
                    </a>
                </td>
                <td>
                        ${listItem.sampleInstanceCount}
                </td>
                <td>
                        ${listItem.type}
                </td>
                <td>
                        ${listItem.pdoKeyCount}
                </td>
                <td>
                        ${listItem.indexCount}
                </td>
                <td>
                    <c:forEach items="${listItem.labBatchCompositions}" var="composition">
                        <span style="white-space:nowrap;">
                            <stripes:link target="JIRA"
                                          href="${composition.labBatch.jiraTicket.browserUrl}"
                                          class="external">
                                ${composition.labBatch.businessKey}
                            </stripes:link>
                            ${composition.count}/${composition.denominator}
                        </span>
                        <br/>
                    </c:forEach>
                </td>
                <td>
                        ${listItem.eventType}
                </td>
                <td>
                        ${listItem.eventLocation}
                </td>
                <td>
                        ${bean.getUserFullNameOrBlank(listItem.eventOperator)}
                </td>
                <td>
                    <fmt:formatDate value="${listItem.eventDate}" pattern="${bean.dateTimePattern}"/>
                </td>
                <td>
                    <fmt:formatDate value="${listItem.creationDate}" pattern="${bean.datePattern}"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

</stripes:layout-definition>
