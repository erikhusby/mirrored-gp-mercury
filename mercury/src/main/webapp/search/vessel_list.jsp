<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>

    <%-- Where in the hosting page to set the id the user chooses --%>
    <%--@elvariable id="vessels" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>
    <%--@elvariable id="showCheckboxes" type="java.lang.Boolean"--%>

    <script type="text/javascript">
        $(document).ready(function () {
            $j('.vessel-checkbox').enableCheckboxRangeSelection({
                checkAllClass:'vessel-checkAll',
                countDisplayClass:'vessel-checkedCount',
                checkboxClass:'vessel-checkbox'});
        });

        function showVesselVisualizer(label) {
            $j('#viewerDiv').load('${ctxpath}/view/vesselView.action?vesselLabel=' + label);
            $j('#viewerDiv').show();
        }

        function showSampleVisualizer(label) {
            $j('#viewerDiv').load('${ctxpath}/view/sampleView.action?vesselLabel=' + label);
            $j('#viewerDiv').show();
        }

    </script>
    <table id="productOrderList" class="table simple">
        <thead>
        <tr>
            <c:if test="${showCheckboxes}">
                <th width="40">

                    <input type="checkbox" class="vessel-checkAll"/><span id="count" class="vessel-checkedCount"></span>
                </th>
            </c:if>
            <th width="30">Vessel Viewer</th>
            <th width="30">Sample List Viewer</th>
            <th>Label</th>
            <th width="80">Sample Count</th>
            <th>Type</th>
            <th width="30">PDO Count</th>
            <th width="30">Index Count</th>
            <th width="30">Lab Batch Count</th>
            <th width="100">Latest Event</th>
            <th width="120">Event Location</th>
            <th>Event User</th>
            <th width="60">Event Date</th>
            <th width="60">Creation Date</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${vessels}" var="vessel">
            <tr>
                <c:if test="${showCheckboxes}">
                    <td>

                        <stripes:checkbox class="vessel-checkbox" name="selectedVesselLabels"
                                          value="${vessel.label}"/>

                    </td>
                </c:if>
                <td>
                    <a href="javascript:showVesselVisualizer('${vessel.label}')">
                        <img width="30" height="30" name="" title="show plate view" src="${ctxpath}/images/plate.png"/>
                    </a>
                </td>
                <td>
                    <a href="javascript:showSampleVisualizer('${vessel.label}')">
                        <img width="30" height="30" name="" title="show sample list" src="${ctxpath}/images/list.png"/>
                    </a>
                </td>
                <td>
                        ${vessel.label}
                </td>
                <td>
                        ${vessel.sampleInstanceCount}
                </td>
                <td>
                        ${vessel.type.name}
                </td>
                <td>
                        ${vessel.pdoKeysCount}
                </td>
                <td>
                        ${vessel.indexesCount}
                </td>
                <td>
                        ${vessel.nearestLabBatchesCount}
                </td>
                <td>
                        ${vessel.latestEvent.labEventType.name}
                </td>
                <td>
                        ${vessel.latestEvent.eventLocation}
                </td>
                <td>
                        ${bean.fullNameMap[vessel.latestEvent.eventOperator]}
                </td>
                <td>
                    <fmt:formatDate value="${vessel.latestEvent.eventDate}" pattern="MM/dd/yyyy"/>
                </td>
                <td>
                    <fmt:formatDate value="${vessel.createdOn}" pattern="MM/dd/yyyy"/>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <div id="viewerDiv" style="display:none"></div>
</stripes:layout-definition>
