;(function ( $, window, document, undefined ) {
    var pluginName = 'plateMap',
        defaults = {
            logEnabled : true,
            metricsSelectorId: '#metricsList',
            onchangeselector : '#heatField',
            metadataDefinitionListId: '#metadataDefinitionList',
            legendId: '#legend',
            tableId: '#platemap',
            datasets:[]
        };

    // The actual plugin constructor
    function Plugin( element, options ) {
        this.element = element;
        this.options = $.extend( {}, defaults, options);
        this.metricsListSelector = $(this.options.metricsSelectorId);
        this.legendSelector = $(this.options.legendId);
        this.metadataSelector = $(this.options.metadataDefinitionListId);
        this._defaults = defaults;
        this._name = pluginName;
        this.init();
    }

    Plugin.prototype.init = function () {
        var datasets = this.options.datasets;
        var plugin = this;
        var legend = this.legendSelector;
        var metricNames = $.map(this.options.datasets, function(val, i) {
            return val.plateMapMetrics.displayName;
        });
        var metricsListBox = this.buildMetricsSelectList(metricNames);
        metricsListBox.change(function() {
            var selectedMetric = this.value;
            var datasetList = $.grep(datasets, function(e){
                return e.plateMapMetrics.displayName === selectedMetric;
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
                var chartType = dataset.plateMapMetrics.chartType;
                plugin.buildLegend(dataset.options);
                $.each(dataset.wellData, function (idx, wellData) {
                    var wellElem = $('#' + wellData.well);
                    plugin.attachMetadata(wellElem, wellData.metadata);
                    if (dataset.plateMapMetrics.displayValue)
                        wellElem.text(wellData.value);
                    if (chartType === 'Category') {
                        for (var i = 0; i < dataset.options.length; i++) {
                            var option = dataset.options[i];
                            var evalFunction = dataset.plateMapMetrics.evalType;
                            if (plugin[evalFunction](wellData.value, option.value)) {
                                wellElem.css('background-color', option.color);
                                break;
                            }
                        }
                    }
                });
            }
        });
    };

    Plugin.prototype.buildMetricsSelectList = function(metricNames) {
        var metricsListBox = this.metricsListSelector;
        metricsListBox.append($("<option>").attr('value', '').text(''));
        $(metricNames).each(function(idx, value) {
            metricsListBox.append($("<option>").attr('value', value).text(value));
        });
        return metricsListBox;
    };

    Plugin.prototype.buildLegend = function(options) {
        var legend = this.legendSelector;
        legend.empty();
        var legendList = $('<ul></ul>').addClass('legend');
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

    Plugin.prototype.attachMetadata = function(wellElem, metadataList) {
        var metadataSelector = this.metadataSelector;
        wellElem.hover(function(){
            metadataSelector.empty();
            if (metadataList != undefined) {
                $.each(metadataList, function (idx, metadata) {
                    var dt = $('<dt></dt>').text(metadata.label);
                    var dd = $('<dd></dd>').text(metadata.value);
                    metadataSelector.append(dt);
                    metadataSelector.append(dd);
                });
            }
        }, function () {
            metadataSelector.empty();
        });
    };

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