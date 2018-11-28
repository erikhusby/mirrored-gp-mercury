<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean"/>
<c:set var="conditionSummary" value="${actionBean.jiraIssues}"/>
<c:set var="conditionIds" value="${actionBean.jiraIssuesIds}"/>
<c:set var="colCount" value=""/>

<stripes:layout-render name="/layout.jsp" pageTitle="Tag Vessel" sectionTitle="Tag Vessel">

    <stripes:layout-component name="extraHead">
        <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
        <style>

            table, td, th {
                border: 1px solid black;
            }

            .nowrap { white-space: nowrap; }

            .item {white-space: nowrap;display:inline  }

            .btn[disabled], button{
                background-color: #5a86de;
                background-image:none;
            }

            .btn-xs{
                background-color: #5a86de;
                background-image:none;
                padding: 0.5px 2px;
                font-size: 8px;
                text-align: right;
                background-image:none;
            }

            .ddl-xs{
                width:75px;
                padding: 0.5px 2px;
                font-size: 8px;
                text-align: right;
                background-image:none;
            }
            .child-left {

                display:inline-block;
            }

            .child-right {
                display:inline-block;
                white-space: normal;
                width: 400px;
                margin-left: 2cm;
            }

            .parent {
                white-space: nowrap;
                width:1000px;
            }


        </style>
        <script src="${ctxpath}/resources/scripts/jsPlumb-2.1.4.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/cherryPick.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.validate-1.14.0.min.js"></script>

        <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean"--%>
        <%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
        <%--@elvariable id="positionMap" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType"--%>

        <script type="text/javascript">

            function displayDevConditions(position) {
                $('.child-right').html(position);
            }

            function setChecked(position) {
                var pos = "[id^=cells_" + position.id +"]";
                $(pos).prop('checked', true);
            }

            //Select rows
            function selectRow(position) {
                var pos = "[id^=cells_" + position.id +"]";
                $(pos).prop('checked', position.checked);
                var labelId = pos.replace("cells_", "label_");
                if(position.checked) {
                    $(pos).attr('name', getDevConditionsID());
                    $(labelId).html(getDevConditions(position));
                }
                else {
                    $(labelId).text("(no condition)");
                }
            }

            //select columns
            function selectCol(position) {
                var posId;
                if(position.id.length > 1) {
                    posId = position.id;
                }
                else {
                    posId = "0" +position.id;
                }

                var pos = "[id$=" + posId +"]";
                $(pos).prop('checked', position.checked);
                var labelId = pos.replace("cells_", "label_");
                if(position.checked) {
                    $(pos).attr('name', getDevConditionsID());
                    $(labelId).html(getDevConditions(position));
                }
                else {
                    $(labelId).text("(no condition)");
                }

            }

            //Retrieve multi-select dev conditions base on user selection.
            function getDevConditions(position) {
                var fld = document.getElementById('ticket_conditions');
                var values = [];
                var text = [];
                var labelId = position.id.replace("cells_", "label_");
                var labelvar = document.getElementById(labelId);
                for (var i = 0; i < fld.options.length; i++) {
                    if(i==0) {
                        text.push("</br>");
                    }
                    if (fld.options[i].selected) {
                        values.push(fld.options[i].value);
                        text.push(fld.options[i].text  + "</br>");
                    }
                }
                return text.join("");
            }

            function getDevConditionsID() {
                var fld = document.getElementById('ticket_conditions');
                var values = [];
                var text = [];
                for (var i = 0; i < fld.options.length; i++) {
                    if (fld.options[i].selected) {
                        values.push(fld.options[i].value);
                    }
                }
                return values.toString();
            }

            //Tag individual tubes based on single checkboxes
            function tagPosition(position) {
                var fld = document.getElementById('ticket_conditions');
                var values = [];
                var text = [];
                var labelId = position.id.replace("cells_","label_");
                var labelvar = document.getElementById(labelId);
                for (var i = 0; i < fld.options.length; i++) {
                    if(i==0) {
                        text.push("</br>");
                    }
                    if (fld.options[i].selected) {
                        values.push(fld.options[i].value);
                        text.push(fld.options[i].text + "</br>");
                    }
                }

                $(position).attr('name', values);
                if (position.checked) {
                    labelvar.innerHTML = text.join("");
                    $(position).prop('checked', true);
                } else {
                    labelvar.innerHTML = "(no condition)";
                }
            }

            //This function creates the JSON object that persists the entire select page state back to the action bean.
            function submitSelectedItems()
            {
                var results = [];
                var selected = [];
                $("input[id^=cells_]:checked").each(function() {

                    var selectionResults = {
                        position: "",
                        selection: "",
                        devCondition: ""
                    }
                    selected.push($(this).attr('id'));
                    selectionResults.position = $(this).attr('id').replace("cells_","");
                    selectionResults.selection = $('label[for="' + $(this).attr('id') + '"]').html();
                    selectionResults.devCondition = $(this).attr('name');
                    results.push(selectionResults);
                });
                document.getElementById("jsonData").value =(JSON.stringify(results));
            }


            $j(document).ready(function () {
                //Default to first item in list box.
                try {
                    document.getElementById("ticket_conditions").selectedIndex = "0";
                }
                catch(err) {
                }

                //Persist page state on post back
                var resultQueue = [];
                if($('#jsonData').val().length > 0) {
                    var paresdJson = findAndReplace($('#jsonData').val(), "*", '"');
                    resultQueue = JSON.parse(paresdJson);
                    $.each(resultQueue, function (index, value) {
                        $( "#cells_" + value.position ).prop( "checked", true );
                        $( "#label_" + value.position ).html(value.selection);
                        $( "#label_" + value.position ).attr('name', value.devCondition);
                    });
                }

                //Select all functionality
                $('#selectAll').click(function () {
                    var fld = document.getElementById('ticket_conditions');
                    var values = [];
                    var text = [];
                    for (var i = 0; i < fld.options.length; i++) {
                        if(i==0) {
                            text.push("</br>");
                        }
                        if (fld.options[i].selected) {
                            values.push(fld.options[i].value);
                            text.push(fld.options[i].text + "</br>");
                        }
                    }
                    if(this.checked) {
                        $("[id^=label_]").html(text.join(""));
                        $("[id^=cells_]").attr('name', values);
                        $("[id^=cells_]").prop( "checked", true );
                }
                    else {
                        $("[id^=label_]").text("(no condition.)");
                        $("[id^=cells_]").attr('name', "");
                        $("[id^=cells_]").prop( "checked", false);
                    }

                });

                function findAndReplace(string, target, replacement) {
                    var i = 0, length = string.length;
                    for (i; i < length; i++) {
                        string = string.replace(target, replacement);
                    }
                    return string;
                }

                //Hide & Show page elemts based on results of searchs
                $(".control-group").removeClass("control-group");
                $(".control-label").removeClass("control-label");
                $(".controls").removeClass("controls");
                $j("#vesselBarcode").attr("value", $("#vesselLabel").val());
                $j("#accordion").accordion({  collapsible:true, active:false, heightStyle:"content", autoHeight:false});

                if (${not actionBean.vesselSearchDone}) {
                    showSearch();
                }
                else {
                    hideSearch();
                }

                if (${not actionBean.ticketSearchDone}) {
                    showTicketSearch();
                }

                if (${actionBean.showResults} ) {
                    showResults();
                }
                else {
                    hideResults();
                }

            });

            function hideResults() {
                $j('#searchResults').hide();
            }

            function showResults() {
                $j('#searchResults').show();
            }

            function showSearch() {
                $j('#searchInput').show();
            }

            function hideSearch() {
                $j('#searchInpuTicket').show();
            }

            function showTicketSearch() {
                $j('#searchInpuTicket').show();
                $j('#searchInput').hide();
            }

            function handleCancelEvent () {
                $j(this).dialog("close");
            }

        </script>

    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form action="/workflow/TagVessel.action" id="orderForm" class="form-horizontal">

            <enhance:out escapeXml="false">
            <stripes:hidden id="jsonData" name="jsonData" value="${actionBean.tagVesselJsonData}"/>
            </enhance:out>
            <input type="hidden" name="scanSource" value="">

            <div id="searchInpuTicket">
                <label for="devTicketKey">Dev Ticket</label>
                <input type="hidden" id="rackMap" name="rackMap" value="${actionBean.rackMap}">
                <input type="text" id="devTicketKey" name="devTicketKey" value="${actionBean.devTicketKey}">

                <input type="submit" id="ticketSearch" name="ticketSearch" class="btn btn-primary" value="Find">
            </div>
            </br>
            <label><h3>${actionBean.ticketSummary}</h3></label>
            <label>${actionBean.displayConditions}</label>
            </br>
            <div style="clear: left;" id="searchInput">
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
                </br>
                <stripes:submit value="Scan" id="vesselSearch" class="btn btn-primary"
                                name="<%= TagVesselActionBean.RACK_SCAN_EVENT %>"/>
                </br>
            </div>

            <c:if test="${actionBean.jiraTicketAvailable}">
                <br>
                <div id="searchResults">
                    <c:choose>

                        <c:when test="${actionBean.showResults}">
                            <div class="parent">
                                <div class="child-left">
                                    <select id="ticket_conditions" name="reasonDdl" style="width:100%;margin-bottom: 50px" multiple>
                                        <c:forEach items="${conditionSummary}" var="conditionValue" varStatus="conditionStatus">
                                                <option value="${conditionIds[conditionStatus.index]}">${conditionValue}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="child-right" >
                                </div>
                            </div>
                            <p></p>
                            <c:set var="geometry" value="${actionBean.vesselGeometry}"/>
                            <table  width="100%" id="src_TABLE_${actionBean.vesselGeometry}">
                                <c:forEach items="${geometry.rowNames}" var="rowName" varStatus="rowStatus">
                                    <c:if test="${rowStatus.first}">
                                        <tr>
                                            <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                                                <c:if test="${columnStatus.first}">
                                                    <td>
                                                        <div style="float:left;">
                                                            <input type="checkbox"  id="selectAll" name="selectAll" value="ALL"><br>&nbsp
                                                            <b>All</b></div> <div style="float:left;">
                                                    </div> <div style="float:right;"></div>
                                                    </td>
                                                </c:if>
                                                <td>
                                                    <div style="float:left;">
                                                        <input type="checkbox"  id="${columnName}" name="tagVesselColumns"
                                                               value="${columnName}" onclick="selectCol(this)"><br>
                                                        <b>${columnName}</b></div> <div style="float:right;">
                                                </div>
                                                </td>
                                            </c:forEach>
                                        </tr>
                                    </c:if>
                                    <tr>
                                        <td>
                                            <input type="checkbox"  id="${rowName}" name="tagVesselRow" value="${rowName}"
                                                   onclick="selectRow(this)"><b>${rowName}</b>
                                        </td>
                                        <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                                            <c:set var="receptacleIndex" value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                                            <c:set var="positionTest" value="${rowName}${columnName}"/>
                                            <c:set var="vesselLabel" value="${actionBean.getVesselLabelByPosition(positionTest)}"/>
                                            <c:if test="${vesselLabel.length() > 1}">
                                                <c:set var="isTagged" value="${actionBean.isVesselTagged(positionTest)}"/>
                                                <c:set var="devCondition" value="${actionBean.displayDevConditions(positionTest)}"/>
                                            </c:if>
                                            <td  align="left">
                                                <c:if test="${vesselLabel.length() > 1}">
                                                    <input type="checkbox"  id="cells_${rowName}${columnName}"
                                                           name="${actionBean.getSelectedId(positionTest)}" value="${rowName}${columnName}" ${isTagged}
                                                           onclick="tagPosition(this)"><label for="cells_${rowName}${columnName}"
                                                                                              name="label_${rowName}${columnName}_" id="label_${rowName}${columnName}">
                                                        ${actionBean.getSelected(positionTest)}</label>
                                                    <stripes:link id="transferVisualizer" event="view" target="_blank"
                                                                  beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean">
                                                        <stripes:param name="barcodes" value="${actionBean.getVesselLabelByPosition(positionTest)}"/>${actionBean.getVesselLabelByPosition(positionTest)}
                                                    </stripes:link>
                                                    <c:if test="${devCondition != ''}">
                                                        <a id="devLink" onclick="displayDevConditions('${devCondition}')" href="javascript:void(0)">(Conditions)</a>
                                                    </c:if>
                                                </c:if>
                                                <c:if test="${vesselLabel.length() == null}">
                                                    <label>(Empty Position)</label>
                                                </c:if>
                                            </td>
                                        </c:forEach>
                                    </tr>
                                </c:forEach>
                            </table>
                            </br>
                            <stripes:submit id="submitSelection" name="tagVessel" value="Submit" onclick="submitSelectedItems()"
                                            class="btn btn-primary"/>
                            <stripes:submit id="submitSelection" name="createSpreadsheet"  onclick="submitSelectedItems()"
                                            value="Create Spreadsheet" class="btn btn-primary"/>
                        </c:when>
                    </c:choose>
                </div>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>