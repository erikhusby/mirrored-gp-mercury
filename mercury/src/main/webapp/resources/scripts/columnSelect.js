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

    var updateFilter = $j.fn.dataTable.util.throttle(
         function (column, filterValue) {
             searchString = filterValue.replace(/[,|\s]+/g, '|');
             var searchInstance = column.search(filterValue ?  searchString : '', true, false, true);
             triggerChosenUpdate(column);
             searchInstance.draw();
         }, 500);

    function updateFilterInfo(column, title, filterLabel, filterValue) {
        var filteredItemsHeader = $j(".filtered-items-header");
        var filterHeader = getElement("<li></li>", {class: "filter-header-item", id: title, text: filterLabel});

        var filterItemTitle = "filter-item-" + title;
        $j("#"+filterItemTitle).remove();
        var filteredItem = $j("<li></li>", {id: filterItemTitle, class: "list"});
        var columnContainer = $j("#filtered-items");

        if (filterValue === "") {
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
            var filteredItemValue = getElement("<span></span>", {class: 'filtering-text', text: filterValue});// DNA
            filteredItemsHeader.append(filterHeader);
            filteredItem.append(filteredItemLabel);
            filteredItemLabel.after(filteredItemValue);
            columnContainer.append(filteredItem);

            filteredItem.on("click",function(){
                $j("th").filter(function () {
                    var columnTitle = $j(this).find(".title").text().trim();
                    return columnTitle === filterLabel;
                }).each(function () {
                    var textArea = $j(this).find("input[type='textarea']");
                    if (textArea.length > 0) {
                        textArea.val('');
                        textArea.trigger("change");
                    } else {
                        var select = $j(this).find("select");
                        if (select.length>0){
                            var optionSelector = "option[value='OPTION_VALUE']".replace("OPTION_VALUE",filterValue);
                            $j(this).find(optionSelector).removeAttr('selected');
                            var eventWhat = {'deselected': ''};
                            select.trigger("chosen:updated", eventWhat);
                        }
                    }
                    updateFilter(column, '');
                });
                $j(this).remove();
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
        var savedFilterValue=column.search();

        var header = $j(column.header());
        var headerLabel = header.text().trim();
        var cleanTitle = headerLabel.replace(/\s+/, '');
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
            var textInput = $j("<input/>", {
                type: 'textarea',
                css: 'height:1',
                class: columnFilterClass,
                value: savedFilterValue,
                placeholder: "Filter " + headerLabel
            });
            header.append(textInput);
            $j(textInput).on('click', function () {
                return false;
            });

            $j(textInput).on('input blur change', function () {
                updateFilter(column, $j(this).val().trim());
                updateFilterInfo(column, cleanTitle, headerLabel, $j(this).val().trim());
            });
        }
        if (selectType === 'select' && filterColumn) {
            var selectFilterId = "filter-" + index;
            var select = $j("<select></select>", {
                id: selectFilterId,
                multiple: true,
                class: columnFilterClass,
                style: 'display: none',
            });

            header.append(select);
            buildHeaderFilterOptions(header, filteredRows);
            // select.find("option[value='" + savedFilterValue + "']").attr('selected', 'selected');
            if (savedFilterValue!=='') {
                select.find("option").filter(function () {
                    return this.value.trim().match(savedFilterValue);
                }).attr('selected', 'selected');
            }
            var width = $j(select).attr('width');
            if (width<=10){
                width=10;
            }
            width = Math.ceil(.8*width)+"em";
            var chosen = select.chosen({
                disable_search_threshold: 10,
                display_selected_options: false,
                display_disabled_options: false,
                width: width,
                inherit_select_classes: true,
                placeholder_text_single: "Select a " + headerLabel,
                placeholder_text_multiple: "Select a " + headerLabel
            });

            chosenColumns.push(chosen);
            $j("div." + columnFilterClass).on("click", function () {
                return false;
            });
            chosen.on("change chosen:updated", function (event, what) {
                // chosen.on("nothing", function (event, what) {
                if (what) {
                    var eventAction = Object.keys(what)[0]; // ['selected','deselected']
                    var currentlySelected = $j(this).find(":selected").map(function () {
                        return this.text
                    }).get();
                    if (eventAction === 'deselected') {
                        var deselectedItems = what['deselected'].trim();
                        if (deselectedItems.length === 0) {
                            $j(this).find(":selected").prop('selected',false);
                            currentlySelected = [];
                        } else {
                            var index = currentlySelected.indexOf(deselectedItems);
                            if (index)
                                if (index == -1) {
                                    currentlySelected.splice(index, 1);
                                }
                        }
                    }
                    var filterText = currentlySelected.join("|");
                    updateFilterInfo(column, cleanTitle, headerLabel, filterText);
                }
            });
            column.on("column-sizing.dt", function () {
                chosen.trigger("chosen:updated");
            });

            // select.trigger("chosen:updated");
            // stop event propagation so clicking the text area won't cause the column to sort.
            $j('.filter-select ul').on('click', function () {
                return false;
            });

            $j(select).on('change blur', function () {
                // api.off('click');
                var values = [];
                $j(this).find("option:selected").each(function () {
                    values.push(this.value.trim());
                });
                updateFilter(column, values.join('|'));
                // api.off(this);
            });
        }
        api.on('init.dt', function (event, settings) {
            updateFilterInfo(column, cleanTitle, headerLabel, savedFilterValue.replace(/\|$/, ''));
        });
    });

    api.on('search.dt', function (event, settings) {
        updateSearchText(settings);
    });

    function buildHeaderFilterOptions(header, columns) {
        var select = $j(header).find("select");
        var htmlExpression = /<(?:.|\n)*?>/gi;
        var uniqueValues = [];
        for (var i = 0; i < columns.length; i++) {
            var cell = columns[i].trim();
            cell = cell.replace(/<(?:.|\n)*?>/gi, '');
            if (cell !== '' && uniqueValues.indexOf(cell) < 0) {
                uniqueValues.push(cell.trim());
            }
        }
        var maxWidth=0;
        uniqueValues.sort().forEach(function (thisOption) {
            var items = $j("<option></option>", {value: thisOption, text: thisOption});
            maxWidth = thisOption.length > maxWidth?thisOption.length:maxWidth;
            $j(select).append(items);
        });

        $j(select).attr('width',maxWidth);

        return $j(select);
    }
    function tableEmpty(settings){
        var api = $j.fn.dataTable.Api(settings);
        return api.data().length===0;
    }

    function updateSearchText(settings) {
        var api = $j.fn.dataTable.Api(settings);
        var currentFullTextSearch = api.search();
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
}
