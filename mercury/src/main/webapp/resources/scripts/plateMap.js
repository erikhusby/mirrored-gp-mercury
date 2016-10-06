(function ( $ ) {
    $.fn.platemap = function( options ) {
        var settings = $.extend({
            onchangeselector : '#heatField',
            datasets: [],
            rowNames : ["A", "B", "C", "D", "E", "F", "G", "H"],
            columnNames : ["01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"]
        }, options );

        var applyCategory = function (wellSelector, data, options) {
            wellSelector.css("background-color", "");
            var nan = isNaN(data.value);
            console.log("isNan: " + nan + " " + data.toString());

            if (!nan && data.toString().indexOf('0.') != -1) {
                data = parseFloat(data) * 100;
                data = data.toFixed(2);
                console.log("new data: " + data);
            }
            var eval = nan ? "equality" : "compare";
            wellSelector.removeClass('success warning error');
            for (var i = 0; i < options.length; i++) {
                var opt = options[i];
                if (eval === 'compare') {
                    if (data.value >= opt.value) {
                        wellSelector.css('background-color', opt.color);
                        break;
                    }
                } else if (eval === 'equality') {
                    if (data.value  === opt.value) {
                        wellSelector.css('background-color', opt.color);
                        break;
                    }
                }
            }
            wellSelector.hover(function(){
                var metadataDl = $('#sampleMetadataList');
                console.log(data.metadata);
                metadataDl.empty();
                if (data.metadata != undefined) {
                    $.each(data.metadata, function (idx, metadata) {
                        var dt = $('<dt></dt>').text(metadata.label);
                        var dd = $('<dd></dd>').text(metadata.value);
                        metadataDl.append(dt);
                        metadataDl.append(dd);
                    });
                }
            }, function () {
                var metadataDl = $('#sampleMetadataList');
                metadataDl.empty();
            });
        };

        // Build select
        var selectContainer = $('<div></div>').addClass('row');
        var spacer = $('<div></div>').addClass('span2');
        selectContainer.append(spacer);
        var metricNames = settings.datasets.map(function(data){return data.label;});
        var sel = $('<select>').attr('id', 'heatField');
        sel.append($("<option>").attr('value', '').text(''));
        $(metricNames).each(function(idx, value) {
            sel.append($("<option>").attr('value', value).text(value));
        });
        selectContainer.append(sel);
        this.append(selectContainer);

        // Handle selected metric change
        sel.change(function() {
            var selectedField = this.value;
            var result = $.grep(settings.datasets, function(e){ return e.label === selectedField; });
            if (result.length === 1) {
                var dataset = result[0];
                console.log(dataset);
                var type = dataset.type;
                var applyFunction = null;
                switch(type) {
                    case "Category":
                        applyFunction = applyCategory;
                    default:
                        applyFunction = applyCategory;
                }
                $.each(dataset.data, function (idx, wellValue) {
                    var well = $('#' + wellValue.well);
                    well.text(wellValue.value);
                    function sortNumber(a,b) {
                        return b.value - a.value;
                    }
                    var sortedOptions = dataset.options.sort(sortNumber);
                    applyFunction(well, wellValue, sortedOptions);
                });

                // Create legend
                var legend = $('#legend');
                legend.empty();
                var legendList = $('<ul></ul>').addClass('legend');
                for (var i = 0; i < dataset.options.length; i++) {
                    var li = $('<li></li>');
                    var opt = dataset.options[i];
                    var span = $('<span></span>');
                    span.css('background-color', opt.color);
                    li.text(opt.name);
                    li.prepend(span);
                    legendList.append(li);
                }
                legend.append(legendList);
            } else {
                $.error("Metric not found in datasets: " + selectedField);
            }
        });

        // Container
        var container = $('<div></div>').addClass('row');
        this.append(container);

        // Build legend
        var legendContainer = $('<div></div>').addClass('span2');
        container.append(legendContainer);

        var legend = $('<div></div>').attr('id', 'legend');
        legendContainer.append(legend);

        // Build Sample Metadata list
        var dl = $('<dl></dl>');
        dl.attr('id', 'sampleMetadataList');
        legendContainer.append(dl);

        var tableContainer = $('<div></div>').addClass('span10');
        container.append(tableContainer);

        // Build Table
        var table = $('<table></table>').addClass('platemap table table-bordered table-condensed');
        var headerTr = $('<tr></tr>');
        table.append(headerTr);
        var blankTh = $('<th></th>').addClass('fit');
        headerTr.append(blankTh);
        tableContainer.append(table);

        $.each(settings.columnNames, function(idx, value){
            var th = $('<th></th>').addClass('fit').text(value);
            headerTr.append(th);
        });

        $.each(settings.rowNames, function (rowIndex, row) {
            var tr = $('<tr></tr>');
            var rowNameTh = $('<th></th>').append(row);
            tr.append(rowNameTh);
            $.each(settings.columnNames, function(colIdx, col){
                var td = $('<td></td>').addClass('heatable').attr('id', row + col);
                tr.append(td);
            });
            table.append(tr);
        });

        return this;
    };

}( jQuery ));
