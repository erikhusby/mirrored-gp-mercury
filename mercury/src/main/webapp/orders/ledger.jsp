<%--<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.BillingLedgerActionBean" %>--%>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.BillingLedgerActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Billing Ledger: ${actionBean.productOrder.jiraTicketKey}: ${actionBean.productOrder.title}" sectionTitle="Billing Ledger: ${actionBean.productOrder.jiraTicketKey}: ${actionBean.productOrder.title}">

<stripes:layout-component name="extraHead">
    <script src="${ctxpath}/resources/scripts/hSpinner.js"></script>

<%-- ================ Page-specific CSS ================ --%>

    <style type="text/css">
        .table th, .table td {
            white-space: nowrap;
        }
        .table td.expand {
            text-align: center;
        }

        #ledger td.ledgerDetail {
            padding: 0 0 3px 10em;
        }
        table.subTable {

        }
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

        .changed.ui-state-disabled {
            background-color: springgreen;
        }

        .pending {
            font-weight: bold;
        }

/*
        .dataTables_filter input {
            width: 300px;
        }
*/
    </style>

<%-- ================ Page-specific JavaScript ================ --%>

    <script type="text/javascript">

        // Holds AJAX-fetched ledger data for the data table
        var ledgerDetails;

        $j(document).ready(function() {

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
            var ledgerTable = $j('#ledger').dataTable({
                'oTableTools': ttExportDefines,
//                'sDom': sDomNoTableToolsButtons,
                'sDom': "<'row-fluid'<'span6'f><'span4'<'#dtButtonHolder'>><'span2'T>r>t<'row-fluid'<'span6'i><'span6'p>>",
                'aaSorting': [[1, 'asc']],
                'aoColumns': [
                    {'bSortable': false},                                                   // 0: checkbox
                    {'bSortable': true},                                                    // 1: sample position
                    {'bSortable': false},                                                   // 2: expand
                    {'bSortable': true},                                                    // 3: sample ID
                    {'bSortable': true},                                                    // 4: collaborator sample ID
                    {'bSortable': true},                                                    // 5: on risk
                    {'bSortable': true},                                                    // 6: status
                    {'bSortable': true},                                                    // 7: DCFM
                    {'bSortable': true, 'sSortDataType': 'input-value', 'sType': 'date'},   // 8: date complete

                    // price item columns
                    <c:forEach items="${actionBean.priceItems}" var="priceItem" varStatus="status">
                    {'bVisible': false},
                    {'bSortable': true, 'sSortDataType': 'input-value', 'sType': 'numeric'}, // ${9 + status.index}: ${priceItem.name}
                    </c:forEach>

                    {'bSortable': true, 'sType': 'title-string'}    // billed
                ]
            });

            /*
             * Set up ledger datatable filters.
             */
            $j('.filterButtonSet').buttonset();

            // Show everything to start, but allocate array with locations for specific filters.
            ledgerTable.dataTableExt.afnFiltering = [allFilter, allFilter, allFilter, allFilter];
            var filterIndexes = {
                'risk': 0,
                'coverage': 1,
                'billed': 2,
                'modified': 3
            };
            $j('#filters .filterOption').click(function(event) {
                var target = $j(event.target);
                var filterIndex = filterIndexes[target.attr('name')];
                var filterFunction = window[target.attr('value')];
                ledgerTable.dataTableExt.afnFiltering[filterIndex] = filterFunction;
                ledgerTable.fnDraw();
                event.preventDefault();
            });
            $j('#filters').show();

            /*
             * Set up ledger detail expand buttons.
             */
            $j('#ledger').on('click', 'td.expand', function() {
                var parentRow = this.parentNode;
                var icon = $j('i', this);
                if (ledgerTable.fnIsOpen(parentRow)) {
                    ledgerTable.fnClose(parentRow);
                    icon.removeClass('icon-minus-sign');
                    icon.addClass('icon-plus-sign');
                } else {
                    var position = $j(parentRow.children[1]).text();
                    var detailTable = $j('<table class="subTable" style="width: 100%;"><thead><tr><th>Price Item</th><th>Quantity</th><th>Quote</th><th>Work Complete</th><th>Billing Session</th><th>Billed Date</th><th>Billing Message</th></tr></thead><tbody></tbody></table>');
                    detailTable.dataTable({
                        aaData: ledgerDetails[position - 1],
                        "aaSorting": [[5, 'asc']]
                    });
                    var detailRow = ledgerTable.fnOpen(parentRow, detailTable, 'ledgerDetail');
                    $j(detailRow).addClass(parentRow.className);
                    icon.removeClass('icon-plus-sign');
                    icon.addClass('icon-minus-sign');
                }
            });

            /*
             * Set up on-risk hover.
             */
            $j('div.onRisk').popover();

            /*
             * Set up date pickers for date complete.
             */
            $j('.dateComplete').datepicker({ dateFormat: 'M d, yy' }).datepicker('refresh');

            /**
             * When rows are selected and one "date complete" is changed, change all selected rows.
             */
            $j('.dateComplete').change(function(event) {
                var $selectedRows = getSelectedRows();
                if ($selectedRows.length > 0) {
                    var $input = $j(event.target);
                    var inputName = $input.attr('name');
                    var value = $input.val();
                    var $selectedInputs = $selectedRows.find('input.dateComplete');
                    for (var i = 0; i < $selectedInputs.length; i++) {
                        var $selectedInput = $selectedInputs.eq(i);
                        if ($selectedInput.attr('name') != inputName) {
                            $selectedInput.val(value);
                        }
                    }
                }
            });

            /*
             * Set up hSpinner widgets for controlling ledger quantities.
             */
            var ledgerQuantities = $j('input.ledgerQuantity');
            ledgerQuantities.hSpinner({
                incremented: function(event, inputName) {
                    updateUnbilledStatus($j('#' + escapeForSelector(inputName)));
                    applyToSelected(inputName, 'increment');
                    updateSubmitButton();
                },
                decremented: function(event, inputName) {
                    updateUnbilledStatus($j('#' + escapeForSelector(inputName)));
                    applyToSelected(inputName, 'decrement');
                    updateSubmitButton();
                },
                // hSpinner widget "input" event, not to be confused with the browser built-in DOM "input" event.
                input: function(event, inputName) {
                    updateUnbilledStatus($j('#' + escapeForSelector(inputName)));
                    applyToSelected(inputName, 'setValue');
                    updateSubmitButton();
                }
            });

            // Update display styles in response to change event fired after auto-fill.
            ledgerQuantities.on('change', function(event) {
                updateUnbilledStatus($j(event.target));
                updateSubmitButton();
            });

            // Update display styles for all quantities when the page loads.
            for (var i = 0; i < ledgerQuantities.length; i++) {
                updateUnbilledStatus(ledgerQuantities.eq(i));
            }

            /*
             * Handle enable/disable of row inputs based on checkboxes.
             */
            $j('#ledger').on('click', 'input:checkbox', function() {
                var allRows = $j('#ledger tbody tr');
                var selectedRows = getSelectedRows();
                if (selectedRows.length > 0) {
                    allRows.find('input.dateComplete').datepicker().datepicker('disable');
                    allRows.find('input.ledgerQuantity').hSpinner().hSpinner('disable');
                    selectedRows.find('input.dateComplete').datepicker().datepicker('enable');
                    selectedRows.find('input.ledgerQuantity').hSpinner().hSpinner('enable');
                } else {
                    allRows.find('input.dateComplete').datepicker().datepicker('enable');
                    allRows.find('input.ledgerQuantity').hSpinner().hSpinner('enable');
                }
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

            $j('#ledgerForm').submit(function() {
                var hiddenInputs = $j(ledgerTable.fnGetHiddenNodes()).find('input');
                hiddenInputs.appendTo($j('#hiddenRowInputs'));
            })

            /*
             * Data for this table comes from DOM and is initially hidden and shown only after applying DataTables. This
             * avoids extra rendering and repositioning/flickering.
             */
            $j('#ledger').show();

            // Last thing to do on document-ready: AJAX-fetch ledger details
            $j.ajax({
                url: '${ctxpath}/orders/ledger.action',
                data: {
                    ledgerDetails: '',
                    orderId: '${actionBean.productOrder.businessKey}'
                },
                dataType: 'json',
                success: function (data) {
                    ledgerDetails = data;
                }
            });
        });

        /*
         * Filter functions
         */
        var numPriceItems = ${actionBean.priceItems.size()};

        var allFilter = function(oSettings, aData, iDataIndex) { return true; };

        var riskFilter = function (oSettings, aData, iDataIndex) {
            return aData[5] != '';
        };

        var noRiskFilter = function(oSettings, aData, iDataIndex) {
            return !riskFilter(oSettings, aData, iDataIndex);
        };

        var coverageMetFilter = function(oSettings, aData, iDataIndex) {
            return aData[7] != '';
        };

        var notCoverageMetFilter = function(oSettings, aData, iDataIndex) {
            return !coverageMetFilter(oSettings, aData, iDataIndex);
        };

        var billedFilter = function(oSettings, aData, iDataIndex) {
            // Assumes that the content is rendered as something indicating a check mark, i.e., "check.png"
            return aData[9 + numPriceItems * 2].includes('check');
        };

        var notBilledFilter = function (oSettings, aData, iDataIndex) {
            return !billedFilter(oSettings, aData, iDataIndex);
        };

        var modifiedFilter = function (oSettings, aData, iDataIndex) {
            /*
             * aData contains a copy of the original cell content which will not have any updates to the text inputs.
             * Therefore, we need to look at the actual rows to extract the current value.
             */
            var row = oSettings.oApi._fnGetTrNodes(oSettings)[iDataIndex];

            for (var i = 0; i < numPriceItems; i++) {
                // aData has all data, including hidden cells, so a little math is needed.
                var original = parseFloat(aData[9 + i*2]);
                // cells contains only the visible cells, so no need to skip over the hidden ones.
                var current = parseFloat($j(row).children().eq(9 + i).find('input:text').val());
                if (original != current) {
                    return true;
                }
            }
            return false;
        };

        function notModifiedFilter(oSettings, aData, iDataIndex) {
            return !modifiedFilter(oSettings, aData, iDataIndex);
        };

        function updateSubmitButton() {
            var changedInputs = $j('input.changed');
            $j('#updateLedgers').attr('disabled', changedInputs.length == 0);
        }

        function getSelectedRows() {
            return $j('input[name=selectedProductOrderSampleIds]:checked').parentsUntil('tbody', 'tr');
        }

        function applyToSelected(inputName, action) {
            /*
             * Select starting with the checked samples, find the parent rows, find the child inputs for the
             * same price item, but not the current input, and apply the action.
             */
            var selectedRows = getSelectedRows();
            var input = $j('#' + escapeForSelector(inputName));
            var priceItemId = input.attr('priceItemId');
            var value = input.val();
            var $quantityInputs = selectedRows.find('input.ledgerQuantity[priceItemId=' + priceItemId + ']');
            for (var i = 0; i < $quantityInputs.length; i++) {
                var $quantityInput = $quantityInputs.eq(i);
                var name = $quantityInput.attr('name');
                if (name != inputName) {
                    $quantityInput.hSpinner(action, value);
                }
                updateUnbilledStatus($j('#' + escapeForSelector(name)));
            }
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
            $input.parentsUntil('tbody', 'tr').find('.unbilledStatus').text(hasUnbilledQuantity ? '*' : '');
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

<%-- ================ Product Order Information ================ --%>

    <p>Product: ${actionBean.productOrder.product.name}</p>
    <p>Quote: ${actionBean.productOrder.quoteId}</p>
    <p>Number of Samples: ${actionBean.productOrder.samples.size()}</p>
    <p>Primary Price Item: ${actionBean.productOrder.product.primaryPriceItem.name}</p>
    <c:if test="${actionBean.productOrder.addOnList != 'no Add-ons'}">
        <p>Add-Ons: ${actionBean.productOrder.addOnList}</p>
    </c:if>

<%-- ================ Ledger Datatable and Form ================ --%>

    <stripes:form id="ledgerForm" action="/orders/ledger.action">

        <%-- Hidden form data for order context --%>
        <stripes:hidden name="orderId"/>

        <%-- Container for inputs that may be hidden because of datatable filters. If the rows are hidden, the form data
             will not be submitted. This could lead to requested changes not being applied. Also, the action bean
             expects there to be form data for all samples. --%>
        <div id="hiddenRowInputs" style="display: none;"></div>

        <%-- Datatable filters --%>
        <div id="filters" class="row-fluid" style="display: none;">
            <div class="span10">
                Risk:
                <span id="riskRadios" class="filterButtonSet">
                    <input type="radio" id="riskOption" name="risk" value="riskFilter" class="filterOption">
                    <label for="riskOption">Yes</label>
                    <input type="radio" id="noRiskOption" name="risk" value="noRiskFilter" class="filterOption">
                    <label for="noRiskOption">No</label>
                    <input type="radio" id="allRiskOption" name="risk" value="allFilter" class="filterOption" checked="checked">
                    <label for="allRiskOption">All</label>
                </span>
                Coverage:
                <span id="coverageRadios" class="filterButtonSet">
                    <input type="radio" id="coverageMetOption" name="coverage" value="coverageMetFilter" class="filterOption">
                    <label for="coverageMetOption">Met</label>
                    <input type="radio" id="notCoverageMetOption" name="coverage" value="notCoverageMetFilter" class="filterOption">
                    <label for="notCoverageMetOption">Not Met</label>
                    <input type="radio" id="allCoverageOption" name="coverage" value="allFilter" class="filterOption" checked="checked">
                    <label for="allCoverageOption">All</label>
                </span>
                Billed:
                <span id="billedRadios" class="filterButtonSet">
                    <input type="radio" id="billedOption" name="billed" value="billedFilter" class="filterOption">
                    <label for="billedOption">Billed</label>
                    <input type="radio" id="notBilledOption" name="billed" value="notBilledFilter" class="filterOption">
                    <label for="notBilledOption">Not Billed</label>
                    <input type="radio" id="allBilledOption" name="billed" value="allFilter" class="filterOption" checked="checked">
                    <label for="allBilledOption">All</label>
                </span>
                Modified:
                <span id="modifiedRadios" class="filterButtonSet">
                    <input type="radio" id="modifiedOption" name="modified" value="modifiedFilter" class="filterOption">
                    <label for="modifiedOption">Modified</label>
                    <input type="radio" id="notModifiedOption" name="modified" value="notModifiedFilter" class="filterOption">
                    <label for="notModifiedOption">Not Modified</label>
                    <input type="radio" id="allModifiedOption" name="modified" value="allFilter" class="filterOption" checked="checked">
                    <label for="allModifiedOption">All</label>
                </span>
            </div>
            <div class="span2" style="text-align: right;">
                <input type="submit" id="updateLedgers" name="updateLedgers" value="Update" class="btn btn-primary" disabled>
            </div>
        </div>

        <%-- Container for button(s) to display in the datatable header. Initially hidden so it's only displayed once
             the datatable is ready to be rendered, especially for large tables. --%>
        <div id="dtButtons" style="text-align: right; display: none;">
            <button id="autoFillButton" class="btn btn-mini">Auto Fill</button>
        </div>

        <%-- The actual ledger table. Initially hidden to avoid rendering the table before the datatable widget is
             ready. --%>
        <table id="ledger" class="table simple" style="display: none">
        <thead>
            <tr>
                <th colspan="3"></th>
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
                <tr>
                    <td>
                        <stripes:checkbox title="${info.sample.samplePosition}" class="shiftCheckbox" name="selectedProductOrderSampleIds" value="${info.sample.productOrderSampleId}"/>
                    </td>
                    <td style="text-align: right">
                        ${info.sample.samplePosition + 1}
                    </td>
                    <td class="expand">
                        <i class="icon-plus-sign"></i>
                    </td>
                    <td>
                        ${info.sample.name}
                        <stripes:hidden name="formData[${info.sample.samplePosition}].sampleName" value="${info.sample.name}"/>
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
                    <td>
                        <!-- status -->
                    </td>
                    <td>
                        <fmt:formatDate value="${info.coverageFirstMet}"/>
                    </td>
                    <td style="text-align: center">
                        <input name="formData[${info.sample.samplePosition}].workCompleteDate"
                               value="${info.dateComplete}"
                               class="dateComplete">
                    </td>

                    <c:forEach items="${actionBean.priceItems}" var="priceItem">
                        <td>
                                ${info.getTotalForPriceItem(priceItem)}
                        </td>
                        <td style="text-align: center">
                            <stripes:hidden name="formData[${info.sample.samplePosition}].quantities[${priceItem.priceItemId}].originalQuantity"
                                            value="${info.getTotalForPriceItem(priceItem)}"/>
                            <%--<input name="${info.sample.samplePosition}-${priceItem.priceItemId}-${info.sample.name}" value="${info.getTotalForPriceItem(priceItem)}" class="ledgerQuantity">--%>
                            <input id="formData[${info.sample.samplePosition}].quantities[${priceItem.priceItemId}].submittedQuantity"
                                   name="formData[${info.sample.samplePosition}].quantities[${priceItem.priceItemId}].submittedQuantity"
                                   value="${info.getTotalForPriceItem(priceItem)}"
                                   class="ledgerQuantity"
                                   priceItemId="${priceItem.priceItemId}"
                                   billedQuantity="${info.getBilledForPriceItem(priceItem)}">
                            <c:if test="${priceItem == actionBean.productOrder.product.primaryPriceItem && info.autoFillQuantity != 0}">
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
                        <span class="unbilledStatus"></span>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
    </stripes:form>

<%--
    <table id="ledger" class="table simple">
        <thead>
            <tr>
                <th>Sample</th>
                <th>Collaborator Sample ID</th>
                <th style="text-align: center">Primary Billed</th>
                <th colspan="6"></th>
            </tr>
            <tr>
                <th></th>
                <th>Price Item</th>
                <th style="text-align: right">Total Quantity</th>
                <th colspan="5"></th>
            </tr>
            <tr>
                <th></th>
                <th></th>
                <th>Billed Date</th>
                <th>Quote</th>
                <th style="text-align: right">Qty</th>
                <th style="text-align: center">Billed</th>
                <th>Billing Session</th>
                <th>Billing Message</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${actionBean.productOrderSampleLedgerInfos}" var="info">
                <tr>
                    <td>${info.sample.name}</td>
                    <td>${info.sample.sampleData.collaboratorsSampleName}</td>
                    <td style="text-align: center"><c:if test="${info.sample.completelyBilled}"><img src="${ctxpath}/images/check.png" title="Yes"></c:if></td>
                    <td colspan="5"></td>
                </tr>
                <c:forEach items="${actionBean.priceItems}" var="priceItem">
                    <c:if test="${info.ledgerEntriesByPriceItem.containsKey(priceItem)}">
                        <tr>
                            <td></td>
                            <td>${priceItem.displayName}</td>
                            <td style="text-align: right">${info.ledgerQuantities[priceItem].billed}</td>
                            <td colspan="5"></td>
                        </tr>
                        <c:forEach items="${info.ledgerEntriesByPriceItem.asMap()[priceItem]}" var="ledgerEntry">
                            <tr>
                                <td></td>
                                <td></td>
                                <td>${ledgerEntry.billingSession.billedDate}</td>
                                <td>${ledgerEntry.quoteId}</td>
                                <td style="text-align: right">${ledgerEntry.quantity}</td>
                                <td style="text-align: center"><c:if test="${ledgerEntry.billed}"><img src="${ctxpath}/images/check.png" title="Yes"></c:if></td>
                                <td>${ledgerEntry.billingSession.billingSessionId}</td>
                                <td>${ledgerEntry.billingMessage}</td>
                            </tr>
                        </c:forEach>
                    </c:if>
                </c:forEach>
            </c:forEach>
        </tbody>
    </table>
--%>

</stripes:layout-component>
</stripes:layout-render>
