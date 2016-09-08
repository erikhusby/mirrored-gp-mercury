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
                        <tr>
                            <td class="tinyWidthColumn">
                                <c:if test="${dto.status.modifiable && dto.regulatoryDesignation ne 'MIXED'}">
                                    <input type="checkbox" class="shiftCheckbox" style="margin-left: 40%"
                                           onchange="rowSelected()" id="checkbox_${item.index}"/>
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
                            <!-- LCSET with link to its JIRA ticket -->
                            <td class="fixedWidthColumn">
                                <a href="${dto.lcsetUrl}" target="JIRA">${dto.lcset}</a>
                            </td>
                            <td class="fixedWidthColumn">${dto.tubeType}</td>
                            <td class="widerFixedWidthColumn">${dto.sequencerModel.displayName}</td>
                            <td class="smallerWidthColumn">${dto.numberSamples}</td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="numLanes" name="dtos[${item.index}].numberLanes" value="${dto.numberLanes}"
                                    ${not dto.status.modifiable ? 'readonly="readonly"' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="loadConc" name="dtos[${item.index}].loadingConc" value="${dto.loadingConc}"
                                 ${not dto.status.modifiable ? 'readonly="readonly"' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="widerFixedWidthColumn">${dto.tubeDate}</td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="readLength" name="dtos[${item.index}].readLength" value="${dto.readLength}"
                                 ${not dto.status.modifiable ? 'readonly="readonly"' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="smallerWidthColumn">${dto.indexType}</td>
                            <td class="smallerWidthColumn">
                                <input style='width:5em' class="numberCycles" name="dtos[${item.index}].numberCycles" value="${dto.numberCycles}"
                                 ${not dto.status.modifiable ? 'readonly="readonly"' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="tinyWidthColumn">${dto.poolTest?'Yes':'No'}</td>
                            <td class="fixedWidthColumn">${dto.regulatoryDesignation}</td>
                            <td><span title="${dto.startingBatchVessels}">${dto.productNameJoin}</span></td>

                            <input type="hidden" name="dtos[${item.index}].selected" id="checkbox_${item.index}_Input" value="false"/>
                            <input type="hidden" name="dtos[${item.index}].priority" value="${dto.priority}"/>
                            <input type="hidden" name="dtos[${item.index}].status" value="${dto.status}"/>
                            <input type="hidden" name="dtos[${item.index}].createdOn" value="${dto.createdOn}"/>
                            <input type="hidden" name="dtos[${item.index}].barcode" value="${dto.barcode}"/>
                            <input type="hidden" name="dtos[${item.index}].lcset" value="${dto.lcset}"/>
                            <input type="hidden" name="dtos[${item.index}].lcsetUrl" value="${dto.lcsetUrl}"/>
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
