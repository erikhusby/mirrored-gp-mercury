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

