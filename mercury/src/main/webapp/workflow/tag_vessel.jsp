<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList" %>
<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean"/>
<c:set var="conditionSummary" value="${actionBean.jiraIssues}"/>
<c:set var="conditionIds" value="${actionBean.jiraIssuesIds}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Tag Vessel" sectionTitle="Tag Vessel">

    <stripes:layout-component name="extraHead">
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


            function showHeader() {
                $j("#accordion").show();
                if(${fn:length(actionBean.foundVessels) == 1}){
                    $j("#accordion").accordion({active: 0})
                }
            }

            function hideResults() {
                $j('#searchResults').hide();
            }

            function showResults() {
                $j('#searchResults').show();
            }

            function showSearch() {
                $j('#searchInput').show();
                $j('#searchResults').hide();
                $j('#searchInpuTicket').hide();
            }

            function hideSearch() {
                showHeader();
                $j('#searchInpuTicket').show();
            }

            function showTicketSearch() {
                $j('#searchInpuTicket').show();
                   showHeader();
            }

            function hideTicketSearch() {
                $j('#searchInpuTicket').hide();
                $j('#searchResults').hide();

            }

            function handleCancelEvent () {
                $j(this).dialog("close");
            }

        </script>

    </stripes:layout-component>
    <stripes:layout-component name="content">
    <stripes:form action="/workflow/TagVessel.action" id="orderForm" class="form-horizontal">

            <stripes:hidden id="abandonComment" name="abandonComment" value=''/>
            <stripes:hidden id="unAbandonComment" name="unAbandonComment" value=''/>
            <stripes:hidden id="vesselBarcode" name="vesselBarcode" value=''/>
            <stripes:hidden id="vesselPosition" name="vesselPosition" value=''/>
            <stripes:hidden id="vesselPositionReason" name="vesselPositionReason" value=''/>

            <div id="searchInput">
                    <label for="searchKey">Barcode</label>
                    <input type="text" id="searchKey" name="searchKey" value="${actionBean.searchKey}">
                    <input type="submit" id="vesselSearch" name="vesselSearch" class="btn btn-primary" value="Find">
            </div>
            </br>

            <c:if test="${not empty actionBean.foundVessels}">
            <div id="accordion" style="display:none;" class="accordion">
                <c:forEach items="${actionBean.foundVessels}" var="vessel" varStatus="status">
                    <div style="padding-left: 30px;padding-bottom: 2px">
                        <stripes:layout-render name="/vessel/tag_vessel_info_header.jsp" bean="${actionBean}"
                                               vessel="${vessel}"/>
                    </div>
                </c:forEach>
            </div>
            </br>
            <div id="searchInpuTicket">
                <label for="devTicketKey">Dev Ticket</label>
                <input type="text" id="devTicketKey" name="devTicketKey" value="${actionBean.devTicketKey}">
                <input type="submit" id="ticketSearch" name="ticketSearch" class="btn btn-primary" value="Find">
            </div>
            <br>
            <div id="searchResults">
                <c:choose>
                    <c:when test="${actionBean.isMultiplePositions}">
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
                                        <c:set var="vesselLabel" value="${actionBean.getVesselLabel(positionTest)}"/>
                                        <td align="right">
                                            <c:choose>
                                                <c:when test="${actionBean.isVesselTagged(positionTest)}">
                                                      <stripes:submit id="${rowName}${columnName}" name="unTagVessel" value="Remove Tag" onclick="tagPosition(this.id)" class="btn btn-primary ${actionBean.shrinkCss('btn-xs')}"/>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:if test="${vesselLabel.length() > 1}">
                                                        <stripes:submit id="${rowName}${columnName}" name="tagVessel" value="Add Tag" onclick="tagPosition(this.id)" class="btn btn-primary ${actionBean.shrinkCss('btn-xs')}"/>
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
                                            <c:if test="${vesselLabel.length() < 1}">
                                                <label>Vessel Missing..</label>
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
