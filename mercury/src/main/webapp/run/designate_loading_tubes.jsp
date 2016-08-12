<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationLoadingTubeActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Designate Loading Tubes" sectionTitle="Designate Loading Tubes">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {

                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [[3, 'asc'], [4, 'asc']]
                    ],
                    "aoColumns":[
                        {"bSortable":false},  // selected
                        {"bSortable":false},  // status
                        {"bSortable":false},  // priority
                        {"bSortable":false},  // barcode
                        {"bSortable":true},  // lcset
                        {"bSortable":true},  // tube type
                        {"bSortable":true},  // sequencer model
                        {"bSortable":true},  // number samples
                        {"bSortable":true},  // number lanes
                        {"bSortable":true},  // loading conc
                        {"bSortable":true},  // tube create date
                        {"bSortable":true},  // read length
                        {"bSortable":true},  // index type
                        {"bSortable":true},  // number cycles
                        {"bSortable":true},  // pool test
                        {"bSortable":true},  // regulatory designation
                        {"bSortable":true},  // product
                    ]
                });

                // Clears fields.
                $j('#lcsetBarcodeText').removeAttr('value');
                $j('#dateRangeDivNaturalLanguageString').value('Custom');
                $j('#multiStatus').attr('value', '');
                $j('#multiPriority').attr('value', '');
                $j('#multiSeqModel').attr('value', '');
                $j('#multiIndexType').attr('value', '');
                $j('#multiPoolTest').attr('value', '');
            });

            function lcsetUpdated() {
                if (!($j('lcsetsBarcodes'.value).trim)) {
                    $j('#loadDenatureBtn').removeAttr("disabled");
                    $j('#loadPoolNormBtn').removeAttr("disabled");
                } else {
                    $j('#loadDenatureBtn').attr("disabled", "disabled");
                    $j('#loadPoolNormBtn').attr("disabled", "disabled");
                }
            };

            function rowUpdated(rowIndex) {
                checkboxId = 'checkbox' + rowIndex;
                $j(checkboxId).attr("checked", "checked");
            };

        </script>
        <style type="text/css">
            /* Fixed width columns except for product name. */
            #tubeList
            .fixedWidthColumn { width: 8em; word-wrap: break-word; }
            .smallerWidthColumn { width: 4em; }
            .tinyWidthColumn { width: 2em; }
            .widerFixedWidthColumn { width: 10em; }

            tr.repeatedBarcode { background: lightgrey; }
        </style>

    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
            <div class="control-group" syle="padding-bottom: 50px;">
                <div style="float: left; width: 66%;">
                    <div class="control-group">
                        <div style="float: left; width: 50%;">
                            <textarea style="width: 95%" id="lcsetBarcodeText" type="text" rows="3" name="lcsetsBarcodes"
                                      placeholder='LCSETs, Loading Tube Barcodes, or leave blank for all undesignated tubes.'
                                      onkeypress="lcsetUpdated()" title=
                                              "Either LCSET names (with or without the LCSET- prefix), or loading tube barcodes,
 or both, or leave blank and click 'Norm Tubes' to get all the undesignated loading
 tubes. The date range filter is always used for the tube lookups."
                                    ></textarea>
                        </div>

                        <div style="float: right; width: 50%" title=
                                "Used to restrict tube lookup when clicking 'Norm Tubes', 'Denature Tubes',
 or 'Pooled Norm Tubes'. Range applies to tube create date.

 Also restricts lookup of existing designation when clicking 'Show Designations'.
 Range applies to designation create date (not tube date).">
                            <div id="dateRangeDiv"
                                 rangeSelector="${actionBean.dateRange.rangeSelector}"
                                 startString="${actionBean.dateRange.startStr}"
                                 endString="${actionBean.dateRange.endStr}">
                            </div>
                            <p>Only items in this date range are shown.</p>
                        </div>
                    </div>

                    <div class="control-group"  style="position: absolute; left: 30%; transform: translate(-50%, -50%);">
                        <stripes:submit id="loadNormBtn" name="loadNorm" value="Norm Tubes" class="btn btn-primary"
                                        title="Adds normalization tubes to the designation display."/>
                        <stripes:submit id="loadDenatureBtn" name="loadDenature" value="Denature Tubes" class="btn btn-primary"
                                        title="Adds denature tubes to the designation display." disabled="disabled"/>
                        <stripes:submit id="loadPoolNormBtn" name="loadPoolNorm" value="Pooled Norm Tubes" class="btn btn-primary"
                                        title="Adds pooled normalization tubes to the designation display." disabled="disabled"/>
                    </div>
                </div>

                <div style="float: right; width: 33%;">
                    <stripes:checkbox id="showQueued" name="showQueued" checked="checked"/>
                    <stripes:label for="showQueued">Include In Queue</stripes:label>
                    <stripes:checkbox id="showProcessed" name="showProcessed"/>
                    <stripes:label for="showProcessed">Include On FCT</stripes:label>
                    <stripes:checkbox id="showAbandoned" name="showAbandoned"/>
                    <stripes:label for="showAbandoned">Include Abandoned</stripes:label>
                    <stripes:checkbox id="append" name="append"/>
                    <stripes:label for="append" title=
                            "Controls whether the current display of designations is either cleared,
 or left in place, before adding new designations to the display.">
                        Append to the list
                    </stripes:label>

                    <stripes:submit id="pending" name="pending" value="Show Designations" class="btn btn-primary"
                                    title="Click to show existing designations."/>
                </div>
            </div>

            <c:if test="${not empty actionBean.rowDtos}">
                <div class="control-group" style="padding-top: 50px;">
                    <table border="0">
                        <tr>
                            <th>Status</th>
                            <th>Priority</th>
                            <th>Sequencer Model</th>
                            <th>Number Lanes</th>
                            <th>Loading Conc</th>
                            <th>Read Length</th>
                            <th>Index Type</th>
                            <th>Number Cycles</th>
                            <th>Pool Test</th>
                            <th>&nbsp;</th>
                        </tr>
                        <tr>
                            <td>
                                <stripes:select style='width:8em' id="multiStatus" name="multiEdit.status">
                                    <stripes:option value=""/>
                                    <stripes:options-collection label="displayName" collection="${actionBean.statusValues}"/>
                                </stripes:select>
                            </td>
                            <td>
                                <stripes:select style='width:8em' id="multiPriority" name="multiEdit.priority">
                                    <stripes:option value=""/>
                                    <stripes:options-collection collection="${actionBean.priorityValues}"/>
                                </stripes:select>
                            </td>
                            <td>
                                <stripes:select id="multiSeqModel" name="multiEdit.sequencerModel">
                                    <stripes:option value=""/>
                                    <stripes:options-collection label="displayName" collection="${actionBean.flowcellValues}"/>
                                </stripes:select>
                            </td>
                            <td>
                                <input style='width:7em' class="multiNumberLanes" name="multiEdit.numberLanes"/>
                            </td>
                            <td>
                                <input style='width:7em' class="multiLoadConc" name="multiEdit.loadingConc"/>
                            </td>
                            <td>
                                <input style='width:7em' class="multiReadLength" name="multiEdit.readLength"/>
                            </td>
                            <td>
                                <stripes:select style='width:8em' id="multiIndexType" name="multiEdit.indexType">
                                    <stripes:option value=""/>
                                    <stripes:options-collection collection="${actionBean.indexTypes}"/>
                                </stripes:select>
                            </td>
                            <td>
                                <input style='width:7em' class="multiNumberCycles" name="multiEdit.numberCycles"/>
                            </td>
                            <td>
                                <stripes:select style='width:7em' id="multiPoolTest" name="multiEdit.poolTest">
                                    <stripes:option value=""/>
                                    <stripes:option value="false">No</stripes:option>
                                    <stripes:option value="true">Yes</stripes:option>
                                </stripes:select>
                            </td>
                            <td>
                                &nbsp;<stripes:submit id="setSelected" name="setMultiple" value="Update Selected Rows"
                                                      class="btn btn-primary" title="Changes the selected rows and saves them.
 If a field is left blank it is skipped."/>
                            </td>
                        </tr>
                    </table>
                </div>
            </c:if>


            <div class="control-group" style="padding-top: 50px;">
                <table id="tubeList" class="table simple">
                    <thead>
                    <tr>
                        <th class="tinyWidthColumn">
                            <input for="count" type="checkbox" class="checkAll"/>
                            <span id="count" class="checkedCount"></span>
                        </th>
                        <th class="smallerWidthColumn">Status</th>
                        <th class="smallerWidthColumn">Priority</th>
                        <th class="fixedWidthColumn">Tube Barcode</th>
                        <th class="fixedWidthColumn">LCSET</th>
                        <th class="fixedWidthColumn">Tube Type</th>
                        <th class="widerFixedWidthColumn">Sequencer Model</th>
                        <th class="smallerWidthColumn">Number Samples</th>
                        <th class="smallerWidthColumn">Number Lanes</th>
                        <th class="smallerWidthColumn">Loading Conc</th>
                        <th class="widerFixedWidthColumn">Tube Created On</th>
                        <th class="smallerWidthColumn">Read Length</th>
                        <th class="smallerWidthColumn">Index Type</th>
                        <th class="smallerWidthColumn">Number Cycles</th>
                        <th class="tinyWidthColumn">Pool Test</th>
                        <th class="fixedWidthColumn">Regulatory Designation</th>
                        <th>Product</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.rowDtos}" var="dto" varStatus="item">
                        <tr class="${dto.repeatedBarcode ? 'repeatedBarcode' : ''}">
                            <td class="tinyWidthColumn">
                                <c:if test="${dto.status.modifiable && dto.regulatoryDesignation ne 'MIXED'}">
                                    <stripes:checkbox class="shiftCheckbox" style="margin-left: 40%" id="${'checkbox' + item.index}"
                                                      name="rowDtos[${item.index}].selected"/>
                                </c:if>
                            </td>
                            <td class="smallerWidthColumn">${dto.status.displayName}</td>
                            <td class="smallerWidthColumn">${dto.priority}</td>
                            <td class="fixedWidthColumn">
                                <!-- Tube barcode with link to Transfer Visualizer. -->
                                <stripes:link id="transferVisualizer" event="view" target="_blank"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean">
                                    <stripes:param name="barcodes" value="${dto.barcode}"/>
                                    ${dto.barcode}
                                </stripes:link>
                            </td>
                            <!-- "Primary" LCSET with link to its JIRA ticket, and any additional LCSETs without links. -->
                            <td class="fixedWidthColumn">
                                <a href="${dto.lcsetUrl}" target="JIRA">${dto.primaryLcset}</a><br/>${dto.additionalLcsetJoin}
                            </td>
                            <td class="fixedWidthColumn">${dto.tubeType}</td>
                            <td class="widerFixedWidthColumn">${dto.sequencerModel.displayName}</td>
                            <td class="smallerWidthColumn">${dto.numberSamples}</td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="numLanes" name="rowDtos[${item.index}].numberLanes" value="${dto.numberLanes}"
                                    ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="loadConc" name="rowDtos[${item.index}].loadingConc" value="${dto.loadingConc}"
                                 ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="widerFixedWidthColumn">${dto.tubeDate}</td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="readLength" name="rowDtos[${item.index}].readLength" value="${dto.readLength}"
                                 ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="smallerWidthColumn">${dto.indexType}</td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="numberCycles" name="rowDtos[${item.index}].numberCycles" value="${dto.numberCycles}"
                                 ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="tinyWidthColumn">${dto.poolTest?'Yes':'No'}</td>
                            <td class="fixedWidthColumn">${dto.regulatoryDesignation}</td>
                            <td><span title="${dto.startingBatchVessels}">${dto.productNameJoin}</span></td>

                            <input type="hidden" name="rowDtos[${item.index}].priority" value="${dto.priority}"/>
                            <input type="hidden" name="rowDtos[${item.index}].status" value="${dto.status}"/>
                            <input type="hidden" name="rowDtos[${item.index}].repeatedBarcode" value="${dto.repeatedBarcode}"/>
                            <input type="hidden" name="rowDtos[${item.index}].createdOn" value="${dto.createdOn}"/>
                            <input type="hidden" name="rowDtos[${item.index}].barcode" value="${dto.barcode}"/>
                            <input type="hidden" name="rowDtos[${item.index}].primaryLcset" value="${dto.primaryLcset}"/>
                            <input type="hidden" name="rowDtos[${item.index}].lcsetUrl" value="${dto.lcsetUrl}"/>
                            <input type="hidden" name="rowDtos[${item.index}].additionalLcsetJoin" value="${dto.additionalLcsetJoin}"/>
                            <input type="hidden" name="rowDtos[${item.index}].tubeType" value="${dto.tubeType}"/>
                            <input type="hidden" name="rowDtos[${item.index}].sequencerModel" value="${dto.sequencerModel}"/>
                            <input type="hidden" name="rowDtos[${item.index}].numberSamples" value="${dto.numberSamples}"/>
                            <input type="hidden" name="rowDtos[${item.index}].tubeDate" value="${dto.tubeDate}"/>
                            <input type="hidden" name="rowDtos[${item.index}].indexType" value="${dto.indexType}"/>
                            <input type="hidden" name="rowDtos[${item.index}].poolTest" value="${dto.poolTest?'Yes':'No'}"/>
                            <input type="hidden" name="rowDtos[${item.index}].regulatoryDesignation" value="${dto.regulatoryDesignation}"/>
                            <input type="hidden" name="rowDtos[${item.index}].productNameJoin" value="${dto.productNameJoin}"/>
                            <input type="hidden" name="rowDtos[${item.index}].startingBatchVessels" value="${dto.startingBatchVessels}"/>

                            <input type="hidden" name="rowDtos[${item.index}].designationId" value="${dto.designationId}"/>
                            <input type="hidden" name="rowDtos[${item.index}].tubeEventId" value="${dto.tubeEventId}"/>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <div class="control-group">
                <stripes:submit id="createFctBtn" name="createFct" value="Create FCT" class="btn btn-primary"
                                style="margin: 0; margin-left: 30%;"/>
            </div>

            <c:if test="${actionBean.createdFcts.size() > 0}">
                <div class="control-group">
                    <hr style="margin: 0; margin-left: 50px"/>
                    <h5 style="margin-left: 50px;">Created FCT Tickets</h5>
                    <ol>
                        <c:forEach items="${actionBean.createdFcts}" var="fct" varStatus="item">
                            <li>
                                <a target="JIRA" href={$fct.url} class="external" target="JIRA"></a>${fct.name}
                            </li>

                            <input type="hidden" name="createdFcts[${item.index}].url" value="${fct.url}"/>
                            <input type="hidden" name="createdFcts[${item.index}].name" value="${fct.name}"/>
                        </c:forEach>
                    </ol>
                </div>
            </c:if>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
