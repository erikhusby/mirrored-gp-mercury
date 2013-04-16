/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2012) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

//----------------------------------------------
// Public utility functions
//----------------------------------------------
function setLastMonth(date) {
    if (date.getMonth() > 0) {
        date.setMonth(date.getMonth() - 1);
    } else {
        date.setMonth(11);
        date.setFullYear(date.getFullYear() - 1);
    }
}

function setNextMonth(date) {
    if (date.getMonth() >= 11) {
        date.setMonth(0);
        date.setFullYear(date.getFullYear() + 1);
    } else {
        date.setMonth(date.getMonth() + 1);
    }
}

function setEndOfDay(date) {
    date.setHours(23);
    date.setMinutes(59);
    date.setSeconds(59);
    date.setMilliseconds(999);
}

function setStartOfDay(date) {
    date.setHours(0);
    date.setMinutes(0);
    date.setSeconds(0);
    date.setMilliseconds(0);
}

var DAY_MILLIS = 1000 * 60 * 60 * 24;

(function($){

    var defaults = {
        rangeFieldName: 'dateRange',
        rangeSelector: 0,
        startString: '',
        endString: '',
        calendarSrc : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAAK/INwWK6QAAABl0RVh0U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAAIESURBVDjLlVJtaxpBEH7uvNSL50skFBqCiDVYpCWiIAjtx4Ih4I/zs78jkD9QioVAUBGNWigqRfpBxSO+3LnbmY13mNQWOvAwuzszz7zsQEoJBomWzWY/V6vVb5lM5oruBr/tYBQKhU+1Wu0r+/CbF6cOA02Tv9jr5gbn+TyGd3cQlQpe40nYFry9xZvLS/y8v8fm+lrZ0lJqukbCTlYwCCsWw3a7RTgex3EggLiuK5jkYkYiynYcjcLcEXOsvjvDNAx0BgPl1O31IIjEPjmBHQ5ja5rodLvK1nl48Ang9dgHRIyyN87O0LNtXFD2FLWmU4B0HKxdF99JDwhvhUCB9CPZLwDd2K/gw+kp3lsW5GYDl5wEg8heEdG7oyNkSGuE4GKBRyL1q6jX69J13b/CcRy5XC4VWPiNYzjWwAFZr9dot9tIp9Po9/uq9/l8jnK57H25L/ohAg4ejUaI0ORzuRxSqRRCoRAosw+P6BmB95inXfAWhdFqtVQ1Dg+UqqNW/Jg/WnhZ4mw2g6DJc/BkMlFnhud3cAb7ZNwOrbaaQzKZ5OXBcDiEQb/GA9XljoqU2A+u0CqzqVgswqKv5awcPB6PfSJ/Bgv6V5uEjoIN+wjQHrDmCjhzIpHAarVSLfktdGlNyTHKZf1LvAqYrNlsolQqPRFMp9MvjUbjI/5D6Dd+sP4NLTpNB1cxufkAAAAASUVORK5CYII=',
        dateFieldClass: 'datepicker',
        choiceSelectionId: '',
        startFieldId: '',
        endFieldId: ''
    };

    $.fn.dateRangeSelector = function(options) {

        var defaultAndPassedOptions = $.extend({}, defaults, options);

        this.removeDateOption = function(position) {
            var options = $(this).data('options');
            options["dateOptions"].remove(position);
        };

        // public DateOption creator
        this.addDateOption = function(position, name, startDate, endDate, preStart, preEnd, postEnd) {
            var options = $(this).data('options');

            var dateOption = new DateOption(name, startDate, endDate);
            dateOption.preStart = preStart;
            dateOption.preEnd = preEnd;
            dateOption.postEnd = postEnd;
            options["dateOptions"].splice(position, 0, dateOption);
            if (options["rangeSelector"] > position) {
                options["rangeSelector"] += 1;
            }
            var optionsHTML = "";
            for (var i=0; i < options["dateOptions"].length; i++) {
                var selectedString = "";
                if (i == options["rangeSelector"]) {
                    selectedString = 'selected="true"';
                }
                optionsHTML += '<option value="' + i + '"' + selectedString + '>' + options["dateOptions"][i].name + '</option>';
            }
            $(options["choiceSelectionId"]).html(optionsHTML);
        };

        return this.each(function() {
            var $container = $(this);

            var options = $.extend({}, defaultAndPassedOptions);

            // If the name was not explicitly set by the caller, use the id
            if (options['rangeFieldName'] == '') {
                options['rangeFieldName'] = $container.attr("rangeFieldName");
            }
            if (options['rangeSelector'] == '') {
                options['rangeSelector'] = $container.attr("rangeSelector");
            }
            if (options['startString'] == '') {
                options['startString'] = $container.attr("startString");
            }
            if (options['endString'] == '') {
                options['endString'] = $container.attr("endString");
            }

            options["choiceSelectionId"] = "#" + $container.attr("id") + "-choices";
            options["startFieldId"] = "#" + $container.attr("id") + "-Calendar-startId";
            options["endFieldId"] = "#" + $container.attr("id") + "-Calendar-endId";
            options["dateOptions"] = createDateOptions();
            $container.data('options', options);

            createComponents($container);
            updateChoice($container);

            // Bind change handlers to each of the various created DOM objects.
            var choices = $(options["choiceSelectionId"]);
            choices.change(function() {
                updateChoice($container);
            });
            var startField = $(options["startFieldId"]);
            startField.change(function() {
                setCustom($container);
                setNaturalLanguageString($container);
            });
            var endField = $(options["endFieldId"]);
            endField.change(function() {
                setCustom($container);
                setNaturalLanguageString($container);
            });
        });
    };

    //----------------------------------------------
    // Private DOM manipulation functions
    //----------------------------------------------
    function createComponents(container) {
        var divId = container.attr("id");
        var options = container.data('options');
        var dateChoicesString = '<select class="dateRangeSelector search-select" id="' + divId + '-choices" name="' + options["rangeFieldName"] + '.rangeSelector">';

        for (var i=0; i < options["dateOptions"].length; i++) {
            var selectedString = "";
            if (i == options["rangeSelector"]) {
                selectedString = 'selected="true"';
            }

            dateChoicesString += '<option value="' + i + '"' + selectedString + '>' + options["dateOptions"][i].name + '</option>';
        }

        dateChoicesString += "</select>";
        dateChoicesString += '&nbsp;<input class="search-input" type="hidden" value="" id="' + divId + 'NaturalLanguageString" name="' +  options["rangeFieldName"] + '.naturalLanguageString"/>';
        container.html(dateChoicesString);

        container.append(createDateField(container, "start"));

        var separator = document.createElement("span");
        separator.setAttribute("id", divId + "DateRangeSeparator");
        separator.appendChild(document.createTextNode(" - "));
        container.append(separator);

        container.append(createDateField(container, "end"));
    }

    function createDateField(container, calendarField) {
        var divId = container.attr("id");
        var options = container.data('options');
        // Create the date field
        var inputField = document.createElement("input");
        inputField.setAttribute("type", "text");
        inputField.setAttribute("class", options["dateFieldClass"] + " search-input");
        inputField.setAttribute("id", divId + "-Calendar-" + calendarField + "Id");
        inputField.setAttribute("name", options["rangeFieldName"] + "." + calendarField);
        inputField.setAttribute("size", "10");
        if (calendarField == "start") {
            inputField.setAttribute("value", options["startString"]);
        } else if (calendarField == "end") {
            inputField.setAttribute("value", options["endString"]);
        }
        return inputField;
    }

    function createDateOptions() {
        dateOptions = new Array();
        dateOptions[0] = createAll();
        dateOptions[1] = createAfter();
        dateOptions[2] = createBefore();
        dateOptions[3] = createToday();
        dateOptions[4] = createYesterday();
        dateOptions[5] = thisWeek();
        dateOptions[6] = sevenDays();
        dateOptions[7] = lastWeek();
        dateOptions[8] = thisMonth();
        dateOptions[9] = oneMonth();
        dateOptions[10] = lastMonth();
        dateOptions[11] = thisYear();
        dateOptions[12] = oneYear();
        dateOptions[13] = lastYear();
        dateOptions[14] = createCustom();
        return dateOptions;
    }

    function setValues(container) {
        var options = container.data('options');
        $(options["choiceSelectionId"]).val(options["rangeSelector"]);
        $(options["startFieldId"]).val(options["startString"]);
        $(options["endFieldId"]).val(options["endString"]);
        updateChoice(container);
    }

    function updateChoice(container) {
        var divId = container.attr("id");
        var options = container.data('options');
        var choice = $(options["choiceSelectionId"]).val();
        var option = dateOptions[choice];

        var startField = $(options["startFieldId"]);
        var endField = $(options["endFieldId"]);
        var separator = $("#" + divId + "DateRangeSeparator");

        if ( (option.startDate != null) && (option.endDate != null) ) {
            separator.show()
        } else {
            separator.hide();
        }

        if (option.startDate == null) {
            startField.hide();
            unbindDatepicker(options["startFieldId"]);
            startField.val("");
        } else {
            startField.show();
            bindDatepicker(options["startFieldId"], options);
        }

        if (option.endDate == null) {
            endField.hide();
            unbindDatepicker(options["endFieldId"]);
            endField.val("");
        } else {
            endField.show();
            bindDatepicker(options["endFieldId"], options);
        }

        option.updateSelection(startField, endField);

        setNaturalLanguageString(container);
    }

    function bindDatepicker(objId, options) {
        $(objId).datepicker({
            showOn: "button",
            buttonImage: options["calendarSrc"],
            buttonImageOnly: true
        });
    }

    function unbindDatepicker(objId) {
        $(objId).datepicker("destroy");
    }

    function setCustom(container) {
        var options = container.data('options');
        var choices = $(options["choiceSelectionId"]);
        if (dateOptions[choices.val()].setCustomOnChange) {
            choices.val(14);
        }
    }

    function setNaturalLanguageString(container) {
        var options = container.data('options');
        var choice = $(options["choiceSelectionId"]).val();
        var dateOption = dateOptions[choice];
        var startFieldValue = $(options["startFieldId"]).val();
        var endFieldValue = $(options["endFieldId"]).val();
        var naturalLanguageString = dateOption.preStart;
        naturalLanguageString += (startFieldValue == null) ? "" : startFieldValue;
        naturalLanguageString += dateOption.preEnd;
        naturalLanguageString += (endFieldValue == null) ? "" : endFieldValue;
        naturalLanguageString += dateOption.postEnd;
        $("#" + container.attr("id") + "NaturalLanguageString").val(naturalLanguageString);
    }

    //----------------------------------------------
    // Private DateOption methods
    //----------------------------------------------
    function DateOption(name, startDate, endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.setCustomOnChange = true;

        // Default update just populates the dates into the fields
        this.updateSelection = function(startDiv, endDiv) {

            if (startDate != null) {
                startDiv.val(formatDate(startDate, "MM/dd/yyyy"));
            }

            if (endDate != null) {
                endDiv.val(formatDate(endDate, "MM/dd/yyyy"));
            }
        }
    }

    function createCustom() {
        var dateOption = new DateOption("Custom", new Date(), new Date());

        dateOption.updateSelection = function(startDiv, endDiv) { };

        dateOption.preStart = "From ";
        dateOption.preEnd = " To ";
        dateOption.postEnd = "";

        return dateOption;

    }

    function createAll() {
        var dateOption = new DateOption("Any", null, null);
        dateOption.setCustomOnChange = false;

        dateOption.updateSelection = function(startDiv, endDiv) {
        };

        dateOption.preStart = "Any Day";
        dateOption.preEnd = "";
        dateOption.postEnd = "";

        return dateOption;

    }

    function createAfter() {
        var dateOption = new DateOption("After", new Date(), null);
        dateOption.setCustomOnChange = false;

        dateOption.updateSelection = function(startDiv, endDiv) {
            endDiv.val("");
            if ((startDiv.val() == null) || (startDiv.val() == "")) {
                startDiv.val(formatDate(dateOption.startDate, "MM/dd/yyyy"));
            }
        };

        dateOption.preStart = "After ";
        dateOption.preEnd = "";
        dateOption.postEnd = "";

        return dateOption;

    }

    function createBefore() {
        var dateOption = new DateOption("Before", null, new Date());
        dateOption.setCustomOnChange = false;

        dateOption.updateSelection = function(startDiv, endDiv) {
            startDiv.val("");
            if ((endDiv.val() == null) || (endDiv.val() == "")) {
                endDiv.val(formatDate(dateOption.endDate, "MM/dd/yyyy"));
            }
        };

        dateOption.preStart = "Before ";
        dateOption.preEnd = "";
        dateOption.postEnd = "";

        return dateOption;
    }

    function createToday() {
        var start = new Date();
        setStartOfDay(start);

        var end = new Date();
        setEndOfDay(end);

        var dateOption = new DateOption("Today", start, end);
        dateOption.preStart = "Today (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function createYesterday() {
        var start = new Date(new Date() - DAY_MILLIS);
        setStartOfDay(start);

        var end = new Date(new Date() - DAY_MILLIS);
        setEndOfDay(end);

        var dateOption = new DateOption("Yesterday", start, end);
        dateOption.preStart = "Yesterday (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function thisWeek() {
        var start = new Date();
        var daysFromWeekStart = start.getDay();

        start = new Date(start - (daysFromWeekStart * DAY_MILLIS));
        setStartOfDay(start);

        var end = new Date(start.getTime() + 6 * DAY_MILLIS);
        setEndOfDay(end);

        var dateOption = new DateOption("This Week", start, end);
        dateOption.preStart = "This Week (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function sevenDays() {
        var start = new Date(new Date() - (6 * DAY_MILLIS));
        setStartOfDay(start);

        var end = new Date();
        setEndOfDay(end);

        var dateOption = new DateOption("Seven Days", start, end);
        dateOption.preStart = "Over the last 7 days (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function lastWeek() {
        var start = new Date();
        var daysFromWeekStart = start.getDay();

        start = new Date(start - ((7 + daysFromWeekStart) * DAY_MILLIS));
        setStartOfDay(start);

        var end = new Date();
        end = new Date(end - ((1 + daysFromWeekStart) * DAY_MILLIS));
        setEndOfDay(end);

        var dateOption = new DateOption("Last Week", start, end);
        dateOption.preStart = "Last week (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function thisMonth() {
        var start = new Date();
        start.setDate(1);
        setStartOfDay(start);

        var end = new Date();
        setNextMonth(end);
        end.setDate(1);
        end = new Date(end - DAY_MILLIS);
        setEndOfDay(end);

        var dateOption = new DateOption("This Month", start, end);
        dateOption.preStart = "This month (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function oneMonth() {
        var start = new Date();
        setLastMonth(start);
        setStartOfDay(start);

        var end = new Date();
        setEndOfDay(end);

        var dateOption = new DateOption("One Month", start, end);
        dateOption.preStart = "Over the past month (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function lastMonth() {

        // Get the first day of last month
        var start = new Date();
        setLastMonth(start);
        start.setDate(1);
        setStartOfDay(start);

        // Get the first day of THIS month and subtract one
        var end = new Date();
        end.setDate(1);
        end = new Date(end - DAY_MILLIS);
        setEndOfDay(end);

        var dateOption = new DateOption("Last Month", start, end);
        dateOption.preStart = "Last month (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function thisYear() {
        var start = new Date();
        start.setMonth(0);
        start.setDate(1);
        setStartOfDay(start);

        var end = new Date();
        end.setYear(end.getFullYear() + 1);
        end.setMonth(0);
        end.setDate(1);
        end = new Date(end - DAY_MILLIS);
        setEndOfDay(end);

        var dateOption = new DateOption("This Year", start, end);
        dateOption.preStart = "This year (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function oneYear() {
        var start = new Date();
        start.setFullYear(start.getFullYear() - 1);
        setStartOfDay(start);

        var end = new Date();
        setEndOfDay(end);

        var dateOption = new DateOption("One Year", start, end);
        dateOption.preStart = "Over the past year (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    function lastYear() {
        var start = new Date();
        start.setFullYear(start.getFullYear() - 1);
        start.setMonth(0);
        start.setDate(1);
        setStartOfDay(start);

        var end = new Date();
        end.setMonth(0);
        end.setDate(1);
        end = new Date(end - DAY_MILLIS);
        setEndOfDay(end);

        var dateOption = new DateOption("Last Year", start, end);
        dateOption.preStart = "Last year (";
        dateOption.preEnd = " - ";
        dateOption.postEnd = ")";

        return dateOption;
    }

    //----------------------------------------------
    // Utility methods copied from CalendarPopup.js
    //----------------------------------------------
    var MONTH_NAMES=new Array('January','February','March','April','May','June','July','August','September','October','November','December','Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec');var DAY_NAMES=new Array('Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sun','Mon','Tue','Wed','Thu','Fri','Sat');
    function LZ(x){return(x<0||x>9?"":"0")+x}
    function formatDate(date,format){format=format+"";var result="";var i_format=0;var c="";var token="";var y=date.getYear()+"";var M=date.getMonth()+1;var d=date.getDate();var E=date.getDay();var H=date.getHours();var m=date.getMinutes();var s=date.getSeconds();var yyyy,yy,MMM,MM,dd,hh,h,mm,ss,ampm,HH,H,KK,K,kk,k;var value=new Object();if(y.length < 4){y=""+(y-0+1900);}value["y"]=""+y;value["yyyy"]=y;value["yy"]=y.substring(2,4);value["M"]=M;value["MM"]=LZ(M);value["MMM"]=MONTH_NAMES[M-1];value["NNN"]=MONTH_NAMES[M+11];value["d"]=d;value["dd"]=LZ(d);value["E"]=DAY_NAMES[E+7];value["EE"]=DAY_NAMES[E];value["H"]=H;value["HH"]=LZ(H);if(H==0){value["h"]=12;}else if(H>12){value["h"]=H-12;}else{value["h"]=H;}value["hh"]=LZ(value["h"]);if(H>11){value["K"]=H-12;}else{value["K"]=H;}value["k"]=H+1;value["KK"]=LZ(value["K"]);value["kk"]=LZ(value["k"]);if(H > 11){value["a"]="PM";}else{value["a"]="AM";}value["m"]=m;value["mm"]=LZ(m);value["s"]=s;value["ss"]=LZ(s);while(i_format < format.length){c=format.charAt(i_format);token="";while((format.charAt(i_format)==c) &&(i_format < format.length)){token += format.charAt(i_format++);}if(value[token] != null){result=result + value[token];}else{result=result + token;}}return result;}

})(jQuery);