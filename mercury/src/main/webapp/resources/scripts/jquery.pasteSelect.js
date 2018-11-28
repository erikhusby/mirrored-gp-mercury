
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

/**
 * This jQuery extension allows the user to check a row for a value in specified column.
 * The checkbox is assumed to be in the first column.
 */
(function ($) {
    $.fn.pasteSelect = function (options) {
        var settings = $.extend({
            columnNames: options.columnNames,
            noun: options.noun,
            pluralNoun: options.pluralNoun
        });
        if (!Array.isArray(settings.columnNames)) {
            settings.columnNames = new Array(settings.columnNames);
        }
        if (!settings.pluralNoun) {
            settings.pluralNoun = settings.noun + "s";
        }
        settings.searchColumnLabel = settings.columnNames.join(" or ");

        return this.each(function () {
            var table = $(this);
            $("body").append(createBarcodeEntryForm());
            var applyButtonName = "Check " + settings.pluralNoun;
            var dialog = $("#listOfBarcodesForm").dialog({
                autoOpen: false,
                height: 400,
                width: 250,
                modal: true,
                buttons: {
                    "ApplyBarcodesButton": {
                        id: "applyBarcodes",
                        text: applyButtonName,
                        click: function () {
                            applyBarcodes(table);
                        }
                    },
                    Cancel: function () {
                        hideBarcodeEntryDialog();
                        dialog.dialog("close");
                    }
                }
            });

            $("#PasteBarcodesList").on("click", function () {
                // if (!isBucketEmpty()) {
                if ($(table).find("tbody>tr").length == 0) {
                    alert(settings.pluralNoun + " is empty.");
                } else {
                    clearBarcodesDialog();
                    stripesMessage.clear();
                    dialog.dialog("open");
                }
            });


            function hideBarcodeEntryDialog() {
                $('#barcodeErrors').hide();
                $('#noEntriesErrors').hide();
                $('#ambiguousEntriesErrors').hide();
            }

            function applyBarcodes(table) {
                clearBarcodesDialogErrors();
                var barcodes = $("#barcodes").val();
                var hasBarcodes = barcodes.trim().length > 0;
                var splitBarcodes = jQuery.unique(barcodes.trim().split(/\s+|,/));

                if (!hasBarcodes) {
                    alert("No " + settings.searchColumnLabel + " were entered.");
                    return;
                }

                var foundBarcodes = [];
                var notFoundBarcodes = [];
                var ambiguousBarcodes = [];
                splitBarcodes.forEach(function (barcode) {
                    foundBarcodes[barcode] = false;
                    notFoundBarcodes[barcode] = false;
                    ambiguousBarcodes[barcode] = false;
                    settings.columnNames.forEach(function (column) {
                        if (!foundBarcodes[barcode] || !notFoundBarcodes[barcode] || !ambiguousBarcodes[barcode]) {
                            var columnIndex = $(table).columnIndexOfHeader(column);
                            if (columnIndex < 0) {
                                // developer error
                                return;
                            }
                            var matchingRows = $(table).find("tr>td:nth-child($columnIndex$):contains('$barcode$')"
                                .replace("$columnIndex$", columnIndex)
                                .replace("$barcode$", barcode));
                            selector = $('td:first', $(matchingRows[columnIndex]).parents('tr'));
                            var numMatchingRows = matchingRows.length;
                            switch (numMatchingRows) {
                                case 0:
                                    if (!foundBarcodes[barcode]) {
                                        notFoundBarcodes[barcode] = true;
                                    }
                                    break;
                                case 1:
                                    var selector = $("td:first", matchingRows.parent("tr"));
                                    foundBarcodes[barcode] = selector;

                                    ambiguousBarcodes[barcode] = false;
                                    notFoundBarcodes[barcode] = false;
                                    break;
                                default:
                                    ambiguousBarcodes[barcode] = true;
                                    foundBarcodes[barcode] = false;
                                    notFoundBarcodes[barcode] = false;
                            }
                        }
                    });
                });
                var ambiguous = false;
                var notFound = false;
                Object.keys(notFoundBarcodes).forEach(function (key) {
                    if (notFoundBarcodes[key]) {
                        addBarcodeNotFoundError(key);
                        notFound = true;
                    }
                });
                Object.keys(ambiguousBarcodes).forEach(function (key) {
                    if (ambiguousBarcodes[key]) {
                        addAmbiguousEntryError(key);
                        ambiguous = true;
                    }
                });

                if (ambiguous || notFound) {
                    if (ambiguous) {
                        showAmbiguousErrors();
                    }
                    if (notFound) {
                        shownoEntriesErrors();
                    }
                } else {
                    Object.keys(foundBarcodes).forEach(function (key) {
                        var checkbox = $(foundBarcodes[key]);
                        checkbox.find(":checkbox").prop('checked', true);
                        checkbox.click();
                    });

                    stripesMessage.set($(table).find("input:checked").length + " " + settings.pluralNoun + " chosen.");
                    dialog.dialog("close");
                }
            }


            function appendRow(jquery, barcode) {
                var row = $("<tr>").append($("<td>", {text: barcode}));
                jquery.append(row);
            }

            function addBarcodeNotFoundError(barcode) {
                appendRow($('#noEntries').find('tbody'), barcode);
            }

            function addAmbiguousEntryError(barcode) {
                appendRow($('#ambiguousEntries').find('tbody'), barcode);
            }

            function showBarcodeErrors() {
                $('#barcodeErrors').show();
            }

            function shownoEntriesErrors() {
                showBarcodeErrors();
                $('#noEntriesErrors').show();
            }

            function showAmbiguousErrors() {
                showBarcodeErrors();
                $('#ambiguousEntriesErrors').show();
            }

            function clearBarcodesDialogErrors() {
                $('#noEntries').find('tbody tr').remove();
                $('#ambiguousEntries').find('tbody tr').remove();

                $('#noEntriesErrors').hide();
                $('#ambiguousEntriesErrors').hide();
            }

            function clearBarcodesDialog() {
                $('#barcodes').val('');
                $('#barcodeErrors').hide();
                clearBarcodesDialogErrors();
            }

            function errorDiv(id, text) {
                var errorDiv = $("<div></div>", {id: id + "Errors"}).hide();
                var errorTable = $("<table></table>", {id: id}).prepend($("<tbody>"));
                var tHead = $("<thead></thead>");
                var tableRow = $("<tr>");
                var tableHeader = $("<th>", {text: text});

                tableRow.prepend(tableHeader);
                tHead.prepend(tableRow);
                errorTable.prepend(tHead);
                errorDiv.prepend(errorTable);
                return errorDiv;
            }

            function createBarcodeEntryForm() {
                var outerDiv = $("<div>", {id: 'listOfBarcodesForm'});
                var form = $("<form>");
                var barcodeTextAreaDiv = $("<div>", {class: 'control-group'});

                barcodeTextAreaDiv.append($("<label>",
                    {for: 'barcodes', class: 'control-label', text: 'Enter ' + settings.searchColumnLabel + ', one per line'}));

                barcodeTextAreaDiv.append($("<textarea>",
                    {name: 'barcodes', id: 'barcodes', class: 'defaultText', cols: 12, rows: 5}));

                form.append(barcodeTextAreaDiv);

                var errorMessage = "We're sorry, but Mercury could not automatically choose " + settings.pluralNoun + " because of the following errors.";
                var barcodeErrorsDiv = $("<div>", {id: 'barcodeErrors'}).append($("<p>", {errorMessage}));
                barcodeErrorsDiv.append(errorDiv("noEntries", "No " + settings.pluralNoun));
                barcodeErrorsDiv.append(errorDiv("ambiguousEntries", "Ambiguous " + settings.pluralNoun));
                form.append(barcodeErrorsDiv);
                outerDiv.append(form);
                outerDiv.hide();
                return outerDiv;
            }
        });
    }
})(jQuery);
