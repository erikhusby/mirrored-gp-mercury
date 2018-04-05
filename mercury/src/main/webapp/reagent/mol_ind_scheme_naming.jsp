<%@ include file="/resources/layout/taglibs.jsp" %>
<%--
  This page allows upload of an .xls .csv or .tsv file containing sequences, for the purpose
  of generating new molecular index scheme names.
--%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexNamingActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Make Molecular Index Names" sectionTitle="Make Molecular Index Names">

    <stripes:layout-component name="extraHead"/>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm">

            <div class="form-horizontal">
                <div class="control-group">
                    <div class="controls">
                        <div style="float: left; width: 33%;">
                            <stripes:file name="sequenceFile" id="sequenceFile"/>
                        </div>
                        <div style="float: right; width: 66%;">
                            <p>Accepts Excel spreadsheet, or .csv file, or .tsv file.</p>
                            <p>The header row should be molecular index positions, either
                                <br/>one position per column, or a column can have positions combined
                                <br/>with a delimiter (use  +  -  |  or &lt;space&gt;) for example: P5+P7</p>
                            <p>The data rows should be index sequences (multiple A, C, T, G, or U characters).
                                <br/>A combination column needs to be delimited, for example: AATAACAA+CCGCCGTT</p>
                        </div>
                    </div>
                </div>
            </div>

            <div class="form-horizontal">
                <div class="control-group">
                    <div class="controls">
                        <stripes:select id="technology" name="technology">
                            <stripes:option value="">Select the index technology</stripes:option>
                            <stripes:options-collection collection="${actionBean.technologies}"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <div style="margin-left: 180px; width: auto;">
                        <stripes:checkbox id="createMissingNames" name="createMissingNames" checked="checked"/>
                        <stripes:label for="createMissingNames">
                            Create molecular index scheme names
                        </stripes:label>
                    </div>
                </div>
                <div class="control-group">
                    <div style="margin-left: 180px; width: auto;">
                        <stripes:checkbox id="downloadSpreadsheet" name="downloadSpreadsheet" checked="checked"/>
                        <stripes:label for="downloadSpreadsheet">
                            Get spreadsheet with molecular index scheme names
                        </stripes:label>
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="upload" value="Upload File" class="btn btn-primary"/>
                    </div>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
