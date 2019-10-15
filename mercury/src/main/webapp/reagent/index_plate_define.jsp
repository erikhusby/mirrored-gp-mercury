<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
 Makes an index plate definition (a template) from an upload of plate wells and their molecular index names.
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Index Plate Definition" sectionTitle="Index Plate Definition">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function() {
                managementLayout(<c:if test="${actionBean.managePage}">true</c:if>);
                layoutNames(true);
            });

            function managementLayout(isManagement) {
                if (isManagement) {
                    $j(".creationLayout").hide();
                    $j(".managementLayout").show();
                    $j("#selectManage").attr('checked', 'checked');
                    if (${not empty actionBean.unusedAndInUse.right}) {
                        $j("#inUseBarcodesDiv").css('display', 'block');
                        $j("#inUsePlateBarcodes").val("${actionBean.unusedAndInUse.right}");
                    }
                    if (${not empty actionBean.unusedAndInUse.left}) {
                        $j("#unusedBarcodesDiv").css('display', 'block');
                        $j("#unusedPlateBarcodes").val("${actionBean.unusedAndInUse.left}");
                    }
                } else {
                    $j(".creationLayout").show();
                    $j(".managementLayout").hide();
                    $j("#selectCreate").attr('checked', 'checked');
                }
            }

            function layoutNames(isName) {
                if (isName) {
                    $j("#plateLayoutSequences").hide();
                    $j("#plateLayoutNames").show();
                    $j("#showIndexNames").attr('checked', 'checked');
                } else {
                    $j("#plateLayoutSequences").show();
                    $j("#plateLayoutNames").hide();
                    $j("#showIndexSequences").attr('checked', 'checked');
                }
            }

            function plateNameChange() {
                $j("#plateContents").hide();
                $j("#inUseBarcodesDiv").css('display', 'none');
                $j("#unusedBarcodesDiv").css('display', 'none');
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <style type="text/css">
            div.inputGroup {
                display: table;
            }
            div.inputGroup > div.inputRow {
                display: table-row;
            }
            div.inputGroup > div.inputRow > div.firstCol {
                display: table-cell;
                width: 10em;
                vertical-align: middle;
                padding-top: 15px;
                padding-right: 20px;
            }
            div.inputGroup > div.inputRow > div.controls {
                display: table-cell;
                vertical-align: middle;
                padding-top: 15px;
            }
            text, textarea, .firstCol, .controls {
                font-size: 12px;
                font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            }
            label, input[type=radio] {
                display:inline;
                vertical-align: middle;
            }
            td.layoutCell {
                horiz-align: center;
                width: 10em;
            }
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="plateDefinitionForm">
            <c:forEach items="${actionBean.plateNameSelection}" varStatus="item">
                <input type="hidden" name="plateNameSelection[${item.index}]"
                       value="${actionBean.plateNameSelection[item.index]}"/>
            </c:forEach>
            <c:forEach items="${actionBean.definitionToBarcode}" varStatus="item">
                <input type="hidden" name="definitionToBarcode[${item.index}]"
                       value="${actionBean.definitionToBarcode[item.index]}"/>
            </c:forEach>

            <!-- Radio button group for type of operation. -->
            <div class="inputGroup" title="Select the type of operation to perform.">
                <div class="inputRow">
                    <div class="firstCol">Operation</div>
                    <div class="control-group controls">
                        <span>
                            <input type="radio" id="selectCreate" name="managePage" value="false" onclick="managementLayout(false)"/>
                            <label for="selectCreate">Create new plate definition</label>
                        </span>
                        <span style="padding-left: 20px;">
                            <input type="radio" id="selectManage" name="managePage" value="true" onclick="managementLayout(true)"/>
                            <label for="selectManage">Manage existing plate definition</label>
                        </span>
                    </div>
                </div>
            </div>

            <!-- -------------------- The layout for creating plate definitions -------------------- -->

            <div class="creationLayout">
                <!-- Radio button group for plate geometry: 96 or 384 well. -->
                <div class="inputGroup">
                    <div class="inputRow">
                        <div class="firstCol">Plate Size</div>
                        <div class="control-group controls">
                        <span>
                            <input type="radio" id="select96well" name="plateGeometry" value="96"
                                   <c:if test="${actionBean.plateGeometry eq '96'}">checked="checked"</c:if>
                            />
                            <label for="select96well">96 well</label>
                        </span>
                            <span style="padding-left: 20px;">
                            <input type="radio" id="select384well" name="plateGeometry" value="384"
                                   <c:if test="${actionBean.plateGeometry eq '384'}">checked="checked"</c:if>
                            />
                            <label for="select384well">384 well</label>
                        </span>
                        </div>
                    </div>
                </div>

                <!-- Selector for reagent type (adapter, primer, etc.) -->
                <div class="inputGroup">
                    <div class="inputRow">
                        <div class="firstCol">Reagent Type</div>
                        <div class="control-group controls">
                            <stripes:select name="reagentType" id="reagentType">
                                <stripes:options-collection collection="${actionBean.reagentTypes}"/>
                            </stripes:select>
                        </div>
                    </div>
                </div>

                <!-- Text box for plate name. -->
                <div class="inputGroup" title="Provide a name for the index plate definition.
An error is given if the name has been used before.">
                    <div class="inputRow">
                        <div class="firstCol">Plate Definition Name</div>
                        <div class="control-group controls">
                            <stripes:text id="plateName" name="plateName" value="${actionBean.plateName}"/>
                        </div>
                    </div>
                </div>

                <!-- Checkbox for overwrite. -->
                <div class="inputGroup" title="Indicate that you want to replace an existing plate definition
with a different one. It must not have existing index plate
instances. If it does, you can remove unused ones using the
'Index Plate Instances' page.">
                    <div class="inputRow">
                        <div class="firstCol">Replace Existing</div>
                        <div class="control-group controls">
                            <stripes:checkbox id="replaceExisting" name="replaceExisting"/>
                        </div>
                    </div>
                </div>

                <div style="float: left; width: 50%;">

                    <!-- File chooser to upload Excel spreadsheet of well positions & index scheme names. -->
                    <div class="inputGroup" title="Provide an Excel spreadsheet.">
                        <div class="inputRow">
                            <div class="firstCol">Spreadsheet</div>
                            <div class="control-group controls">
                                <stripes:file name="spreadsheet" id="spreadsheet"/>
                            </div>
                        </div>
                    </div>

                    <!-- Button to "Create Plate Definition". -->
                    <div class="inputGroup" title="Upload and create/update the plate definition.">
                        <div class="inputRow">
                            <div class="firstCol"></div>
                            <div class="controls">
                                <stripes:submit id="createDefinition" name="createDefinition" value="Create" class="btn btn-primary"/>
                            </div>
                        </div>
                    </div>
                </div>
                <div style="float: right; width: 50%;">
                    <p>Accepts an Excel spreadsheet.
                    <ul>
                        <li>The header row is optional. If present it is ignored, but it must not have<br/>
                            a well position in column A or else it will be mistaken for a data row.</li>
                        <li>The data rows should have two columns.<br/>
                            Column A should contain the well position (A1 or A01, etc.).<br/>
                            Column B should contain the molecular index name (such as Illumina_P5-Wodel_P7-Zajic).
                        </li>
                    </ul>
                    </p>
                </div>
            </div>


            <!-- -------------------- A different layout used for management. -------------------- -->


            <div class="managementLayout">
                <!-- Selects a plate definition. -->
                <div class="inputGroup" title="Select a plate definition to manage.">
                    <div class="inputRow">
                        <div class="firstCol">Plate Definition Name</div>
                        <div class="control-group controls">
                            <stripes:select id="selectedPlateName" name="selectedPlateName"
                                            value="${actionBean.selectedPlateName}" onchange="plateNameChange()">
                                <stripes:option value=" "/>
                                <stripes:options-collection collection="${actionBean.plateNameSelection}"/>
                            </stripes:select>
                        </div>
                    </div>
                </div>

                <!-- Generates a layout (positions and contents) for an index plate definition. -->
                <div class="inputGroup" title="Shows a grid of the plate with well position and content.">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="controls">
                            <stripes:submit id="findLayout" name="findLayoutD" value="Show Layout" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>

                <!-- Displays the layout data. -->
                <c:if test="${not empty actionBean.plateLayoutNames}">
                    <!-- Radio button group to flip between names and sequences. -->
                    <div class="inputGroup" title="Show index names or show index sequences.">
                        <div class="inputRow">
                            <div class="control-group controls">
                                <span>
                                    <input type="radio" id="showIndexNames" name="namesOrSequences" value="true" onclick="layoutNames(true)"/>
                                    <label for="showIndexNames">Show names</label>
                                </span>
                                <span style="padding-left: 20px;">
                                    <input type="radio" id="showIndexSequences" name="namesOrSequences" value="false" onclick="layoutNames(false)"/>
                                    <label for="showIndexSequences">Show sequences</label>
                                </span>
                            </div>
                        </div>
                    </div>

                    <div id="plateContents" style="padding-top: 10px;">
                        <table id="plateLayoutNames" border="2">
                            <tbody>
                            <c:forEach items="${actionBean.plateLayoutNames}" var="layoutRow">
                                <tr>
                                    <c:forEach items="${layoutRow}" var="layoutCell">
                                        <td class="layoutCell">${layoutCell}</td>
                                    </c:forEach>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>

                        <table id="plateLayoutSequences" border="2">
                            <tbody>
                            <c:forEach items="${actionBean.plateLayoutSequences}" var="layoutRow">
                                <tr>
                                    <c:forEach items="${layoutRow}" var="layoutCell">
                                        <td class="layoutCell">${layoutCell}</td>
                                    </c:forEach>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:if>

                <!-- Rename the selected definition -->
                <div class="inputGroup" title="Rename the selected plate definition.">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="control-group controls">
                            <stripes:submit id="renameDefinition" name="renameDefinition" value="Rename" class="btn btn-primary"/>
                            <span style="padding-left: 10px;">
                                <input type="text" id="newDefinitionName" name="newDefinitionName" placeholder="[new plate definition name]"/>
                            </span>
                        </div>
                    </div>
                </div>

                <!-- Delete the selected definition -->
                <div class="inputGroup" title="Delete the selected plate definition.">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="control-group controls">
                            <stripes:submit id="deleteDefinition" name="deleteDefinition" value="Delete" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>

                <!-- Find index plates made from selected definition -->
                <div class="inputGroup" title="Finds plates made from the selected definition.">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="control-group controls">
                            <stripes:submit id="findInstances" name="findInstances" value="Find Instances" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>

                <!-- Text area to display the barcodes of the plates. -->
                <div class="inputGroup">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="controls">
                            <span id="inUseBarcodesDiv" style="display: none">
                                <p>In Use Plate Barcodes </p>
                                <textarea id="inUsePlateBarcodes" readonly="true" cols="104" rows="4" style="width: 100%;"></textarea>
                            </span>
                            <br/>
                            <span id="unusedBarcodesDiv" style="display: none">
                                <p>Unused Plate Barcodes </p>
                                <textarea id="unusedPlateBarcodes" readonly="true" cols="104" rows="4" style="width: 100%;"></textarea>
                            </span>
                        </div>
                    </div>
                </div>

            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
