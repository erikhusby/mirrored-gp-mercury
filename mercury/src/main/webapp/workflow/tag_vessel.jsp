<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean" %>

<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean"/>
<c:set var="conditionSummary" value="${actionBean.jiraIssues}"/>
<c:set var="conditionIds" value="${actionBean.jiraIssuesIds}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Tag Vessel" sectionTitle="Tag Vessel">

    <stripes:layout-component name="extraHead">
    <%@ include file="/vessel/rack_scanner_list_with_sim_part1.jsp" %>
        <style>

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


        </style>
        <script src="${ctxpath}/resources/scripts/jsPlumb-2.1.4.js"></script>
        <script type="text/javascript" src="${ctxpath}/resources/scripts/cherryPick.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.validate-1.14.0.min.js"></script>

        <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean"--%>
        <%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
        <%--@elvariable id="positionMap" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType"--%>

        <script type="text/javascript">
            $(function() {
                $('#reasonCode').change(function() {
                    var x = $(this).val();
                    $('#abandonComment').val(x);
                });
            });

            function myFunction() {
                document.getElementById("mySubmit").disabled = true;
            }

            function tagPosition(position) {
                var reason = $("#"+"reason_"+position);
                $j("#vesselPosition").attr("value", position);
                $j("#vesselPositionReason").attr("value", reason.val());
            }

            $j(document).ready(function () {
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

            <stripes:hidden id="vesselBarcode" name="vesselBarcode" value=''/>
            <stripes:hidden id="vesselPosition" name="vesselPosition" value=''/>
            <stripes:hidden id="vesselPositionReason" name="vesselPositionReason" value=''/>
            <input type="hidden" name="scanSource" value="">

            <div id="searchInpuTicket">
                <label for="devTicketKey">Dev Ticket</label>
                <input type="hidden" id="rackMap" name="rackMap" value="${actionBean.rackMap}">
                <input type="text" id="devTicketKey" name="devTicketKey" value="${actionBean.devTicketKey}">
                <input type="submit" id="ticketSearch" name="ticketSearch" class="btn btn-primary" value="Find">
            </div>
            </br>
                <label><h3>${actionBean.ticketSummary}</h3></label>
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
                            <c:set var="geometry" value="${actionBean.vesselGeometry}"/>
                            <table id="src_TABLE_${actionBean.vesselGeometry}">
                                <c:forEach items="${geometry.rowNames}" var="rowName" varStatus="rowStatus">
                                    <c:if test="${rowStatus.first}">
                                        <tr>
                                            <td nowrap><div style="float:right;"></div> <div style="float:right;"></div></td>
                                            <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                                                <td nowrap><div style="float:left;">${columnName}</div> <div style="float:right;"> </div></td>
                                            </c:forEach>
                                        </tr>
                                    </c:if>
                                    <tr>
                                        <td>${rowName} </br></td>
                                        <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                                            <c:set var="receptacleIndex" value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                                            <c:set var="positionTest" value="${rowName}${columnName}"/>
                                            <c:set var="vesselLabel" value="${actionBean.getVesselLabelByPosition(positionTest)}"/>
                                            <td align="right">
                                                <c:choose>
                                                    <c:when test="${actionBean.isVesselTagged(positionTest)}">
                                                          <stripes:submit id="${rowName}${columnName}" name="unTagVessel" value="Remove Tag" onclick="tagPosition(this.id)" class="btn btn-primary ${actionBean.shrinkCss('btn-xs')}"/>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <c:if test="${vesselLabel.length() > 1}">
                                                            <stripes:submit id="${rowName}${columnName}" name="tagVessel" value="Add Tag" onclick="tagPosition(this.id)" class="btn btn-primary ${actionBean.shrinkCss('btn-xs')}"/>
                                                        </c:if>
                                                        <c:if test="${vesselLabel.length() == null}">
                                                            <label>(Empty Position)</label>
                                                        </c:if>
                                                    </c:otherwise>
                                                </c:choose>
                                                <c:if test="${vesselLabel.length() > 1}">
                                                    <select class="filterDropdown ${actionBean.shrinkCss('ddl-xs')}" id="reason_${rowName}${columnName}" name="reasonDdl">
                                                        <c:forEach items="${conditionSummary}" var="conditionValue" varStatus="conditionStatus">
                                                            <c:if test="${conditionStatus.count > 3}">
                                                                <option value="${conditionIds[conditionStatus.index]}">${conditionValue}</option>
                                                            </c:if>
                                                        </c:forEach>
                                                        <option selected="SELECT">${actionBean.getSelected(positionTest)}</option>
                                                    </select>
                                                </c:if>
                                            </td>
                                        </c:forEach>
                                    </tr>
                                </c:forEach>
                            </table>
                        </c:when>
                    </c:choose>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
