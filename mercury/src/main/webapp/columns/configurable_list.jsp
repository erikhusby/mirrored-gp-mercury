<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.search.PaginationDao" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean" %>

<%-- This page displays a configurable list for a given entity --%>

<stripes:layout-definition>
<%-- headers and result rows --%>
<%--@elvariable id="resultList" type="org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList.ResultList"--%>

<%-- key for object that holds pagination state --%>
<%--@elvariable id="sessionKey" type="java.lang.String"--%>

<%-- column set used for display, needed for re-sorting and paging --%>
<%--@elvariable id="columnSetName" type="java.lang.String"--%>

<%-- list of column sets in which data can be downloaded to Excel --%>
<%--@elvariable id="downloadColumnSets" type="List<ConfigurableListUtils.ColumnSet>"--%>

<%-- The action bean to call for re-sorting and paging --%>
<%--@elvariable id="action" type="java.lang.String"--%>

<%-- True if there can be additional columns, beyond those in a column set --%>
<%--@elvariable id="downloadViewedColumns" type="java.lang.Boolean"--%>

<%-- The name of the entity being listed --%>
<%--@elvariable id="entityName" type="java.lang.Boolean"--%>

<%-- If pagination is AJAX based, the div to be updated --%>
<%--@elvariable id="ajaxDiv" type="java.lang.Boolean"--%>
<script type="text/javascript">
    atLeastOneChecked = function (name, form) {
        var checkboxes = form.getInputs("checkbox", name);
        for (var i = 0; i < checkboxes.length; ++i) {
            if (checkboxes[i].checked) {
                return true;
            }
        }
        alert('You must check at least one box');
        return false;
    };

    // When bottom or top checkAllButton checked, opposite button should also be set to same state
    checkOtherAllBox = function (otherID, cbState ) {
        $j(otherID)[0].checked = cbState;
        return true;
    };

    function getWorkRequestSearchFormData() {

        assignSearchNames(document.getElementById('searchForm'));
        $j("#searchResultsForm${entityName} :input[isacopy]").remove();  // This is in case they hit submit twice after a validation problem.

        // Have to handle the multi-selects separately.
        var theSelect = $j("#selectedColumnDefNames").clone();
        theSelect.attr("id", "selectedColumnDefNamesClone");
        theSelect.hide().attr("isacopy", "y").appendTo("#searchResultsForm${entityName}");

        var htmlSelectVar = document.getElementById("selectedColumnDefNamesClone");
        for (var j = 0; j < htmlSelectVar.options.length; j++) {
            htmlSelectVar.options[j].selected = true;
        }

        // Add all the searchInstance form fields.
        $j("#searchForm :input[name^='searchInstance']").clone().hide().attr("isacopy", "y").appendTo("#searchResultsForm${entityName}");
    }
</script>

<%
    // Get the pagination object from the session and make it available to EL
    PaginationDao.Pagination pagination = (PaginationDao.Pagination) request.getSession().getAttribute(
            ConfigurableSearchActionBean.PAGINATION_PREFIX + pageContext.getAttribute("sessionKey"));
    pageContext.setAttribute("pagination", pagination);
%>
<c:choose>
<%-- If the column is Count Only (e.g. Public Search), we don't need a table --%>
<c:when test="${resultList.headers[0].viewHeader == 'Count Only'}">
    Count: ${resultList.resultRows[0].sortableCells[0]}
</c:when>
<c:otherwise>
<c:if test="${fn:length(resultList.resultRows) > 0 && empty ajaxDiv}">
    <a href="#search${entityName}ListEnd">Jump to end</a>
</c:if>
<c:if test="${not empty ajaxDiv}">
    <%-- For Quick Find, update the summary section --%>
    <script type="text/javascript">
        $('${entityName}SummaryDiv').innerHTML = "${fn:length(pagination.idList)}";
    </script>
</c:if>
<stripes:form action="/util/ConfigurableList.action" id="searchResultsForm${entityName}">
<input type="hidden" name="sessionKey" value="${sessionKey}"/>
<input type="hidden" name="entityName" value="${entityName}"/>

<div id="${entityName}ResultsDiv" class="form-inline"
<c:if test="${not empty ajaxDiv}">
    style="height:200px;overflow:auto;"
</c:if>
>
<table width="100%" class="table simple dataTable" id="${entityName}ResultsTable">
    <c:forEach items="${resultList.resultRows}" var="resultRow" varStatus="status">
        <%--@elvariable id="resultRow" type="edu.mit.broad.bsp.core.util.columns.ConfigurableListUtils.ResultRow"--%>
        <%-- Break up the table every few hundred rows, because the browser uses unreasonable amounts of
        CPU to render large tables --%>
        <c:if test="${status.index % 500 == 0}">
            <thead>
            <tr>
                <th><input for="count" type="checkbox" class="checkAll" id="checkAllTop" onclick="checkOtherAllBox('#checkAllBottom', this.checked );"><span id="count" class="checkedCount">0</span></th>
                <c:forEach items="${resultList.headers}" var="resultListHeader" varStatus="status">
                    <%-- Render headers, and links for sorting by column --%>
                    <c:choose>
                        <c:when test="${not empty pagination and pagination.numberPages == 1}">
                            <%-- Results with only one page are sortable in-memory --%>
                            <th class="sortable ${resultList.resultSortColumnIndex == status.index ?
                            (resultList.resultSortDirection == 'ASC' ? 'sorted order2' : 'sorted order1' ) : ''}"
                                    >
                                <stripes:link
                                        href="${action}?search=&entityName=${entityName}&sessionKey=${sessionKey}&columnSetName=${columnSetName}&sortColumnIndex=${status.index}&sortDirection=${resultList.resultSortColumnIndex == status.index && resultList.resultSortDirection == 'ASC' ? 'DSC' : 'ASC'}">
                                    ${resultListHeader.viewHeader}</stripes:link>
                            </th>
                        </c:when>
                        <c:otherwise>
                            <c:choose>
                                <c:when test="${not empty resultListHeader.sortPath}">
                                    <%-- Multi-page results are sortable in the database, if the column has a sort path --%>
                                    <th class="sortable ${resultListHeader.sortPath == param.dbSortPath ?
                                    (resultList.resultSortDirection == 'ASC' ? 'sorted order2' : 'sorted order1' ) : ''}"
                                            >
                                        <stripes:link
                                                href="${action}?search=&entityName=${entityName}&sessionKey=${sessionKey}&columnSetName=${columnSetName}&dbSortPath=${resultListHeader.sortPath}&sortDirection=${resultListHeader.sortPath == param.dbSortPath && resultList.resultSortDirection == 'ASC' ? 'DSC' : 'ASC'}">
                                            ${resultListHeader.viewHeader}</stripes:link>
                                    </th>
                                </c:when>
                                <c:otherwise>
                                    <%-- No dbSortPath, therefore no sort link --%>
                                    <th class="sortable">
                                        ${resultListHeader.viewHeader}
                                    </th>
                                </c:otherwise>
                            </c:choose>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </tr>
            </thead>
            <tbody>
        </c:if>
        <%-- Render data rows and cells --%>
        <tr ${status.index%2==0 ? "class=\"even\"" : "class=\"odd\""}>
            <td>
                <input name="selectedIds" value="${resultRow.resultId}" class="shiftCheckbox" type="checkbox">
            </td>
            <c:forEach items="${resultRow.renderableCells}" var="cell">
                <td>${cell}</td>
            </c:forEach>
        </tr>
        <c:if test="${(status.index + 1)%500 == 0}">
            </tbody>
</table>
<table width="100%">
    </c:if>
    </c:forEach>
</table>
</div>
<table class="table">
    <tr>
        <c:if test="${fn:length(resultList.resultRows) > 0}">
            <td valign="middle"><input for="count" type="checkbox" class="checkAll"  style="margin-left: -5px" id="checkAllBottom" onclick="checkOtherAllBox('#checkAllTop', this.checked );"></td>
        </c:if>

        <td colspan="100" nowrap>
            <%-- Links to move between database pages --%>
            <c:choose>
                <c:when test="${not empty pagination and pagination.numberPages > 1}">
                    Page:
                    <c:forEach begin="0" end="${pagination.numberPages - 1}" var="pageNumber">
                        <c:choose>
                            <%-- If there's no pageNumber parameter, we must be on the first page, so don't show a link to that page  --%>
                            <c:when test="${empty param.pageNumber && pageNumber == 0}">
                                <strong>${pageNumber + 1}</strong>
                            </c:when>
                            <%-- Don't show a link to the page we're currently on --%>
                            <c:when test="${not empty param.pageNumber && param.pageNumber == pageNumber}">
                                <strong>${pageNumber + 1}</strong>
                            </c:when>
                            <%-- Links to other pages --%>
                            <c:otherwise>
                                <c:choose>
                                    <c:when test="${empty ajaxDiv}">
                                        <a href="${action}?searchPage=&entityName=${entityName}&pageNumber=${pageNumber}&sessionKey=${sessionKey}&columnSetName=${columnSetName}&dbSortPath=${param.dbSortPath}">${pageNumber + 1}</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="#"
                                           onclick="new Ajax.Updater('${ajaxDiv}', '${action}?searchPage=&entityName=${entityName}&pageNumber=${pageNumber}&sessionKey=${sessionKey}&columnSetName=${columnSetName}&dbSortPath=${param.dbSortPath}&entityName=${entityName}');return false;">${pageNumber + 1}</a>
                                    </c:otherwise>
                                </c:choose>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                    of <span id="${entityName}Count">${fn:length(pagination.idList)}</span> ${entityName}s
                    <br/>
                </c:when>
                <c:otherwise>
                    ${entityName} count: <span id="${entityName}Count">${fn:length(resultList.resultRows)}</span>
                </c:otherwise>
            </c:choose>
        </td>
    </tr>
    <tr>
        <td>&nbsp;</td>
        <td style="vertical-align: middle">
            <a name="search${entityName}ListEnd"></a>
            <c:if test="${fn:length(resultList.resultRows) > 0}">
                <%-- Controls to allow user to specify column set for downloading to Excel--%>
                Column download set:
                <select name="downloadColumnSetName">
                    <c:if test="${downloadViewedColumns}">
                        <option>Viewed Columns</option>
                    </c:if>
                    <c:forEach items="${downloadColumnSets}" var="columnSet">
                        <c:set var="optionValue" value="${columnSet.level}|${columnSet.name}"/>
                        <option value="${optionValue}">
                                ${columnSet.level} - ${columnSet.name}
                        </option>
                    </c:forEach>
                </select>

                <%-- Download checked --%>
                <%-- Set name on click, because the same submit is used by addCheckedToBasket AJAX --%>
                <input type="submit" name="downloadFromIdList" id="downloadFromIdList"
                       value="Download Checked"
                       onclick="if(atLeastOneChecked('selectedIds', this.form)){ this.name='downloadFromIdList'; return true;} else {return false;}"/>
                <c:if test="${not empty pagination and pagination.numberPages > 1}">
                    <input type="submit" name="downloadAllPages" id="downloadAllPages"
                           value="Download All Pages"/>
                </c:if>

                <c:if test="${entityName == 'Sample'}">
                    <%-- Add checked to basket--%>
                    <script type="text/javascript">
                        addCheckedToBasket = function (form) {
                            if (atLeastOneChecked('selectedIds', form)) {
                                $("downloadFromIdList").name = "addCheckedToBasket";
                                var pars = $("searchResultsForm${entityName}").serialize(true);
                                var url = '${ctxpath}/util/ConfigurableList.action';

                                new Ajax.Updater(
                                        'sampleBasketCount',
                                        url,
                                        {
                                            method: 'post',
                                            parameters: pars
                                        });
                            }
                        };
                    </script>
                    <a href="#" onclick="addCheckedToBasket($('searchResultsForm${entityName}'));">(Add checked to
                        Basket)</a>
                </c:if>
                <%-- Tooltip help --%>
                <img id="${entityName}checkboxesTooltip" src="${ctxpath}/images/help.png" alt="help" title="
                    You can check individual boxes next to each row, or check the box at the top or
                    bottom of the list to check all rows.
                    <ul>
                        <li>Column download set: Choose the set of columns you want to download to Excel</li>
                        <li>Download Checked: Download checked rows to Excel</li>
                        <li>Plating Request For Checked: Take the checked samples to the Plating Request Filter
                            page
                        </li>
                        <li>Add checked to Basket: Add the checked samples to the sample basket (check the
                            contents of your basket first, by clicking the basket icon in the top right
                            corner of the page)
                        </li>
                    </ul>
                </div>">

            </c:if>
        </td>
    </tr>
</table>
</stripes:form>
</c:otherwise>
</c:choose>

</stripes:layout-definition>