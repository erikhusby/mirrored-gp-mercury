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
            $('.' + options.countDisplayClass).text($("input." + options.checkboxClass + ":checked").length);
        },

        checkAll:function (input, options) {
            $('.' + options.checkboxClass).prop("checked", input.checked);
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
                        ).prop("checked", e.target.checked);
                    }

                    $options.rangeSelection.updateCheckCount($options);
                    $lastCheckbox = currentIndex;
                });
            });
        }
    });
})(jQuery);

(function ($, undefined) {
    $.fn.clearable = function () {
        var $this = this;
        $this.wrap('<span class="clear-holder" />');
        var helper = $('<span class="clear-helper" title="Click to clear">&#215;</span>');
        $this.parent().append(helper);
        helper.click(function() {
            $this.val("").keyup();
        });
    };
})(jQuery);

/*
 * Compares strings of the form PDO-25 or RP-100, considering first the project name first
 * lexically and second the issue number numerically.
 */
function fn_title_jira_asc(a, b) {
    // JIRA ticket regexp.
    var re = /^([A-Z]+)-(\d+)$/;

    var aMatchingArray = a.match(re);
    var bMatchingArray = b.match(re);

    // If the array is null or the number of match groups is < 3, the match failed.
    var aMatchFail = (aMatchingArray == null || aMatchingArray.length < 3);
    var bMatchFail = (bMatchingArray == null || bMatchingArray.length < 3);

    // Do a simple lexical compare if both fail.
    if (aMatchFail && bMatchFail) {
        return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    }
    // Things that don't match the JIRA regexp always sort before things that do.
    else if (aMatchFail) {
        return -1;
    }
    else if (bMatchFail) {
        return 1;
    }

    // Compare projects first, lexically.
    var aProject = aMatchingArray[1];
    var bProject = bMatchingArray[1];
    if (aProject != bProject) {
        return ((aProject < bProject) ? -1 : ((aProject > bProject) ? 1 : 0));
    }

    // Drop back to numerical comparision of issue ids if the projects are the same.
    var aIssue = parseFloat(aMatchingArray[2]);
    var bIssue = parseFloat(bMatchingArray[2]);

    return ((aIssue < bIssue) ? -1 : ((aIssue > bIssue) ? 1 : 0));
}

// Simple ascending compare.
function fn_title_string_asc(a, b) {
    return ((a < b) ? -1 : ((a > b) ? 1 : 0));
}

// Simple ascending compare.
function fn_title_numeric_asc(aString, bString) {
    var a = parseFloat(aString);
    var b = parseFloat(bString);
    return ((a < b) ? -1 : ((a > b) ? 1 : 0));
}


// Pulls out a title attribute if any, otherwise returns the empty string.
function fn_title_pre(a) {

    // This regular expression looks for a word boundary, followed by the literal 'title'.  'title' may be followed
    // by zero or more whitespace characters, an equals sign, and zero or more whitespace characters again.  Next we
    // want to see either a single or double quote, followed by one or more characters that do not match that first
    // single or double quote, followed by a terminal quote that does match that first single or double quote.
    // Getting this regex to work in JavaScript required the use of a negative lookahead:
    //
    // http://stackoverflow.com/questions/8055727/negating-a-backreference-in-regular-expressions

    var regex = /\btitle\s*=\s*(["'])((?:(?!\1).)*)\1/;

    var matchingArray = a.match(regex);

    // If there is no title attribute simply return the empty string.
    if (matchingArray == null) {
        return '';
    }

    // There are two captures in the regex in addition to the global \0 capture.
    if (matchingArray.length < 3) {
        return '';
    }

    return matchingArray[2];
}

function fn_us_date_asc(a,b) {
    var x = new Date(a), y = new Date(b);
    return ((x < y) ? -1 : ((x > y) ?  1 : 0));
}

// Extend sorting for datatables to allow for title sorting.
jQuery.extend( jQuery.fn.dataTableExt.oSort, {

    "title-us-date-pre": function (a) {
        return fn_title_pre(a).toLowerCase();
    },

    "title-us-date-asc": function (a, b) {
        return fn_us_date_asc(a, b);
    },

    "title-us-date-desc": function (a, b) {
        return fn_us_date_asc(b, a);
    },

    "title-string-pre": function (a) {
        return fn_title_pre(a).toLowerCase();
    },

    "title-string-asc": function (a, b) {
        return fn_title_string_asc(a, b);
    },

    "title-string-desc": function (a, b) {
        return fn_title_string_asc(b, a);
    },

    "title-numeric-pre": function (a) {
        return fn_title_pre(a).toLowerCase();
    },

    "title-numeric-asc": function (a, b) {
        return fn_title_numeric_asc(a, b);
    },

    "title-numeric-desc": function (a, b) {
        return fn_title_numeric_asc(b, a);
    },

    "title-jira-pre": function (a) {
        return fn_title_pre(a).toUpperCase();
    },

    "title-jira-asc": function (a, b) {
        return fn_title_jira_asc(a, b);
    },

    "title-jira-desc": function (a, b) {
        return fn_title_jira_asc(b, a);
    }
});

/**
 *  Find the column index of the supplied column header.
 */
(function($) {
    $.fn.columnIndexOfHeader = function (columnHeader) {
        return $(this).find("tr th").filter(function () {
                return $(this).text() === columnHeader;
            }).index() + 1;
    };
})(jQuery);

(function ($) {
    return stripesMessage = {
        set: function (message, alertType, fieldSelector) {
            stripesMessage.clear();
            addStripesMessage(message, alertType, fieldSelector);
        },
        add: function (message, alertType, fieldSelector) {
            addStripesMessage(message, alertType, fieldSelector);
        },
        clear: function () {
            $("[class ^= 'alert']").remove();
        }
    };

    function getAlertClass(alertType) {
        if (alertType == undefined) {
            alertType = "success";
        }
        return 'alert-' + alertType;
    }

    function addStripesMessageDiv(alertType, fieldSelector) {
        var messageBox = $(document.createElement("div"));
        messageBox.css({"margin-left": "20%", "margin-right": "20%"});
        messageBox.addClass("alert").addClass(alertType);
        if (fieldSelector != undefined) {
            $(fieldSelector).addClass(alertType);
        }
        messageBox.append('<button type="button" class="close" data-dismiss="alert">&times;</button>');
        messageBox.append('<ul></ul>');

        $('.page-body').before(messageBox);
        return messageBox;
    }

    function addStripesMessage(message, alertType, fieldSelector) {
        var alertClass = getAlertClass(alertType);
        var messageBoxJquery = $("div.alert-" + alertClass);
        if (messageBoxJquery.length == 0) {
            messageBoxJquery = addStripesMessageDiv(alertClass, fieldSelector);
        }
        messageBoxJquery.find("ul").append("<li>" + message + "</li>");
    }

})(jQuery);

String.prototype.escapeJson= function() {
    return this.replace(/\n/g, "\\n")
               .replace(/\&/g, "\\&")
               .replace(/\r/g, "\\r")
               .replace(/\t/g, "\\t")
};


