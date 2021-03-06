;(function ( $, window, document, undefined ) {
    var pluginName = 'plateMap',
        defaults = {
            logEnabled : true,
            metricsSelectorClass: '.metricsList',
            metadataDefinitionListClass: '.metadataDefinitionList',
            metadataDefinitionPlateListClass: '.metadataDefinitionPlateList',
            legendClass: '.legend',
            tableClass: '.platemap',
            platemaps: []
        };

    // The actual plugin constructor
    function Plugin( element, options ) {
        this.element = element;
        this.options = $.extend( {}, defaults, options);
        this.platemaps = $(this.options.platemaps);
        this.tables = $(this.options.tableClass);
        this.legends = $(this.options.legendClass);
        this.metadataFields = $(this.options.metadataDefinitionListClass);
        this.plateMetadataFields = $(this.options.metadataDefinitionPlateListClass);
        this.metrics = $(this.options.metricsSelectorClass);
        this._defaults = defaults;
        this._name = pluginName;
        this.init();
    }

    Plugin.prototype.init = function() {
        var plugin = this;
        $.each(this.platemaps, function (index, platemap) {
            var legend = plugin.legends[index];
            var metadata = plugin.metadataFields[index];
            var plateMetadata = plugin.plateMetadataFields[index];
            var metric = plugin.metrics[index];
            plugin.initPlatemap(platemap, legend, metadata, metric, plateMetadata);
        });
    };

    Plugin.prototype.initPlatemap = function (platemap, legend, metadata, metric, plateMetadata) {
        var plugin = this;
        var datasets = platemap.datasets;
        var legend = $(legend);
        var metricNames = $.map(platemap.datasets, function(val, i) {
            return val.displayMetrics.displayName;
        });
        var metricsListBox = this.buildMetricsSelectList(metricNames, $(metric));
        var plateMetadataSelector = $(plateMetadata);
        var cellMetadataSelector = $(metadata);
        plugin.attachMetadata(null, platemap.plateMetadata, plateMetadataSelector);

        var cellIdPrefix = (platemap.label === null) ? "#" : '#' + platemap.label + "_";

        // Status
        if( platemap.wellStatusMap != null ) {
            $.each(platemap.wellStatusMap, function (idx, well) {
                var wellIdTag = cellIdPrefix + idx;
                var wellElem = $(wellIdTag);
                switch (well) {
                    case "Empty":
                        wellElem.attr('class', 'metricCell noSample');
                        break;
                    case "Blacklisted":
                        wellElem.attr('class', 'metricCell blacklisted');
                        break;
                    case "Abandoned":
                        wellElem.attr('class', 'metricCell abandoned');
                        break;
                    default:
                        wellElem.attr('class', 'metricCell');
                }
            });
        }

        // Attach metadata to each cell
        $.each(platemap.wellMetadataMap, function (cell, metavals) {
            plugin.attachMetadata($(cellIdPrefix + cell), metavals, cellMetadataSelector);
        });

        metricsListBox.change(function() {
            var selectedMetric = this.value;
            var datasetList = $.grep(datasets, function(e){
                return e.displayMetrics.displayName === selectedMetric;
            });
            // Clear all cells and legend
            var wells = $(".metricCell");
            $.each(wells, function () {
                $(this).text("");
                $(this).css("background-color", "");
            });

            legend.empty();
            if (datasetList.length === 1){
                var dataset = datasetList[0];
                plugin.log(dataset);
                var chartType = dataset.displayMetrics.chartType;
                plugin.buildLegend(legend, dataset.options);
                $.each(dataset.wellData, function (idx, wellData) {
                    var wellIdTag = cellIdPrefix + wellData.well;
                    var wellElem = $(wellIdTag);
                    if (dataset.displayMetrics.displayValue)
                        wellElem.text(wellData.value);
                    if (chartType === 'Category') {
                        for (var i = 0; i < dataset.options.length; i++) {
                            var option = dataset.options[i];
                            var evalFunction = dataset.displayMetrics.evalType;
                            if (plugin[evalFunction](wellData.value, option.value)) {
                                wellElem.css('background-color', option.color);
                                break;
                            }
                        }
                    } else if (chartType === 'Duplicate') {
                        var duplicateOption = dataset.options[0];
                        var goodOption = dataset.options[1];
                        var noneOption = dataset.options[2];
                        var thisWellVal = wellData.value;
                        var foundDupe = false;
                        var noValue = thisWellVal === "";
                        if (!noValue) {
                            $.each(dataset.wellData, function (idx, otherWellData) {
                                var otherWellId = cellIdPrefix + otherWellData.well;
                                if (wellIdTag !== otherWellId) {
                                    var otherWellList = otherWellData.value.split(" ");
                                    var thisWellList = thisWellVal.split(" ");
                                    for (var i = 0; i < thisWellList.length; i++) {
                                        thisWellVal = thisWellList[i];
                                        for (var j = 0; j < otherWellList.length; j++) {
                                            var thatWellVal = otherWellList[j];
                                            if (thisWellVal === thatWellVal) {
                                                wellElem.css('background-color', duplicateOption.color);
                                                foundDupe = true;
                                                var wellMetadata = platemap.wellMetadataMap[wellData.well];
                                                plugin.highlightOnHover(wellElem, otherWellId, wellMetadata, cellMetadataSelector);
                                                break;
                                            }
                                        }
                                    }
                                }
                            });
                        } else {
                            wellElem.css('background-color', noneOption.color);
                        }
                        if (!foundDupe && !noValue) {
                            wellElem.css('background-color', goodOption.color);
                            wellElem.css('border-top', '1px solid #ccc');
                        }
                    }
                });
            }
        });
    };

    Plugin.prototype.highlightOnHover = function(wellElement, otherWellId, metadataList, metadataSelector) {
        wellElement.hover(function() { // Mouse Enter
            $(this).css('border', '1px solid #F00');
            $(otherWellId).css('border', '1px solid #F00');
            metadataMouseEnter(wellElement, metadataSelector);
        }, function() { // Mouse Leave
            $(this).css('border', '');
            $(this).css('border-top', '1px solid #ccc');
            $(otherWellId).css('border', '');
            $(otherWellId).css('border-top', '1px solid #ccc');
            metadataMouseLeave(metadataSelector);
        });
    };

    Plugin.prototype.buildMetricsSelectList = function(metricNames, metricsListBox) {
        metricsListBox.append($("<option>").attr('value', '').text(''));
        $(metricNames).each(function(idx, value) {
            metricsListBox.append($("<option>").attr('value', value).text(value));
        });
        return metricsListBox;
    };

    Plugin.prototype.buildLegend = function(legend, options) {
        legend.empty();
        var legendList = $('<ul></ul>').addClass('legend');
        if( !options ) return;
        for (var i = 0; i < options.length; i++) {
            var option = options[i];
            var li = $('<li></li>');
            var span = $('<span></span>');
            span.css('background-color', option.color);
            li.text(option.name);
            li.prepend(span);
            legendList.append(li);
        }
        legend.append(legendList);
    };

    Plugin.prototype.attachMetadata = function(wellElem, metadataList, metadataSelector) {
        if (wellElem != null) {
            wellElem.data("metadata", metadataList);
            wellElem.hover(function(){
                metadataMouseEnter(wellElem, metadataList, metadataSelector);
            }, function () {
                metadataMouseLeave(metadataSelector);
            });
        } else {
            if (metadataList !== undefined) {
                $.each(metadataList, function (idx, metadata) {
                    var dt = $('<dt></dt>').text(metadata.label);
                    var dd = $('<dd></dd>').text(metadata.value);
                    metadataSelector.append(dt);
                    metadataSelector.append(dd);
                });
            }
        }
    };

    function metadataMouseEnter(wellElem, metadataList, metadataSelector) {
        if (metadataSelector === undefined) {
            return;
        }
        metadataSelector.empty();
        var metadata = wellElem.data("metadata");
        if (metadata !== undefined) {
            $.each(metadataList, function (idx, metadata) {
                if( metadata.label ) {
                    var dt = $('<dt></dt>').text(metadata.label);
                    var dd = $('<dd></dd>').text(metadata.value);
                    metadataSelector.append(dt);
                    metadataSelector.append(dd);
                }
            });
        }
    }

    function metadataMouseLeave(metadataSelector) {
        metadataSelector.empty();
    }

    Plugin.prototype.greaterThanOrEqual = function(a, b) {
        return Number(a) >= Number(b);
    };

    Plugin.prototype.equals = function(a, b) {
        return a === b;
    };

    Plugin.prototype.greaterThan = function(a, b) {
        return Number(a) > Number(b);
    };

    Plugin.prototype.log = function(msg) {
        if (this.options.logEnabled && typeof console != "undefined") {
            try {
                console.log(msg);
            }
            catch (e) {
            }
        }
    };

    // preventing against multiple instantiations
    $.fn[pluginName] = function ( options ) {
        return this.each(function () {
            if (!$.data(this, 'plugin_' + pluginName)) {
                $.data(this, 'plugin_' + pluginName,
                    new Plugin( this, options ));
            }
        });
    }

})( jQuery, window, document );