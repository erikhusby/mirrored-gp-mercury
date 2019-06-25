<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
 Makes an index plate definition (a template) from an upload of plate wells and their molecular index names.
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Manage Index Plate Definitions" sectionTitle="Manage Index Plate Definitions">
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
            <stripes:hidden name="plateName"/>

            <c:if test="${!actionBean.plateNames.isEmpty()}">
                <!-- Show list of definitions with a checkbox selector -->
                <div class="inputGroup" title="Either select a plate definition or enter plate barcode(s) below.">
                    <c:forEach items="${actionBean.plateNames}" var="nameItem" varStatus="item">
                        <div class="inputRow">
                            <div class="firstCol">
                                <stripes:checkbox group="" name="selectedName" value="${nameItem}" class="shiftCheckbox"/>
                            </div>
                            <div class="controls">${nameItem}</div>
                            <input type="hidden" name="plateNames[${item.index}]" value="${nameItem}"/>
                        </div>
                    </c:forEach>
                </div>
            </c:if>

            <!-- Rename the selected definition -->
            <div class="inputGroup" title="Rename the selected plate definition.">
                <div class="inputRow">
                    <div class="firstCol">
                        <stripes:submit id="renameDefintion" name="renameDefinition" value="Rename" class="btn btn-primary"/>
                    </div>
                    <div class="control-group controls">
                        <input type="text" id="newDefinitionName" name="newDefinitionName"/>
                    </div>
                </div>
            </div>

            <!-- Delete the selected definition -->
            <div class="inputGroup" title="Delete the selected plate definition.">
                <div class="inputRow">
                    <div class="firstCol">
                        <stripes:submit id="deleteDefintion" name="deleteDefinition" value="Delete" class="btn btn-primary"/>
                    </div>
                </div>
            </div>

            <!-- Find index plates made from definition -->
            <div class="inputGroup" title="Shows the barcodes of plates instantiated from the selected definition.">
                <div class="inputRow">
                    <div class="firstCol">
                        <stripes:submit id="findInstances" name="findInstances" value="Find Instances" class="btn btn-primary"/>
                    </div>
                </div>
            </div>

            <c:if test="${!plateBarcodes.isEmpty()}">
                <h4>Index plates made from the selected plate definition</h4>
                <div class="inputGroup">
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

                <!-- Delete the selected plate instances. -->
                <div class="inputRow" title="Delete the selected plate instances.">
                    <div class="firstCol">
                        <stripes:submit id="deleteInstances" name="deleteInstances" value="Delete" class="btn btn-primary"/>
                    </div>
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
