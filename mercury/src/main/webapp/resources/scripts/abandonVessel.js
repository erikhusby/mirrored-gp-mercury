;(function ( $, window, document, undefined ) {
    var pluginName = 'layoutMap',
        defaults = {
            logEnabled : true,
            emptyClass: 'emptyCell',
            abandonedClass: 'abandonedCell',
            unabandonedClass: 'unabandonedCell'
        };

    // The actual plugin constructor
    function Plugin( element, options ) {
        this.element = element;
        this.options = $.extend( {}, defaults, options);
        this.layout = this.options.layout;
        this.emptyClass = this.options.emptyClass;
        this.abandonedClass = this.options.abandonedClass;
        this.unabandonedClass = this.options.unabandonedClass;
        this._defaults = defaults;
        this._name = pluginName;
        this.init();
    }

    Plugin.prototype.init = function() {
        this.initLayout();
    };

    Plugin.prototype.initLayout = function () {
        var plugin = this;
        var layoutJson = plugin.layout;
        var headerBarcode = layoutJson.containerBarcode;
        $.each(layoutJson.abandonCells, function( idx, abandonCell ) {
            var cell = $('#cell_' + abandonCell.row + "_" + abandonCell.column);
            if( cell.length == 0 ) {
                return;
            }
            cell.data('abandonData', abandonCell);
            if (abandonCell.barcode === headerBarcode) {
                // Only show container positions for plates etc.
                cell.html(abandonCell.vesselPosition);
            } else {
                // Otherwise show tube barcode
                cell.html(abandonCell.barcode);
            }
            if (abandonCell.empty) {
                cell.attr('class', plugin.emptyClass);
            } else if (!abandonCell.abandoned) {
                cell.attr('class', plugin.unabandonedClass);
            } else {
                cell.attr('class', plugin.abandonedClass);
                cell.html(cell.html() + '<br/>' + abandonCell.abandonReasonDisplay);
            }
        });

        // Event handlers for table, row, and column selectors
        $.each( $('#layoutMap th'), function( idx, cell ) {
            if( cell.id.indexOf('col_') == 0 ) {
                $(cell).click(plugin.selectColumn);
            } else if ( cell.id.indexOf('row_') == 0 ) {
                $(cell).click(plugin.selectRow);
            } else if ( cell.id === 'tableSelector' ) {
                $(cell).click(plugin.selectAll);
            }
        } );

        $.each( $('#layoutMap td'), function( idx, cell ) {
            if( cell.id.indexOf('cell_') == 0 ) {
                $(cell).click(plugin.selectCell);
            }
        } );
    };

    Plugin.prototype.selectColumn = function(event) {
        var jqSrcCell = $(event.delegateTarget);
        if( jqSrcCell.attr('id').indexOf('col_') < 0 ) {
            return;
        }
        var colId = jqSrcCell.attr('id').replace('col_', '');
        var doSelect = toggleBulkSelector( jqSrcCell );
        $.each( $('#layoutMap tr'), function( idx_r, row ) {
            var child = $(row).find('th');
            if( child.length == 0 || child[0].id.indexOf('row_') < 0 ) return;
            var rowId = child[0].id.replace('row_', '');
            $.each( row.children, function( idx_c, cell ) {
                if ( cell.id === 'cell_' + rowId + '_' + colId ) {
                    var jqCell = $(cell);
                    toggleSelected( jqCell, doSelect );
                }
            } );
        } );
    };

    Plugin.prototype.selectRow = function(event) {
        var jqSrcCell = $(event.delegateTarget);
        if( jqSrcCell.attr('id').indexOf('row_') < 0 ) {
            return;
        }
        var doSelect = toggleBulkSelector( jqSrcCell );
        $.each( jqSrcCell.siblings(), function( idx, cell ) {
            var jqCell = $(cell);
            toggleSelected( jqCell, doSelect );
        } );
    };

    Plugin.prototype.selectCell = function(event) {
        var jqCell = $(event.delegateTarget);
        if( jqCell.attr('id').indexOf('cell_') < 0 ) {
            return;
        }
        if( jqCell.data('abandonData').empty ) return;
        var cssClass = jqCell.attr( 'class');
        var doSelect = ( cssClass.indexOf('cellSelected') < 0 );
        toggleSelected( jqCell, doSelect );
    };

    Plugin.prototype.selectAll = function(event) {
        var jqSrcCell = $(event.delegateTarget);
        if( jqSrcCell.attr('id') !== 'tableSelector' ) {
            return;
        }
        var doSelect = toggleBulkSelector( jqSrcCell );

        $.each( $('#layoutMap td'), function( idx, cell ) {
            if( cell.id.indexOf('cell_') < 0 ) {
                return;
            }
            var jqCell = $(cell);
            toggleSelected( jqCell, doSelect )
        } );
    };

    function toggleSelected( jqCell, doSelect ) {
        if( jqCell.data('abandonData').empty ) return;
        // Toggle
        var cssClass = jqCell.attr( 'class' );
        var selectedClass = 'cellSelected';
        if( doSelect && cssClass.indexOf(selectedClass) < 0 ) {
            jqCell.attr( 'class', cssClass + ' ' + selectedClass );
        }
        if( !doSelect ) {
            jqCell.attr( 'class', cssClass.replace( ' ' + selectedClass, '' ) );
        }
    }

    function toggleBulkSelector( jqCell ) {
        var selectedClass = 'cellSelected';
        var unSelectedClass = 'cellUnSelected';
        var cssClass = jqCell.attr( 'class' );
        if( cssClass === selectedClass ) {
            jqCell.attr( 'class', unSelectedClass );
            return false;
        }
        if( cssClass === unSelectedClass ) {
            jqCell.attr( 'class', selectedClass );
            return true;
        }
    }

    Plugin.prototype.log = function(msg) {
        if (this.options.logEnabled && typeof console != "undefined") {
            try {
                console.log(msg);
            } catch (e) {
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