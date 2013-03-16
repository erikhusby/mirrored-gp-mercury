/**
 * Various Bootstrap and DataTables interaction fixes and improvements.  It also includes specific DataTables
 * extended functionality.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */

/*
 * Define the TableTools export resources and types.
 */
var ttExportDefines = {
    "sSwfPath": "/Mercury/resources/scripts/DataTables-1.9.4/extras/TableTools/media/swf/copy_csv_xls.swf",
    "aButtons": [ "copy", "csv", "print" ]
};

/**
 *  Set the defaults for DataTables initialization
 */
$j.extend( true, $j.fn.dataTable.defaults, {
    "sDom": "<'row-fluid'<'span6'f><'span6'T>r>t<'row-fluid'<'span6'i><'span6'p>>",
    "bAutoWidth": false,
    "bInfo": false,
    "bStateSave": true,
    "bJQueryUI": false,
    "bPaginate": false,
    "sPaginationType": "bootstrap",
    "bLengthChange": false,
    "oLanguage": {
        "sLengthMenu": "_MENU_ records per page"
        }
} );


/* Default class modification */
$j.extend( $j.fn.dataTableExt.oStdClasses, {
    "sWrapper": "dataTables_wrapper form-inline"
} );

var filterDropdownHtml = "<div class='filterOptions'>using <select class='filterDropdown'><option value='all'>All of the words</option><option value='any'>Any of the words</option></select></div>";

/**
 * Dynamically add the HTML element for the dropdown and the choices, as well as define the
 * dropdown behavior when the user changes it (clicks on it and selects another item).
 *
 * @param oTable
 * @param tableID
 */
function includeAdvancedFilter(oTable, tableID) {
    $j(tableID + "_filter").append(filterDropdownHtml);
    $j(tableID + "_filter").find(".filterDropdown").change(function() {
        chooseFilterForData(oTable);
    });

    $j(".dataTables_filter input").keyup();
}

/**
 * Define the regular expression for the AND and OR filter.  The OR filter needs to create
 * the regex on the fly as the user adds characters into the filter input textbox.
 *
 * @param oTable
 */
function chooseFilterForData(oTable) {
    $j(".dataTables_filter input").unbind('keyup');
    $j(".dataTables_filter input").keyup(function() {
        var useOr = false;
        var dataTableName = $j(this).attr("aria-controls");
        if ($j("#" + dataTableName + "_filter").find(".filterDropdown").val() == "any") {
            useOr = true;
        }

        var filterInput = $j(".dataTables_filter input").val();
        if (useOr) {
            // OR
            var searchRegex = ".";
            if (filterInput != '') {
                var searchRegex = "(" + filterInput.trim().split(" ").join("+|") + "+)";
            }
            oTable.fnFilter( searchRegex, null, true, false );
        } else {
            // AND
            if (filterInput != '') {
                oTable.fnFilter(filterInput, null, false, true);
            }
        }
        $j(".dataTables_filter input").val( filterInput );
    });

    $j(".dataTables_filter input").keyup();
}


/* API method to get paging information */
$j.fn.dataTableExt.oApi.fnPagingInfo = function ( oSettings ) {
    return {
        "iStart":         oSettings._iDisplayStart,
        "iEnd":           oSettings.fnDisplayEnd(),
        "iLength":        oSettings._iDisplayLength,
        "iTotal":         oSettings.fnRecordsTotal(),
        "iFilteredTotal": oSettings.fnRecordsDisplay(),
        "iPage":          Math.ceil( oSettings._iDisplayStart / oSettings._iDisplayLength ),
        "iTotalPages":    Math.ceil( oSettings.fnRecordsDisplay() / oSettings._iDisplayLength )
    };
};


/* Bootstrap style pagination control */
$j.extend( $j.fn.dataTableExt.oPagination, {
    "bootstrap": {
        "fnInit": function( oSettings, nPaging, fnDraw ) {
            var oLang = oSettings.oLanguage.oPaginate;
            var fnClickHandler = function ( e ) {
                e.preventDefault();
                if ( oSettings.oApi._fnPageChange(oSettings, e.data.action) ) {
                    fnDraw( oSettings );
                }
            };

            $j(nPaging).addClass('pagination').append(
                '<ul>'+
                    '<li class="prev disabled"><a href="#">&larr; '+oLang.sPrevious+'</a></li>'+
                    '<li class="next disabled"><a href="#">'+oLang.sNext+' &rarr; </a></li>'+
                    '</ul>'
            );
            var els = $('a', nPaging);
            $j(els[0]).bind( 'click.DT', { action: "previous" }, fnClickHandler );
            $j(els[1]).bind( 'click.DT', { action: "next" }, fnClickHandler );
        },

        "fnUpdate": function ( oSettings, fnDraw ) {
            var iListLength = 5;
            var oPaging = oSettings.oInstance.fnPagingInfo();
            var an = oSettings.aanFeatures.p;
            var i, j, sClass, iStart, iEnd, iHalf=Math.floor(iListLength/2);

            if ( oPaging.iTotalPages < iListLength) {
                iStart = 1;
                iEnd = oPaging.iTotalPages;
            }
            else if ( oPaging.iPage <= iHalf ) {
                iStart = 1;
                iEnd = iListLength;
            } else if ( oPaging.iPage >= (oPaging.iTotalPages-iHalf) ) {
                iStart = oPaging.iTotalPages - iListLength + 1;
                iEnd = oPaging.iTotalPages;
            } else {
                iStart = oPaging.iPage - iHalf + 1;
                iEnd = iStart + iListLength - 1;
            }

            for ( i=0, iLen=an.length ; i<iLen ; i++ ) {
                // Remove the middle elements
                $('li:gt(0)', an[i]).filter(':not(:last)').remove();

                // Add the new list items and their event handlers
                for ( j=iStart ; j<=iEnd ; j++ ) {
                    sClass = (j==oPaging.iPage+1) ? 'class="active"' : '';
                    $('<li '+sClass+'><a href="#">'+j+'</a></li>')
                        .insertBefore( $('li:last', an[i])[0] )
                        .bind('click', function (e) {
                            e.preventDefault();
                            oSettings._iDisplayStart = (parseInt($('a', this).text(),10)-1) * oPaging.iLength;
                            fnDraw( oSettings );
                        } );
                }

                // Add / remove disabled classes from the static elements
                if ( oPaging.iPage === 0 ) {
                    $('li:first', an[i]).addClass('disabled');
                } else {
                    $j('li:first', an[i]).removeClass('disabled');
                }

                if ( oPaging.iPage === oPaging.iTotalPages-1 || oPaging.iTotalPages === 0 ) {
                    $j('li:last', an[i]).addClass('disabled');
                } else {
                    $j('li:last', an[i]).removeClass('disabled');
                }
            }
        }
    }
} );

/**
 * Clear the filter contents.
 *
 * @param oSettings
 */
$j.fn.dataTableExt.oApi.fnFilterClear = function (oSettings) {
    /* Remove global filter */
    oSettings.oPreviousSearch.sSearch = "";

    /* Remove the text of the global filter in the input boxes */
    if (typeof oSettings.aanFeatures.f != 'undefined') {
        var n = oSettings.aanFeatures.f;
        for (var i = 0, iLen = n.length; i < iLen; i++) {
            $('input', n[i]).val('');
        }
    }

    /* Remove the search text for the column filters - NOTE - if you have input boxes for these
     * filters, these will need to be reset
     */
    for (var i = 0, iLen = oSettings.aoPreSearchCols.length; i < iLen; i++) {
        oSettings.aoPreSearchCols[i].sSearch = "";
    }

    /* Redraw */
    oSettings.oApi._fnReDraw(oSettings);
};

/*
 * TableTools Bootstrap compatibility
 * Required TableTools 2.1+
 */
if ( $j.fn.DataTable.TableTools ) {
    // Set the classes that TableTools uses to something suitable for Bootstrap
    $j.extend( true, $j.fn.DataTable.TableTools.classes, {
        "container": "DTTT btn-group",
        "buttons": {
            "normal": "btn btn-mini",
            "disabled": "disabled"
        },
        "collection": {
            "container": "DTTT_dropdown dropdown-menu",
            "buttons": {
                "normal": "",
                "disabled": "disabled"
            }
        },
        "print": {
            "info": "DTTT_print_info modal"
        },
        "select": {
            "row": "active"
        }
    } );

    // Have the collection use a bootstrap compatible dropdown
    $j.extend( true, $j.fn.DataTable.TableTools.DEFAULTS.oTags, {
        "collection": {
            "container": "ul",
            "button": "li",
            "liner": "a"
        }
    } );
}
