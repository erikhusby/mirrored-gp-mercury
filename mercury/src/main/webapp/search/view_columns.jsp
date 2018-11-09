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
        });

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
        <tr style="width: 90%">
            <td style="padding-left: 5px">
                <select name="sourceColumnDefNames" id="sourceColumnDefNames"
                        multiple="true" size="10">
                    <c:forEach items="${availableMapGroupToColumnNames}" var="entry" varStatus="iter">
                        <optgroup label="${entry.key}" id="col_grp_${iter.index}"<c:if test="${not empty actionBean.availableMapGroupToHelpText[iter.index]}"> class="help-option"</c:if>>
                            <c:forEach items="${entry.value}" var="columnConfig">
                                <%-- Some criteria terms are excluded from result selection --%>
                                <c:if test="${not columnConfig.isExcludedFromResultColumns()}">
                                    <option id="${columnConfig.uiId}_col" value="${columnConfig.name}"
                                            <c:if test="${not empty columnConfig.helpText}"> class="help-option"</c:if>
                                            <c:if test="${not empty columnConfig.resultParamConfigurationExpression}"> data-has-params="true" data-element-name="${columnConfig.name}" </c:if>
                                            <c:if test="${empty columnConfig.resultParamConfigurationExpression}"> data-has-params="false" </c:if>
                                            ondblclick="chooseColumns($j('#sourceColumnDefNames')[0], $j('#selectedColumnDefNames')[0]);">${columnConfig.name}</option>
                                </c:if>
                            </c:forEach>
                        </optgroup>
                    </c:forEach>
                </select>
            </td>
            <td valign="middle">
                <a href="javascript:chooseColumns($j('#sourceColumnDefNames')[0], $j('#selectedColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/start.png" alt="Choose Column"
                         title="Choose Column"/></a><br/>
                <a href="javascript:removeColumns($j('#selectedColumnDefNames')[0], $j('#sourceColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/left.png" alt="Remove Column"
                         title="Remove Column"/></a>
            </td>
            <td style="padding-left: 5px">
                <select name="searchInstance.predefinedViewColumns" id="selectedColumnDefNames"
                        multiple="true" size="10" style="width: 280px">
                    <c:if test="${not empty predefinedViewColumns}">
                        <c:forEach items="${predefinedViewColumns}" var="entry" varStatus="iter">
                            <c:if test="${not empty viewColumnParamMap[iter.index]}"><option data-has-params="true" data-element-name="${viewColumnParamMap[iter.index].getElementName()}" ondblclick="var evt=$j.Event('dblclick');evt.delegateTarget=this;editColumnParams(evt);" value='${fn:escapeXml( viewColumnParamMap[iter.index] )}'>${viewColumnParamMap[iter.index].getSingleValue("userColumnName")}</option></c:if>
                            <c:if test="${empty viewColumnParamMap[iter.index]}"><option data-has-params="false">${entry}</option></c:if>
                        </c:forEach>
                    </c:if>
                </select>
            </td>
            <td valign="middle">
                <a href="javascript:moveOptionsUp($j('#selectedColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/up.png" alt="Move Up"
                         title="Move Up"/></a><br/>
                <a href="javascript:moveOptionsDown($j('#selectedColumnDefNames')[0]);">
                    <img style="vertical-align:middle;" border="0" src="${ctxpath}/images/down.png" alt="Move Down"
                         title="Move Down"/></a>
            </td>
            <td style="padding-left: 30px;vertical-align: top;display: inline-block;min-width: 350px">
                <c:if test="${actionBean.configurableSearchDef.traversalEvaluators != null}">
                    <c:if test="${actionBean.configurableSearchDef.customTraversalOptions  != null}">
                        <label>Apply Custom Traversal Logic:  (Exclude initial entities <input type="checkbox" id="excludeInitialEntitiesFromResults" name="searchInstance.excludeInitialEntitiesFromResults" <c:if test="${actionBean.searchInstance.excludeInitialEntitiesFromResults}">checked='true'</c:if>/>)</label><br />
                        <select id="customTraversalOptionConfig" name="searchInstance.customTraversalOptionConfig" style="width:240px;display: inline-block">
                            <option value="none">None</option>
                            <c:forEach items="${actionBean.configurableSearchDef.customTraversalOptions}" var="customTraversalOption">
                                <c:if test="${actionBean.searchInstance.customTraversalOptionName eq customTraversalOption.key}">
                                    <option <c:if test='${empty actionBean.searchInstance.customTraversalOptionParams}'>
                                        value="${customTraversalOption.key}"</c:if><c:if test='${not empty actionBean.searchInstance.customTraversalOptionParams}'>
                                        value="${fn:escapeXml( actionBean.searchInstance.customTraversalOptionParams )}"</c:if> data-element-name="${customTraversalOption.key}" data-has-user-customization="${customTraversalOption.value.userConfigurable}" selected="true" >${customTraversalOption.value.label}</option>
                                </c:if>
                                <c:if test="${actionBean.searchInstance.customTraversalOptionName ne customTraversalOption.key}">
                                    <option value="${customTraversalOption.key}" data-element-name="${customTraversalOption.key}" data-has-user-customization="${customTraversalOption.value.userConfigurable}">${customTraversalOption.value.label}</option>
                                </c:if>
                            </c:forEach>
                        </select> <div id="customTraversalOptionEdit" style="display: inline-block; min-width: 18px; max-width: 18px; min-height:18px; max-height:18px; padding: 0px; border-width:0px; background-image: url('/Mercury/images/ui-icons_2e83ff_256x240.png'); background-repeat: no-repeat; background-position: -640px 0px;"> </div><br />
                    </c:if>
                    <c:forEach items="${actionBean.configurableSearchDef.traversalEvaluators}" var="traversalMapEntry">
                        <input type="checkbox" id="${traversalMapEntry.key}" name="searchInstance.traversalEvaluatorValues['${traversalMapEntry.key}']" <c:if test="${actionBean.searchInstance.traversalEvaluatorValues[traversalMapEntry.key]}">checked='true'</c:if>/> ${traversalMapEntry.value.label}<br />
                    </c:forEach>
                </c:if>
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
