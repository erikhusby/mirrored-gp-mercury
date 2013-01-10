    /*
 * Useful jQuery functions that are simple (and too annoying) to make into many different 
 * jQuery plugins.  Many of these are commonly used and wrapping them into a single script
 * speeds up the loading time of the page.
 * 
 * The descriptions of how to use each function is within the comment code for the plugin
 * but you will see the list of what's contained in this file right here:
 * 
 * - SingleSubmit
 * - Checkbox Range Selection
 * - Checkbox Counter 
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */

(function ($) {
    $.fn.extend({
        /*
         * SingleSubmit - jQuery multiple submit prevention plugin
         *
         * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
         *
         * Prevent double clicking on a form's button by adding some class to the form
         * (i.e. class="single-submit") and add to your page's javascript:
         *      $j(".single-submit").singleSubmit({ });
         */

        //pass the options variable to the function
        singleSubmit:function (options) {
            //Set the default values, use comma to separate the settings
            var $defaults = {
                waitText:'Please wait... '
            };

            var $options = $.extend($defaults, options);

            return this.each(function () {
                var $o = $options;
                var $obj = $(this);

                $obj.find("input:submit").one("click", function (event) {
                    var target = event.target;
                    var submitName = target.name;
                    var submitValue = target.value;
                    var form = $(target).closest("form");
                    $(form).append("<input type=\"hidden\" name=\"" + submitName + "\" value=\"" + submitValue + "\">");
                    $(target).after($o.waitText);
                    $(target).prop("disabled", true);
                    $(form).submit();
                });
            });
        }
    });
})(jQuery);

(function ($) {
    var $defaults = {
        checkAllClass:'checkAll',
        countDisplayClass:'checkedCount',
        checkboxClass:'shiftCheckbox'
    };

    $.fn.extend({
        /*
         * Checkbox Range Selection - jQuery plugin to allow a multiple select of checkboxes using the Shift key.
         *
         * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
         * @author <a href="mailto:hrafal@broadinstitute.org">Howie Rafal</a>
         *
         * Enable checkbox range selection with a check all checkbox AND an automatic display of the number of items
         * checked. If you just use the defaults (above) all you have to do is call:
         *
         *                $j('.your-checkbox-class').enableCheckboxRangeSelection();
         *
         * and it will
         *
         *     1. look for all checkboxes with the class: shiftCheckbox and make that shift selectable
         *     2. Make any checkbox with the class: checkAll and have that select all values
         *     3. Take any DOM element with the class: checkedCount and drop in the number of items selected
         *
         * If you need multiple classes to work or want to define your own versions, you can do that by passing
         * in overrides to the options like:
         *
         *     			$j('input.validator-checkbox').enableCheckboxRangeSelection({
         *                      checkAllClass : 'validator-checkAll',
         *                      countDisplayClass : 'validator-checkedCount',
         *                      checkboxClass : 'validator-checkbox'
         *              });
         */
        //pass the options variable to the function
        updateCheckCount:function (options) {
            $('.' + options.countDisplayClass).text($("input." + options.checkboxClass + ":checked").size());
        },

        checkAll:function (input, options) {
            $('.' + options.checkboxClass).attr('checked', input.checked);
            this.updateCheckCount(options);
        },

        enableCheckboxRangeSelection:function (options) {
            var $lastCheckbox = null;

            //Set the default values, use comma to separate the settings
            var $options = $.extend([], $defaults, options);
            $options.rangeSelection = this;

            $("input." + $options.checkAllClass).click(function (e) {
                $options.rangeSelection.checkAll(this, $options);
            });

            $options.rangeSelection.updateCheckCount($options);

            return this.each(function () {
                var obj = $(this);

                obj.click(function (e) {
                    // get the current full list and assume on any reorder the lastCheckbox will be reset by a user click
                    var currentList = $('.' + $options.checkboxClass);
                    var currentIndex = currentList.index(e.target);

                    if ($lastCheckbox != null && e.shiftKey) {
                        currentList.slice(
                                Math.min($lastCheckbox, currentIndex),
                                Math.max($lastCheckbox, currentIndex) + 1
                        ).attr({checked:e.target.checked ? "checked" : ""});
                    }

                    $options.rangeSelection.updateCheckCount($options);
                    $lastCheckbox = currentIndex;
                });
            });
        }
    });
})(jQuery);


// extend sorting for datatables to allow for title sorting
jQuery.extend( jQuery.fn.dataTableExt.oSort, {
    "title-string-pre": function ( a ) {
        var matchingArray = a.match(/title="(.*?)"/);
        if (matchingArray == null) {
            return '';
        }

        if (matchingArray.length < 2) {
            return '';
        }

        return matchingArray[1].toLowerCase();
    },

    "title-string-asc": function ( a, b ) {
        return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    },

    "title-string-desc": function ( a, b ) {
        return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    },

    "title-numeric-pre": function ( a ) {
        var x = a.match(/title="*(-?[0-9\.]+)/)[1];
        return parseFloat( x );
    },

    "title-numeric-asc": function ( a, b ) {
        return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    },

    "title-numeric-desc": function ( a, b ) {
        return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    }
});

