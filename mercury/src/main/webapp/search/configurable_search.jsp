<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"%>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%-- This page allows the user to construct a user-defined search.  It is also used to display
 pre-constructed, read-only searches like Cancer Phenotype Search. --%>

<stripes:layout-render name="/layout.jsp" pageTitle="User-Defined Search" sectionTitle="User-Defined Search">
<stripes:layout-component name="extraHead">

    <script src="${ctxpath}/resources/scripts/search_utils.js" type="text/javascript"></script>
    <script src="${ctxpath}/resources/scripts/url-1.8.6.min.js" type="text/javascript"></script>

    <%-- Need to stomp some global layout settings:
         label html5 tag in search terms consumes entire horizontal layout area (Chrome only?), remove padding
         select element are padded in mercury.css resulting in bad vertical layout
     --%>
    <style>
        label {display:inline; margin-left: 5px;}
        input.displayTerm, input.termoperator, input.termvalue, input.rackScanData { margin: 3px; }
        <%-- Firefox select options allow this, Chrome quietly ignores --%>
       .help-option { background-image: url("${ctxpath}/images/help.png");
           background-repeat: no-repeat;
           background-position: right top; }
    </style>
</stripes:layout-component>
<stripes:layout-component name="content">
<h4>Search For ${actionBean.entityType.displayName}</h4>
<c:choose>
    <c:when test="${actionBean.readOnly}">
        <h1>${actionBean.selectedSearchName}</h1>
    </c:when>
    <c:otherwise>
        Choose a Search term, then click Add Term; choose a Column view set, then click Choose column set, or click
        Available Result Columns then click right arrow; click the Search button.
        Click the plus sign to expand the Saved Searches section and save your search,
        or load a saved search.
    </c:otherwise>
</c:choose>
Move the mouse over the question marks to see details about each section.
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"--%>
<stripes:form action="/search/ConfigurableSearch.action" onsubmit="return validateAndSubmit(this)"
              id="searchForm">
    <stripes:hidden name="readOnly" id="readOnly"/>
    <stripes:hidden name="entityName" id="entityName"/>
    <stripes:hidden name="minimal" id="minimal"/>
    <c:choose>
        <c:when test="${actionBean.readOnly}">
            <stripes:hidden name="selectedSearchName" id="selectedSearchName"/>
        </c:when>
        <c:otherwise>
            <fieldset>
                <legend>Saved Searches <img id="savedSearchesTooltip" src="${ctxpath}/images/help.png" alt="help">
                    <img id="savedSearchesPlus" src="${ctxpath}/images/plus.gif"
                         onclick="toggleVisible('savedSearchesDiv', this);" alt="expand / contract saved searches">
                </legend>
                <div style="display:none" id="savedSearchesDiv">
                    <p>
                        <label>Search Name:</label>
                        <stripes:select name="selectedSearchName">
                            <stripes:options-collection collection="${actionBean.searchInstanceNames.entrySet()}" label="key" value="value"/>
                        </stripes:select>
                        <stripes:submit name="fetchSearch" value="Load Search" onclick="this.wasClicked = true" class="btn btn-primary" />
                        <stripes:submit name="updateSearch" value="Update Search" class="btn btn-primary" />
                        <stripes:submit name="deleteSearch" value="Delete Search" onclick="this.wasClicked = true" class="btn btn-primary" />
                    </p>

                    <p>
                        <label>New Search Level:</label>
                        <stripes:select name="newSearchLevel" id="newSearchLevel">
                            <stripes:options-collection collection="${actionBean.newSearchLevels.entrySet()}" label="key" value="value" />
                        </stripes:select>
                        <label>New Search Name:</label>
                        <stripes:text name="newSearchName" id="newSearchName"/>
                        <stripes:submit name="saveNewSearch" value="Save New Search"
                                        onclick="return validateNewSearch();" class="btn btn-primary" />
                    </p>
                </div>
            </fieldset>
        </c:otherwise>
    </c:choose>

    <fieldset>
        <legend>Search Terms <img id="searchTooltip" src="${ctxpath}/images/help.png" alt="help"></legend>

        <c:if test="${not actionBean.readOnly}">
            <%-- Allow user to add top-level terms, and terms that are derived from
            constrained values (e.g. phenotype names) --%>
            <table>
                <tr style="vertical-align: top;">
                <td style="padding-right: 8px;text-align: right"><label>Search terms: </label></td>
                    <td style="padding-right: 6px"><stripes:select name="searchTermSelect" id="searchTermSelect" size="5">
                    <c:forEach items="${actionBean.configurableSearchDef.mapGroupSearchTerms}" var="entry">
                        <optgroup label="${entry.key}">
                            <c:forEach items="${entry.value}" var="searchTerm">
                                <option id="${searchTerm.uiId}_opt" value="${searchTerm.name}" ondblclick="addTerm()"
                                <%-- Firefox select options allow this style class, Chrome quietly ignores --%>
                                <c:if test="${not empty searchTerm.helpText}"> class="help-option"</c:if>>${searchTerm.name}</option>
                                <c:if test="${searchTerm.addDependentTermsToSearchTermList}">
                                    <c:forEach items="${searchTerm.constrainedValues}" var="constrainedValue">
                                        <option value="${constrainedValue.code}" ondblclick="addTerm()"
                                                searchTerm="${searchTerm.name}">${constrainedValue.label}</option>
                                    </c:forEach>
                                </c:if>
                            </c:forEach>
                        </optgroup>
                    </c:forEach>
                </stripes:select></td>
                    <td><stripes:button id="addTermBtn" name="addTermBtn" value="Add Term" class="btn btn-primary"/>
                <img id="addTermTooltip" src="${ctxpath}/images/help.png" alt="help"></td>
                </tr>
                <tr style="vertical-align: top">
                    <td style="padding-right: 8px;text-align: right;padding-top: 10px"> <label>Filter: </label></td>
                <td style="padding-top: 10px"><input type="text" id="filterSearchTerms" onkeyup="filterSelect($j('#searchTermSelect')[0], this);"></td>
            </table>

            <hr style="margin: 4px 0px"/>

        </c:if>


        <div id="searchInstanceDiv">
            <%-- Render existing search --%>
            <c:set var="searchValues" value="${actionBean.searchInstance.searchValues}" scope="request"/>
            <jsp:include page="recurse_search_terms.jsp"/>
        </div>
    </fieldset>
    <fieldset class="control-group">
        <legend>Result Columns <img id="resultColumnsTooltip" src="${ctxpath}/images/help.png" alt="help"></legend>
        <!-- Allow user to choose column sets -->
        <stripes:layout-render name="/columns/view_column_sets.jsp"/>
        <stripes:button id="chooseColumnSetBtn" name="chooseColumnSetBtn" value="Choose Column Set" onclick="chooseColumnSet();" class="btn btn-primary"/>
        <stripes:layout-render name="/search/view_columns.jsp"
                               availableMapGroupToColumnNames="${actionBean.availableMapGroupToColumnNames}"
                               predefinedViewColumns="${actionBean.searchInstance.predefinedViewColumns}"
                               viewColumnParamMap="${actionBean.searchInstance.viewColumnParamMap}"/>

    </fieldset>
    <div style="padding-left: 6px; margin-left: 2px; margin-top: 4px; border-bottom-width: 1px; margin-bottom: 25px;">
        <stripes:submit name="search" value="Search" class="btn btn-primary"/></div>

</stripes:form>
<!-- Show results -->
<fieldset>
    <legend>Results</legend>
    <stripes:layout-render name="/columns/configurable_list.jsp"
                           entityName="${actionBean.entityName}"
                           sessionKey="${actionBean.sessionKey}"
                           columnSetName="${actionBean.columnSetName}"
                           downloadColumnSets="${actionBean.downloadColumnSets}"
                           resultList="${actionBean.configurableSampleList}"
                           action="${ctxpath}/search/ConfigurableSearch.action"
                           downloadViewedColumns="True"
                           isDbSortAllowed="${actionBean.searchInstance.isDbSortable}"
                           dbSortPath="${actionBean.dbSortPath}"
                           dataTable="false"/>
</fieldset>
<script type="text/javascript">
/**
 * Called when the user wants to add, to the Available list, all columns in a set
 */
function chooseColumnSet() {
    var columnSetSelect = $j('#columnSetName')[0];
<c:forEach items="${actionBean.viewColumnSets}" var="columnSet">
    if (columnSetSelect.options[columnSetSelect.selectedIndex].text == '${columnSet.level} - ${columnSet.name}') {
    <c:forEach items="${columnSet.columns}" var="column">
        chooseColumn("${column}");
    </c:forEach>
    }
</c:forEach>
}
</script>
<div id="savedSearchesDescription" style="display: none;">
    <ul>
        <li>Search Name: choose a previously saved search (each name is prefixed with its visibility)</li>
        <li>Load Search: loads the chosen search</li>
        <li>Update Search: saves any changes you have made to a loaded search</li>
        <li>Delete Search: deletes the chosen search</li>
        <li>New search level: choose the visibility of a new search you want to save</li>
        <li>New search name: enter the name of a new search you want to save</li>
        <li>Save New Search: save a new search you have created</li>
    </ul>
</div>
<div id="addTermDescription" style="display: none;">
    <ul>
        <li>Pick a search term from the drop down list, then click Add Term. To narrow down the
            list of search terms, click in the Filter field and type part of the name of the term
            you're looking for.</li>
        <li>Click the red X next to an added term, to remove it from the search.</li>
    </ul>
</div>
<div id="searchDescription" style="display: none;">
    <ul>
        <li>You can change the type of the comparison for each term, then enter one or more values.
            If you leave a value blank, or don't change it from '(Choose one)', it won't be
            included in the search query.</li>
        <li>The checkbox next to each term determines whether the
            associated value is included in the results.</li>
        <li>Dates are mm/dd/yyyy format.</li>
        <li>For wildcard comparisons, use "_" to match any single character in this position,
            and "%" to match any number of characters in this position.</li>
    </ul>
</div>
<div id="resultColumnsDescription" style="display: none;">
    <ul>
        <li>Column view set: choose a pre-defined set of results columns (and / or select
            individual columns from Available)
        </li>
        <li>Choose column set: adds the selected Column view set to the Chosen list</li>
        <li>Available: select columns you want to display in the search results, then click Right arrow</li>
        <li>Filter: type here, to limit the Available list to columns that match what you typed</li>
        <li>Right arrow: moves selected Available columns to Chosen</li>
        <li>Left arrow: moves selected Chosen columns to Available</li>
        <li>Chosen: lists the columns you want to display in the search results</li>
        <li>Up arrow: moves selected Chosen columns higher in the order, the results are sorted by
            the first column (the top-to-bottom list is displayed left-to-right across the
            page of results)
        </li>
        <li>Down arrow: moves selected Chosen columns lower in the order</li>
    </ul>
</div>
<div id="traversalOptionDescription" style="display: none;">
    <c:if test="${actionBean.configurableSearchDef.traversalEvaluators != null}">
        <p>Available Options:</p>
        <ul>
        <c:forEach items="${actionBean.configurableSearchDef.traversalEvaluators}" var="traversalMapEntry">
            <li>${traversalMapEntry.value.helpNote}</li>
        </c:forEach>
        </ul>
    </c:if>
</div>
<%-- A div with a unique ID for search terms tool tips (search term has help text)  --%>
<c:forEach items="${actionBean.availableMapGroupToColumnNames}" var="entry">
    <c:forEach items="${entry.value}" var="searchTerm">
        <c:if test="${not empty searchTerm.helpText}">
<div id="${searchTerm.uiId}_dscr" style="display: none;">${searchTerm.helpText}</div>
        </c:if>
    </c:forEach>
</c:forEach>
<%-- A div with a unique ID for column group tool tips (column group can have help text)  --%>
<c:forEach items="${actionBean.availableMapGroupToHelpText.entrySet()}" var="entry">
    <div id="col_grp_${entry.key}_dscr" style="display: none;">${entry.value}</div>
</c:forEach>

<script type="text/javascript">
    $j(function(){
        // This is required in order to render HTML in title attributes.
        $j.widget("ui.tooltip", $j.ui.tooltip, {
            options: {
                content: function () {
                    return $j(this).prop('title');
                }
            }
        });

        <c:if test="${not actionBean.readOnly}">
            $j('#savedSearchesTooltip').attr('title', function(){
                return $j('#savedSearchesDescription').remove().html();
            });
            $j('#addTermTooltip').attr('title', function(){
                return $j('#addTermDescription').remove().html();
            });
        </c:if>
        $j('#searchTooltip').attr('title', function(){
            return $j('#searchDescription').remove().html();
        });
        $j('#resultColumnsTooltip').attr('title', function(){
            return $j('#resultColumnsDescription').remove().html();
        });
        $j('#traversalOptionTooltip').attr('title', function(){
            return $j('#traversalOptionDescription').remove().html();
        });

<%-- Initialize search terms tool tips for result columns (if search term has help text)  --%>
        <c:forEach items="${actionBean.availableMapGroupToColumnNames}" var="entry">
         <c:forEach items="${entry.value}" var="searchTerm">
          <c:if test="${not empty searchTerm.helpText}">
        $j('#${searchTerm.uiId}_col').attr('title', function(){
            // Don't remove, need to share...
            return $j('#${searchTerm.uiId}_dscr').html();
        });
        $j('#${searchTerm.uiId}_col').tooltip({
            position: { my: "left top",
                at: "right+10 top+35",
                of: "#sourceColumnDefNames" },
            show: false
        });
          </c:if>
         </c:forEach>
        </c:forEach>
<%-- Initialize search terms tool tips for search terms (if search term has help text)  --%>
        <c:forEach items="${actionBean.configurableSearchDef.mapGroupSearchTerms}" var="entry">
        <c:forEach items="${entry.value}" var="searchTerm">
        <c:if test="${not empty searchTerm.helpText}">
        $j('#${searchTerm.uiId}_opt').attr('title', function(){
            // Don't remove, need to share...
            return $j('#${searchTerm.uiId}_dscr').html();
        });
        <%-- Firefox tooltip works for 1 line select list, Chrome needs to have size > 1 or quietly ignores functionality --%>
        $j('#${searchTerm.uiId}_opt').tooltip({
            position: { my: "left top",
                at: "left bottom-10",
                of: "#addTermBtn" },
            show: false
        });
        </c:if>
        </c:forEach>
        </c:forEach>
<%-- Initialize tool tips for groups of search terms --%>
        <c:forEach items="${actionBean.availableMapGroupToHelpText.entrySet()}" var="entry">
        $j('#col_grp_${entry.key}').attr('title', function(){
            // Don't remove, need to share...
            return $j('#col_grp_${entry.key}_dscr').html();
        });
        $j('#col_grp_${entry.key}').tooltip({
            position: { my: "left top",
                at: "right+10 top+35",
                of: "#sourceColumnDefNames" },
            show: false
        });
        </c:forEach>

        $j(document).tooltip();

        // Bump width of list boxes out so background icon doesn't overlay longest text
        var selectList = $j('#sourceColumnDefNames');
        selectList.outerWidth( selectList.outerWidth() + 40 );
        selectList = $j('#searchTermSelect');
        selectList.outerWidth( selectList.outerWidth() + 40 );
    });
</script>

<%-- Adds the overlay elements for ajax rack scanner See: /vessel/ajax_div_rack_scanner.jsp --%>
<div id="rack_scan_overlay">
    <%@include file="/vessel/ajax_div_rack_scanner.jsp"%>
</div>
<%-- Adds the overlay elements for selecting result column params --%>
<div id="resultParamsOverlay" style="display: none">
    <div id="resultParamsError" style="color:red"></div>
    <form>
        <div id="resultParamsInputs"> </div>
        <p><input type="reset" value="Cancel" name="resultParamsCancelBtn" id="resultParamsCancelBtn" class="btn btn-primary"/>&nbsp;&nbsp;<input type="submit" value="Done" name="resultParamsDoneBtn" id="resultParamsDoneBtn" class="btn btn-primary"/></p>
    </form>
</div>
</stripes:layout-component>
</stripes:layout-render>