/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

function initColumnSelect(settings, columnNames, filterStatusSelector, columnFilterClass, ctxPath) {
    var api = new $j.fn.dataTable.Api(settings);
    if (tableEmpty(settings)){
        return;
    }
    var tabIndex=0;

    var filterStatusContainer = $j(filterStatusSelector); // MaterialType-filters

    filterStatusContainer.append($j("<ul></ul>", {
        class: "filtered-items-header list",
    }));
    var extraDiv = $j("<div></div>", {id: "extraDiv"});
    filterStatusContainer.append(extraDiv);
    extraDiv.append($j("<ul></ul>", {class: "list", id: "filtered-items"}));
    var chosenColumns=[];

    function triggerChosenUpdate(column) {
        chosenColumns.forEach(function(chosenItem){
            if (chosenItem.attr("id") !== column.id){
                chosenItem.trigger("chosen:updated");
            }
        })
    }

    function getElement(html, map) {
        var selector;
        var keys = Object.keys(map);
        if (keys.includes("id")) {
            var value = map["id"];
            if (value) {
                selector = "#" + value;
            }
        }
        if (keys.includes("class")) {
            var value = map["class"];
            if (value) {
                selector += "." + value;
            }
        }
        return $j(selector).length ? $j(selector) : $j(html, map);
    }

    function buildSearchTerm(filterValue, selectType) {
        var searchTerm = filterValue;
        if (searchTerm!=="") {
            if (selectType === "select") {
                searchTerm = "^" + escape(filterValue) + "$";
            } else {
                searchTerm = filterValue;
            }
        }
        return searchTerm;
    }

    function escape(text){
        return $j.fn.dataTable.util.escapeRegex(text);
    }

    function updateFilter(column, filterValue, selectType = 'text') {
        var searchString = '';
        if (Array.isArray(filterValue)) {
            filterValue.forEach(function (value, index) {
                searchString += buildSearchTerm(value, selectType);
                if (index !== filterValue.length - 1) {
                    searchString += "|";
                }
            });
        } else {
            searchString = buildSearchTerm(filterValue, selectType);
        }
        column.search(searchString, true, false, true).draw();
        if (selectType === 'select') {
            triggerChosenUpdate(column);
        }
    }

    function stripRegex(inputText) {
        return inputText.replace(/[^\w\s\W]|\^|\$|\\/gi, '');
    }

    function updateFilterInfo(column, title, filterLabel, filterValue) {
        var filteredItemsHeader = $j(".filtered-items-header");
        var filterHeader = getElement("<li></li>", {class: "filter-header-item", id: title, text: filterLabel});

        var filterItemTitle = "filter-item-" + title;
        $j("#"+filterItemTitle).remove();
        var filteredItem = $j("<li></li>", {id: filterItemTitle, class: "list"});
        var columnContainer = $j("#filtered-items");

        if (filterValue.length === 0) {
            filteredItemsHeader.find(filterHeader).remove();
            columnContainer.find(filteredItem).remove();
        } else {
            var labelClass = 'filtering-label';

            var filteredItemLabel = getElement("<span></span>", {class: labelClass, text: filterLabel + ":"});// eg Material Type
            var filteredItedBullet = getElement("<img/>", {
                src: ctxPath + '/images/error.png',
                title: "Clear " + filterLabel + " filter"
            });
            filteredItedBullet.hover(function(){
                $j(this).css("cursor", "pointer");
            });
            filteredItemLabel.prepend(filteredItedBullet);
            var filterText;
            if (Array.isArray(filterValue)) {
                filterText = filterValue.join(", ");
            } else {
                filterText = filterValue.replace('|', ', ');
            }
            filterValue = filterText;
            filterText = stripRegex(filterText);
            var filteredItemValue = getElement("<span></span>", {
                class: 'filtering-text',
                text: filterText
            });// DNA

            filteredItemsHeader.append(filterHeader);
            filteredItem.append(filteredItemLabel);
            filteredItemLabel.after(filteredItemValue);
            columnContainer.append(filteredItem);
            filteredItem.on("click",function(){
                // wrapped in closure since this is created in a loop
                (function (label, value) {
                    $j("th").filter(function () {
                        var columnTitle = $j(this).find(".title").text().trim();
                        return columnTitle === label;
                    }).each(function () {
                        var textArea = $j(this).find("input[type='textarea']");
                        if (textArea.length > 0) {
                            textArea.val('');
                            textArea.trigger("input");
                        } else {
                            var select = $j(this).find("select");
                            if (select.length > 0) {
                                var optionSelector = "option[value='OPTION_VALUE']".replace("OPTION_VALUE", value);
                                $j(this).find(optionSelector).removeAttr('selected');
                                var eventWhat = {'deselected': ''};
                                select.trigger("chosen:updated", eventWhat);
                            }
                        }
                        updateFilter(column, '', 'select');
                    });
                    $j(this).remove();
                })(filterLabel, filterValue);
            });
        }
        if (filterStatusContainer.find("li").length==0){
            filterStatusContainer.fadeOut(75);
        }else{
            filterStatusContainer.fadeIn(75);
        }
    }

    api.columns().every(function (index) {
        var column = api.column(index);
        var filteredRows = api.column(index, {search: 'none'}).data();
        var savedFilterValue=unEscape(column.search());

        var header = $j(column.header());
        var headerLabel = header.text().trim();
        var cleanTitle = headerLabel.replace(/\s+/g, '');
        if (cleanTitle!==''){
            tabIndex++;
        }
        var selectType = "select";
        var filterColumn = false;
        columnNames.forEach(function (col) {
            $j.each(col, function (column, type) {
                if (column === headerLabel) {
                    selectType = type;
                    columnHeader = column;
                    filterColumn = true;
                }
            })
        });

        if (selectType === 'text' && filterColumn) {
            savedFilterValue = savedFilterValue.replace(/\|/g,' ');
            var textInput = $j("<input/>", {
                type: 'textarea',
                css: 'height:1, display: inline-block',
                class: columnFilterClass,
                tabindex: tabIndex,
                value: stripRegex(savedFilterValue),
                placeholder: "Filter " + headerLabel
            });
            $j(textInput).prop("title","Enter text to filter on " + header.text());
            var inputContainer=$j("<span></span>", {'class': 'search-field'});
            inputContainer.append(textInput);
            header.append(inputContainer);

            // do not sort column when input field is clicked.
            $j(textInput).on('click', stopPropagation);
            $j(textInput).on('input', function () {
                var searchInput = $j(this).val().trim();
                updateFilter(column, searchInput.split(/\s+/));
                updateFilterInfo(column, cleanTitle, headerLabel, searchInput);
            });
        }
        if (selectType === 'select' && filterColumn) {
            var selectFilterId = "filter-" + index;
            var select = $j("<select></select>", {
                id: selectFilterId,
                multiple: true,
                title: "click to select a " + headerLabel,
                tabindex: tabIndex,
                class: columnFilterClass
            });

            header.append(select);
            buildHeaderFilterOptions(header, filteredRows);

            if (savedFilterValue!=='') {
                select.find("option").filter(function () {
                    return this.value.trim().match(savedFilterValue);
                }).attr('selected', 'selected');
            }

            $j("#" + selectFilterId).on("chosen:ready", refreshStyling);

            var chosen = select.chosen({
                disable_search_threshold: 10,
                display_selected_options: false,
                display_disabled_options: false,
                enable_split_word_search: false,
                search_contains: true,
                width: 'auto',
                inherit_select_classes: true,
                placeholder_text_single: "Select a " + headerLabel,
                placeholder_text_multiple: "Select a " + headerLabel
            });

            chosenColumns.push(chosen);

            chosen.on("change chosen:updated", function (event, what) {
                // chosen.on("nothing", function (event, what) {
                if (what) {
                    var eventAction = Object.keys(what)[0]; // ['selected','deselected']
                    if (eventAction === 'deselected') {
                        var deselectedItems = what['deselected'].trim();
                        if (deselectedItems.length === 0) {
                            $j(this).find(":selected").prop('selected',false);
                        }
                    }
                    var currentSelection = $j(this).find(":selected").map(function () {
                        return this.text
                    }).get();
                    updateFilterInfo(column, cleanTitle, headerLabel, currentSelection);
                }
            });
            column.on("column-sizing.dt", function () {
                chosen.trigger("chosen:updated");
            });

            $j(select).on('change', function () {
                // wrapped in closure since this is created in a loop
                (function (dtColumn, select) {
                    var values = [];
                    $j(select).find("option:selected").each(function () {
                        values.push(this.value.trim());
                    });
                    updateFilter(dtColumn, values, selectType);
                    refreshStyling();
                })(column, this);
            });
        }
        if (filterColumn) {
            api.on('init.dt', function (event, settings) {
                updateFilterInfo(column, cleanTitle, headerLabel, savedFilterValue);
            });
        }
    });

    // do not sort column when input field is clicked.
    api.on('init.dt', function (event, settings) {
        $j(".chosen-container").on("click", ".chosen-choices, .chosen-results", stopPropagation);
    });

    api.on('search.dt', function (event, settings) {
        updateSearchText(settings);
    });

    function buildHeaderFilterOptions(header, columns) {
        var select = $j(header).find("select");
        var uniqueValues = [];
        for (var i = 0; i < columns.length; i++) {
            var cell = columns[i].trim();
            cell = cell.replace(/<(?:.|\n)*?>/gi, '').trim();
            if (cell !== '' && uniqueValues.indexOf(cell) < 0) {
                uniqueValues.push(cell.trim());
            }
        }
        for (var option of uniqueValues.sort()) {
            option = unEscape(option);
            var items = $j("<option></option>", {value: option, text: option});
            $j(select).append(items);
        }
        return $j(select);
    }

    // unescape html encoded character`s such as '&amp;'
    function unEscape(s) {
        return $j("<textarea/>").html(s).text();
    }

    function tableEmpty(settings){
        var api = $j.fn.dataTable.Api(settings);
        return api.data().length===0;
    }

    function stopPropagation(evt) {
        if (evt.stopPropagation !== undefined) {
            evt.stopPropagation();
        } else {
            evt.cancelBubble = true;
        }
    }

    function updateSearchText(settings) {
        var api = $j.fn.dataTable.Api(settings);
        var currentFullTextSearch = escape(api.search());
        if (currentFullTextSearch !== undefined) {
            var defaultText = "any text";
            var matchContent = defaultText;
            if (!isBlank(currentFullTextSearch)) {
                matchContent = currentFullTextSearch;
            }
            var textJQuery = jQuery("<span></span>", {text: matchContent});
            textColor = "";
            if (matchContent !== defaultText) {
                textColor = "red";
            }
            textJQuery.css("color", textColor);
            $j(".dtFilters").html("<b>Search text matches</b>: " + textJQuery[0].outerHTML);
        }
    }

    function refreshStyling() {
        $j('.chosen-drop, .chosen-container, .search-field input').css('width', 'auto');
        $j('.chosen-choices, .search-choice').addClass('ellipsis');
        $j('.chosen-drop, .chosen-container').css('min-width', '6em');
        $j(".search-field input").css("font-size", "smaller");
        $j('.chosen-drop,.chosen-results li, li.search-choice span').css("white-space", "nowrap");
    }
}
