<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Search Vessels" sectionTitle="Search">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function showResult(type) {
                $j('#' + type + 'Div').show();
            }

            function showVisualizer(div, label) {
                $j('#' + div).show();
                $j('#' + div).load('${ctxpath}/view/vesselView.action?vesselLabel=' + label);
            }

            function hideResult(type) {
                $j('#' + type + 'Div').hide();
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="searchForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group" style="margin-bottom:5px;">
                    <stripes:label for="barcode" class="control-label" style="width: 60px;">barcode</stripes:label>
                    <div class="controls" style="margin-left: 80px;">
                        <stripes:textarea rows="5" cols="80" name="searchKey" id="name"
                                          title="Enter the value to search"
                                          style="font-size: x-small; width:auto;" class="defaultText"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls" style="margin-left: 80px;">
                        <stripes:submit name="search" value="Search"/>
                    </div>
                </div>
            </div>
        </stripes:form>

        <c:if test="${not actionBean.hasResults}">
            No Results Found
        </c:if>

        <c:if test="${not empty actionBean.foundVessels}">
            <div class="tableBar">
                Found ${fn:length(actionBean.foundVessels)} Vessels
                <a id="vesselAnchor" href="javascript:showResult('vessel')" style="margin-left: 20px;">show</a>
                <a id="vesselAnchorHide" href="javascript:hideResult('vessel')"
                   style="display:none; margin-left: 20px;">hide</a>
            </div>
            <div id="vesselDiv" style="display:none">
                <table id="productOrderList" class="table simple">
                    <thead>
                    <tr>
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
                    <c:forEach items="${actionBean.foundVessels}" var="vessel">
                        <tr>
                            <td>
                                <a href="javascript:showVisualizer('vesselViewerDiv', '${vessel.label}')"
                                   style="margin-left: 20px;">show</a>
                            </td>
                            <td>
                                <a href="javascript:showVisualizer('sampleViewerDiv', '${vessel.label}')"
                                   style="margin-left: 20px;">show</a>
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
                                get vessel transient pdo key(s)
                            </td>
                            <td>
                                get vessel transient indexes
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
                                    ${actionBean.fullNameMap[vessel.latestEvent.eventOperator]}
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
            </div>

            <div id="vesselViewerDiv" style="display:none">
            </div>
            <div id="sampleViewerDiv" style="display:none">
                samp
            </div>
        </c:if>

        <c:if test="${not empty actionBean.foundSamples}">
            <div class="tableBar">
                Found ${fn:length(actionBean.foundSamples)} Samples
                <a id="sampleAnchor" href="javascript:showResult('sample')" style="margin-left: 20px;">show</a>
                <a id="sampleAnchorHide" href="javascript:hideResult('sample')"
                   style="display:none; margin-left: 20px;">hide</a>
            </div>
            <div id="sampleDiv" style="display:none"></div>
        </c:if>

        <c:if test="${not empty actionBean.foundPDOs}">
            <div class="tableBar">
                Found ${fn:length(actionBean.foundPDOs)} PDOs
                <a id="pdoAnchor" href="javascript:showResult('pdo')" style="margin-left: 20px;">show</a>
                <a id="pdoAnchorHide" href="javascript:hideResult('pdo')"
                   style="display:none; margin-left: 20px;">hide</a>
            </div>
            <div id="pdoDiv" style="display:none"></div>
        </c:if>

        <c:if test="${not empty actionBean.foundBatches}">
            <div class="tableBar">
                Found ${fn:length(actionBean.foundBatches)} Batches
                <a id="batchAnchor" href="javascript:showResult('batch')" style="margin-left: 20px;">show</a>
                <a id="batchAnchorHide" href="javascript:hideResult('batch')" style="display:none; margin-left: 20px;">hide</a>
            </div>
            <div id="batchDiv" style="display:none"></div>
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>
