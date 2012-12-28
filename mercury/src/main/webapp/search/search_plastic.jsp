<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchPlasticActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Search Vessels" sectionTitle="Search Vessels">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(
                    $j('#searchResults').dataTable( {
                        "oTableTools": ttExportDefines,
                        "aaSorting": [[0,'asc']],
                        "aoColumns": [
                            {"bSortable": true},                    // Sample #
                            {"bSortable": true},                    // Label
                            {"bSortable": true},                    // type
                            {"bSortable": true},                    // Lab Batches
                            {"bSortable": true},                    // Latest Event
                            {"bSortable": true},                    // Event Location
                            {"bSortable": true},                    // Event User
                            {"bSortable": true, "sType": "date"},   // Event Date
                            {"bSortable": true, "sType": "html"}]   // Creation Date
                    })
            );
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="searchForm" class="form-horizontal">
            <div class="form-horizontal">
                <div class="control-group" style="margin-bottom:5px;">
                    <stripes:label for="barcode" class="control-label" style="width: 60px;">barcode</stripes:label>
                    <div class="controls" style="margin-left: 80px;">
                        <stripes:text name="barcode" id="barcode" title="Enter the barcode to search" style="font-size: x-small; height: 14px;" class="defaultText"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls" style="margin-left: 80px;">
                            <stripes:submit name="search" value="Search"/>
                    </div>
                </div>
            </div>

            <div class="tableBar">
                Results
            </div>

            <table id="searchResults" class="table simple">
                <thead>
                    <tr>
                        <th>Sample #</th>
                        <th>Label</th>
                        <th>Type</th>
                        <th>Lab Batches</th>
                        <th>Latest Event</th>
                        <th>Event Location</th>
                        <th>Event User</th>
                        <th>Event Date</th>
                        <th>Creation Date</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${actionBean.foundVessels}" var="vessel">
                        <tr>
                            <td>${vessel.sampleInstanceCount}</td>
                            <td>${vessel.label}</td>
                            <td>${vessel.type.name}</td>
                            <td>
                                <c:forEach items="${vessel.labBatchesList}" var="batch">
                                    ${batch.batchName}<br/>
                                </c:forEach>
                            </td>
                            <td>${vessel.latestEvent.labEventType.name}</td>
                            <td>${vessel.latestEvent.eventLocation}</td>
                            <td>${actionBean.fullNameMap[vessel.latestEvent.eventOperator]}</td>
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
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
