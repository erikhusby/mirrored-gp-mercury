<%-- Prompts user to select the correct lcset for a loading tube, as part of the process of creating designation tubes. --%>

<stripes:layout-render name="/layout.jsp" pageTitle="Select Loading Tube LCSET" sectionTitle="Select Loading Tube LCSET">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {

                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[[0, 'asc']],
                    "aoColumns":[
                        {"bSortable":false},  // barcode
                        {"bSortable":false},  // tube date
                        {"bSortable":false}   // lcset
                    ]
                });

            });

        </script>
        <style type="text/css">
            #tubeList
            .fixedWidth { width: 8em; word-wrap: break-word; }
            .wider { width: 20em; word-wrap: break-word; }
        </style>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">

            <c:if test="${fn:contains(actionBean.class.name, 'DesignationActionBean')}">
                <input type='hidden' name="dateRangeStart" value="${actionBean.dateRange.start}"/>
                <input type='hidden' name="dateRangeEnd" value="${actionBean.dateRange.end}"/>
                <input type='hidden' name="dateRangeSelector" value="14"/> <!-- Set to "custom" otherwise dates are not saved. -->
                <input type='hidden' name="lcsetsBarcodes" value="${actionBean.lcsetsBarcodes}"/>
                <input type='hidden' name="tubeLcsetSelectionCount" value="${actionBean.tubeLcsetAssignments.size()}"/>
            </c:if>

            <c:forEach items="${actionBean.dtos}" var="dto" varStatus="item">
                <input type="hidden" name="dtos[${item.index}].priority" value="${dto.priority}"/>
                <input type="hidden" name="dtos[${item.index}].status" value="${dto.status}"/>
                <input type="hidden" name="dtos[${item.index}].createdOn" value="${dto.createdOn}"/>
                <input type="hidden" name="dtos[${item.index}].barcode" value="${dto.barcode}"/>
                <input type="hidden" name="dtos[${item.index}].lcset" value="${dto.lcset}"/>
                <input type="hidden" name="dtos[${item.index}].chosenLcset" value="${dto.chosenLcset}"/>
                <input type="hidden" name="dtos[${item.index}].lcsetUrl" value="${dto.lcsetUrl}"/>
                <input type="hidden" name="dtos[${item.index}].tubeType" value="${dto.tubeType}"/>
                <input type="hidden" name="dtos[${item.index}].sequencerModel" value="${dto.sequencerModel}"/>
                <input type="hidden" name="dtos[${item.index}].numberSamples" value="${dto.numberSamples}"/>
                <input type="hidden" name="dtos[${item.index}].tubeDate" value="${dto.tubeDate}"/>
                <input type="hidden" name="dtos[${item.index}].indexType" value="${dto.indexType}"/>
                <input type="hidden" name="dtos[${item.index}].pairedEndRead" value="${dto.pairedEndRead?'Yes':'No'}"/>
                <input type="hidden" name="dtos[${item.index}].poolTest" value="${dto.poolTest?'Yes':'No'}"/>
                <input type="hidden" name="dtos[${item.index}].regulatoryDesignation" value="${dto.regulatoryDesignation}"/>
                <input type="hidden" name="dtos[${item.index}].product" value="${dto.product}"/>
                <input type="hidden" name="dtos[${item.index}].startingBatchVessels" value="${dto.startingBatchVessels}"/>

                <input type="hidden" name="dtos[${item.index}].numberLanes" value="${dto.numberLanes}"/>
                <input type="hidden" name="dtos[${item.index}].loadingConc" value="${dto.loadingConc}"/>
                <input type="hidden" name="dtos[${item.index}].readLength" value="${dto.readLength}"/>


                <input type="hidden" name="dtos[${item.index}].designationId" value="${dto.designationId}"/>
            </c:forEach>

            <p>These tubes were found to have samples in multiple LCSETs.</p>
            <p>Please select which LCSET should be used for the flowcell designation.</p>
            <p>If no selection is made then the tube will not be used to create a designation.</p>

            <div class="control-group">
                <div style="float: left; width: 50%;">
                    <table id="tubeList" class="table simple">
                        <thead>
                        <tr>
                            <th>Tube Barcode</th>
                            <th>Tube Create Date</th>
                            <th>LCSET</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.tubeLcsetAssignments}" var="dto" varStatus="item">
                            <tr>
                                <td>
                                    <stripes:link id="transferVisualizer" event="view" target="_blank"
                                                  beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean">
                                        <stripes:param name="barcodes" value="${dto.barcode}"/>
                                        ${dto.barcode}
                                    </stripes:link>
                                </td>
                                <td>
                                    <fmt:formatDate value="${dto.tubeDate}" pattern="MM/dd/yyyy HH:mm a"/>
                                </td>
                                <td>
                                        <c:forEach items="${dto.lcsetNameUrls}" var="lcsetPair" varStatus="lcsetPairStatus">
                                            <c:if test="${lcsetPairStatus.index > 0}">
                                                <br/>
                                            </c:if>
                                            <stripes:radio value="${lcsetPair.left}" id="select_${dto.barcode}_${lcsetPair.left}"
                                                           name="tubeLcsetAssignments[${item.index}].selectedLcsetName"/>
                                            <a href="${lcsetPair.right}" target="JIRA">${lcsetPair.left}</a>
                                        </c:forEach>
                                </td>

                                <input type="hidden" name="tubeLcsetAssignments[${item.index}].barcode" value="${dto.barcode}"/>
                                <input type="hidden" name="tubeLcsetAssignments[${item.index}].designationId" value="${dto.designationId}"/>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div style="float: right; width: 50%;"></div>
            </div>
            <div class="control-group">
                <stripes:submit id="submitTubeLcsets" name="submitTubeLcsets" value="Continue" class="btn btn-primary"
                                title="Click to proceed with creating the designations."/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
