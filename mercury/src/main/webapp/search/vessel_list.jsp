<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>

    <%-- Where in the hosting page to set the id the user chooses --%>
    <%--@elvariable id="vessels" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>

    <script type="text/javascript">
        $(document).ready(function () {
            $j('.vessel-checkbox').enableCheckboxRangeSelection({
                checkAllClass: 'vessel-checkAll',
                countDisplayClass: 'vessel-checkedCount',
                checkboxClass: 'vessel-checkbox'});
            });

        function showVesselVisualizer(div, label) {
            $j('#' + div).load('${ctxpath}/view/vesselView.action?vesselLabel=' + label);
            $j('#' + div).show();
        }

        function showSampleVisualizer(div, label) {
            $j('#' + div).show();
        }

    </script>
    <table id="productOrderList" class="table simple">
        <thead>
        <tr>
            <th width="40">
                <input for="count" type="checkbox" class="vessel-checkAll"/><span id="count" class="vessel-checkedCount"></span>
            </th>
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
                <td>
                    <stripes:checkbox class="vessel-checkbox" name="selectedProductOrderBusinessKeys" value="${vessel.label}"/>
                </td>

                <td>
                    <a href="javascript:showVesselVisualizer('vesselViewerDiv', '${vessel.label}')">
                        <img width="30" height="30" name="" title="show plate view" src="${ctxpath}/images/plate.png"/>
                    </a>
                </td>
                <td>
                    <a href="javascript:showSampleVisualizer('sampleViewerDiv', '${vessel.label}')">
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
                    ${vessel.getPdoKeysString}
                </td>
                <td>
                    ${bean.getIndexesMap[vessel.label]}
                </td>
                <td>
                    ${vessel.nearestLabBatchesString}
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

    <div id="vesselViewerDiv" style="display:none">
    </div>
    <div id="sampleViewerDiv" style="display:none">
    </div>
</stripes:layout-definition>