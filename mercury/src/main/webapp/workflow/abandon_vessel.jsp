<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Abandon Vessel" sectionTitle="Abandon Vessel">

    <stripes:layout-component name="extraHead">

        <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.presentation.workflow.abandonvesselactionbean"--%>

        <script type="text/javascript">
            $(function() {
                $('#reasonCode').change(function() {
                    var x = $(this).val();
                    $('#abandonComment').val(x);
                });
            });

            function showAbandonDialog() {
                $j("#dialogAction").attr("name", "abandonVesselItem");
                $j("#abandonVesselBarcode").text($("#vesselLabel").val());
                $j("#abandonDialog").dialog("open").dialog("option", "width", 600);
            }


            $j(document).ready(function () {

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

                function searchRegulatoryInfo() {
                    $j.ajax({
                        type: "POST",
                        url: '${ctxpath}/workflow/AbandonVessel.action',
                        dataType: 'html'
                    });

                }

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
            <stripes:hidden id="vesselBarcode" name="vesselBarcode" value=""/>


        <div id="searchInput">
            <%--<stripes:form  beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean">--%>
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
                    <div>
                        <stripes:button id="abandonVesselItem" name="abandonVesselItem" value="Abandon Vessel" class="btn btn-primary"
                                        onclick="showAbandonDialog()"/>
                        <security:authorizeBlock roles="<%= roles(LabManager) %>">
                                <stripes:submit id="unAbandonVessel" name="unAbandonVessel" value="un-abandon" class="btn btn-primary"/>
                        </security:authorizeBlock>
                        <div id="toggleText" style="display: none">
                            <stripes:submit id="abandonVessel" name="abandonVessel" value="abandon" class="btn btn-primary"/>
                        </div>
                    </div>
            </c:if>
            <div id="abandonDialog" style="width:600px;display:none;">
                <p>Abandon Vessel</p>
                <p>Vessel Barcode (<span id="abandonVesselBarcode"> </span>)</p>
                <p style="clear:both">
                    <label>Reason:</label>
                </p>

                <select class='filterDropdown' id="reasonCode" name="reasonCode">
                    <option value="Failed QC">Failed QC</option>
                    <option value="Lab incident">Lab incident</option>
                    <option value="Equipment failure">Equipment failure</option>
                    <option value="Depleted">Depleted</option>
                </select>
            </div>
        </div>
    </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
