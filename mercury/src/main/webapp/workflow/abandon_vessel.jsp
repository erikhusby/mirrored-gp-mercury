<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>
<%@ page import="org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList" %>
<stripes:useActionBean var="actionBean" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean"/>
<c:set var="reasonCodes" value="${actionBean.reasonCodes}"/>
<stripes:layout-render name="/layout.jsp" pageTitle="Abandon Vessel" sectionTitle="Abandon Vessel">

    <stripes:layout-component name="extraHead">
        <style>
            .btn {
                background-image:none;
            }
            .btn-primary {
                background-color: #5a86de;
                background-image:none;
            }

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

        <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.presentation.workflow.abandonvesselactionbean"--%>
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

            function abandonAllPositionReason() {
                var reason = $("#reasonCodeAllPositions").val();
                $j("#vesselPositionReason").attr("value", reason);
            }

            function abandonPositions(position) {
                var reason = $("#"+"reason_"+position).val();
                $j("#vesselPosition").attr("value", position);
                $j("#vesselPositionReason").attr("value", reason);
            }

            function showAbandonDialog() {
                $j("#dialogAction").attr("name", "abandonVesselItem");
                $j("#abandonVesselBarcode").text($("#vesselLabel").val());
                $j("#abandonDialog").dialog("open").dialog("option", "width", 600);
            }

            $j(document).ready(function () {

                $j("#vesselBarcode").attr("value", $("#vesselLabel").val());

                $j("#abandonDialog").dialog({
                    modal: true,
                    autoOpen: false,
                    buttons: [
                        {
                            id: "abandonOkButton",
                            text: "OK",
                            click: function () {
                                $j(this).dialog("close");
                                $j("#abandonOkButton").attr("disabled", "disabled");
                                $j("#abandonStatus").attr("value", $j("#abandonDialogId").attr("checked") != undefined);
                                $j("#abandonComment").attr("value", $( "#reasonCode option:selected" ).text());
                                $j("#vesselBarcode").attr("value", $("#vesselLabel").val());
                                $('#abandonVessel').trigger('click');
                            }
                        },
                        {
                            text: "Cancel",
                            click: handleCancelEvent
                        }
                    ]
                });

                $j("#accordion").accordion({  collapsible:true, active:false, heightStyle:"content", autoHeight:false});
                $j("#accordion").show();
                if(${fn:length(actionBean.foundVessels) == 1}){
                    $j("#accordion").accordion({active: 0})
                }

                if (${not actionBean.searchDone}) {
                    showSearch();
                }
                else {
                    hideSearch()
                }
            });

            function showSearch() {
                $j('#searchInput').show();
                $j('#searchResults').hide();
            }

            function hideSearch() {
                $j('#searchInput').hide();
                $j('#searchResults').show();
            }

            function handleCancelEvent () {
                $j(this).dialog("close");
            }

        </script>

    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form action="/workflow/AbandonVessel.action" id="orderForm" class="form-horizontal">

            <stripes:hidden id="abandonComment" name="abandonComment" value=''/>
            <stripes:hidden id="unAbandonComment" name="unAbandonComment" value=''/>
            <stripes:hidden id="vesselBarcode" name="vesselBarcode" value=''/>
            <stripes:hidden id="vesselPosition" name="vesselPosition" value=''/>
            <stripes:hidden id="vesselPositionReason" name="vesselPositionReason" value=''/>

            <div id="searchInput">
                    <label for="vesselBarcode">Vessel Barcode</label>
                    <input type="text" id="searchKey" name="searchKey">
                    <input type="submit" id="vesselSearch" name="vesselSearch" value="Find">
            </div>
            <div id="searchResults">

            <c:if test="${not actionBean.resultsAvailable}"> ${actionBean.resultSummaryString} </c:if>
            <c:if test="${not empty actionBean.foundVessels}">
                <div id="resultSummary">${actionBean.resultSummaryString} </div>

                <div id="accordion" style="display:none;" class="accordion">
                    <c:forEach items="${actionBean.foundVessels}" var="vessel" varStatus="status">
                        <div style="padding-left: 30px;padding-bottom: 2px">
                            <stripes:layout-render name="/vessel/vessel_info_header.jsp" bean="${actionBean}"
                                                   vessel="${vessel}"/>
                        </div>
                    </c:forEach>
                </div>
                </br>

                <c:choose>
                    <c:when test="${actionBean.isMultiplePositions}">
                        <p style="clear:both">
                            <label>Reason:</label>
                        </p>
                        <div>
                        <select class='filterDropdown' id="reasonCodeAllPositions" name="reasonCodeAllPositions">
                            <c:forEach items="${reasonCodes}" var="reasonValue" varStatus="reasonStatus">
                                <option value="${reasonValue}">${reasonValue}</option>
                            </c:forEach>
                        </select>
                        <c:choose>
                            <c:when test="${actionBean.isVesselAbandoned()}">
                                <security:authorizeBlock roles="<%= roles(Developer, PDM ,LabManager) %>">
                                    <stripes:submit id="unAbandonVessel" name="unAbandonVessel" value="Unbandon All Positions" class="btn btn-primary"/>
                                </security:authorizeBlock>
                            </c:when>
                            <c:otherwise>
                                <stripes:submit id="abandonAllPositions" name="abandonAllPositions" value="Abandon All Positions" class="btn btn-primary" onclick="abandonAllPositionReason()"/>
                            </c:otherwise>
                        </c:choose>

                            <div id="toggleText" style="display: none">
                                <stripes:submit id="abandonVessel" name="abandonVessel" value="abandon" class="btn btn-primary"/>
                            </div>
                        </div>

                        <h3>Please select specific position(s) to abandon. Otherwise, the entire chip will be abandoned.</h3>
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
                                        <c:set var="wellTest" value="${rowName}${columnName}"/>
                                        <td align="right">
                                            <c:choose>
                                            <c:when test="${actionBean.isPositionAbandoned(wellTest)}">
                                                <security:authorizeBlock roles="<%= roles(Developer, PDM ,LabManager) %>">
                                                <stripes:submit id="${rowName}${columnName}" name="unAbandonPosition" value="Unabandon" onclick="abandonPositions(this.id)" class="btn btn-primary ${actionBean.shrinkCss('btn-xs')}"/>
                                                </security:authorizeBlock>
                                            </c:when>
                                            <c:otherwise>
                                                <stripes:submit id="${rowName}${columnName}" name="abandonPosition" value="Abandon" onclick="abandonPositions(this.id)" class="btn btn-primary ${actionBean.shrinkCss('btn-xs')}"/>
                                            </c:otherwise>
                                            </c:choose>
                                            <select class="filterDropdown ${actionBean.shrinkCss('ddl-xs')}" id="reason_${rowName}${columnName}" name="reasonDdl">
                                                <c:forEach items="${reasonCodes}" var="reasonValue" varStatus="reasonStatus">
                                                    <option value="${reasonValue}">${reasonValue}</option>
                                                </c:forEach>
                                                <option selected="selected">${actionBean.getAbandonReason(wellTest)}</option>
                                            </select>
                                        </td>
                                    </c:forEach>
                                </tr>
                            </c:forEach>
                        </table>
                        </br>
                    </c:when>
                    <c:otherwise>
                        <div>
                            <c:choose>
                            <c:when test="${actionBean.isVesselAbandoned()}">
                                <security:authorizeBlock roles="<%= roles(Developer, PDM , LabManager) %>">
                                <stripes:submit id="unAbandonVessel" name="unAbandonVessel" value="Unabandon" class="btn btn-primary"/>
                                </security:authorizeBlock>
                            </c:when>
                            <c:otherwise>
                                <stripes:button id="abandonVesselItem" name="abandonVesselItem" value="Abandon Vessel" class="btn btn-primary"
                                                onclick="showAbandonDialog()"/>
                            </c:otherwise>
                            </c:choose>
                            <div id="toggleText" style="display: none">
                                <stripes:submit id="abandonVessel" name="abandonVessel" value="abandon" class="btn btn-primary"/>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>

            </c:if>
            <div id="abandonDialog" style="width:600px;display:none;">
                <p>Abandon Vessel</p>
                <p>Vessel Barcode (<span id="abandonVesselBarcode"> </span>)</p>
                <p style="clear:both">
                    <label>Reason:</label>
                </p>
                <select class='filterDropdown' id="reasonCode" name="reasonCode">
                    <c:forEach items="${reasonCodes}" var="reasonValue" varStatus="reasonStatus">
                        <option value="${reasonValue}">${reasonValue}</option>
                    </c:forEach>
                </select>
            </div>
        </div>
    </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
