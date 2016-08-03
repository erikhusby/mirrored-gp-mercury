
(function ($) {
    return pasteToSelect = {
        initialize: function (tableSelector, options) {
            var columnNames = options.columnNames;
            var noun = options.noun;
            var pluralNoun = options.pluralNoun;
            if (pluralNoun === undefined) {
                pluralNoun = noun + "s";
            }
            $("body").append(createBarcodeEntryForm(pluralNoun, columnNames.join(" or ")));
            dialog = $("#listOfBarcodesForm").dialog({
                autoOpen: false,
                height: 400,
                width: 250,
                modal: true,
                buttons: {
                    "ApplyBarcodesButton": {
                        id: "applyBarcodes",
                        text: "Apply barcodes",
                        click: function () {
                            applyBarcodes(tableSelector, columnNames, noun, pluralNoun);
                        }
                    },
                    Cancel: function () {
                        dialog.dialog("close");
                    }
                }
            });

            $("#PasteBarcodesList").on("click", function () {
                // if (!isBucketEmpty()) {
                clearBarcodesDialog();
                dialog.dialog("open");
                // }
                // else {
                //     alert("There are no samples in the bucket.");
                // }
            });

            hideBarcodeEntryDialog();
        }
    };

    function hideBarcodeEntryDialog() {
        $('#barcodeErrors').hide();
        $('#noEntriesErrors').hide();
        $('#ambiguousEntriesErrors').hide();
    }


    /**
     *  Find the column index for supplied column header. Table columns are zero based.
     */
    function findColumnIndexForHeader(columnHeader) {
        return $("#bucketEntryView").find("tr th").filter(function () {
                return $(this).text() === columnHeader;
            }).index() + 1;
    }

    function applyBarcodes(tableSelector, columns, noun, pluralNoun) {
        if (!Array.isArray(columns)) {
            columns = new Array(columns);
        }
        clearBarcodesDialogErrors();

        var hasBarcodes = barcodes.value.trim().length > 0;
        var splitBarcodes = jQuery.unique(barcodes.value.trim().split(/\s+|,/));

        if (!hasBarcodes) {
            alert("No barcodes were entered.");
            return;
        }

        var foundBarcodes = [];
        var notFoundBarcodes = [];
        var ambiguousBarcodes = [];
        splitBarcodes.forEach(function (barcode){
            foundBarcodes[barcode] = false;
            notFoundBarcodes[barcode] = false;
            ambiguousBarcodes[barcode] = false;
            columns.forEach(function(column) {
                if (!foundBarcodes[barcode]) {
                    var columnIndex = findColumnIndexForHeader(column);
                    if (columnIndex < 0) {
                        // developer error
                        return;
                    }
                    var matchingRows = $(tableSelector + "tr>td:nth-child($columnIndex):contains($barcode$)".replace("$columnIndex", columnIndex).replace("$barcode$", barcode));
                    selector = $('td:first', $(matchingRows[columnIndex]).parents('tr'));
                    var numMatchingRows = matchingRows.length;
                    switch (numMatchingRows) {
                        case 0:
                            notFoundBarcodes[barcode] = true;
                            break;
                        case 1:
                            foundBarcodes[barcode] = true;
                            var selector = $('td:first', $(matchingRows[0]).parents('tr'));
                            selector.find(":checkbox").prop('checked', true);
                            selector.trigger('click');
                            break;
                        default:
                            ambiguousBarcodes[barcode] = true;
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
                addAmbiguousBucketEntryError(key);
                ambiguous = true;
            }
        });

        if (ambiguous || notFound) {
            if (ambiguous) {
                showAmbiguousBucketEntryErrors();
            }
            if (notFound) {
                shownoEntriesErrors();
            }
        } else {
            stripesMessage.set(Object.keys(foundBarcodes).length + " " + pluralNoun+ " successfully chosen.");
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

    function addAmbiguousBucketEntryError(barcode) {
        appendRow($('#ambiguousEntries').find('tbody'), barcode);
    }

    function showBarcodeErrors() {
        $('#barcodeErrors').show();
    }

    function shownoEntriesErrors() {
        showBarcodeErrors();
        $('#noEntriesErrors').show();
    }

    function showAmbiguousBucketEntryErrors() {
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

    function createBarcodeEntryForm(pluralNoun, searchColumnLabel) {
        var outerDiv = $("<div>", {id: 'listOfBarcodesForm'});
        var form = $("<form>");
        var barcodeTextAreaDiv = $("<div>", {class: 'control-group'});

        barcodeTextAreaDiv.append($("<label>",
            {for: 'barcodes', class: 'control-label', text: 'Enter ' + searchColumnLabel +', one per line'}));

        barcodeTextAreaDiv.append($("<textarea>",
            {name: 'barcodes', id: 'barcodes', class: 'defaultText', cols: 12, rows: 5}));

        form.append(barcodeTextAreaDiv);

        var errorMessage = "We're sorry, but Mercury could not automatically choose " + pluralNoun + " because of the following errors.";
        var barcodeErrorsDiv = $("<div>", {id: 'barcodeErrors'}).append($("<p>", {errorMessage}));
        barcodeErrorsDiv.append(errorDiv("noEntries", "No " + pluralNoun));
        barcodeErrorsDiv.append(errorDiv("ambiguousEntries", "Ambiguous " + pluralNoun));
        form.append(barcodeErrorsDiv);
        outerDiv.append(form);
        outerDiv.hide();
        return outerDiv;
    }

})(jQuery);
