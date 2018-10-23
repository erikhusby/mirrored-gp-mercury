<%@ include file="/resources/layout/taglibs.jsp" %>
<%--
  This page allows upload of an .xls .csv or .tsv file containing sequences, for the purpose
  of generating new molecular index scheme names.
--%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexNamingActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Make Molecular Index Names" sectionTitle="Make Molecular Index Names">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
        $j(document).ready(function () {
            if (${actionBean.molecularIndexNames.size() > 0}) {
                $j('#downloadBtn').removeAttr("disabled");
                $j('#downloadBtn').removeAttr("style");
            } else {
                $j('#downloadBtn').attr("disabled", "disabled");
                $j('#downloadBtn').attr("style", "background-color: #C0C0F0");
            }
        });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="uploadForm">
            <!-- Persists the upload and names so ActionBean can make a download spreadsheet. -->
            <c:forEach items="${actionBean.indexPositions}" varStatus="item">
                <input type="hidden" name="indexPositions[${item.index}]" value="${item.current}"/>
            </c:forEach>
            <c:forEach items="${actionBean.dataRows}" varStatus="item">
                <input type="hidden" name="dataRows[${item.index}]" value="${item.current}"/>
            </c:forEach>
            <c:forEach items="${actionBean.molecularIndexNames}" varStatus="item">
                <input type="hidden" name="molecularIndexNames[${item.index}]" value="${item.current}"/>
            </c:forEach>

            <div style="float: left; width: 33%;">
                <div>
                    <div class="control-group">
                        <div class="controls" style="padding: 10px;">
                            <stripes:file name="sequenceFile" id="sequenceFile"/>
                        </div>
                        <div class="controls" style="padding: 10px;">
                            <stripes:select id="technology" name="technology">
                                <stripes:option value="">Select the index technology</stripes:option>
                                <stripes:options-collection collection="${actionBean.technologies}"/>
                            </stripes:select>
                        </div>
                        <div class="controls" style="padding: 10px">
                            Create missing index names &MediumSpace;
                            <stripes:checkbox id="createMissingNames" name="createMissingNames" style="vertical-align:top;"/>
                        </div>

                        <div class="controls" style="padding: 10px;">
                            <stripes:submit id="uploadBtn" name="upload" value="Upload File" class="btn btn-primary"/>
                        </div>

                        <div class="controls" style="padding: 10px;">
                            <stripes:submit id="downloadBtn" name="download" value="Download Index Names" class="btn btn-primary"/>
                        </div>
                    </div>
                </div>
            </div>
            <div style="float: right; width: 66%;">
                <p>Accepts Excel spreadsheet, or .csv file, or .tsv file.
                <ul>
                <li>The header row should have only molecular index positions, in one or more columns. For example:
                    <ul>
                        <li>P5+P7 (one column, combined with <b>+ - |</b> or &lt;space&gt;)</li>
                        <li>IS1,P7 (in two columns)</li>
                    </ul>
                </li>
                <li>The data rows should be the corresponding index sequences, for example:
                    <ul>
                        <li>AATAACAA+CCGCCGTT (one column, combined with <b>+ - |</b> or &lt;space&gt;)</li>
                        <li>AAGCGTAG,CCGCCGTT (in two columns)</li>
                    </ul>
                </li>
                </ul>
                </p>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
