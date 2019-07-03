<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
 Makes an index plate from a previously definition (template).
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Index Plate Instance" sectionTitle="Index Plate Instance">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function() {
                managementLayout(<c:if test="${actionBean.managePage}">true</c:if>);
            });

            function managementLayout(isManagement) {
                if (isManagement) {
                    $j(".creationLayout").hide();
                    $j(".managementLayout").show();
                    $j("#selectManage").attr('checked', 'checked');
                } else {
                    $j(".creationLayout").show();
                    $j(".managementLayout").hide();
                    $j("#selectCreate").attr('checked', 'checked');
                }
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
            div.even {
                background-color: #fff;
            }
            div.odd {
                background-color: #f5f5f5;
            }
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="plateDefinitionForm">
            <!-- Radio button group for type of operation. -->
            <div class="inputGroup" title="Select the type of operation to perform.">
                <div class="inputRow">
                    <div class="firstCol">Operation</div>
                    <div class="control-group controls">
                        <span>
                            <input type="radio" id="selectCreate" name="managePage" value="false" onclick="managementLayout(false)"/>
                            <label for="selectCreate">Create new plates</label>
                        </span>
                        <span style="padding-left: 20px;">
                            <input type="radio" id="selectManage" name="managePage" value="true" onclick="managementLayout(true)"/>
                            <label for="selectManage">Manage existing plates</label>
                        </span>
                    </div>
                </div>
            </div>

            <!-- -------------------- The layout for creating plate instances -------------------- -->

            <div class="creationLayout">

                <c:forEach items="${actionBean.plateNames}" varStatus="item">
                    <stripes:hidden name="plateNames[${item.index}]"/>
                </c:forEach>

                <!-- Selects a plate definition. -->
                <div class="inputGroup" title="Select a plate definition to use for new index plate instances.">
                    <div class="inputRow">
                        <div class="firstCol">Plate Definition Name</div>
                        <div class="control-group controls">
                            <stripes:select name="selectedPlateName" value="${actionBean.selectedPlateName}">
                                <stripes:option value=" "/>
                                <stripes:options-collection collection="${actionBean.plateNames}"/>
                            </stripes:select>
                        </div>
                    </div>
                </div>

                <!-- Text box for Sales Order Number. -->
                <div class="inputGroup" title="Provide a sales order number for the plates.">
                    <div class="inputRow">
                        <div class="firstCol">Sales Order Number</div>
                        <div class="control-group controls">
                            <stripes:text id="salesOrderNumber" name="salesOrderNumber"/>
                        </div>
                    </div>
                </div>

                <!-- Checkbox for overwrite. -->
                <div class="inputGroup" title="Indicate that you want to replace contents of the plate(s),
provided the plate(s) have not been used in a transfer.">
                    <div class="inputRow">
                        <div class="firstCol">Replace Existing</div>
                        <div class="control-group controls">
                            <stripes:checkbox id="replaceExisting" name="replaceExisting"/>
                        </div>
                    </div>
                </div>

                <div style="float: left; width: 50%;">
                    <!-- File chooser to upload Excel spreadsheet of barcodes of the plates to be created. -->
                    <div class="inputGroup" title="Provide an Excel spreadsheet.">
                        <div class="inputRow">
                            <div class="firstCol">Spreadsheet</div>
                            <div class="control-group controls">
                                <stripes:file name="spreadsheet" id="spreadsheet"/>
                            </div>
                        </div>
                    </div>

                    <!-- Button to create plate instance. -->
                    <div class="inputGroup" title="Makes a new plate for each of the barcodes, using the plate
definition to determine the well positions and reagent content.">
                        <div class="inputRow">
                            <div class="firstCol"></div>
                            <div class="controls">
                                <stripes:submit id="createInstance" name="createInstance" value="Create Plates" class="btn btn-primary"/>
                            </div>
                        </div>
                    </div>
                </div>

                <div style="float: right; width: 50%;">
                    <p>Accepts an Excel spreadsheet.
                    <ul>
                        <li>The header row is optional. If present it is ignored, but it must not have<br/>
                            a number in column A or else it will be mistaken for a data row.</li>
                        <li>The data rows should have one column.<br/>
                            Column A should contain the plate barcode (such as 012345678901).
                        </li>
                    </ul>
                    </p>
                </div>
            </div>


            <!-- -------------------- A different layout used for management. -------------------- -->


            <div class="managementLayout">
                <!-- Input textarea for plate barcodes. -->
                <div class="inputGroup">
                    <div class="inputRow">
                        <div class="firstCol">Plate barcodes</div>
                        <div class="controls">
                            <stripes:textarea id="plateBarcodeTextarea" name="plateBarcodeString" cols="104" rows="4" style="width: 100%;"/>
                        </div>
                    </div>
                </div>

                <!-- Find definition(s) for plate(s) -->
                <div class="inputGroup" title="Finds the plate definitions for the given plate barcodes.">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="controls">
                            <stripes:submit id="findDefinitions" name="findDefinitions" value="Find Plate Definitions" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>

                <!-- Displays the definitions and their barcodes. -->
                <c:if test="${not empty actionBean.definitionToBarcode}">
                    <div class="inputGroup">
                        <c:forEach items="${actionBean.definitionToBarcode}" var="row" varStatus="item">
                            <div class="inputRow ${item.index%2==0 ? "even" : "odd"}">
                                <div class="firstCol">Plates made from ${row.left}</div>
                                <div class="controls">${row.right}
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>

                <!-- Button to delete the plate instances. -->
                <div class="inputGroup" title="Deletes the plates if they are all unused.">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="controls">
                            <stripes:submit id="deleteInstances" name="deleteInstances" value="Delete Plates" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
