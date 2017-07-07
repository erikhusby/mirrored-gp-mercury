            <div class="control-group" style="padding-top: 50px;">
                <table id="tubeList" class="table simple">
                    <thead>
                    <tr>
                        <th class="width2">
                            <input for="count" type="checkbox" class="checkAll"/>
                            <span id="count" class="checkedCount"></span>
                        </th>
                        <th class="width4">Status</th>
                        <th class="width4">Priority</th>
                        <th class="width8">Tube Barcode</th>
                        <th class="width8">LCSET</th>
                        <th class="width8">Tube Type</th>
                        <th class="width10">Sequencer Model</th>
                        <th class="width4">Number Samples</th>
                        <th class="width4">Number Lanes</th>
                        <th class="width4">Loading Conc</th>
                        <th class="width10">Tube Created On</th>
                        <th class="width4">Read Length</th>
                        <th class="width4">Index Type</th>
                        <th class="width4">Paired End</th>
                        <th class="width4">Number Cycles</th>
                        <th class="width2">Pool Test</th>
                        <th class="width8">Regulatory Designation</th>
                        <th>Product</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.dtos}" var="dto" varStatus="item">
                        <tr>
                            <td class="width2">
                                <c:if test="${dto.status.modifiable && dto.regulatoryDesignation ne 'MIXED'}">
                                    <input type="checkbox" class="shiftCheckbox" style="margin-left: 40%"
                                           onchange="rowSelected()" id="checkbox_${item.index}"/>
                                </c:if>
                            </td>
                            <td class="width4">${dto.status.displayName}</td>
                            <td class="width4">${dto.priority}</td>
                            <td class="width8">
                                <!-- Tube barcode with link to Transfer Visualizer. -->
                                <stripes:link id="transferVisualizer" event="view" target="_blank"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean">
                                    <stripes:param name="barcodes" value="${dto.barcode}"/>
                                    ${dto.barcode}
                                </stripes:link>
                            </td>
                            <!-- LCSET with link to its JIRA ticket -->
                            <td class="width8">
                                <a href="${dto.lcsetUrl}" target="JIRA">${dto.lcset}</a>
                            </td>
                            <td class="width8">${dto.tubeType}</td>
                            <td class="width10">${dto.sequencerModel.displayName}</td>
                            <td class="width4">${dto.numberSamples}</td>
                            <td class="width4">
                                <input style='width:5em' class="numLanes" name="dtos[${item.index}].numberLanes" value="${dto.numberLanes}"
                                    ${not dto.status.modifiable ? 'readonly="readonly"' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="width4">
                                <input style='width:5em' class="loadConc" name="dtos[${item.index}].loadingConc" value="${dto.loadingConc}"
                                 ${not dto.status.modifiable ? 'readonly="readonly"' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="width10">${dto.tubeDate}</td>
                            <td class="width4">
                                <input style='width:5em' class="readLength" name="dtos[${item.index}].readLength" value="${dto.readLength}"
                                 ${not dto.status.modifiable ? 'readonly="readonly"' : ''} onkeypress="rowUpdated(${item.index})"/>
                            </td>
                            <td class="width4">${dto.indexType}</td>
                            <td class="width4">${dto.pairedEndRead?'Yes':'No'}</td>
                            <td class="width4">${dto.calculateCycles()}</td>
                            <td class="width2">${dto.poolTest?'Yes':'No'}</td>
                            <td class="width8">${dto.regulatoryDesignation}</td>
                            <td><span title="${dto.startingBatchVessels}">${dto.product}</span></td>

                            <input type="hidden" name="dtos[${item.index}].selected" id="checkbox_${item.index}_Input" value="false"/>
                            <input type="hidden" name="dtos[${item.index}].priority" value="${dto.priority}"/>
                            <input type="hidden" name="dtos[${item.index}].status" value="${dto.status}"/>
                            <input type="hidden" name="dtos[${item.index}].createdOn" value="${dto.createdOn}"/>
                            <input type="hidden" name="dtos[${item.index}].barcode" value="${dto.barcode}"/>
                            <input type="hidden" name="dtos[${item.index}].lcset" value="${dto.lcset}"/>
                            <input type="hidden" name="dtos[${item.index}].lcsetUrl" value="${dto.lcsetUrl}"/>
                            <input type="hidden" name="dtos[${item.index}].chosenLcset" value="${dto.chosenLcset}"/>
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

                            <input type="hidden" name="dtos[${item.index}].designationId" value="${dto.designationId}"/>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
