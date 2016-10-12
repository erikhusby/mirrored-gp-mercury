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

    var filterStatusContainer = $j(filterStatusSelector); // MaterialType-filters
    // filterStatusContainer.hide();
    filterStatusContainer.append($j("<ul></ul>", {
        class: "filtered-items-header list",
    }));
    var extraDiv = $j("<div></div>", {id: "extraDiv"});
    filterStatusContainer.append(extraDiv);
    extraDiv.append($j("<ul></ul>", {class: "list", id: "filtered-items"}));

    api.columns().every(function (index) {
        var column = api.column(index);
        var savedFilterValue=column.search();

        var header = $j(column.header());
        var headerText = header.text().trim();
        var cleanTitle = headerText.replace(/\s+/, '');
        var selectType = "select";
        var filterColumn = false;
        columnNames.forEach(function (col) {
            $j.each(col, function (column, type) {
                if (column === headerText) {
                    selectType = type;
                    columnHeader = column;
                    filterColumn = true;
                }
            })
        });
        var width = Math.ceil(header.outerWidth() * 1.5) + "px";

        function updateFilter(column, input) {
            searchString = input.replace(/[,|\s]+/g, '|');
            var searchInstance = column.search(input ?  searchString : '', true, false, true);
            console.log(searchString);
            searchInstance.draw();
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

        function updateFilterInfo(filterLabel, filterValue) {
            var filteredItemsHeader = $j(".filtered-items-header");
            var filterHeader = getElement("<li></li>", {class: "filter-header-item", id: cleanTitle, text: headerText});

            var filterItemTitle = "filter-item-" + cleanTitle;
            $j("#"+filterItemTitle).remove();
            var filteredItem = $j("<li></li>", {id: filterItemTitle, class: "list"});

            var columnContainer = $j("#filtered-items");

            if (filterValue === "") {
                filteredItemsHeader.find(filterHeader).remove();
                columnContainer.find(filteredItem).remove();
            } else {
                var labelClass = 'filtering-label';

                var filteredItemLabel = getElement("<span></span>", {class: labelClass, text: filterLabel + ":"});// eg Material Type
                var filteredItedBullet = getElement("<img/>", {src: ctxPath+ '/images/error.png',title:"clear filter"});
                filteredItemLabel.prepend(filteredItedBullet);
                var filteredItemValue = getElement("<span></span>", {class: 'filtering-text', text: filterValue});// DNA
                filteredItemsHeader.append(filterHeader);
                filteredItem.append(filteredItemLabel);
                filteredItemLabel.after(filteredItemValue);
                columnContainer.append(filteredItem);

                filteredItedBullet.on("click",function(){
                    $j("th").filter(function(){
                        return $j(this).html().split("<")[0].trim() === filterLabel;
                    }).each(function () {
                        var textArea = $j(this).find("input[type='textarea']");
                        if (textArea.length>0){
                            textArea.val('');
                            // textArea.removeAttr('value');
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
                    })
                })
            }
            if (filterStatusContainer.find("li").length==0){
                filterStatusContainer.hide();
            }else{
                filterStatusContainer.show();
            }
        }

        if (selectType === 'text' && filterColumn) {
            var textInput = $j("<input/>", {
                type: 'textarea',
                css: 'height:1',
                class: columnFilterClass,
                value: savedFilterValue
            });
            header.append(textInput);
            $j(textInput).on('click', function () {
                return false;
            });
            var doUpdateFilter = $j.fn.dataTable.util.throttle(
                function (where, what) {
                    updateFilter(where, what)
                }, 800);

            $j(textInput).on('keyup change blur', function () {
                doUpdateFilter(column, $j(this).val().trim());
                updateFilterInfo(headerText, $j(this).val().trim());
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
            $j("<br/>").prependTo(select);
            header.append(select);
            buildHeaderFilterOptions(header, column.data());
            select.find("option[value='" + savedFilterValue + "']").attr('selected', 'selected');

            var chosen = select.chosen({
                disable_search_threshold: 10,
                display_selected_options: false,
                display_disabled_options: false,
                width: width,
                inherit_select_classes: true,
                placeholder_text_single: "Select a " + headerText,
                placeholder_text_multiple: "Select a " + headerText
            });
            chosen.on("change chosen:updated", function (event, what) {
                // chosen.on("nothing", function (event, what) {
                if (what) {
                    var eventAction = Object.keys(what)[0]; // ['selected','deselected']
                    updateFilterInfo(headerText, what[eventAction]);
                }
            });
            $j(".filtering-label img").on("click",function(){
                console.log("click ", this);
                debugger;

                // chosen.trigger("change", $j(this).text())
            });

            // select.trigger("chosen:updated");
            // stop event propagation so clicking the text area won't cause the column to sort.
            $j('.filter-select ul').on('click', function () {
                return false;
            });

            // $j('.filter-input').hide();
            $j(select).on('change blur', function () {
                // api.off('click');
                var values = [];
                $j(this).find("option:selected").each(function () {
                    values.push(this.value);
                });
                updateFilter(column, values.join(','));
                // api.off(this);

            });
        }
        api.on('init.dt', function (event, settings) {
                updateFilterInfo(headerText, savedFilterValue);
        });
    });

    api.on('search.dt', function (event, settings) {
        updateSearchText(settings);
    });
}

function buildHeaderFilterOptions(header, columns) {
    var select = $j(header).find("select");

    function clearHtml(htmlString) {
        return htmlString.replace(/<(?:.|\n)*?>/gm, '').trim();
    }

    var uniqueValues = [];
    columns.each(function (row) {
        var cell = clearHtml(row);
        if (uniqueValues.indexOf(cell) < 0) {
            uniqueValues.push(cell);
        }
    });

    uniqueValues.sort().forEach(function (thisOption) {
        var items = $j("<option></option>", {value: thisOption, text: thisOption});
        $j(select).append(items);
    });

    return $j(select);
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
