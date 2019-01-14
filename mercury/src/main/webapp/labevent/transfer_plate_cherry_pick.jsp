<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.StockSampleType" %>
<%--
This fragment is used by manual_transfer.jsp to re-use code when displaying a source plate / rack and a target
plate / rack.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/resources/layout/taglibs.jsp" %>


<%--@elvariable id="stationEvent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType"--%>
<%--@elvariable id="plate" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType"--%>
<%--@elvariable id="positionMap" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType"--%>
<%--@elvariable id="actionBean" type="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"--%>
<%--@elvariable id="vesselTypeGeometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry"--%>
<%--@elvariable id="section" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"--%>
<%--@elvariable id="stationEventIndex" type="java.lang.Integer"--%>
<%--@elvariable id="source" type="java.lang.Boolean"--%>
<%--@elvariable id="massRemoved" type="java.lang.Boolean"--%>

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
        padding: 0.5px 2px;
        font-size: 8px;
        border-radius: 8px;
        text-align: right;
        background-image:none;
    }

    .xs-col{
        background-color: #33cc00;
    }

    .xs-all{
        background-color: #c266ff;
    }

    .xs-row{
        padding: 0.5px 2px;
        font-size: 8px;
        border-radius: 8px;
        text-align: right;
        background-image:none;
        background-color: #ff8c1a;
    }

</style>
<script type="text/javascript">
    $j(document).ready(function () {
        // Update the destination fields with the value entered in the update all destinations field.
        $j('#updateAllDestinationVolumes').change(function () {
            $j('[id^=destRcpVol]').val($(this).val());
        });
    });
</script>

<c:set var="sourceVolRemove" value="${source and actionBean.labEventType.removeDestVolFromSource()}"/>
<%-- todo jmt id is not unique --%>
<div class="control-group vessel-container" id="container0" data-direction="${source ? "src" : "dest"}" data-event-index="${stationEventIndex}">

    <c:if test="${!source and actionBean.labEventType.enableStockTypeSelection()}">
        <p class="control-group">
            <stripes:label for="markStockType" style="padding-right:30px;">Select a stock type to mark destinations:</stripes:label>
            <c:forEach items="<%= StockSampleType.values()%>" var="stockType">
                <stripes:label for="markStockType">${stockType.name}</stripes:label>

                <input type="radio" id="selectedStockType${stationEventIndex}"
                       name="selectedStockType"
                       class="clearable smalltext" autocomplete="off"
                       value="${stockType.name}"
                       <c:if test="${stockType.name == actionBean.selectedStockType}">checked</c:if>
                />
            </c:forEach>
        </p>
    </c:if>

    <c:if test="${!source}">
        <p class="control-group">
            <label for="updateAllDestinationVolumes">Update All Destination Volumes:</label><input type="text" id="updateAllDestinationVolumes"/>
        </p>
    </c:if>

    <label>Type </label>${plate[0].physType}
    <input type="hidden" name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate[0]' : 'plate[0]'}.physType"
           value="${plate[0].physType}"/>

    <label for="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}">Barcode</label>
    <input type="text" ${vesselTypeGeometry.barcoded ? "" : "readonly"} id="${source ? 'src' : 'dst'}PltBcd${stationEventIndex}" autocomplete="off"
           name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate[0]' : 'plate[0]'}.barcode"
           value="${plate[0].barcode}" class="container-barcode ${vesselTypeGeometry.barcoded ? "clearable" : ""} barcode unique" ${stationEventIndex == 0 ? "required" : ""}/>

    <c:if test="${stationEvent.class.simpleName == 'PlateCherryPickEvent'}">
        <div style="display: none;">
        <stripes:label for="${source ? 'src' : 'dst'}Section">Section</stripes:label>
        <c:choose>
            <c:when test="${not empty section}">
                ${section.sectionName}
                <stripes:hidden name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate[0]' : 'plate[0]'}.section"
                                value="${section.sectionName}"/>
            </c:when>
            <c:otherwise>
                <stripes:select name="stationEvents[${stationEventIndex}].${source ? 'sourcePlate[0]' : 'plate[0]'}.section"
                                id="${source ? 'src' : 'dst'}Section${stationEventIndex}">
                    <stripes:options-enumeration
                            enum="org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection"
                            label="sectionName"/>
                </stripes:select>
            </c:otherwise>
        </c:choose>
            <c:if test="${source and massRemoved}">
                <label for="${source ? 'src' : 'dst'}PltMass${stationEventIndex}">Mass To Remove</label>
                <input type="text" id="${source ? 'src' : 'dst'}PltMass${stationEventIndex}" autocomplete="off"
                       name="massesRemoved[${stationEventIndex}]"
                       value="${actionBean.massesRemoved[stationEventIndex]}" class="barcode"/>

            </c:if>
        </div>
    </c:if>
    <c:if test="${not empty positionMap}">
        <c:set var="geometry" value="${vesselTypeGeometry.vesselGeometry}"/>
        <%--@elvariable id="geometry" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry"--%>
        <c:if test="${vesselTypeGeometry.displayName.startsWith('Matrix96')}">

            <c:if test="${source}">
                <!-- Adds the dropdowns for lab and scanner, and possibly a file chooser. -->
                <stripes:layout-render name="/vessel/rack_scanner_list_with_sim_part2.jsp" bean="${actionBean}"/>
            </c:if>
            <div class="controls">
                <stripes:submit value="Scan" id="scanBtn_${source}" class="btn btn-primary"
                                onclick="this.form['scanIndex'].value='${stationEventIndex}';this.form['scanSource'].value='${source}';"
                                name="<%= ManualTransferActionBean.RACK_SCAN_EVENT %>"/>
            </div>

            Or hand scan 2D barcodes.

        </c:if>

        <c:if test="${sourceVolRemove eq 'true'}">
            <stripes:label for="srcDepleteAll">Deplete All</stripes:label>
            <input type="checkbox" id="srcDepleteAll${stationEventIndex}"
                   name="depleteAll[${stationEventIndex}]"
                   class="clearable smalltext" autocomplete="off"
                    <c:if test="${actionBean.depleteAll[stationEventIndex]}"> checked="checked" </c:if>/>
        </c:if>

        <%-- todo jmt id is not unique --%>
        <table id="${source ? 'src' : 'dest'}_TABLE_${vesselTypeGeometry.vesselGeometry}">
              <c:forEach items="${geometry.rowNames}" var="rowName" varStatus="rowStatus">
                <c:if test="${rowStatus.first}">
                    <tr>
                        <td><div style="float:right;"><button id="selectAll${source ? 'src' : 'dest'}" type="button" class="btn btn-primary btn-xs xs-all">Select</button></div></td>
                        <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                            <td nowrap><div style="float:left;">${columnName}</div> <div style="float:right;"> <button id="btn_${source ? 'src' : 'dest'}_col_${columnName}" type="button" class="btn btn-primary btn-xs xs-col">Select</button></div></td>
                        </c:forEach>
                    </tr>
                </c:if>
                <tr>
                    <c:set var="paddingStyle" value="${(sourceVolRemove eq 'true') ? 'style=\"padding-top: 10px\"' : ''}" />

                    <td ${paddingStyle}>${rowName} </br><button id="btn_${source ? 'src' : 'dest'}_row_${rowName}" type="button" class="btn btn-primary btn-xs xs-row" tabindex="-1">Select</button></td>
                    <c:set var="volumeType"
                            value="${(source && actionBean.labEventTypeByIndex(stationEventIndex).manualTransferDetails.sourceVolume()) || (!source && actionBean.labEventTypeByIndex(stationEventIndex).manualTransferDetails.targetVolume()) ? 'text' : 'hidden'}"/>
                    <c:forEach items="${geometry.columnNames}" var="columnName" varStatus="columnStatus">
                        <c:set var="receptacleIndex"
                                value="${rowStatus.index * geometry.columnCount + columnStatus.index}"/>
                        <td align="left" ${paddingStyle}>
                            <c:if test="${empty rowName}">${geometry.vesselPositions[receptacleIndex]}</c:if>
                            <input type="text" id="${source ? 'src' : 'dest'}RcpBcd${stationEventIndex}_${receptacleIndex}"
                                   name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap[0]' : 'positionMap[0]'}.receptacle[${receptacleIndex}].barcode"
                                   value="${actionBean.findReceptacleAtPosition(positionMap[0], geometry.vesselPositions[receptacleIndex]).barcode}"
                                   class="clearable smalltext unique" autocomplete="off" placeholder="barcode"/>
                            <input type="hidden"
                                   name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap[0]' : 'positionMap[0]'}.receptacle[${receptacleIndex}].position"
                                   value="${geometry.vesselPositions[receptacleIndex]}"/>
                            <input type="hidden"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}[0].receptacle[${receptacleIndex}].receptacleType"
                                    value="${source ? actionBean.labEventTypeByIndex(stationEventIndex).manualTransferDetails.sourceBarcodedTubeType : actionBean.labEventTypeByIndex(stationEventIndex).manualTransferDetails.targetBarcodedTubeType}"/>
                            <c:if test="${volumeType == 'text'}">
                                </br>
                            </c:if>
                            <input type="${volumeType}" id="${source ? 'src' : 'dest'}RcpVol${stationEventIndex}_${receptacleIndex}"
                                    id="${source ? 'src' : 'dest'}RcpVol${stationEventIndex}_${receptacleIndex}"
                                    name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}[0].receptacle[${receptacleIndex}].volume"
                                    value="${actionBean.findReceptacleAtPosition(positionMap[0], geometry.vesselPositions[receptacleIndex]).volume}"
                                    class="clearable smalltext" autocomplete="off" placeholder="volume"/>
                            <input type="${(sourceVolRemove eq 'true') ? 'checkbox' : 'hidden'}"
                                   id="${source ? 'src' : 'dest'}RcpVolChk${stationEventIndex}_${receptacleIndex}"
                                   name="mapPositionToDepleteFlag[${geometry.vesselPositions[receptacleIndex]}]"
                                   class="clearable smalltext" autocomplete="off"
                                    <c:if test="${source && actionBean.mapPositionToDepleteFlag[geometry.vesselPositions[receptacleIndex]]}"> checked="checked" </c:if>/>
                            <c:if test="${not empty actionBean.labEventTypeByIndex(stationEventIndex).resultingMaterialType && !source}">
                                <%-- This is primarily for messages forwarded to BSP --%>
                                <input type="hidden"
                                        name="stationEvents[${stationEventIndex}].${source ? 'sourcePositionMap' : 'positionMap'}[0].receptacle[${receptacleIndex}].materialType"
                                        value="${actionBean.labEventTypeByIndex(stationEventIndex).resultingMaterialType.displayName}"/>
                            </c:if>
                            </br>
                            <button data-position="${geometry.vesselPositions[receptacleIndex]}" id="${rowName}${columnName}_${source ? 'src' : 'dest'}_RcpBcd${stationEventIndex}_${receptacleIndex}" type="button" class= "${source ? 'src' : 'dest'}_col_${columnStatus.index} ${source ? 'src' : 'dest'}_row_${rowStatus.index} btn btn-primary btn-xs" disabled tabindex="-1">Select</button>
                        </td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </table>
    </c:if>
</div>
