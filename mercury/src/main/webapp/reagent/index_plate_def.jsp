<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
 Makes an index plate definition (a template) from an upload of plate wells and their molecular index names.
--%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Index Plate Definition" sectionTitle="Index Plate Definition">
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

        <stripes:form beanclass="${actionBean.class.name}" id="plateDefinitionForm">
            <!-- Radio button group for plate geometry: 96 or 384 well. -->
            <div class="inputGroup">
                <div class="inputRow">
                    <div class="firstCol">Plate Size</div>
                    <div class="control-group controls">
                        <span>
                            <input type="radio" id="select96well" name="plateGeometry" value="96"/>
                            <label for="select96well">96 well</label>
                        </span>
                        <span style="padding-left: 20px;">
                            <input type="radio" id="select384well" name="plateGeometry" value="384"/>
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
                        <stripes:select name="reagentType">
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
                        <stripes:text id="plateName" name="plateName"/>
                    </div>
                </div>
            </div>

            <!-- Checkbox for overwrite. -->
            <div class="inputGroup" title="Indicates your intent to replace an existing plate definition with a new one.
This can only be done when there are no existing index plate instances.
Use the Index Plate Admin page should be used for a simple rename, and
also for removing existing plate instances.">
                <div class="inputRow">
                    <div class="firstCol">Replace Existing</div>
                    <div class="control-group controls">
                        <stripes:checkbox id="allowOverwrite" name="allowOverwrite"/>
                    </div>
                </div>
            </div>

            <!-- File chooser to upload Excel spreadsheet of well positions & index scheme names. -->
            <div class="inputGroup" title="Provide an Excel spreadsheet with rows having two columns.
The first column has position (A1 or A01, etc.).
The second has index name (such as Illumina_P5-Wodel_P7-Zajic).
The header row is optional. If present it is ignored.">
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
                        <stripes:submit id="uploadDefinition" name="uploadDefinition" value="Upload" class="btn btn-primary"/>
                    </div>
                </div>
            </div>

        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>
