/** ************* Initialization ***** */
$j( document ).ready( function() {

    $(document).keypress(disableReturnKey);

    syncChosenAvailable();
    initResultParamOverlay();

    var selectedOption = $j("#customTraversalOptionConfig > option:selected");
    var editElement = $j("#customTraversalOptionEdit");
    if( eval( selectedOption.data("hasUserCustomization") ) ) {
        editElement.css("background-position", "-64px -112px");
    }

    // Attach an onchange listener to custom traversal
    $j("#customTraversalOptionConfig").change(showCustomTraverserOptions);
    $j("#customTraversalOptionEdit").click(showCustomTraverserOptions);
    $j("#addTermBtn").click(addTerm);

} );

/** Disable return key, to prevent inadvertently loading saved searches */
function disableReturnKey(evt) {
    var evtLocal = (evt) ? evt : ((event) ? event : null);
    var node = (evtLocal.target) ? evtLocal.target : ((evtLocal.srcElement) ? evtLocal.srcElement : null);
    if (node.name == "barcode") {
        return true;
    }
    return !((evtLocal.keyCode == 13) && (node.type == "text"));
}

/** Expand and collapse the Saved Searches section */
function toggleVisible(id, img) {
    var div = document.getElementById(id);
    if (div.style.display == 'block') {
        img.src = "/Mercury/images/plus.gif";
        div.style.display = "none";
    } else {
        img.src = "/Mercury/images/minus.gif";
        div.style.display = "block";
    }
}

function validateNewSearch() {
    if (document.getElementById('newSearchName').value.trim().length == 0) {
        alert("You must enter a name for the new search");
        return false;
    }
    var newSearchLevelSelect = document.getElementById('newSearchLevel');
    var newSearchLevel = newSearchLevelSelect.options[newSearchLevelSelect.selectedIndex].innerHTML;
    if (newSearchLevel !== 'USER') {
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
        url: '/Mercury/search/ConfigurableSearch.action',
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
        url: '/Mercury/search/ConfigurableSearch.action',
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

/**
 * Add column to chosen list, and make it invisible in available list (so it can't
 * be chosen again)
 * @param available multi-select of available columns
 * @param chosen multi-select of chosen columns
 */
chooseColumns = function (available, chosen) {
    var selected = $j( available ).find( ":selected");
    for(var i = 0; i < selected.length; i++) {
        var option = $j( selected[i] );
        if (option.css("display") == "" || option.css("display") == 'block') {
            if (option.data("hasParams") ) {
                showColumnOptions(option, false);
                // Do NOT hide this column
                return;
            } else {
                var newOption = document.createElement('option');
                newOption.text = option.text();
                newOption.value = option.val();
                chosen.options[chosen.options.length] = newOption;
            }
        }
    }
    for (i = available.options.length - 1; i >= 0; i--) {
        if (available.options[i].selected) {
            if (!option.val().endsWith(":")) {
                available.options[i].style.display = 'none';
            }
        }
    }
};

/**
 * Show child options for result columns to select from to add column to chosen list
 * @param available multi-select of available columns
 * @param chosen multi-select of chosen columns
 */
showColumnOptions = function (option, isEdit) {
    if (!option.data("hasParams") ) {
        return;
    }
    var overlayDiv = $j( "#resultParamsOverlay" );
    overlayDiv.dialog("option","paramType", "SEARCH_TERM");
    overlayDiv.dialog("option","entityName", $j("#entityName").val());
    overlayDiv.dialog("option","elementName", option.data("elementName"));
    overlayDiv.dialog("option","resultParams", option.val());
    overlayDiv.dialog("open");
    if( isEdit ) {
        // Hold a reference to option being edited
        overlayDiv.dialog("option","srchTermEditSrc", option);
    }
};

editColumnParams = function (evt) {
    var option = $j(evt.delegateTarget);
    if (!option.data("hasParams") ) {
        return;
    }
    showColumnOptions(option, true);
};

toggleParamListShowAll = function( srcSpan, targetId ) {
    var targetJq = $j("#" + targetId );
    srcSpan = $j(srcSpan);
    if( srcSpan.data("showAll") ) {
        srcSpan.data("showAll", false );
        srcSpan.css("background-position", "-32px -128px");
        targetJq.find(":not(:selected)").css("display","none");
    } else {
        srcSpan.data("showAll", true );
        srcSpan.css("background-position", "-64px -128px");
        targetJq.find("option").css("display","block");
    }
};

/**
 * Remove column from chosen, and make it visible in available
 * @param chosen multi-select of chosen columns
 * @param available multi-select of available columns
 */
removeColumns = function (chosen, available) {
    for (var i = 0; i < chosen.options.length; i++) {
        var option = chosen.options[i];
        if (option.selected) {
            for (var j = 0; j < available.options.length; j++) {
                if (available.options[j].text == option.text) {
                    available.options[j].style.display = 'block';
                }
            }
        }
    }
    for (i = chosen.options.length - 1; i >= 0; i--) {
        if (chosen.options[i].selected) {
            chosen.remove(i);
        }
    }
};

/*
 Change order of items in select
 */
moveOptionsUp = function (selectList) {
    var selectOptions = selectList.getElementsByTagName('option');
    for (var i = 1; i < selectOptions.length; i++) {
        var opt = selectOptions[i];
        if (opt.selected) {
            selectList.removeChild(opt);
            selectList.insertBefore(opt, selectOptions[i - 1]);
        }
    }
};

/*
 Change order of items in select
 */
moveOptionsDown = function (selectList) {
    var selectOptions = selectList.getElementsByTagName('option');
    for (var i = selectOptions.length - 2; i >= 0; i--) {
        var opt = selectOptions[i];
        if (opt.selected) {
            var nextOpt = selectOptions[i + 1];
            opt = selectList.removeChild(opt);
            nextOpt = selectList.replaceChild(opt, nextOpt);
            selectList.insertBefore(nextOpt, opt);
        }
    }
};

/**
 * Remove options in a select, that don't match what the user typed in a text box
 * @param select from which to remove options
 * @param text box in which user typed filtering characters
 */
filterSelect = function (select, text) {
    var firstOption = true;
    for (var i = 0; i < select.options.length; i++) {
        if (select.options[i].text.toLowerCase().indexOf(text.value.toLowerCase()) >= 0) {
            select.options[i].style.display = 'block';
            if (firstOption) {
                select.selectedIndex = i;
            }
            firstOption = false;
        } else {
            select.options[i].style.display = 'none';
        }
    }
};

syncChosenAvailable = function () {
    var available = $j('#sourceColumnDefNames')[0];
    var chosen = $j('#selectedColumnDefNames')[0];
    for (var i = 0; i < chosen.options.length; i++) {
        var option = chosen.options[i];
        for (var j = 0; j < available.options.length; j++) {
            if (available.options[j].text == option.text) {
                available.options[j].style.display = 'none';
            }
        }
    }

    if(chosen.options.length == 0){
        for (var i = 0; i < searchDefaultColumns.length; i++) {
            for (var j = 0; j < available.options.length; j++) {
                if (available.options[j].text == searchDefaultColumns[i]) {
                    var newOption = document.createElement('option');
                    newOption.text = searchDefaultColumns[i];
                    newOption.value = searchDefaultColumns[i];
                    chosen.options[chosen.options.length] = newOption;
                    available.options[j].style.display = 'none';
                }
            }
        }
    }
};

/* Ajax rack scanner implementation: See /vessel/ajax_div_rack_scanner.jsp for dependent functionality
 * TODO JMS Ugh! - Global variable? Do something, document or scan div data attribute? */
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
 * Validates form fields against data type.  Assigns names to form elements.
 */
function validateAndSubmit(form) {
    var fetchSearch = document.getElementsByName('fetchSearch');
    var deleteSearch = document.getElementsByName('deleteSearch');
    if((fetchSearch.length > 0 && fetchSearch[0].wasClicked) || (deleteSearch.length > 0 &&deleteSearch[0].wasClicked)) {
        // If we're loading a search or deleting a search, don't validate
        return true;
    }
    for(var i = 0; i < form.elements.length; i++) {
        var dataType = form.elements[i].getAttribute("dataType");
        if(dataType != null && dataType == "Date") {
            if(form.elements[i].value != '' && !form.elements[i].value.match(/^\d{1,2}\/\d{1,2}\/\d{4}$/)) {
                alert("Date doesn't match mm/dd/yyyy: " + form.elements[i].value);
                form.elements[i].focus();
                return false;
            }
        }
    }
    // Select all the column definition names, so they get POSTed
    var columnNames = $j('#selectedColumnDefNames')[0];
    if(columnNames.options.length > 0) {
        for(var j = 0; j < columnNames.options.length; j++) {
            columnNames.options[j].selected = true;
        }
    } else {
        // Users have a tendency to highlight a column in Available, and then forget to click right arrow
        alert("Choose at least one result column");
        return false;
    }
    assignSearchNames(form);
    return true;
}

/**
 * Assign Stripes indexed property names to each form element, so the form can be
 * submitted to the server.  Stripes will create an object graph of the search terms
 * and their children.
 * @param form the form to be submitted
 */
function assignSearchNames(form) {
    // The search instance contains a top level array of searchValues, and each
    // searchValue contains an array of children, and there may be grandchildren etc.,
    // so we have to keep track of the current position in these nested arrays.
    var indexArray = [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1];
    // The first three levels (form, fieldset and top level div) don't count, so set
    // depth to ignore them
    searchNamesRecurse(form, -3, indexArray);
}

/**
 * Recurse over nested search terms, assigning Stripes names.
 * @param parent the element that invoked this method recursively
 * @param termDepth the depth of the recursion
 * @param indexArray keeps track of the current sibling at each level in the hierarchy of search terms
 */
function searchNamesRecurse(parent, termDepth, indexArray) {
    var childNodes = parent.childNodes;
    for(var i=0; i < childNodes.length; i++){
        var node = childNodes[i];
        // if node is an element
        if(node.nodeType == 1) {
            // indexOf because jQuery date picker adds hasDatepicker to class
            if(node.className.indexOf("termvalue") >= 0) {
                // Between operators have 2 text fields
                var suffix;
                if(node.tagName == "SELECT" && node.getAttribute("multiple")) {
                    suffix = "values";
                } else if(node.getAttribute("between2") != null) {
                    suffix = "values[1]";
                } else {
                    suffix = "values[0]";
                }
                node.setAttribute("name", "searchInstance." +
                        stripesChildren(termDepth, indexArray) + suffix);
            } else {
                switch (node.className) {
                    case "searchterm" :
                        break;
                    case "termname" :
                        // Found a term, so increment the index for the current depth,
                        // and clear the index for the next level down
                        indexArray[termDepth] = indexArray[termDepth] + 1;
                        indexArray[termDepth + 1] = -1;
                        node.setAttribute("name", "searchInstance." +
                                stripesChildren(termDepth, indexArray) + "termName");
                        break;
                    case "termoperator" :
                        node.setAttribute("name", "searchInstance." +
                                stripesChildren(termDepth, indexArray) + "operator");
                        break;
                    case "termtype" :
                        node.setAttribute("name", "searchInstance." +
                                stripesChildren(termDepth, indexArray) + "dataType");
                        break;
                    case "displayTerm" :
                        node.setAttribute("name", "searchInstance." +
                                stripesChildren(termDepth, indexArray) + "includeInResults");
                        break;
                    case "valueSetWhenLoaded" :
                        node.setAttribute("name", "searchInstance." +
                                stripesChildren(termDepth, indexArray) + "valueSetWhenLoaded");
                        break;
                    case "rackScanData" :
                        node.setAttribute("name", "searchInstance." +
                            stripesChildren(termDepth, indexArray) + "rackScanData");
                        break;
                }
            }
            searchNamesRecurse(node, termDepth + 1, indexArray);
        }
    }
}

/**
 * Build the Stripes property name, with nested indexed properties
 * @param termDepth how deeply we have recursed
 * @param indexArray holds the current position for each sibling in the hierarchy
 * @return a dot notation path to one object property in the hierarchy
 */
function stripesChildren(termDepth, indexArray) {
    var parameterFragment = 'searchValues[' + indexArray[0] + '].';
    for(var levelIndex = 1; levelIndex <= termDepth; levelIndex++) {
        parameterFragment += 'children[' + indexArray[levelIndex] + '].';
    }
    return parameterFragment;
}

/**
 * Pop up configuration dialogue if a custom traverser is selected which requires a user defined configuration
 */
showCustomTraverserOptions = function(evt) {
    var selectedOption = $j("#customTraversalOptionConfig > option:selected");
    var editElement = $j("#customTraversalOptionEdit");
    if( eval( selectedOption.data("hasUserCustomization") ) ) {
        /**
         * Show child options for result columns to select from to add column to chosen list
         * @param available multi-select of available columns
         * @param chosen multi-select of chosen columns
         */
        var overlayDiv = $j( "#resultParamsOverlay" );
        overlayDiv.dialog("option","paramType", "CUSTOM_TRAVERSER");
        overlayDiv.dialog("option","entityName", $j("#entityName").val());
        overlayDiv.dialog("option","elementName", selectedOption.data("elementName"));
        overlayDiv.dialog("option","resultParams", selectedOption.val());
        overlayDiv.dialog("open");
        editElement.css("background-position", "-64px -112px");

    } else {
        editElement.css("background-position", "-640px 0px");
    }
};

initResultParamOverlay = function(){ //<%-- Dialog div element at bottom of configurable_search.jsp --%>
    var paramDialog = $j( "#resultParamsOverlay" ).dialog({
        title: "Select Parameters",
        paramType:"",
        entityName:"",
        elementName:"",
        resultParams:{},
        autoOpen: false,
        height: 540,
        width: 320,
        modal: true,
        open: function(){
            var paramType = paramDialog.dialog("option","paramType");
            var entityName = paramDialog.dialog("option","entityName");
            var elementName = paramDialog.dialog("option","elementName");
            var resultParams = paramDialog.dialog("option","resultParams");
            paramDialog.dialog("option","srchTermEditSrc", null); // This is set after open if an edit is requested
            paramDialog.dialog("option", "reset")();
            $j.ajax({
                url: '/Mercury/search/ResultParams.action',
                data: { "paramsFetch":""
                    , "paramType":paramType
                    , "entityName":entityName
                    , "elementName":elementName
                    , "resultParams":resultParams},
                type: 'post',
                dataType: 'html',
                cache: true,
                complete: function (returnData, status) {
                    if( status === "success" ) {
                        $j("#resultParamsInputs")[0].innerHTML = returnData.responseText;
                    } else {
                        var errDiv = $j("#resultParamsError");
                        var message = status + ": " + returnData.responseText;
                        errDiv.text(message);
                        errDiv.css('display','block');
                    }
                },
            })
        },
        // Clears out any previous error message state
        reset: function(){
            $j("#resultParamsError").text("").css('display','none');
        },
        error: function(msg){
            $j("#resultParamsError").text(msg).css('display','block');
        },
        validate: function(){
            var isValid = true;
            $j("#resultParamsOverlay form").find(":input").each(
                function( index, element ) {
                    element = $j(element);
                    if( element.attr("type") === "text" && element.attr("id") !== "dlg_filterColumns" && element.val() == "" ) {
                        element.css("border-color","#FF0000");
                        $j("#resultParamsError").text("Values are required").css('display','block');
                        isValid = false;
                    } else if( element.find("option").length > 0 && element.find("option:selected").length === 0 ) {
                        element.css("border-color","#FF0000");
                        $j("#resultParamsError").text("Values are required").css('display','block');
                        isValid = false;
                    } else {
                        element.css("border-color","rgb(204, 204, 204)");
                    }
                }
            );
            return isValid;
        }
    });
    paramDialog.find( "form" ).on( "submit", function( event ) {
        event.preventDefault();
        paramDialog.dialog("option", "reset")();
        if( !paramDialog.dialog("option", "validate")() ) {
            return;
        }

        var userColumnName = $( this ).find( "#userColumnName" ).val();
        var rsltParamVal = {paramType:null,entityName:null,elementName:null,paramValues:[]};
        rsltParamVal.paramType = paramDialog.dialog("option", "paramType");
        rsltParamVal.entityName = paramDialog.dialog("option", "entityName");
        rsltParamVal.elementName = paramDialog.dialog("option", "elementName");
        var params = $( this ).serializeArray();
        rsltParamVal.paramValues = params;

        if( rsltParamVal.paramType == "SEARCH_TERM") {

            var resultColumnSelect = $j('#selectedColumnDefNames')[0];
            // Overwrite if an edit is being processed
            var editOption = paramDialog.dialog("option","srchTermEditSrc");
            if( editOption == null ) {
                editOption = $j(document.createElement('option'));
                resultColumnSelect.options[resultColumnSelect.options.length] = editOption[0];
            }
            editOption.text(userColumnName);
            editOption.val(JSON.stringify(rsltParamVal));
            editOption.data("hasParams", true);
            editOption.dblclick( editColumnParams );
        } else {
            var traverserSelect = $j("#customTraversalOptionConfig").find("option:selected");
            traverserSelect.val(JSON.stringify(rsltParamVal));
        }
        paramDialog.dialog("close");
    });
    paramDialog.find( "#resultParamsCancelBtn" ).on( "click", function(event){
        paramDialog.dialog("close");
    });
};

