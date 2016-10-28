        <div class="control-group" style="padding-top: 50px;">
            <table border="0">
                <tr>
                        <th class="width4"/>
                        <th class="width8">Status</th>
                        <th class="width8">Priority</th>
                        <th class="width24"/>
                        <th class="width16">Sequencer Model</th>
                        <th class="width8"/>
                        <th class="width4">Number Lanes</th>
                        <th class="width4">Loading Conc</th>
                        <th class="width16"/>
                        <th class="width4">Read Length</th>
                        <th class="width4">Index Type</th>
                        <th class="width4">Paired End</th>
                        <th class="width4"/>
                        <th class="width4">Pool Test</th>
                        <th class="width24"/>
                        <th/>
                </tr>
                <tr>
                    <td/>
                    <td>
                        <input type="hidden" id="multiStatusInput" name="multiEdit.status"/>
                        <stripes:select style='width:8em' id="multiStatus" class="multiEditSelect" name="" title=
"unsaved - any changes to the row will not be saved.
 Queued - the designation is saved and ready for FCT creation.
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
                    <td/>
                    <td>
                        <input type="hidden" id="multiSeqModelInput" name="multiEdit.sequencerModel"/>
                        <stripes:select style='width:16em' id="multiSeqModel" class="multiEditSelect" name="">
                            <stripes:option value=""/>
                            <stripes:options-collection label="displayName" collection="${actionBean.utils.flowcellValues}"/>
                        </stripes:select>
                    </td>
                    <td/>
                    <td>
                        <input style='width:4em' class="multiNumberLanes" name="multiEdit.numberLanes"/>
                    </td>
                    <td>
                        <input style='width:4em' class="multiLoadConc" name="multiEdit.loadingConc"/>
                    </td>
                    <td/>
                    <td>
                        <input style='width:4em' class="multiReadLength" name="multiEdit.readLength"/>
                    </td>
                    <td>
                        <input type="hidden" id="multiIndexTypeInput" name="multiEdit.indexType"/>
                        <stripes:select style='width:8em' id="multiIndexType" class="multiEditSelect" name="" title=
"Index type is used when generating the setup read structure,
 when calculating the number of cycles, and when determining
 which designations can be combined on one flowcell.">
                            <stripes:option value=""/>
                            <stripes:options-collection collection="${actionBean.utils.indexTypes}"/>
                        </stripes:select>
                    </td>
                    <td>
                        <input type="hidden" id="multiPairedEndInput" name="multiEdit.pairedEndRead"/>
                        <stripes:select style='width:6em' id="multiPairedEnd" class="multiEditSelect" name="">
                            <stripes:option value=""/>
                            <stripes:option value="false">No</stripes:option>
                            <stripes:option value="true">Yes</stripes:option>
                        </stripes:select>
                    </td>
                    <td/>
                    <td>
                        <input type="hidden" id="multiPoolTestInput" name="multiEdit.poolTest"/>
                        <stripes:select style='width:6em' id="multiPoolTest" class="multiEditSelect" name="">
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
