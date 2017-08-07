<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"%>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%-- This page allows the user to construct a user-defined search.  It is also used to display
 pre-constructed, read-only searches like Cancer Phenotype Search. --%>

<stripes:layout-render name="/layout.jsp" pageTitle="User-Defined Search" sectionTitle="User-Defined Search">
<stripes:layout-component name="extraHead">

    <script src="${ctxpath}/resources/scripts/search_utils.js" type="text/javascript"></script>
    <script src="${ctxpath}/resources/scripts/url-1.8.6.min.js" type="text/javascript"></script>
    <script type="text/javascript">
        /** Disable return key, to prevent inadvertently loading saved searches */
        function disableReturnKey(evt) {
            var evtLocal = (evt) ? evt : ((event) ? event : null);
            var node = (evtLocal.target) ? evtLocal.target : ((evtLocal.srcElement) ? evtLocal.srcElement : null);
            if (node.name == "barcode") {
                return true;
            }
            return !((evtLocal.keyCode == 13) && (node.type == "text"));
        }

        document.onkeypress = disableReturnKey;

        /** Expand and collapse the Saved Searches section */
        function toggleVisible(id, img) {
            var div = document.getElementById(id);
            if (div.style.display == 'block') {
                img.src = "${ctxpath}/images/plus.gif";
                div.style.display = "none";
            } else {
                img.src = "${ctxpath}/images/minus.gif";
                div.style.display = "block";
            }
        }

    </script>
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
                    <td><stripes:button id="addTermBtn" name="addTermBtn" value="Add Term" onclick="addTerm();" class="btn btn-primary"/>
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
function validateNewSearch() {
    if (document.getElementById('newSearchName').value.strip() == '') {
        alert("You must enter a name for the new search");
        return false;
    }
    var newSearchLevelSelect = document.getElementById('newSearchLevel');
    var newSearchLevel = newSearchLevelSelect.options[newSearchLevelSelect.selectedIndex].value;
    if (newSearchLevel != 'USER') {
        return confirm("Are you sure you want to save this search at " + newSearchLevel +
                " level?  (If you want this search to be visible to you only, click " +
                "Cancel and change the level to USER)");
    }
    return true;
}

/*
 Several places need entity name
 */
function getEntityName() {
    return $j('#searchForm :input[name=entityName]' ).val();
}


/*
 Add a top level term by making an AJAX request.
 */
function addTerm() {
    var select = $j('#searchTermSelect')[0];
    var option = select.options[select.selectedIndex];
    var searchTerm = option.getAttribute('searchTerm');
    var searchTermName = option.value;
    var parameters;
    if (searchTerm == null) {
        parameters = 'addTopLevelTerm&searchTermName=' + searchTermName + '&entityName=' + getEntityName();
    } else {
        parameters = 'addTopLevelTermWithValue&searchTermName=' + searchTerm + '&searchTermFirstValue=' + searchTermName
                + '&entityName=' + getEntityName();
    }
    new $j.ajax({
        url: '${ctxpath}/search/ConfigurableSearch.action',
        type: 'get',
        dataType: 'html',
        data: parameters,
        success: function (returnData) {
            $("#searchInstanceDiv").append(returnData);
        }
    });
}

/*
 Add child search terms to a non-leaf node, by making an AJAX request.
 To get the correct child terms, we have to send to the server all the ancestor
 terms, using a Stripes URL like searchInstance.searchValues[0].children[0].termName=x&
 searchInstance.searchValues[0].children[0].values[0]=y
 Each parent in the URL has only one child, so there is no ambiguity in the path
 from ultimate ancestor to ultimate descendant.
 */
function nextTerm(link) {
    var startDiv = link.parentNode;
    // Find out how deeply nested the divs are, so we can build the Stripes parameter correctly
    var div = startDiv;
    var termDepth = 0;
    // for each ancestor div
    while (div.className == 'searchterm') {
        termDepth++;
        div = div.parentNode;
    }
    div = startDiv;
    var parameters;
    var termValue;
    var currentLevel = 0;
    // for each ancestor div
    while (div.className == 'searchterm') {
        // get value of hidden field (term name)
        var termNameHidden = document.getElementById(div.id + '_name');
        currentLevel++;
        // concat into Stripes indexed property name
        var parameterFragment = 'searchInstance.searchValues[0].';
        for (var levelIndex = currentLevel; levelIndex < termDepth; levelIndex++) {
            parameterFragment += 'children[0].';
        }
        parameterFragment += 'termName=' + termNameHidden.getAttribute('value') + '&';
        // get value of ddlb or text box (term value)
        termValue = document.getElementById(div.id + '_value');
        var value;
        if (termValue.tagName == 'SELECT') {
            value = termValue.options[termValue.selectedIndex].value;
            if (value == '${actionBean.searchInstance.chooseValue}') {
                alert('You must choose a value');
                return;
            }
        } else {
            value = termValue.getAttribute('value');
        }
        // concat into Stripes indexed property name
        parameterFragment += 'searchInstance.searchValues[0].';
        for (levelIndex = currentLevel; levelIndex < termDepth; levelIndex++) {
            parameterFragment += 'children[0].';
        }
        parameterFragment += 'values[0]=' + value + '&';
        parameters = parameterFragment + parameters;
        div = div.parentNode;
    }
    parameters = 'addChildTerm&readOnly=' + $j.url('?readOnly') + '&entityName=' + getEntityName() + '&' + parameters;
    // AJAX append to current div
    new $j.ajax({
        url: '${ctxpath}/search/ConfigurableSearch.action',
        type: 'get',
        dataType: 'html',
        data: parameters,
        success: function (returnData) {
            $("#" + startDiv.id).append(returnData);
        }
    });
    // Remove the "Add sub-term" link, we don't want duplicate children
    link.parentNode.removeChild(link);
}


/*
 Called when the user changes the operator, this function replaces form field(s),
 depending on the new operator
 IN operator : change a select to multiple, or create a textarea.
 BETWEEN operator : create a pair of input / texts with "and" between them
 otherwise : create a single input / text
 */
function changeOperator(operatorSelect) {

    var operator = operatorSelect.options[operatorSelect.selectedIndex].value;
    var valueElement = operatorSelect;
    var textInput1;
    var textInput2;
    var andText;
    var textarea;
    var rackScanButton;
    var rackScanData;
    // Find the value element (could be text, textarea or select)
    while (valueElement != null) {
        if (valueElement.nodeType == 1) {
            // indexOf because jQuery date picker adds hasDatepicker to class
            if (valueElement.className.indexOf("termvalue") >= 0) {
                break;
            }
        }
        valueElement = valueElement.nextSibling;
    }

    // Remove rack scan button if one exists
    rackScanButton = valueElement.nextSibling;
    while( rackScanButton != null ) {
        if( rackScanButton.nodeName == "INPUT" && rackScanButton.getAttribute("name") == "rackScanBtn" ) {
            // Remove scan JSON hidden field if exists
            var hiddenJSON = rackScanButton.nextSibling;
            if( hiddenJSON != null && hiddenJSON.nodeName == "INPUT" && hiddenJSON.getAttribute("name") == "rackScanData" ) {
                valueElement.parentNode.removeChild(hiddenJSON);
            }
            valueElement.parentNode.removeChild(rackScanButton);
            rackScanButton = null;
            break;
        }
        rackScanButton = rackScanButton.nextSibling;
    }

    // indexOf because jQuery date picker adds hasDatepicker to class
    if (valueElement.className.indexOf("termvalue") >= 0) {

        if (valueElement.tagName == "SELECT") {
            if (operator == "IN" || operator == "NOT_IN") {

                // Change single select to multiple, remove "(Choose one)"
                valueElement.setAttribute("multiple", true);
                valueElement.setAttribute("size", 10);
                if (valueElement.options[0].value == '${actionBean.searchInstance.chooseValue}' ||
                        valueElement.options[0].text == '${actionBean.searchInstance.chooseValue}') {
                    valueElement.remove(0);
                }
            }
            else {

                // Change multiple select to single, add "(Choose one)"
                valueElement.removeAttribute("multiple");
                valueElement.removeAttribute("size");
                if (valueElement.options[0].value != '${actionBean.searchInstance.chooseValue}') {
                    var newOption = document.createElement('option');
                    newOption.text = '${actionBean.searchInstance.chooseValue}';
                    try {
                        valueElement.add(newOption, valueElement.options[0]);
                    } catch (ex) {
                        // IE7 method has different signature
                        valueElement.add(newOption, 0);
                    }
                }
            }
        }
        else {

            var valueElementId = valueElement.id;
            var dataType = valueElement.getAttribute("dataType");
            if (operator == "IN" || operator == "NOT_IN") {

                // create a textarea to allow the user to paste multiple lines
                textarea = document.createElement("TEXTAREA");
                textarea.className = "termvalue";
                textarea.id = valueElementId;
                textarea.setAttribute("rows", 4);
                textarea.setAttribute("dataType", valueElement.getAttribute("dataType"));
                // If we're replacing a pair of text boxes for the "between" operator
                if (valueElement.getAttribute("between1") != null) {
                    andText = valueElement.nextSibling;
                    andText.parentNode.removeChild(andText);
                    textInput2 = valueElement.nextSibling;
                    textInput2.parentNode.removeChild(textInput2);
                }
                // Create a rack scan button if input is configured to support one
                if( valueElement.parentNode.getAttribute("rackScanSupported")){
                    rackScanButton = document.createElement("INPUT");
                    rackScanButton.setAttribute("id", "rackScanBtn");
                    rackScanButton.setAttribute("name", "rackScanBtn");
                    rackScanButton.setAttribute("value", "Rack Scan");
                    rackScanButton.setAttribute("class", "btn btn-primary");
                    rackScanButton.setAttribute("onclick", "startRackScan(this);");
                    rackScanButton.setAttribute("type", "button");
                    valueElement.parentNode.appendChild(rackScanButton);
                    rackScanData = document.createElement("INPUT");
                    rackScanData.setAttribute("id", "rackScanData_" + valueElementId );
                    rackScanData.setAttribute("name", "rackScanData");
                    rackScanData.setAttribute("value", "");
                    rackScanData.setAttribute("class", "rackScanData");
                    rackScanData.setAttribute("type", "hidden");
                    valueElement.parentNode.appendChild(rackScanData);
                }
                valueElement.parentNode.replaceChild(textarea, valueElement);
            } else if (operator == "BETWEEN") {
                // create two inputs with text "and" between them
                textInput1 = document.createElement("INPUT");
                textInput1.id = valueElementId + "1";
                textInput1.setAttribute("type", "text");
                textInput1.setAttribute("between1", "true");
                textInput1.className = "termvalue";
                textInput1.setAttribute("dataType", dataType);

                andText = document.createTextNode("and");

                textInput2 = document.createElement("INPUT");
                textInput2.id = valueElementId + "2";
                textInput2.setAttribute("type", "text");
                textInput2.setAttribute("between2", "true");
                textInput2.className = "termvalue";
                textInput2.setAttribute("dataType", dataType);
                valueElement.parentNode.insertBefore(textInput1, valueElement);
                valueElement.parentNode.insertBefore(andText, valueElement);
                valueElement.parentNode.replaceChild(textInput2, valueElement);

                if (dataType == 'DATE' || dataType == 'DATE_TIME') {
                    $j("#" + valueElementId + "1").datepicker();
                    $j("#" + valueElementId + "2").datepicker();
                }
            } else {
                // create a text box for a single value
                textInput1 = document.createElement("INPUT");
                textInput1.id = valueElementId;
                textInput1.setAttribute("type", "text");
                textInput1.className = "termvalue";
                textInput1.setAttribute("dataType", dataType);
                // If we're replacing a pair of text boxes for the "between" operator
                if (valueElement.getAttribute("between1") != null) {
                    andText = valueElement.nextSibling;
                    andText.parentNode.removeChild(andText);
                    textInput2 = valueElement.nextSibling;
                    textInput2.parentNode.removeChild(textInput2);
                }
                valueElement.parentNode.replaceChild(textInput1, valueElement);

                if (dataType == 'DATE' || dataType == 'DATE_TIME' ) {
                    $j("#" + valueElementId).datepicker();
                }
            }
        }
    }
}

/*
 Called when the user clicks a Remove icon, this function deletes the associated
 search term.
 */
function removeTerm(link) {
    var searchTerm = link.parentNode;
    var br = document.getElementById(searchTerm.id + '_br');
    if (br != null) {
        searchTerm.parentNode.removeChild(br);
    }

    if( searchTerm.parentNode.nodeName.toLowerCase() == 'div' ) {
        if( searchTerm.parentNode.classList.contains('searchterm')) {
            // Restore the "Add sub-term" link
            var button = document.createElement("input");
            button.setAttribute("type","button");
            button.setAttribute("id","addSubTermBtn");
            button.setAttribute("class", "btn btn-primary");
            button.setAttribute("value","Add Sub-Term");
            button.onclick = function () {
                nextTerm(this);
                return false;
            };
            searchTerm.parentNode.appendChild(button);
        }
    }

    searchTerm.parentNode.removeChild(searchTerm);

}

<%-- Ajax rack scanner implementation: See /vessel/ajax_div_rack_scanner.jsp for dependent functionality --%>
var rackScanSrcBtn = null;
function rackScanComplete() {
    var barcodes = $j("#rack_scan_overlay").data("results");
    //alert(barcodes);
    var textarea;
    if( barcodes != null && rackScanSrcBtn != null ) {
        // Store rack scan raw JSON in hidden form field
        var rackScanDataElement = rackScanSrcBtn.nextSibling;
        rackScanDataElement.setAttribute("value", barcodes);

        // Extract barcodes from JSON and enter in text field
        var scanJSON = $j.parseJSON(barcodes);
        textarea = rackScanSrcBtn.previousSibling;
        while( textarea != null ) {
            if( textarea.className == "termvalue" ) {
                var multiText = "";
                $j.each( scanJSON.scans, function(index){
                    multiText = multiText + this.barcode + "\n";
                });
                textarea.textContent = multiText;
                break;
            }
            textarea = textarea.previousSibling;
        }
    }
    $j("#rack_scan_overlay").dialog("close");
    $j("#rack_scan_overlay").removeData("results");
    rackScanSrcBtn = null;
    $j("#rack_scan_inputs").html("");
}

/**
 * Called when the user changes a dependee search value, invalidating
 * its children (e.g. if the user changes a trait name, the list of trait values
 * is no longer valid, and must be removed).
 * @param select dependee select that changed
 */
function changeDependee(select) {
    // if there are child search terms, remove them
    var searchTerm = select.parentNode;
    var childNodes = searchTerm.childNodes;
    var foundChildren = false;
    for (var i = 0; i < childNodes.length; i++) {
        var node = childNodes[i];
        if (node.nodeType == 1) {
            if (node.className == "searchterm") {
                node.parentNode.removeChild(node);
                foundChildren = true;
            }
        }
    }
    if (foundChildren) {
        // Restore the "Add sub-term" link
        var button = document.createElement("input");
        button.setAttribute("type","button");
        button.setAttribute("id","addSubTermBtn");
        button.setAttribute("class", "btn btn-primary");
        button.setAttribute("value","Add Sub-Term");
        button.onclick = function () {
            nextTerm(this);
            return false;
        };
        select.parentNode.appendChild(button);
    }
}

/**
 * Adds a column to the Chosen list, and hides the column in the Available list
 * @param columnName
 */
function chooseColumn(columnName) {
    var availableSelect = $j('#sourceColumnDefNames')[0];
    var chosenSelect = $j('#selectedColumnDefNames')[0];
    var found = false;

    // Is the column already chosen?
    for (var i = 0; i < chosenSelect.options.length; i++) {
        if (chosenSelect[i].text == columnName) {
            found = true;
            break;
        }
    }
    // Add the column
    if (!found) {
        var newOption = document.createElement('option');
        newOption.text = columnName;
        chosenSelect.options[chosenSelect.options.length] = newOption;
    }
    // Hide the column in the Available select
    for (i = availableSelect.options.length - 1; i >= 0; i--) {
        if (availableSelect.options[i].text == columnName) {
            availableSelect.options[i].style.display = 'none';
        }
    }

}

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