<%@ include file="/resources/layout/taglibs.jsp" %>

<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2013 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.FingerprintWorkflowActionBean"/>

<stripes:layout-render name="/layout.jsp"
                       pageTitle="Create Fingerprint State"
                       sectionTitle="Create Fingerprint State ">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#sampleRunList').dataTable( {
                    "oTableTools": ttExportDefines,
                    "aaSorting": [[0,'asc']],
                    "aoColumns": [
                        {"bSortable": true, "sType": "html"},
                        {"bSortable": true},
                        {"bSortable": true},
                    ]
                });

                $j('.sample-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'sample-checkAll',
                    countDisplayClass:'sample-checkedCount',
                    checkboxClass:'sample-checkbox'});
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <stripes:form beanclass="${actionBean.class.name}" id="createForm" class="form-horizontal">
            <stripes:hidden name="submitString" value="${actionBean.submitString}"/>

            <div class="control-group">
                <stripes:label for="sampleIds" name="Sample Ids" class="control-label"/>
                <div class="controls">
                    <stripes:textarea id="sampleIds" name="sampleIds"/>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <stripes:submit name="search" class="btn btn-primary" value="Search"/>
                </div>
            </div>
        </stripes:form>

        <c:if test="${not empty actionBean.alignmentDirectoryDtos}">
            <stripes:form beanclass="${actionBean.class.name}"
                          id="showScanForm" class="form-horizontal">
                <stripes:hidden name="submitString" value="${actionBean.submitString}"/>
                <table id="sampleRunList" class="table simple">
                    <thead>
                    <tr>
                        <th width="30px">
                            <input type="checkbox" class="sample-checkAll" title="Check All"/>
                            <span id="count" class="sample-checkedCount"></span>
                        </th>
                        <th>Sample</th>
                        <th>Alignment Directory</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.alignmentDirectoryDtos}" var="dto" varStatus="idx">
                        <tr>
                            <td>
                                <stripes:checkbox class="sample-checkbox" name="selectedDirectories"
                                                  value="${dto.outputDirectory}"/>
                                <stripes:hidden name="alignmentDirectoryDtos[${idx.index}].outputDirectory" value="${dto.outputDirectory}"/>
                                <stripes:hidden name="alignmentDirectoryDtos[${idx.index}].bamFile" value="${dto.bamFile}"/>
                                <stripes:hidden name="alignmentDirectoryDtos[${idx.index}].vcfFile" value="${dto.vcfFile}"/>
                                <stripes:hidden name="alignmentDirectoryDtos[${idx.index}].haplotypeDatabase" value="${dto.haplotypeDatabase}"/>
                                <stripes:hidden name="alignmentDirectoryDtos[${idx.index}].sampleKey" value="${dto.sampleKey}"/>
                            </td>
                            <td>${dto.sampleKey}
                            </td>
                            <td>${dto.outputDirectory}
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>

                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="create" class="btn btn-primary" value="Submit"/>
                    </div>
                </div>
            </stripes:form>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
