<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%-- This Stripes layout displays a list of available columns, and a list of chosen columns, with
buttons to move columns from one to the other --%>

<%-- map from column group name to list of columns in that group --%>
<%--@elvariable id="availableMapGroupToColumnNames" type="java.util.Map<java.lang.String, java.util.List<edu.mit.broad.bsp.core.datavo.preference.ListConfig.ColumnConfig>>"--%>
<%-- list of columns that have already been chosen --%>
<%--@elvariable id="predefinedViewColumns" type="java.util.List"--%>
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
                    var newOption = document.createElement('option');
                    newOption.text = option.text;
                    newOption.value = option.value;
                    chosen.options[chosen.options.length] = newOption;
                }
            }
            for (i = available.options.length - 1; i >= 0; i--) {
                if (available.options[i].selected) {
                    available.options[i].style.display = 'none';
                }
            }
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
         * @param available multi-select of available columns
         * @param chosen multi-select of chosen columns
         */
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

        $( document ).ready( syncChosenAvailable );
    </script>
    <br/>
    <!-- Allow user to choose individual result columns -->
    <table class="resultColumns">
        <tr>
            <td><label>Available</label></td>
            <td>&nbsp;</td>
            <td><label>Chosen</label></td>
            <td>&nbsp;</td>
        </tr>
        <tr>
            <td rowspan="2" style="padding-left: 5px">
                <select name="sourceColumnDefNames" id="sourceColumnDefNames"
                        multiple="true" size="10">
                    <c:forEach items="${availableMapGroupToColumnNames}" var="entry">
                        <optgroup label="${entry.key}">
                            <c:forEach items="${entry.value}" var="columnConfig">
                                <%-- Some criteria terms are excluded from result selection --%>
                                <c:if test="${not columnConfig.isExcludedFromResultColumns()}">
                                    <option ondblclick="chooseColumns($j('#sourceColumnDefNames')[0], $j('#selectedColumnDefNames')[0]);">${columnConfig.name}</option>
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
                        multiple="true" size="10">
                    <c:if test="${not empty predefinedViewColumns}">
                        <c:forEach items="${predefinedViewColumns}" var="entry">
                            <option>${entry}</option>
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
        <tr style="padding-top: 4px">
            <td colspan="2"><label>Filter: </label><input type="text" id="filterColumns" onkeyup="filterSelect($j('#sourceColumnDefNames')[0], this);">
            </td>
        </tr>
    </table>
</stripes:layout-definition>
