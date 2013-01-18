/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */

// Auto-Fill Plugin
// Written by Joe Sak http://www.joesak.com/2008/11/19/a-jquery-function-to-auto-fill-input-fields-and-clear-them-on-click/
(function($){$.fn.autofill=function(options){var defaults={value:'First Name',defaultTextColor:"#A1A1A1",activeTextColor:"#000000"};var options=$.extend(defaults,options);return this.each(function(){var obj=$(this);obj.css({color:options.defaultTextColor}).val(options.value).focus(function(){if(obj.val()==options.value){obj.val("").css({color:options.activeTextColor});}}).blur(function(){if(obj.val()==""){obj.css({color:options.defaultTextColor}).val(options.value);}});});};})(jQuery);