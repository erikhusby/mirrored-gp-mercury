<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationFctActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create FCT from Designated Loading Tubes" sectionTitle="Create FCT from Designated Loading Tubes">

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
                $j('#multiStatus').attr('value', '');
                $j('#multiPriority').attr('value', '');
                $j('#multiSeqModel').attr('value', '');
                $j('#multiIndexType').attr('value', '');
                $j('#multiPoolTest').attr('value', '');
            });

            function rowUpdated(rowIndex) {
                checkboxId = '#checkbox_' + rowIndex;
                $j(checkboxId).prop("checked", true);
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
                            <stripes:options-collection label="displayName" collection="${actionBean.utils.statusValues}"/>
                        </stripes:select>
                    </td>
                    <td>
                        <stripes:select style='width:8em' id="multiPriority" name="multiEdit.priority">
                            <stripes:option value=""/>
                            <stripes:options-collection collection="${actionBean.utils.priorityValues}"/>
                        </stripes:select>
                    </td>
                    <td>
                        <stripes:select id="multiSeqModel" name="multiEdit.sequencerModel">
                            <stripes:option value=""/>
                            <stripes:options-collection label="displayName" collection="${actionBean.utils.flowcellValues}"/>
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
                            <stripes:options-collection collection="${actionBean.utils.indexTypes}"/>
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
                <c:forEach items="${actionBean.dtos}" var="dto" varStatus="item">
                    <tr class="${dto.repeatedBarcode ? 'repeatedBarcode' : ''}">
                        <td class="tinyWidthColumn">
                            <c:if test="${dto.status.modifiable && dto.regulatoryDesignation ne 'MIXED'}">
                                <stripes:checkbox class="shiftCheckbox" style="margin-left: 40%" id="checkbox_${item.index}"
                                                  name="dtos[${item.index}].selected" checked="false"/>
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
                            <a href="${dto.lcsetUrl}" target="JIRA">${dto.lcset}</a><br/>${dto.additionalLcsetJoin}
                        </td>
                        <td class="fixedWidthColumn">${dto.tubeType}</td>
                        <td class="widerFixedWidthColumn">${dto.sequencerModel.displayName}</td>
                        <td class="smallerWidthColumn">${dto.numberSamples}</td>
                        <td class="smallerWidthColumn">
                            <input style='width:5em' class="numLanes" name="dtos[${item.index}].numberLanes" value="${dto.numberLanes}"
                                ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                        </td>
                        <td class="smallerWidthColumn">
                            <input style='width:5em' class="loadConc" name="dtos[${item.index}].loadingConc" value="${dto.loadingConc}"
                             ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                        </td>
                        <td class="widerFixedWidthColumn">${dto.tubeDate}</td>
                        <td class="smallerWidthColumn">
                            <input style='width:5em' class="readLength" name="dtos[${item.index}].readLength" value="${dto.readLength}"
                             ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                        </td>
                        <td class="smallerWidthColumn">${dto.indexType}</td>
                        <td class="smallerWidthColumn">
                            <input style='width:5em' class="numberCycles" name="dtos[${item.index}].numberCycles" value="${dto.numberCycles}"
                             ${not dto.status.modifiable ? 'disabled' : ''} onkeypress="rowUpdated(${item.index})"/>
                        </td>
                        <td class="tinyWidthColumn">${dto.poolTest?'Yes':'No'}</td>
                        <td class="fixedWidthColumn">${dto.regulatoryDesignation}</td>
                        <td><span title="${dto.startingBatchVessels}">${dto.productNameJoin}</span></td>

                        <input type="hidden" name="dtos[${item.index}].priority" value="${dto.priority}"/>
                        <input type="hidden" name="dtos[${item.index}].status" value="${dto.status}"/>
                        <input type="hidden" name="dtos[${item.index}].repeatedBarcode" value="${dto.repeatedBarcode}"/>
                        <input type="hidden" name="dtos[${item.index}].createdOn" value="${dto.createdOn}"/>
                        <input type="hidden" name="dtos[${item.index}].barcode" value="${dto.barcode}"/>
                        <input type="hidden" name="dtos[${item.index}].lcset" value="${dto.lcset}"/>
                        <input type="hidden" name="dtos[${item.index}].lcsetUrl" value="${dto.lcsetUrl}"/>
                        <input type="hidden" name="dtos[${item.index}].additionalLcsetJoin" value="${dto.additionalLcsetJoin}"/>
                        <input type="hidden" name="dtos[${item.index}].tubeType" value="${dto.tubeType}"/>
                        <input type="hidden" name="dtos[${item.index}].sequencerModel" value="${dto.sequencerModel}"/>
                        <input type="hidden" name="dtos[${item.index}].numberSamples" value="${dto.numberSamples}"/>
                        <input type="hidden" name="dtos[${item.index}].tubeDate" value="${dto.tubeDate}"/>
                        <input type="hidden" name="dtos[${item.index}].indexType" value="${dto.indexType}"/>
                        <input type="hidden" name="dtos[${item.index}].poolTest" value="${dto.poolTest?'Yes':'No'}"/>
                        <input type="hidden" name="dtos[${item.index}].regulatoryDesignation" value="${dto.regulatoryDesignation}"/>
                        <input type="hidden" name="dtos[${item.index}].productNameJoin" value="${dto.productNameJoin}"/>
                        <input type="hidden" name="dtos[${item.index}].startingBatchVessels" value="${dto.startingBatchVessels}"/>

                        <input type="hidden" name="dtos[${item.index}].designationId" value="${dto.designationId}"/>
                        <input type="hidden" name="dtos[${item.index}].tubeEventId" value="${dto.tubeEventId}"/>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>

        <div class="control-group">
            <stripes:submit id="createFctBtn" name="createFct" value="Create FCT"
                            class="btn btn-primary" style="margin: 0; margin-left: 30%;"/>
            <stripes:submit id="reloadDesignationBtn" name="reloadDesignations" value="Clear and Reload"
                            class="btn btn-primary"/>
        </div>

        <c:if test="${actionBean.createdFcts.size() > 0}">
            <div class="control-group">
                <hr style="margin: 0; margin-left: 50px"/>
                <h5 style="margin-left: 50px;">Created FCT Tickets</h5>
                <ol>
                    <c:forEach items="${actionBean.createdFcts}" var="fctUrl" varStatus="item">
                        <li>
                            <a target="JIRA" href={$fctUrl.right} class="external" target="JIRA">${fctUrl.left}</a>
                        </li>

                        <input type="hidden" name="createdFcts[${item.index}].left" value="${fctUrl.left}"/>
                        <input type="hidden" name="createdFcts[${item.index}].right" value="${fctUrl.right}"/>
                    </c:forEach>
                </ol>
            </div>
        </c:if>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
