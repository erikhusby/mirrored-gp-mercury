<%--<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.BillingLedgerActionBean" %>--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry" %>

<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.BillingLedgerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Billing Ledger: ${actionBean.productOrder.jiraTicketKey}: ${actionBean.productOrder.title}" sectionTitle="Billing Ledger: ${actionBean.productOrder.jiraTicketKey}: ${actionBean.productOrder.title}">

<stripes:layout-component name="extraHead">
    <script src="${ctxpath}/resources/scripts/hSpinner.js"></script>
    <script src="${ctxpath}/resources/scripts/modalMessages.js"></script>
    <script src="${ctxpath}/resources/scripts/jquery.ajaxMultiQueue.js"></script>

<%-- ================ Page-specific CSS ================ --%>

    <style type="text/css">
        #filters {
            margin-top: 6px;
            margin-bottom: 6px;
        }
        #filters .nav {
            margin-bottom: 5px;
        }
        #filters .nav li.dropdown {
            margin-right: 1em;
        }
        #filters .nav li {
            font-size: 14px;
        }
        #filters .nav li.dropdown > span {
            font-size: 14px;
        }
        #filters .nav > li > a {
            font-size: 14px;
            padding-top: 6px;
            padding-bottom: 6px;
            padding-left: 8px;
            padding-right: 8px;
            text-decoration: none;
        }
        #filters .nav .dropdownLabel {
            line-height: 14px;
            padding-top: 6px;
            padding-bottom: 6px;
            margin-top: 2px;
            margin-bottom: 2px;
        }
        #filters .dropdown-menu a {
            clear: none;
            font-size: 14px;
        }
        #filters .dropdown-menu .checkContainer {
            float: left;
            padding-left: 3px;
            padding-top: 2px;
        }

        /* Prevent sample rows from wrapping so that they're all the same height. */
        .table th, .table td {
            white-space: nowrap;
        }

        /* Allow wrapping for popovers (on-risk hover) and ledger detail rows (specifically Billing Message). */
        .table td .popover,
        .table td .subTable td {
            white-space: normal;
        }

        .table td.expand {
            cursor: pointer;
            text-align: center;
        }

        .table td.expand.loading {
            cursor: progress;
        }

        #ledger td.ledgerDetail {
            padding: 0 0 3px 10em;
        }

        /* Normalize coloring of ledger detail tables. The usual alternating rows and hover colors are not needed. */
        table.subTable thead tr {
            background: none;
        }
        table.subTable thead th {
            font-weight: normal;
        }
        tr.even table.subTable tr,
        tr.even table.subTable tr td.sorting_1,
        tr.even table.subTable tr td.sorting_2,
        tr.even table.subTable tr td.sorting_3
        {
            background-color: #fff;
        }
        tr.odd table.subTable tr,
        tr.odd table.subTable tr td.sorting_1,
        tr.odd table.subTable tr td.sorting_2,
        tr.odd table.subTable tr td.sorting_3
        {
            background-color: #f5f5f5;
        }

        table.simple tr.abandoned {
            background-color: #FF99CC;
        }

        .dateComplete {
            text-align: center;
            width: 7em;
        }

        .ledgerQuantity {
            text-align: center;
            width: 4em;
        }

        .changed {
            background-color: lawngreen;
        }

        .oldDateComplete {
            background-color: #FFCC66;
        }

        .futureDateComplete {
            background-color: #FF9999;
        }

        .changed.ui-state-disabled,
        .changed.hasDatepicker:disabled {
            background-color: springgreen;
        }

        /* Mimic styles that would be applied if ui-state-disabled was correctly applied to datepicker widgets. */
        .hasDatepicker:disabled {
            opacity: .35;
        }

        .ledgerQuantity.pending, .unbilledStatus.pending {
            font-weight: bold;
        }

        .autowidth select {
            width: auto;
        }
        .processing {
            z-index: 200;
        }
        .dataTables_info {
            clear: none;
            padding-top: 5px;
            padding-left: 1em;
        }

        div.alert {
            margin: 2px;
        }

        .alert ul {
            margin-bottom: 0px;
        }
    </style>

<%-- ================ Page-specific JavaScript ================ --%>
    <script type="text/javascript">

        // Holds AJAX-fetched ledger data for the data table
        var ledgerDetails;

        var dateCompleteWarningThreshold = new Date(${actionBean.threeMonthsAgo});

        document.addEventListener('readystatechange', () => console.log('readyState:' + document.readyState + " " + performance.now()));

        $j(document).ready(function() {
            if ("${actionBean.successMessage}" !== ""){
                modalMessages("success").add("${actionBean.successMessage}");

            }
            /*
             * Extract the values from the quantity fields to use when sorting.
             */
            $j.fn.dataTableExt.afnSortData['input-value'] = function(oSettings, iDataColumn, iVisibleColumn) {
                return $j.map(oSettings.oApi._fnGetTrNodes(oSettings), function (tr, i) {
                    return $j('td:eq(' + iVisibleColumn + ') input:text', tr).val();
                });
            };

            /*
             * Configure ledger datatable.
             */
            enableDefaultPagingOptions();
            var ledgerQuantities = $j('input.ledgerQuantity');
            var ledgerTable = $j('#ledger').dataTable({
                <%-- copy/csv/print buttons don't work very will with the hidden columns and text inputs. Buttons need
                     to be overridden with custom mColumns and fnCellRender. Disabling this feature for now by removing
                     the "T" from the sDom string.
                'sDom': "<'row-fluid'<'span6'f><'span4'<'#dtButtonHolder'>><'span2'T>r>t<'row-fluid'<'span6'i><'span6'p>>",
                --%>
                'sDom': "<'row-fluid'<'span6'fi>><'row-fluid'<'span6 .autowidth'l><'span4 pull-right'<'#dtButtonHolder'>><'.processing'r>>t<'row-fluid'<'span6 autowidth' l><'span6'p>>",
                /*
                 * It's not useful to save filter state for this UI. Also, hSpinner widgets will not be initialized if
                 * they are filtered out of the DOM when the page loads
                 */
                "iDisplayLength": 50,
                'bStateSave': true,
                'aaSorting': [[2, 'asc']],
                'aoColumns': [
                    {'bSortable': false},                                                   // 0: checkbox
                    {'bVisible': false},                                                    // 1: search text
                    {'bSortable': true},                                                    // 2: sample position
                    {'bSortable': false},                                                   // 3: expand
                    {'bSortable': true},                                                    // 4: sample ID
                    {'bSortable': true},                                                    // 5: collaborator sample ID
                    {'bSortable': true},                                                    // 6: on risk
                    {'bSortable': true},                                                    // 7: status
                    {'bSortable': true},                                                    // 8: DCFM
                    {'bSortable': true, 'sSortDataType': 'input-value', 'sType': 'date'},   // 9: date complete

                    // price item columns
                    <c:forEach items="${actionBean.priceItems}" var="priceItem" varStatus="status">
                    {'bVisible': false},
                    {'bSortable': true, 'sSortDataType': 'input-value', 'sType': 'numeric'}, // ${10 + status.index}: ${priceItem.name}
                    </c:forEach>

                    {'bSortable': true, 'sType': 'title-string'}    // billed
                ],
                fnInitComplete: function(){
                    /*
                     * Set up hSpinner widgets for controlling ledger quantities.
                     */
                    ledgerQuantities.hSpinner({
                        additionalChangedSelector: "tr",
//                        initialVisibility: 'hidden',
                        originalValue: function (element) {
                            return element.querySelector('input[type="hidden"]').value;
                        },
                        incremented: function (event, inputName) {
                            console.time("incremented");
                            var $escaped = $j("#" + escapeForSelector(inputName));
                            updateUnbilledStatus($escaped);
                            applyToSelected(inputName, $escaped, 'increment');
                            updateSubmitButton();
                            console.timeEnd("incremented");
                        },
                        decremented: function (event, inputName) {
                            console.time("decremented");
                            var $escaped = $j("#" + escapeForSelector(inputName));
                            updateUnbilledStatus($escaped);
                            applyToSelected(inputName, $escaped, 'decrement');
                            updateSubmitButton();
                            console.timeEnd("decremented")
                        },
                        // hSpinner widget "input" event, not to be confused with the browser built-in DOM "input" event.
                        input: function (event, inputName) {
                            var $escaped = $j("#" + escapeForSelector(inputName));
                            updateUnbilledStatus($escaped);
                            applyToSelected(inputName, $escaped, 'setValue');
                            updateSubmitButton();
                        }
                    });

                    /**
                     * When rows are selected and one "date complete" is changed, change all selected rows.
                     */
                    $j('#ledger').on('change', '.dateComplete', function(event) {
                        var $selectedRows = $j(getSelectedRows());
                        var $input = $j(event.target);
                        var value = $input.val();
                        if ($selectedRows.length > 0) {
                            var inputName = $input.attr('name');
                            var $selectedInputs = $selectedRows.find('input.dateComplete:enabled');
                            for (var i = 0; i < $selectedInputs.length; i++) {
                                var $selectedInput = $selectedInputs.eq(i);
                                if ($selectedInput.attr('name') != inputName) {
                                    $selectedInput.val(value);
                                    var changed = value != $selectedInput.attr('originalValue');
                                    $selectedInput.toggleClass('changed', changed);
                                    $selectedInput.closest("tr").toggleClass('changed', changed);
                                    updateDateCompleteValidation($selectedInput);
                                }
                            }
                        }
                        var changed = value != $input.attr('originalValue');
                        $input.toggleClass('changed', changed);
                        $input.closest("tr").toggleClass('changed', changed);
                        updateDateCompleteValidation($input);

                        updateSubmitButton();
                    });
                }
            });
            // Reuse the existing filter input, but unbind its usual behavior and replace it with our own.
            $j(".dataTables_filter input").unbind('keyup');
            $j('.dataTables_filter input').on('input', function() {
                var tab = RegExp("\\t", "g");
                var filter = $j(this).val().trim().replace(tab, " ");
                var regexFilter = '.';
                if (filter != '') {
                    regexFilter = '(' + filter.split(' ').join('|') + ')';
                }
                ledgerTable.fnFilter(regexFilter, 1, true, false);
            });

            /*
             * Set up ledger datatable filters.
             */
            $j('.filterButtonSet').buttonset();

            // Show everything to start, but allocate array with locations for specific filters.
            ledgerTable.dataTableExt.afnFiltering = [allFilter, allFilter, allFilter, allFilter, allFilter];
            var filterIndexes = {
                'risk': 0,
                'coverage': 1,
                'billed': 2,
                'abandoned': 3,
                'modified': 4
            };
            $j('#filters .filterOption').click(function(event) {
                var $target = $j(event.target);
                var filterIndex = filterIndexes[$target.attr('name')];
                var filterFunction = window[$target.attr('value')];
                var filterText = $target.text()
                var pill = $target.parentsUntil('li.dropdown').siblings
                ('a');
                var check = $target.siblings('div');
                var allChecks = $target.parentsUntil('li.dropdown', 'ul.dropdown-menu').find('.checkContainer');
                pill.html(filterText + ' <b class="caret">');
                allChecks.html('');
                check.html('<i class="icon-ok"></i>');
                ledgerTable.dataTableExt.afnFiltering[filterIndex] = filterFunction;
                ledgerTable.fnDraw();
                event.preventDefault();
            });
            $j('#filters').show();

            /*
             * Set up on-risk hover.
             */
            $j('div.onRisk').popover();

            /*
             * Set up date pickers for date complete.
             */
            $j('.dateComplete').datepicker({ dateFormat: 'M d, yy', maxDate: 0 }).datepicker('refresh');

            // Update display styles for all date complete inputs when the page loads.
            var dateCompleteInputs = $j('.dateComplete');
            for (var i = 0; i < dateCompleteInputs.length; i++) {
                updateDateCompleteValidation(dateCompleteInputs.eq(i));
            }

            // Update display styles in response to change event fired after auto-fill.
            ledgerQuantities.on('change', function(event) {
                updateUnbilledStatus($j(event.target));
                updateSubmitButton();
            });

            // Update display styles for all quantities when the page loads.
            for (var i = 0; i < ledgerQuantities.length; i++) {
                updateUnbilledStatus(ledgerQuantities.eq(i));
            }

            function toggleHidden(row) {
                var $hspinner = $j(row).find(".hSpinner");
                if ($hspinner.hSpinner('option','hidden') === false){
                    $hspinner.hSpinner('option',{hidden:true});
                } else {
                    $hspinner.hSpinner('option',{hidden:false});
                }
            }

            $j("#ledger tbody tr").on('click', function(event){
                var $hSpinner = $j(".hSpinner");
                $hSpinner.filter(":visible").hSpinner('option',{hidden:true});
                toggleHidden(event.currentTarget, "toggle");
            });


            /*
             * Handle enable/disable of row inputs based on checkboxes.
             */
            $j('#ledger').on('click', 'input:checkbox', function () {
                console.time("onClick input:checkbox");
                var selectedRows = getSelectedRows();
                var unselectedRows = $j("#ledger tbody tr").not(selectedRows);
                if (selectedRows.length > 0) {
                    unselectedRows.filter(".dateComplete").datepicker().datepicker('disable')
                    unselectedRows.filter(".ledgerQuantity").hSpinner().hSpinner('disable')
                    var isChecked = function () {
                        $j(this).closest("tr").filter("input[name=selectedProductOrderSampleIds]:checked") != 0;
                    };
                    $j('input.dateComplete.pending').filter(isChecked()).datepicker().datepicker('enable');
                    $j('input.ledgerQuantity').filter(isChecked()).hSpinner().hSpinner('enable');
                } else {
                    var inputs = unselectedRows.find("input");
                    inputs.filter(".dateComplete.pending").datepicker().datepicker('enable')
                    inputs.filter(".ledgerQuantity").datepicker().datepicker('enable');
                }
                console.timeEnd("onClick input:checkbox");
            });

            /*
             * This page's DataTable reserves a spot, #dtButtonHolder (see its sDom property), above the table between
             * the filter input and download buttons. This one-liner moves the existing #dtButtons element into that
             * reserved spot.
             */
            $j('#dtButtons').detach().appendTo('#dtButtonHolder').show();

            $j('#autoFillButton').click(function(event) {
                event.preventDefault();
                autoFill();
            });

            $j('#ledgerForm').submit(function (event) {
                console.log("beginning");
                console.time("submit");
                $j('#updateLedgers').attr('disabled', true);


                var infoMessages = new modalMessages("info");
                infoMessages.add("Updating Ledgers...", "updateStatus");
//                var changedInputs;
//                var hiddenInputContainer = document.getElementById('hiddenRowInputs');
                try {
                    var allDataByRow={};
                    var allRows=[];
                    var changedRows = $j(ledgerTable.fnGetNodes()).filter('.changed');
                    var dom = changedRows.find("input").filter("[name^='ledgerData']").get();
                    console.time("copy inputs");
                    for (var i = dom.length - 1; i >= 0; i--) {
                        var input = {};
                        input['name'] = dom[i].name;
                        input['value'] = dom[i].value;
                        var rowNum=dom[i].getAttribute('data-rownum');
                        row = allDataByRow[rowNum];
                        if (row===undefined){
                            row=[];
                        }
                        row.push(input);
                        allDataByRow[rowNum] = row;
                        if (allRows.indexOf(rowNum)===-1){
                            allRows.push(rowNum);
                        }
                    }
                    console.timeEnd("copy inputs");
//                    var allData = $j(hiddenInputContainer).children();
                    event.preventDefault();

                    // get keys as an array;
                    var rowsRemaining = allRows.length;
                    var rowsCompleted = 0;
                    var queue = $j.ajaxMultiQueue(5);
                    while (allRows.length > 0) {
                        var submitRows = allRows.splice(0,1000);
                        var submitData=[];

                        submitRows.forEach(function(key){
                            allDataByRow[key].forEach(function (mapEntry) {
                                submitData = submitData.concat(mapEntry);
                            });
                        });


//                        var serializedData = $j(submitData).serializeArray();
                        if (allRows.length===0){
                            submitData.push({name:'redirectOnSuccess',value: true})
                        }

                        queue.queue({
                            url: '${ctxpath}/orders/ledger.action?updateLedgers&orderId=${actionBean.productOrder.businessKey}',
                            data: submitData,
                            type: 'post',
                            dataType: 'json',
                            success: function (json) {
                                var dataItems = json['data'];
                                var samples = [];
                                for (var i = 0; i < dataItems.length; i++) {
                                    samples.push(dataItems[i].sampleName);
                                }
                                var message = undefined;
                                rowsRemaining-=samples.length;
                                rowsCompleted += samples.length;
                                if (samples.length <= 20) {
                                    message = "Ledger data updated for ".concat(samples.join(", ")).concat(".");
                                } else {
                                    message = "Ledger data updated for ".concat(rowsCompleted).concat(" samples, ").concat(rowsRemaining).concat(" remaining.");
                                }

                                infoMessages.add(message, "updateStatus");
                                message = "Successfully updated ".concat(rowsCompleted).concat(" ledger entries.");
                                if (json.redirectOnSuccess) {
                                    modalMessages('success').add("Reloading page...");

                                    $j(".changed").removeClass("changed");
                                    setTimeout(function () {
                                        window.location.replace("${ctxpath}/orders/ledger.action?orderId=${actionBean.productOrder.businessKey}&successMessage="+message)
                                    }, 5000);
                                }
                            },
                            error: function (json) {
                                errorMessages = new modalMessages("error");
                                console.log(json);
                                if (json.responseJSON) {

                                    var errors = json.responseJSON['error'];
                                    for (var i = 0; i < errors.length; i++) {
                                        errorMessages.add(errors[i]);
                                    }
                                    if (errors.length === 0) {
                                        errorMessages.add("Unknown Error: ", json.statusText);
                                    }
                                }
                            }
                        });
                    }
                } catch (e) {
                    errorMessages = new modalMessages("error");
                    var errorMessage = "Error collecting ledger entries: '" + e + "'";
                    errorMessages.add(errorMessage);
                }
            });

            $j('#helpButton').click(function() {
                $j('#helpDialog').dialog({
                    draggable: false,
                    modal: true,
                    resizable: false,
                    width: 800
                });
            });

            /*
             * Data for this table comes from DOM and is initially hidden and shown only after applying DataTables. This
             * avoids extra rendering and repositioning/flickering.
             */
            $j('#ledger').show();

            // Enable the submit button if there are changes from a previous form submit that had validation errors
            updateSubmitButton();

            $j("td.expand").on('click', function(){
                var $cell = $j(this);
                var parentRow = this.parentNode;
                var productOrderSampleId = $j(parentRow).find("input[name='selectedProductOrderSampleIds']").val()
                var detailTable = $j(parentRow).next().find(".subTable");
                var icon = $j('i', this);
                if (detailTable.length === 0) {
                    $cell.addClass('loading');
                    $j.ajax({
                        url: '${ctxpath}/orders/ledger.action',
                        data: {
                            ledgerDetails: '',
                            orderId: '${actionBean.productOrder.businessKey}',
                            productOrderSampleId: productOrderSampleId
                        },
                        dataType: 'json',
                        success: function (data) {
                            ledgerDetails = data;
                            detailTable = $j('<table class="subTable" style="width: 100%;"><thead><tr><th>Price Item</th><th>Quantity</th><th>Quote</th><th>Work Complete</th><th>Billing Session</th><th>Billed Date</th><th>Billing Message</th></tr></thead><tbody></tbody></table>');
                            detailTable.dataTable({
                                aaData: ledgerDetails,
                                "aaSorting": [[5, 'asc']]
                            });
                            var detailRow = ledgerTable.fnOpen(parentRow, detailTable, 'ledgerDetail');
                            $j(detailRow).addClass(parentRow.className);
                            icon.removeClass('icon-plus-sign');
                            icon.addClass('icon-minus-sign');
                            $cell.removeClass('loading');
                        }
                    });
                } else {
                    ledgerTable.fnClose(parentRow, detailTable, 'ledgerDetail');
                    icon.removeClass('icon-minus-sign');
                    icon.addClass('icon-plus-sign');
                }
            });
        });

        /*
         * Filter functions
         */
        var numPriceItems = ${actionBean.priceItems.size()};

        var allFilter = function(oSettings, aData, iDataIndex) { return true; };

        var riskFilter = function(oSettings, aData, iDataIndex) {
            return aData[6] != '';
        };

        var noRiskFilter = function(oSettings, aData, iDataIndex) {
            return !riskFilter(oSettings, aData, iDataIndex);
        };

        var coverageMetFilter = function(oSettings, aData, iDataIndex) {
            return aData[8] != '';
        };

        var notCoverageMetFilter = function(oSettings, aData, iDataIndex) {
            return !coverageMetFilter(oSettings, aData, iDataIndex);
        };

        var billedFilter = function(oSettings, aData, iDataIndex) {
            // Assumes that the content is rendered as something indicating a check mark, i.e., "check.png"
            return aData[10 + numPriceItems * 2].indexOf('check') != -1;
        };

        var notBilledFilter = function(oSettings, aData, iDataIndex) {
            return !billedFilter(oSettings, aData, iDataIndex);
        };

        var abandonedFilter = function(oSettings, aData, iDataIndex) {
            return aData[7] == 'Abandoned';
        };

        var notAbandonedFilter = function(oSettings, aData, iDataIndex) {
            return !abandonedFilter(oSettings, aData, iDataIndex);
        };

        var modifiedFilter = function(oSettings, aData, iDataIndex) {
            /*
             * aData contains a copy of the original cell content which will not have any updates to the text inputs.
             * Therefore, we need to look at the actual rows to extract the current value.
             */
            var row = oSettings.oApi._fnGetTrNodes(oSettings)[iDataIndex];
            var $children = $j(row).children();

            for (var i = 0; i < numPriceItems; i++) {
                // aData has all data, including hidden cells, so a little math is needed.
                var original = parseFloat(aData[10 + i*2]);
                // cells contains only the visible cells, so no need to skip over the hidden ones.
                var current = parseFloat($children.eq(9 + i).find('input:text').val());
                if (original != current) {
                    return true;
                }
            }
            return false;
        };

        var notModifiedFilter = function(oSettings, aData, iDataIndex) {
            return !modifiedFilter(oSettings, aData, iDataIndex);
        };

        function updateSubmitButton() {
<c:if test="${!actionBean.productOrderListEntry.billing}">
            var changedInputs = $j('input.changed');
            $j('#updateLedgers').attr('disabled', changedInputs.length == 0);
</c:if>
        }

        function getSelectedRows() {
            return $j('#ledger tbody input[name=selectedProductOrderSampleIds]:checked').closest("tr");
        }

        function applyToSelected(inputName, input, action) {
            /*
             * Select starting with the checked samples, find the parent rows, find the child inputs for the
             * same price item, but not the current input, and apply the action.
             */
            var selectedRows = getSelectedRows();
            var priceItemId = input.attr('priceItemId');
            var value = input.val();
            var $quantityInputs = selectedRows.find('input.ledgerQuantity[priceItemId=' + priceItemId + ']');
            for (var i = 0; i < $quantityInputs.length; i++) {
                var $quantityInput = $quantityInputs.eq(i);
                var name = $quantityInput.attr('name');
                if (name != inputName) {
                    $quantityInput.hSpinner(action, value);
                    updateUnbilledStatus($j('#' + escapeForSelector(name)));
                }
            }
        }

        /**
         * Updates the style for date complete inputs based on range validation rules.
         */
        function updateDateCompleteValidation($input) {
            var value = $input.val();
            var date = new Date(value);
            $input.toggleClass('oldDateComplete', date.getTime() < dateCompleteWarningThreshold.getTime());
            $input.toggleClass('futureDateComplete', date.getTime() > new Date().getTime());
        }

        /**
         * Updates display/style for a sample based on whether or not there is (or will be after any modifications are
         * saved) any ledger entries that have not been billed yet. This is indicated both as a style on the quantity
         * and as an asterisk in the "Billed" column. This is an indication that whether or not the sample is billed is
         * dependent on the pending processing of the unbilled ledger entries.
         */
        function updateUnbilledStatus($input) {
            var hasUnbilledQuantity = parseFloat($input.val()) != parseFloat($input.attr('billedQuantity'));
            $input.toggleClass('pending', hasUnbilledQuantity);
            var row = $input.parentsUntil('tbody', 'tr');
            var hasUnbilledQuantityForAnyPriceItem = row.find('input.ledgerQuantity.pending').length > 0;
            row.find('.unbilledStatus').text(hasUnbilledQuantityForAnyPriceItem ? '*' : '');
            var dateComplete = row.find('.dateComplete');
            dateComplete.toggleClass('pending', hasUnbilledQuantityForAnyPriceItem);
            var datePicker = dateComplete.datepicker();
            if (hasUnbilledQuantityForAnyPriceItem) {
                datePicker.datepicker('enable');
            } else {
                datePicker.datepicker('disable');
            }
        }

        function autoFill() {
            $j('#ledger .autoFillQuantity').each(function() {
                var $autoFillValueInput = $j(this);
                var $ledgerQuantityInput = $autoFillValueInput.parent().find('.ledgerQuantity');
                $ledgerQuantityInput.hSpinner('setValue', $autoFillValueInput.val());
                updateUnbilledStatus($ledgerQuantityInput)
            });
            updateSubmitButton();
        }

        function escapeForSelector(value) {
            // https://learn.jquery.com/using-jquery-core/faq/how-do-i-select-an-element-by-an-id-that-has-characters-used-in-css-notation/
            <%--
              - Literal concatenation is being used here to avoid a bug in the EL implementation and/or spec regarding
              - how to handle "\$" when not followed by "{". Currently, running on JBoss AS 7, the behavior is to turn
              - "\\$" into "\$", which is not desired, and doesn't seem necessary since there is not a "{" after the
              - "$". In order to avoid this problem (now and in the future if we move to an application server with
              - different behavior), the "\" and "$" are being split up in the source and rejoined at runtime.
              -
              - See https://bz.apache.org/bugzilla/show_bug.cgi?id=57136 for more (confusing) information.
              --%>
            return value.replace(/(:|\.|\[|\]|,)/g, '\\' + '$1');
        }
    </script>
</stripes:layout-component>

<stripes:layout-component name="content">

    <div id="helpDialog" title="Help" style="display: none">
        <h4>Introduction</h4>
        <p>This interface allows you to request billing for samples in a product order. Changes made here will be
            eligible for billing to Broad Quotes/SAP through a new billing session.</p>

        <h4>Sorting and Filtering</h4>
        <p>Samples can be sorted by clicking on any column header. Shift-click to sort on multiple columns.</p>
        <p>Buttons are provided to filter samples by:</p>
        <ul>
            <li>On Risk</li>
            <li>Coverage Met</li>
            <li>Billed</li>
            <li>Abandoned</li>
            <li>Modified (since page load)</li>
        </ul>
        <p>Also, the "Filter" input will filter samples by Sample ID and Collaborator Sample ID. Space-separated filter
            terms are independent, so you can find multiple samples by entering multiple IDs (paste in a list of sample
            IDs, for example).</p>

        <h4>Making Changes</h4>
        <p>For each sample, there are inputs for:</p>
        <ul>
            <li>Primary Price Item</li>
            <li>Replacement Price Item (if applicable)</li>
            <li>Add-on Price Items (only those selected for this order)</li>
        </ul>
        <p>
            The quantities represent the desired quantity to be billed for each sample and price item, including amounts
            that have been billed to Broad Quotes/SAP and amounts that are still pending. Changes to these quantities
            will queue billing actions to Broad Quotes/SAP.
        </p>

        <h4>Auto Fill</h4>
        <p>The <b>Auto Fill</b> button will fill in any billing quantities that Mercury believes should be billed. The
            quantities will be entered into the inputs as if you had made the changes yourself, allowing you to review
            Mercury's decisions before saving them.</p>
        <p>Currently, the determination is based on coverage having been met (DCFM).</p>

        <h4>Columns</h4>
        <ul>
            <li><b>Sample Information</b>
                <ul>
                    <li><b>Checkbox</b>: Select samples for bulk updates. Shift-click to select a range of samples.
                        Clicking the checkbox in the header will select all/none. The header will also display the
                        number of samples selected.</li>
                    <li><b>Sample #</b>: The position of each sample in the order.</li>
                    <li><b>Expand Ledger Details</b>: Clicking the <i class="icon-plus-sign"></i> icon will show all of
                        the ledger entries for the sample. Note that modifications on the page will not appear in the
                        details until they are saved.</li>
                    <li><b>Sample ID</b>: The Broad sample ID.</li>
                    <li><b>Collaborator Sample ID</b>: The externally assigned sample ID.</li>
                    <li><b>On Risk</b>: A <img src="${ctxpath}/images/check.png"> is displayed if the sample is on risk.
                        Hover over the check mark to see the risk details.</li>
                    <li><b>Status</b>: Shows whether or not the sample has been "Abandoned" on this order.</li>
                    <li><b>DCFM</b>: Date Coverage First Met. This is the same information that is available in
                        Tableau.</li>
                </ul>
            </li>
            <li><b>Billing</b>
                <ul>
                    <li><b>Date Complete</b>: Select the date the work was complete. This is the date that will be sent
                        to Broad Quotes/SAP for billing. If there is no pending billing, this is pre-populated with
                        DCFM.</li>
                    <li><b>Price Item(s)</b>: Enter the desired quantities for each price item.</li>
                    <li><b>Billed</b>: A <img src="${ctxpath}/images/check.png"> is displayed when the primary (or a
                        replacement) price item has been billed to Broad Quotes/SAP. A "<span class="pending">*</span>"
                        indicates that there are ledger changes that have not been billed that may affect this
                        status.</li>
                </ul>
            </li>
        </ul>
    </div>

<%-- ================ Product Order Information ================ --%>

    <div class="row-fluid">
        <div class="span3"><span class="label-form">Product</span><br>${actionBean.productOrder.product.name}</div>
        <div class="span3"><span class="label-form">Price Item</span><br>${actionBean.productOrder.determinePriceItemByCompanyCode(actionBean.productOrder.product).name}</div>
        <div class="span2"><span class="label-form">Quote</span><br>${actionBean.productOrder.quoteId}</div>
        <div class="span1">${actionBean.productOrder.samples.size()} samples</div>
        <div class="span2" style="text-align: right">
            <c:choose>
                <c:when test="${actionBean.productOrderListEntry.readyForReview}">
                        <span class="badge badge-warning">
                            <%=ProductOrderListEntry.LedgerStatus.READY_FOR_REVIEW.getDisplayName()%>
                        </span>
                </c:when>
                <c:when test="${actionBean.productOrderListEntry.billing}">
                        <span class="badge badge-info">
                            <%=ProductOrderListEntry.LedgerStatus.BILLING_STARTED.getDisplayName()%>
                        </span>
                </c:when>
                <c:when test="${actionBean.productOrderListEntry.readyForBilling}">
                        <span class="badge badge-success">
                            <%=ProductOrderListEntry.LedgerStatus.READY_TO_BILL.getDisplayName()%>
                        </span>
                </c:when>
            </c:choose>
        </div>
        <div class="span1" style="text-align: right"><button id="helpButton" class="btn btn-small"><i class="icon-question-sign"></i></button></div>
    </div>
    <c:if test="${actionBean.productOrder.addOnList != 'no Add-ons'}">
        <div class="row-fluid">
            <div class="span12"><span class="label-form">Add-Ons:</span> ${actionBean.productOrder.addOnList}</div>
        </div>
    </c:if>

<%-- ================ Ledger Datatable and Form ================ --%>

    <stripes:form id="ledgerForm" action="/orders/ledger.action">

        <%-- Hidden form data for order context --%>
        <stripes:hidden name="orderId"/>

        <%-- If any rows are hidden due to filtering or paging, the hidden input values will not be submitted. This
             could lead to action bean errors and requested changes not being applied. Therefore, when the form is
             submitted, inputs from hidden rows will be gathered and moved into this div. --%>
        <div id="hiddenRowInputs" style="display: none;"></div>

        <stripes:hidden name="changedDate"/>
        <stripes:hidden name="renderedPriceItemNames"/>

        <%-- Datatable filters --%>
        <div id="filters" class="row-fluid" style="display: none;">
            <div class="span11">
                <ul class="nav nav-pills">
                    <li class="dropdownLabel">Risk:</li>
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" href="#">Any <b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><div class="checkContainer"><i class="icon-ok"></i></div><a tabindex="-1" href="#" class="filterOption" name="risk" value="allFilter">Any</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="risk" value="riskFilter">Yes</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="risk" value="noRiskFilter">No</a></li>
                        </ul>
                    </li>
                    <li class="dropdownLabel">Coverage:</li>
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" href="#">Any <b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><div class="checkContainer"><i class="icon-ok"></i></div><a tabindex="-1" href="#" class="filterOption" name="coverage" value="allFilter">Any</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="coverage" value="coverageMetFilter">Met</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="coverage" value="notCoverageMetFilter">Not Met</a></li>
                        </ul>
                    </li>
                    <li class="dropdownLabel">Billed:</li>
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" href="#">Any <b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><div class="checkContainer"><i class="icon-ok"></i></div><a tabindex="-1" href="#" class="filterOption" name="billed" value="allFilter">Any</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="billed" value="billedFilter">Yes</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="billed" value="notBilledFilter">No</a></li>
                        </ul>
                    </li>
                    <li class="dropdownLabel">Abandoned:</li>
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" href="#">Any <b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><div class="checkContainer"><i class="icon-ok"></i></div><a tabindex="-1" href="#" class="filterOption" name="abandoned" value="allFilter">Any</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="abandoned" value="abandonedFilter">Yes</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="abandoned" value="notAbandonedFilter">No</a></li>
                        </ul>
                    </li>
                    <li class="dropdownLabel">Modified:</li>
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" href="#">Any <b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><div class="checkContainer"><i class="icon-ok"></i></div><a tabindex="-1" href="#" class="filterOption" name="modified" value="allFilter">Any</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="modified" value="modifiedFilter">Yes</a></li>
                            <li><div class="checkContainer"></div><a tabindex="-1" href="#" class="filterOption" name="modified" value="notModifiedFilter">No</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
            <div class="span1" style="text-align: right;">
                <c:choose>
                    <c:when test="${actionBean.productOrderListEntry.billing}">
                        <button id="updateLedgers" class="btn" title="No updates allowed while billing is in progress" disabled><strike>Update</strike></button>
                    </c:when>
                    <c:otherwise>
                        <input type="submit" id="updateLedgers" name="updateLedgers" value="Update" class="btn btn-primary" disabled>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- Container for button(s) to display in the datatable header. Initially hidden so it's only displayed once
             the datatable is ready to be rendered, especially for large tables. --%>
        <div id="dtButtons" style="text-align: right; display: none;">
            <button id="autoFillButton" class="btn btn-mini">Auto Fill</button>
        </div>
    </stripes:form>
        <%-- The actual ledger table. Initially hidden to avoid rendering the table before the datatable widget is
             ready. --%>
        <table id="ledger" class="table simple" style="display: none">
        <thead>
            <tr>
                <th colspan="4"></th>
                <th colspan="5" style="text-align: center">Sample Information</th>
                <th colspan="${actionBean.priceItems.size() * 2 + 2}" style="text-align: center">Billing</th>
            </tr>
            <tr>
                <th>
                    <input id="checkAllSamples" for="count" type="checkbox" class="checkAll"/>
                    <span id="count" class="checkedCount"></span>
                </th>
                <th></th>
                <th></th>
                <th></th>
                <th>Sample ID</th>
                <th>Collaborator Sample ID</th>
                <th style="text-align: center">On Risk</th>
                <th style="text-align: center">Status</th>
                <th title="Date Coverage First Met">DCFM</th>
                <th style="text-align: center">Date Complete</th>
                <c:forEach items="${actionBean.priceItems}" var="priceItem">
                    <th>Original value for ${priceItem.name}</th>
                    <th style="text-align: center">${priceItem.name}</th>
                </c:forEach>
                <th style="text-align: center">Billed</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${actionBean.productOrderSampleLedgerInfos}" var="info">
                <tr class="${info.sample.deliveryStatus.displayName == 'Abandoned' ? 'abandoned' : ''}">
                    <td>
                        <input type="checkbox" title="${info.sample.samplePosition}" class="shiftCheckbox" name="selectedProductOrderSampleIds" value="${info.sample.productOrderSampleId}">
                    </td>
                    <td>
                        ${info.sample.name}
                        ${info.sample.sampleData.collaboratorsSampleName}
                    </td>
                    <td style="text-align: right">
                        ${info.sample.samplePosition + 1}
                    </td>
                    <td class="expand">
                        <i class="icon-plus-sign"></i>
                    </td>
                    <td>
                        ${info.sample.name}
                        <input type="hidden" data-rownum = "${info.sample.samplePosition}"
                               name="ledgerData[${info.sample.samplePosition}].sampleName"
                               value="${info.sample.name}"/>
                    </td>
                    <td>
                        ${info.sample.sampleData.collaboratorsSampleName}
                    </td>
                    <td style="text-align: center">
                        <c:if test="${info.sample.onRisk}">
                            <div class="onRisk" title="On Risk Details for ${info.sample.name}" rel="popover" data-trigger="hover" data-placement="right" data-html="true" data-content="<div style='text-align: left'>${info.sample.riskString}</div>">
                                <img src="${ctxpath}/images/check.png"> ...
                            </div>
                        </c:if>
                    </td>
                    <td style="text-align: center">
                        ${info.sample.deliveryStatus.displayName}
                    </td>
                    <td style="text-align: center">
                        <fmt:formatDate value="${info.coverageFirstMet}"/>
                    </td>
                    <td style="text-align: center">
                        <c:set var="submittedCompleteDate"
                               value="${actionBean.ledgerData[info.sample.samplePosition].completeDateFormatted}"/>
                        <c:set var="currentValue"
                               value="${submittedCompleteDate != null ? submittedCompleteDate : info.dateCompleteFormatted}"/>
                        <input name="ledgerData[${info.sample.samplePosition}].workCompleteDate"
                               value="${currentValue}" data-rownum = "${info.sample.samplePosition}"
                               originalValue="${info.dateCompleteFormatted}"
                               class="dateComplete ${currentValue != info.dateCompleteFormatted ? 'changed' : ''}">
                    </td>

                    <c:forEach items="${actionBean.priceItems}" var="priceItem">
                        <td>
                                ${info.getTotalForPriceItem(priceItem)}
                        </td>
                        <td style="text-align: center">
                            <input type="hidden" data-rownum = "${info.sample.samplePosition}"
                                   name="ledgerData[${info.sample.samplePosition}].quantities[${priceItem.priceItemId}].originalQuantity"
                                   value="${info.getTotalForPriceItem(priceItem)}"/>
                            <c:set var="submittedQuantity" value="${actionBean.ledgerData[info.sample.samplePosition].quantities[priceItem.priceItemId].submittedQuantity}"/>
                            <input id="ledgerData[${info.sample.samplePosition}].quantities[${priceItem.priceItemId}].submittedQuantity"
                                   name="ledgerData[${info.sample.samplePosition}].quantities[${priceItem.priceItemId}].submittedQuantity"
                                   value="${submittedQuantity != null ? submittedQuantity : info.getTotalForPriceItem(priceItem)}"
                                   class="ledgerQuantity" data-rownum = "${info.sample.samplePosition}"
                                   priceItemId="${priceItem.priceItemId}"
                                   billedQuantity="${info.getBilledForPriceItem(priceItem)}">
                            <c:if test="${priceItem == actionBean.productOrder.determinePriceItemByCompanyCode(actionBean.productOrder.product) && info.autoFillQuantity != 0}">
                                <input type="hidden"
                                       name="${info.sample.samplePosition}-autoFill-${info.sample.name}"
                                       value="${info.autoFillQuantity}"
                                       class="autoFillQuantity">
                            </c:if>
                        </td>
                    </c:forEach>

                    <td style="text-align: center">
                        <c:if test="${info.sample.completelyBilled}">
                            <img src="${ctxpath}/images/check.png" title="Yes">
                        </c:if>
                        <span class="unbilledStatus pending"></span>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

</stripes:layout-component>
</stripes:layout-render>
