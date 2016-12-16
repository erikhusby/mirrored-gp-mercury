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

/*
bindWithDelay jQuery plugin
Author: Brian Grinstead
MIT license: http://www.opensource.org/licenses/mit-license.php

http://github.com/bgrins/bindWithDelay
http://briangrinstead.com/files/bindWithDelay

Usage:
    See http://api.jquery.com/bind/
    .bindWithDelay( eventType, [ eventData ], handler(eventObject), timeout, throttle )

Examples:
    $("#foo").bindWithDelay("click", function(e) { }, 100);
    $(window).bindWithDelay("resize", { optional: "eventData" }, callback, 1000);
    $(window).bindWithDelay("resize", callback, 1000, true);
*/

(function($) {

$.fn.bindWithDelay = function( type, data, fn, timeout, throttle ) {

    if ( $.isFunction( data ) ) {
        throttle = timeout;
        timeout = fn;
        fn = data;
        data = undefined;
    }

    // Allow delayed function to be removed with fn in unbind function
    fn.guid = fn.guid || ($.guid && $.guid++);

    // Bind each separately so that each element has its own delay
    return this.each(function() {

        var wait = null;

        function cb() {
            console.log(new Date())
            var e = $.extend(true, { }, arguments[0]);
            var ctx = this;
            var throttler = function() {
                wait = null;
                fn.apply(ctx, [e]);
            };

            if (!throttle) { clearTimeout(wait); wait = null; }
            if (!wait) { wait = setTimeout(throttler, timeout); }
        }

        cb.guid = fn.guid;

        $(this).bind(type, data, cb);
    });
};

})(jQuery);
