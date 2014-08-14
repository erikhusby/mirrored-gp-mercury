<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailActionBean" />

<stripes:layout-render name="/layout.jsp" pageTitle="Audit Trail Search" sectionTitle="Audit Trail Search" showCreate="false">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {

                $j('#auditTrailTable').dataTable({
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[4,'desc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "numeric"},        // rev id
                        {"bSortable": true, "sType": "date"},           // rev date
                        {"bSortable": true, "sType": "html"},           // username
                        {"bSortable": true, "sType": "html"}            // entities
                    ]
                })

            });
        </script>

        <style type="text/css">
            /* Fixed width columns except for "Entities" column. */
            #auditTrailTable { table-layout: fixed; }
            .columnRevId { width: 8em; }
            .columnRevDate { width: 10em; }
            .columnUser { width: 8em; }
        </style>
    </stripes:layout-component>


    <stripes:layout-component name="content">

        <stripes:label for="searchForm" class="form-label">
            Enter search criteria:
        </stripes:label>
        <stripes:form beanclass="${actionBean.class.name}" id="searchForm">
            <input type="hidden" name="_sourcePage" value="<%request.getServletPath();%>"/>
            <div class="search-horizontal">
                <div class="control-group">
                    <stripes:label for="dateRangeDiv" class="control-label">
                        Date range
                    </stripes:label>
                    <div class="controls">
                        <div id="dateRangeDiv"
                             rangeSelector="${actionBean.dateRange.rangeSelector}"
                             searchStartDate="${actionBean.dateRange.startTime}"
                             searchEndDate="${actionBean.dateRange.endTime}">
                        </div>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="search_username" class="control-label">
                        Username
                    </stripes:label>
                    <div class="controls">
                        <stripes:select id="search_username" name="searchUsername" class="search-input" style="width:250px">
                            <stripes:option label="Any User" value="${actionBean.anyUser}"/>
                            <stripes:options-collection collection="${actionBean.auditUsernames}"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="searchEntityDisplayName" class="control-label">
                        Type of Entity
                    </stripes:label>
                    <div class="controls">
                        <stripes:select id="searchEntityDisplayName" name="searchEntityDisplayName" class="search-input" style="width:250px">
                            <stripes:option label="Any Type" value="${actionBean.anyEntity}"/>
                            <stripes:options-collection collection="${actionBean.entityDisplayNames}"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <div class="control-label">&#160;</div>
                    <div class="controls actionButtons">
                        <stripes:submit name="listAuditTrails" value="Search" style="margin-right: 10px;margin-top:10px;" class="btn btn-mini"/>
                    </div>
                </div>

            </div>

        </stripes:form>

        <div class="clearfix"></div>
        <p>
        <table class="table simple" id="auditTrailTable">
            <thead>
            <tr>
                <th class="columnRevId">Rev Id</th>
                <th class="columnRevDate">Rev Date</th>
                <th class="columnUser">User</th>
                <th class="columnEntities">Entities</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.auditTrailList}" var="auditTrail">
                <tr>
                    <td class="columnRevId">
                            ${auditTrail.revId}
                    </td>
                    <td class="columnRevDate">
                        <fmt:formatDate value="${auditTrail.revDate}" pattern="${actionBean.preciseDateTimePattern}"/>
                    </td>
                    <td class="columnUser">
                            ${auditTrail.username}
                    </td>
                    <td class="columnEntities">
                        <c:forEach items="${auditTrail.entityTypeNames}" var="item">
                            <stripes:link href="${actionBean.auditTrailEntryActionBean}" event="viewAuditTrailEntries">
                                <stripes:param name="revId" value="${auditTrail.revId}"/>
                                <stripes:param name="displayClassname" value="${item}"/>
                                ${item}
                            </stripes:link>
                            &nbsp;
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        </p>

    </stripes:layout-component>
</stripes:layout-render>
