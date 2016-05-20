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

(function ($) {
        $.fn.filterColumn = function (columnName, values, options) {
            var columnIndexName = "columnIndex";
            var filtersName = "filters";
            var defaultFilterName = "columnFilter";
            var invertName = "invertResult";
            var oTable = $(this).dataTable();

            // This is the easiest way to have default options.

            var defaults = {
                filterLabel: "Click to filter",
                hideMatchingRows: true,
                selectedValues: [],
                selectionClass: defaultFilterName,
                invertResultClass: "invert_" + defaultFilterName,
                filteringTextSelector: idSelector(defaultFilterName.concat("_filteringText"))
            };
            var settings = $.extend({}, defaults, options);
            var inverseSelector = classSelector(settings.invertResultClass);


            // oTable.fnSettings().aoStateLoadParams.push({
            //     "fn": function (oSettings, oData) {
            //         if (oSettings.oLoadedState.columnFilterState != undefined) {
            //             selectedValues = oSettings.oLoadedState.columnFilterState;
            //         }
            //         if (oSettings.oLoadedState.columnFilterHideMatchingRows != undefined) {
            //             invertName = oSettings.oLoadedState.columnFilterHideMatchingRows;
            //         }
            //         return oData + selectedValues;
            //     },
            //     "sName": "loadColumnFilterState"
            // });
            //
            // oTable.fnSettings().aoStateSaveParams.push({
            //     "fn": function (oS, sVal) {
            //         var filterSelect = "select" + classSelector(settings.selectionClass)
            //         var selectedOptions =  $(filterSelect).val() == undefined ? [] :  $(filterSelect).val();
            //         if (selectedOptions !==undefined) {
            //             return sVal + selectedOptions;
            //         }
            //     },
            //     "sName": "columnFilterState"
            // });
            // oTable.fnSettings().aoStateSaveParams.push({
            //     "fn": function (oS, sVal) {
            //         inverseSelector = classSelector(settings.invertResultClass);
            //         var invertResult = $(inverseSelector).val();
            //         if (invertResult!==undefined) {
            //             return sVal + invertResult;
            //         }
            //     },
            //     "sName": "columnFilterHideMatchingRows"
            // });


            return this.each(function () {
                setData(columnIndexName, columnIndex());
                getSelectOptions();
                options = [];
                // oSettings = oTable.fnSettings();
                // if (oSettings.oFeatures.bStateSave && oSettings.oLoadedState !== null) {
                //     if (oSettings.oLoadedState.columnFilterState != undefined) {
                //         selectedValues = oSettings.oLoadedState.columnFilterState;
                //     }
                //     if (oSettings.oLoadedState.columnFilterHideMatchingRows != undefined) {
                //         selectedValues = oSettings.oLoadedState.columnFilterHideMatchingRows;
                //     }
                //
                // }
                var filterSelect = "select" + classSelector(settings.selectionClass);

                var chosen = $(filterSelect).chosen({"width": "auto"});
                var infoSection;
                chosen.ready(function () {
                    infoSection = $(oTable.selector + "_wrapper").find("[class$='_info']");
                });
                chosen.on("change chosen:updated", function (event, what) {
                    if (what === undefined) {
                        oTable.fnDraw();
                    }
                    var selectedOptions = $(this).val() == undefined ? [] : $(this).val();
                    var invertResult = $(inverseSelector).val();
                    var filters = [];
                    var filterText = "<b>Filtering column</b> 'COLUMN_NAME' <b>for rows WITH_OR_WITHOUT values:</b> ";
                    var withOrWithout;
                    if (invertResult === "true" && selectedOptions) {
                        filters = values.slice(0);
                        withOrWithout = "without";
                        for (i = 0; i < selectedOptions.length; i++) {
                            index = filters.indexOf(selectedOptions[i]);
                            if (index > -1) {
                                filters.splice(index, 1);
                            }
                        }
                        // when inverting we want to also see rows with empty values in column;
                        if (filters.indexOf("") == -1) {
                            filters.push("");
                        }
                    } else {
                        filters = selectedOptions;
                        withOrWithout = "with";
                    }
                    filterText = filterText.replace("COLUMN_NAME", columnName).replace("WITH_OR_WITHOUT", withOrWithout);
                    setData(filtersName, filters);
                    
                    if (selectedOptions.length > 0) {
                        var moreText = ", ";
                        var lastValue = selectedOptions.slice(-1)[0];
                        for (i = 0; i < selectedOptions.length; i++) {
                            value = selectedOptions[i];
                            if (lastValue === value || lastValue === "") {
                                moreText = ""
                            }
                            filterText = filterText.concat(selectedOptions[i]).concat(moreText);
                        }
                        if (settings.filteringTextSelector !== undefined && settings.filteringTextSelector !== "") {
                            var filterStatusDivId = "columnFilterStatus";
                            if ($(idSelector(filterStatusDivId)).length===0) {
                                var filterTextDiv = jQuery("<div></div>", {id: filterStatusDivId});
                                $(settings.filteringTextSelector).append(filterTextDiv);
                            }
                            $(idSelector(filterStatusDivId)).html(filterText);
                            if (infoSection) {
                                $(infoSection).hide();
                                $(filterTextDiv).append(infoSection);
                                $(infoSection).fadeIn();
                            }
                        }
                    }
                    oTable.fnDraw();
                });

                $(inverseSelector).change(function () {
                    var inverse = $(inverseSelector).val();
                    var whatChanged = {[invertName]: inverse};
                    $(filterSelect).trigger("chosen:updated", whatChanged);
                });

                $.fn.dataTableExt.afnFiltering.push(
                    function (oSettings, aData, iDataIndex) {
                        var filters = getData(filtersName);
                        if (filters == undefined || filters.length == 0) {
                            return true;
                        }
                        var index = getData(columnIndexName);
                        var valueForColumn = aData[index];

                        filterRow = false;

                        // var filterRow = undefined;
                        $.each(filters, function (unused, cellValue) {
                                var columnMatches = cellValue === valueForColumn;

                                if (!filterRow) {
                                    if (columnMatches) {
                                        filterRow = true;
                                    } else {
                                        filterRow = false;
                                    }
                                }
                            }
                        );
                        return filterRow;
                    }
                );

                if (settings.selectedValues) {
                    $(filterSelect).trigger("chosen:updated", "init");
                }

                return this;
            });


            function columnIndex() {
                return oTable.dataTable().find("th").filter(
                    function () {
                        if ($(this).text() === columnName) {
                            return this;
                        }
                    }).index();
            }

            function setData(variableName, variable) {
                jQuery.data(document.body)[variableName] = variable;
            }

            function getData(variableName) {
                var data = jQuery.data(document.body)[variableName];
                if (data == undefined) {
                    data = [];
                }
                return data;
            }

            function getSelectOptions() {
                container = $(classSelector(settings.selectionClass));

                if (settings.hideMatchingRows) {
                    filterStatusOption = jQuery("<select></select>", {
                        "class": settings.invertResultClass,
                        "width": "auto"
                    });
                    filterStatusOption.append(jQuery("<option></option>", {
                        "html": "Show Matching Rows", "value": "false"
                    }));
                    filterStatusOption.append(jQuery("<option></option>", {
                        "html": "Exclude Matching Rows", "value": "true", "selected": "true"
                    }));
                    container.append(filterStatusOption);
                }
                select = jQuery("<select></select>", {
                    "class": settings.selectionClass,
                    "data-placeholder": settings.filterLabel,
                    "multiple": true
                });

                for (i = 0; i < values.length; i++) {
                    listItem = values[i];
                    attributes = {"value": listItem, "html": listItem};
                    for (j = 0; j < settings.selectedValues.length; j++) {
                        if (settings.selectedValues[j] === listItem) {
                            attributes.selected = true;
                        }
                    }
                    select.append(jQuery("<option></option>", attributes));
                }
                container.append(select);
                return container;
            }


            function idSelector(value) {
                return "#".concat(value)
            }

            function classSelector(value) {
                return ".".concat(value)
            }
        };

    }(jQuery)
);
