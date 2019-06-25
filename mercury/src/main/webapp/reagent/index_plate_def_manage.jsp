<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
 Makes an index plate definition (a template) from an upload of plate wells and their molecular index names.
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Index Plate Definitions" sectionTitle="Manage Index Plate Definitions">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#definitionNameList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[[2, 'asc']],
                    "aoColumns":[
                        {"bSortable": false}, // checkbox
                        {"bSortable":true},   // name
                    ]
                });

                $j('#plateBarcodeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[[2, 'asc']],
                    "aoColumns":[
                        {"bSortable": false}, // checkbox
                        {"bSortable":true},   // barcode
                    ]
                });
            });
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
        </style>

        <stripes:form beanclass="${actionBean.class.name}" id="plateDefManageForm">
            <c:if test="${!actionBean.plateNames.isEmpty()}">
                <!-- Show list of definitions with a checkbox selector -->
                <table id="defintionNameList" class="table simple">
                    <thead>
                    <tr>
                        <th/>
                        <th>Name</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.plateNames}" var="nameItem" varStatus="item">
                        <tr>
                            <td>
                                <stripes:checkbox name="selectedName" value="${nameItem}" class="shiftCheckbox"/>
                            </td>
                            <td>${nameItem}</td>
                            <input type="hidden" name="plateNames[${item.index}]" value="${nameItem}"/>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:if>

            <c:if test="${not empty actionBean.plateName}">
                <div class="inputGroup">
                    <!-- Rename definition -->
                    <div class="inputRow">
                        <div class="firstCol">Rename To</div>
                        <div class="control-group controls">
                            <input type="text" id="newDefinitionName" name="newDefinitionName"/>
                        </div>
                    </div>

                    <div class="inputRow">
                        <div class="firstCol">
                            <stripes:submit id="renameDefintion" name="renameDefinition" value="Rename" class="btn btn-primary"/>
                        </div>
                    </div>

                    <!-- Delete definition -->
                    <div class="inputRow">
                        <div class="firstCol">
                            <stripes:submit id="deleteDefintion" name="deleteDefinition" value="Delete" class="btn btn-primary"/>
                        </div>
                    </div>

                    <!-- Find index plates made from definition -->
                    <div class="inputRow">
                        <div class="firstCol">
                            <stripes:submit id="findInstances" name="findInstances" value="Find Instances" class="btn btn-primary"/>
                        </div>
                    </div>

                    <!-- Show list of index plate instances with a checkbox selector. -->
                    <c:if test="${!plateBarcodes.isEmpty()}">
                        <h4>Index plates made from the selected plate definition</h4>
                        <div class="inputRow">
                            <table id="plateBarcodeList" class="table simple">
                                <thead>
                                <tr>
                                    <th>Plate Barcode</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${actionBean.plateBarcodes}" var="nameItem" varStatus="item">
                                    <tr>
                                        <td>${nameItem}</td>
                                        <input type="hidden" name="plateBarcodes[${item.index}]" value="${nameItem}"/>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:if>
                </div>
            </c:if>
            <!-- Text area to upload plate barcodes -->
            <!-- Find definition(s) for plate(s) -->
            <!-- Delete plates(s) but only if not part of a transfer -->

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
