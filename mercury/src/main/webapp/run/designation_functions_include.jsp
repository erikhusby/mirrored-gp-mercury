
            // Makes a row be selected when one of its inputs was changed.
            function rowUpdated(rowIndex) {
                $j('#checkbox_' + rowIndex).prop('checked', true);
                rowSelected();
            };

            // Enables/disables buttons depending on checkbox count.
            function rowSelected() {
                numberSelected = $('.shiftCheckbox:checked').length;
                if (numberSelected > 0) {
                    $j('#setMultipleBtn').removeAttr("disabled");
                    $j('#createFctBtn').removeAttr("disabled");
                } else {
                    $j('#setMultipleBtn').attr("disabled", "disabled");
                    $j('#createFctBtn').attr("disabled", "disabled");
                }
            }

            // Copies the value of a control to a corresponding hidden input and then clears the control.
            // This decoupling of checkboxes and selects is necessary because if Stripes updates the
            // action bean using the control's name attribute, then Stripes repopulates the control in
            // the subsequent page and it cannot be cleared by the jsp.

            function updateHiddenInputs() {
                $j('.shiftCheckbox').each(function(idx, element) {
                    itemValue = $j(element).prop('checked');
                    hiddenInputId = '#' + $j(element).attr('id') + '_Input';
                    $j(hiddenInputId).prop('value', itemValue);
                    $j(element).removeAttr('checked');
                });

                $j('.multiEditSelect').each(function(idx, element) {
                    itemValue = $j(element).prop('value');
                    hiddenInputId = '#' + $j(element).attr('id') + 'Input';
                    $j(hiddenInputId).prop('value', itemValue);
                    $j(element).prop('value', '');
                });
            }

