<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>

    <%-- Where in the hosting page to set the id the user chooses --%>
    <%--@elvariable id="listItems" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>

    <script type="text/javascript">

        function plasticHistoryListRedraw() {
            var eventDateIdx = 9;

            $j('#plasticHistoryList').dataTable({
                "bDestroy":"true",
                "oTableTools":ttExportDefines,
                "aaSorting":[
                    [eventDateIdx, 'asc']
                ],
                "aoColumns":[
                    {"bSortable":true},
                    //label
                    {"bSortable":true, "sType":"numeric"},
                    //Sample count
                    {"bSortable":true},
                    //Type
                    {"bSortable":true, "sType":"numeric"},
                    //Pdo count
                    {"bSortable":true, "sType":"numeric"},
                    //Index count
                    {"bSortable":true, "sType":"numeric"},
                    //Lab batch count
                    {"bSortable":true},
                    //Latest event
                    {"bSortable":true},
                    //Event location
                    {"bSortable":true},
                    //Event user
                    {"bSortable":true, "sType":"date"},
                    //Event date
                    {"bSortable":true, "sType":"date"  //Creation date
                    }
                ]
            });
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
            <th width="30">Lab Batch Count</th>
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
                        ${listItem.labBatchCount}
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
                    <fmt:formatDate value="${listItem.eventDate}" pattern="MM/dd/yyyy HH:MM:ss.SSS"/>
                </td>
                <td>
                    <fmt:formatDate value="${listItem.creationDate}" pattern="MM/dd/yyyy"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

</stripes:layout-definition>
