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
                        <input type="hidden" id="multiStatusInput" name="multiEdit.status"/>
                        <stripes:select style='width:8em' id="multiStatus" class="multiEditSelect" name="" title=
"Changes the status of the selected rows:
 Queued - the designation is ready for FCT creation.
 Abandoned - the designation is hidden from further use.
 In FCT - the designation is on an FCT. It cannot be edited.">
                            <stripes:option value=""/>
                            <stripes:options-collection label="displayName" collection="${actionBean.utils.statusValues}"/>
                        </stripes:select>
                    </td>
                    <td>
                        <input type="hidden" id="multiPriorityInput" name="multiEdit.priority"/>
                        <stripes:select style='width:8em' id="multiPriority" class="multiEditSelect" name="">
                            <stripes:option value=""/>
                            <stripes:options-collection collection="${actionBean.utils.priorityValues}"/>
                        </stripes:select>
                    </td>
                    <td>
                        <input type="hidden" id="multiSeqModelInput" name="multiEdit.sequencerModel"/>
                        <stripes:select id="multiSeqModel" class="multiEditSelect" name="">
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
                        <input type="hidden" id="multiIndexTypeInput" name="multiEdit.indexType"/>
                        <stripes:select style='width:8em' id="multiIndexType" class="multiEditSelect" name="">
                            <stripes:option value=""/>
                            <stripes:options-collection collection="${actionBean.utils.indexTypes}"/>
                        </stripes:select>
                    </td>
                    <td>
                        <input style='width:7em' class="multiNumberCycles" name="multiEdit.numberCycles"/>
                    </td>
                    <td>
                        <input type="hidden" id="multiPoolTestInput" name="multiEdit.poolTest"/>
                        <stripes:select style='width:7em' id="multiPoolTest" class="multiEditSelect" name="">
                            <stripes:option value=""/>
                            <stripes:option value="false">No</stripes:option>
                            <stripes:option value="true">Yes</stripes:option>
                        </stripes:select>
                    </td>
                    <td>
                        &nbsp;<stripes:submit id="setMultipleBtn" name="setMultiple" value="Update Selected Rows"
                                              class="btn btn-primary" onclick="updateHiddenInputs()" disabled="disabled"
                                              title="Changes the selected rows and saves them.
 A blank edit field causes no change on the row."/>
                    </td>
                </tr>
            </table>
        </div>
