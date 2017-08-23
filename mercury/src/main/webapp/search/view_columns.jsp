<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%-- This Stripes layout displays a list of available columns, and a list of chosen columns, with
buttons to move columns from one to the other --%>

<%-- map from column group name to list of columns in that group --%>
<%--@elvariable id="availableMapGroupToColumnNames" type="java.util.Map<java.lang.String, java.util.List<org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation>>"--%>
<%-- list of columns that have already been chosen --%>
<%--@elvariable id="predefinedViewColumns" type="java.util.List"--%>
<%--@elvariable id="viewColumnParamMap" type="java.util.Map<java.lang.Integer,SearchTerm.ResultParams>"--%>
<stripes:layout-definition>
    <script type="text/javascript">
        /**
         * Add column to chosen list, and make it invisible in available list (so it can't
         * be chosen again)
         * @param available multi-select of available columns
         * @param chosen multi-select of chosen columns
         */
        chooseColumns = function (available, chosen) {
            for (var i = 0; i < available.options.length; i++) {
                var option = available.options[i];
                if (option.selected && (option.style.display == "" || option.style.display == 'block')) {
                    if (jQuery.data(option, "hasParams") ) {
                        showColumnOptions(available, chosen, option);
                        // Do NOT hide this column
                        return;
                    } else {
                        var newOption = document.createElement('option');
                        newOption.text = option.text;
                        newOption.value = option.value;
                        chosen.options[chosen.options.length] = newOption;
                    }
                }
            }
            for (i = available.options.length - 1; i >= 0; i--) {
                if (available.options[i].selected) {
                    if (!option.value.endsWith(":")) {
                        available.options[i].style.display = 'none';
                    }
                }
            }
        };

        /**
         * Show child options for result columns to select from to add column to chosen list
         * @param available multi-select of available columns
         * @param chosen multi-select of chosen columns
         */
        showColumnOptions = function (available, chosen, option) {
            if (!jQuery.data(option, "hasParams") ) {
                return;
            }
            var overlayDiv = $j( "#resultParamsOverlay" );
            overlayDiv.dialog("option","searchTermName", option.value);
            overlayDiv.dialog("option","entityName", $j("#entityName").val());
            overlayDiv.dialog("open");

        };

        initResultParamOverlay = function(){ <%-- Dialog div element at bottom of configurable_search.jsp --%>
            var dialog = $j( "#resultParamsOverlay" ).dialog({
                title: "Select Result Parameters",
                searchTermName:"",
                entityName:"",
                autoOpen: false,
                height: 500,
                width: 320,
                modal: true,
                open: function(){
                    var entityName = $j( this ).dialog("option","entityName");
                    var searchTermName = $j( this ).dialog("option","searchTermName");
                    $j("#resultParamsPrompt").text("Column '" + searchTermName + "' options:");
                    dialog.dialog("option", "reset")();
                    $j.ajax({
                        url: '${ctxpath}/search/ResultParams.action',
                        data: { "paramsFetch":""
                            , "searchTermName":searchTermName
                            , "entityName":entityName},
                        type: 'get',
                        dataType: 'html',
                        cache: true,
                        complete: function (returnData, status) {
                            if( status === "success" ) {
                                $j("#resultParamsInputs")[0].innerHTML = returnData.responseText;
                            } else {
                                var errDiv = $j("#resultParamsError");
                                var message = status + ": " + returnData.responseText;
                                errDiv.text(message);
                                errDiv.css('display','block');
                            }
                        },
                    })
                },
                // Clears out any previous error message state
                reset: function(){
                    $j("#resultParamsError").text("").css('display','none');
                },
                error: function(msg){
                    $j("#resultParamsError").text(msg).css('display','block');
                }
            });
            dialog.find( "form" ).on( "submit", function( event ) {
                event.preventDefault();
                dialog.dialog("option", "reset")();

                var userColumnName = $( this ).find( "#userColumnName" ).val();
                if( userColumnName.trim().length == 0 ) {
                    dialog.dialog("option", "error")("User column name is required.");
                    return;
                }

                // TODO: Dynamic validation

                var rsltParamVal = {searchTermName:null,userColumnName:null,paramValues:[]};
                rsltParamVal.searchTermName = dialog.dialog("option", "searchTermName");
                rsltParamVal.userColumnName = userColumnName;
                var params = $( this ).serializeArray();
                rsltParamVal.paramValues = params;

                var chosenColumns = $j('#selectedColumnDefNames')[0];
                var newOption = document.createElement('option');
                newOption.text = userColumnName;
                newOption.value = JSON.stringify(rsltParamVal);
                chosenColumns.options[chosenColumns.options.length] = newOption;
                dialog.dialog("close");
            });
            dialog.find( "#resultParamsCancelBtn" ).on( "click", function(event){
                dialog.dialog("close");
            });
        };

        /**
         * Remove column from chosen, and make it visible in available
         * @param chosen multi-select of chosen columns
         * @param available multi-select of available columns
         */
        removeColumns = function (chosen, available) {
            for (var i = 0; i < chosen.options.length; i++) {
                var option = chosen.options[i];
                if (option.selected) {
                    for (var j = 0; j < available.options.length; j++) {
                        if (available.options[j].text == option.text) {
                            available.options[j].style.display = 'block';
                        }
                    }
                }
            }
            for (i = chosen.options.length - 1; i >= 0; i--) {
                if (chosen.options[i].selected) {
                    chosen.remove(i);
                }
            }
        };

        /*
         Change order of items in select
         */
        moveOptionsUp = function (selectList) {
            var selectOptions = selectList.getElementsByTagName('option');
            for (var i = 1; i < selectOptions.length; i++) {
                var opt = selectOptions[i];
                if (opt.selected) {
                    selectList.removeChild(opt);
                    selectList.insertBefore(opt, selectOptions[i - 1]);
                }
            }
        };

        /*
         Change order of items in select
         */
        moveOptionsDown = function (selectList) {
            var selectOptions = selectList.getElementsByTagName('option');
            for (var i = selectOptions.length - 2; i >= 0; i--) {
                var opt = selectOptions[i];
                if (opt.selected) {
                    var nextOpt = selectOptions[i + 1];
                    opt = selectList.removeChild(opt);
                    nextOpt = selectList.replaceChild(opt, nextOpt);
                    selectList.insertBefore(nextOpt, opt);
                }
            }
        };

        /**
         * After the page is rendered, any columns in the chosen list must be made invisible in
         * the available list (so they can't be chosen twice)
         * Also, if no columns are selected, populate chosen with search definition default(s)
         * @param available multi-select of available columns
         * @param chosen multi-select of chosen columns
         */
        var searchDefaultColumns = [<c:set var="listDelim" value=""
        /><c:forEach items="${actionBean.configurableSearchDef.defaultResultColumns}" var="defaultTerm"
            ><c:forEach items="${availableMapGroupToColumnNames}" var="entry"
                ><c:forEach items="${entry.value}" var="columnConfig"
                    ><c:if test="${columnConfig.isDefaultResultColumn() and columnConfig.name == defaultTerm.name}"
                        >${listDelim}"${defaultTerm.name}"<c:set var="listDelim" value=","
                    /></c:if
                ></c:forEach
            ></c:forEach
        ></c:forEach>];

        syncChosenAvailable = function () {
            var available = $j('#sourceColumnDefNames')[0];
            var chosen = $j('#selectedColumnDefNames')[0];
            for (var i = 0; i < chosen.options.length; i++) {
                var option = chosen.options[i];
                for (var j = 0; j < available.options.length; j++) {
                    if (available.options[j].text == option.text) {
                        available.options[j].style.display = 'none';
                    }
                }
            }

            if(chosen.options.length == 0){
               for (var i = 0; i < searchDefaultColumns.length; i++) {
                    for (var j = 0; j < available.options.length; j++) {
                        if (available.options[j].text == searchDefaultColumns[i]) {
                            var newOption = document.createElement('option');
                            newOption.text = searchDefaultColumns[i];
                            newOption.value = searchDefaultColumns[i];
                            chosen.options[chosen.options.length] = newOption;
                            available.options[j].style.display = 'none';
                        }
                    }
                }
            }
        };

        /*
         * Result columns with parameters need to be flagged as such by attaching data
         */
        var columnsWithParams = [<c:set var="listDelim" value=""
        /><c:forEach items="${availableMapGroupToColumnNames}" var="entry"
            ><c:forEach items="${entry.value}" var="columnConfig"
            ><c:if test="${not columnConfig.isExcludedFromResultColumns() and not empty columnConfig.resultParamConfigurationExpression}"><c:out value ="${listDelim}"/><c:set var="listDelim" value=","
        />"${columnConfig.name}"</c:if
        ></c:forEach
        ></c:forEach>];

        flagResultColsWithParams = function(){
            if( columnsWithParams.length === 0 ) {
                return;
            }
            var colSelect = $j('#sourceColumnDefNames')[0];
            for (var i = 0; i < colSelect.options.length; i++) {
                var option = colSelect.options[i];
                jQuery.data(option,"hasParams", false);
                for( j = 0; j < columnsWithParams.length; j++ ) {
                    if( option.value === columnsWithParams[j] ) {
                        jQuery.data(option,"hasParams", true);
                        break;
                    }
                }
            }
        };


        /**
         * Remove options in a select, that don't match what the user typed in a text box
         * @param select from which to remove options
         * @param text box in which user typed filtering characters
         */
        filterSelect = function (select, text) {
            var firstOption = true;
            for (var i = 0; i < select.options.length; i++) {
                if (select.options[i].text.toLowerCase().indexOf(text.value.toLowerCase()) >= 0) {
                    select.options[i].style.display = 'block';
                    if (firstOption) {
                        select.selectedIndex = i;
                    }
                    firstOption = false;
                } else {
                    select.options[i].style.display = 'none';
                }
            }
        };

        $j( document ).ready( syncChosenAvailable );
        $j( document ).ready( function() {
            $j( "#pageSizeSlider" ).slider({
                value:${actionBean.searchInstance.pageSize},
                min: 100,
                max: 3000,
                step: 100,
                slide: function( event, ui ) {
                    $j( "#userPageSize" ).val( ui.value );
                    $j( "#userPageSizeDisplay" ).html(ui.value);
                }
            });
            $j( "#userPageSize" ).val( $j( "#pageSizeSlider" ).slider( "value" ) );
            $j( "#userPageSizeDisplay" ).html( $j( "#pageSizeSlider" ).slider( "value" ) );
        } );
        $j( document ).ready( flagResultColsWithParams );
        $j( document ).ready( initResultParamOverlay );
    </script>
    <br/>
    <!-- Allow user to choose individual result columns -->
    <table class="resultColumns">
        <tr>
            <td><label>Available</label></td>
            <td>&nbsp;</td>
            <td><label>Chosen</label></td>
            <td>&nbsp;</td>
        <c:if test="${actionBean.configurableSearchDef.traversalEvaluators != null}">
            <td style="padding-left: 20px">
            <label><img id="traversalOptionTooltip" src="${ctxpath}/images/help.png" alt="help">&nbsp;&nbsp;Expand Search Results to Include: </label>
            &nbsp;</td>
        </c:if>
        </tr>
        <tr>
            <td rowspan="2" style="padding-left: 5px">
                <select name="sourceColumnDefNames" id="sourceColumnDefNames"
                        multiple="true" size="10">
                    <c:forEach items="${availableMapGroupToColumnNames}" var="entry" varStatus="iter">
                        <optgroup label="${entry.key}" id="col_grp_${iter.index}"<c:if test="${not empty actionBean.availableMapGroupToHelpText[iter.index]}"> class="help-option"</c:if>>
                            <c:forEach items="${entry.value}" var="columnConfig">
                                <%-- Some criteria terms are excluded from result selection --%>
                                <c:if test="${not columnConfig.isExcludedFromResultColumns()}">
                                    <option id="${columnConfig.uiId}_col" value="${columnConfig.name}"
                                            <c:if test="${not empty columnConfig.helpText}"> class="help-option"</c:if>
                                            ondblclick="chooseColumns($j('#sourceColumnDefNames')[0], $j('#selectedColumnDefNames')[0]);">${columnConfig.name}</option>
                                </c:if>
                            </c:forEach>
                        </optgroup>
                    </c:forEach>
                </select>
            </td>
            <td valign="bottom">
                <a href="javascript:chooseColumns($j('#sourceColumnDefNames')[0], $j('#selectedColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/start.png" alt="Choose Column"
                         title="Choose Column"/>
                </a>

            </td>
            <td rowspan="2" style="padding-left: 5px">
                <select name="searchInstance.predefinedViewColumns" id="selectedColumnDefNames"
                        multiple="true" size="10" style="width: 280px">
                    <c:if test="${not empty predefinedViewColumns}">
                        <c:forEach items="${predefinedViewColumns}" var="entry" varStatus="iter">
                            <c:if test="${not empty viewColumnParamMap[iter.index]}"><option value='${fn:replace( viewColumnParamMap[iter.index] ,"\'","&#39;")}'>${viewColumnParamMap[iter.index].userColumnName}</option></c:if>
                            <c:if test="${empty viewColumnParamMap[iter.index]}"><option>${entry}</option></c:if>
                        </c:forEach>
                    </c:if>
                </select>
            </td>
            <td valign="bottom">
                <a href="javascript:moveOptionsUp($j('#selectedColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/up.png" alt="Move Up"
                         title="Move Up"/>
                </a>
            </td>
            <td rowspan="2" style="padding-left: 30px;vertical-align: top">
                <c:if test="${actionBean.configurableSearchDef.traversalEvaluators != null}">
                    <c:if test="${actionBean.configurableSearchDef.customTraversalOptions  != null}">
                        <label>Apply Custom Traversal Logic:  (Exclude initial entities <input type="checkbox" id="excludeInitialEntitiesFromResults" name="searchInstance.excludeInitialEntitiesFromResults" <c:if test="${actionBean.searchInstance.excludeInitialEntitiesFromResults}">checked='true'</c:if>/>)</label><br />
                        <select id="customTraversalOptionName" name="searchInstance.customTraversalOptionName" style="width:240px">
                            <option value="none">None</option>
                            <c:forEach items="${actionBean.configurableSearchDef.customTraversalOptions}" var="customTraversalOption">
                                <option value="${customTraversalOption.key}" <c:if test="${actionBean.searchInstance.customTraversalOptionName eq customTraversalOption.key}">selected="true"</c:if> >${customTraversalOption.value.label}</option>
                            </c:forEach>
                        </select> <br />
                    </c:if>
                    <c:forEach items="${actionBean.configurableSearchDef.traversalEvaluators}" var="traversalMapEntry">
                        <input type="checkbox" id="${traversalMapEntry.key}" name="searchInstance.traversalEvaluatorValues['${traversalMapEntry.key}']" <c:if test="${actionBean.searchInstance.traversalEvaluatorValues[traversalMapEntry.key]}">checked='true'</c:if>/> ${traversalMapEntry.value.label}<br />
                    </c:forEach>
                </c:if><br />
            </td>
        </tr>
        <tr>
            <td valign="top">
                <a href="javascript:removeColumns($j('#selectedColumnDefNames')[0], $j('#sourceColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/left.png" alt="Remove Column"
                         title="Remove Column"/>
                </a>
            </td>
            <td valign="top">
                <a href="javascript:moveOptionsDown($j('#selectedColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/down.png" alt="Move Down"
                         title="Move Down"/>
                </a>
            </td>
        </tr>
        <tr>
            <td colspan="4" style="padding-top: 6px"><label>Filter: </label><input type="text" id="filterColumns" onkeyup="filterSelect($j('#sourceColumnDefNames')[0], this);"></td>
        </tr>
    </table>
    <table style="margin-top: 6px">
    <tr>
        <td><label>Page size: </label></td>
        <td style="font-weight:bold; width: 40px; padding-right: 10px"><div id="userPageSizeDisplay"></div><input type="hidden" name="searchInstance.pageSize" id="userPageSize" value="${actionBean.searchInstance.pageSize}"/></td>
        <td style="width: 500px"><div id="pageSizeSlider"></div></td>
    </tr>
    </table>
</stripes:layout-definition>
