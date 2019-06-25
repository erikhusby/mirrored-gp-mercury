<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
 Makes an index plate from a previously definition (template).
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create Index Plate from Definition" sectionTitle="Create Index Plate from Definition">
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

            <!-- Selects a plate definition. -->
            <div class="inputGroup" title="Select a plate definition to use for new index plate instances.">
                <div class="inputRow">
                    <div class="firstCol">Plate Definition Name</div>
                    <div class="control-group controls">
                        <stripes:select name="plateName" value="${actionBean.plateName}">
                            <stripes:options-collection collection="${actionBean.plateNames}"/>
                        </stripes:select>
                    </div>
                </div>
            </div>
            <c:forEach items="${actionBean.plateNames}" varStatus="varStatus">
                <stripes:hidden name="plateNames[${varStatus.index}]"/>
            </c:forEach>

            <!-- Generates a layout (positions and contents) for an index plate definition. -->
            <c:if test="${empty actionBean.plateLayout}">
                <div class="inputGroup" title="Shows a grid of the plate with well position and content.">
                    <div class="inputRow">
                        <div class="firstCol"></div>
                        <div class="controls">
                            <stripes:submit id="findLayout" name="findLayout" value="Show Layout" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>
            </c:if>

            <!-- Displays the layout data. -->
            <c:if test="${not empty actionBean.plateLayout}">
                <div id="layoutContents" style="padding-top: 10px; display: block";>
                    <table id="layoutCellGrid" border="2">
                        <tbody>
                        <c:forEach items="${actionBean.plateLayout}" var="layoutRow">
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

            <!-- Text box for Sales Order Number. -->
            <div class="inputGroup" title="The sales order number that should be associated with the plates.">
                <div class="inputRow">
                    <div class="firstCol">Sales Order Number</div>
                    <div class="control-group controls">
                        <stripes:text id="salesOrderNumber" name="salesOrderNumber"/>
                    </div>
                </div>
            </div>

            <!-- File chooser to upload Excel spreadsheet of barcodes of the plates to be created. -->
            <div class="inputGroup" title="Provide an Excel spreadsheet with rows having one column.
The first column should be the new plate barcode.
Plate barcodes will be leading zero filled to make 12 digits.
The header row is optional. If present, it is ignored.">
                <div class="inputRow">
                    <div class="firstCol">Spreadsheet</div>
                    <div class="control-group controls">
                        <stripes:file name="spreadsheet" id="spreadsheet"/>
                    </div>
                </div>
            </div>

            <!-- Button to "Create Plate Definition" -->
            <div class="inputGroup" title="Makes a new plate for each of the barcodes using the plate
definition to determine the well positions and reagent content.">
                <div class="inputRow">
                    <div class="firstCol"></div>
                    <div class="controls">
                        <stripes:submit id="createInstance" name="createInstance" value="Create" class="btn btn-primary"/>
                    </div>
                </div>
            </div>

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
