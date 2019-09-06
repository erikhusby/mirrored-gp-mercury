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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AlignmentActionBean"/>

<stripes:layout-render name="/layout.jsp"
                       pageTitle="Create Alignment State"
                       sectionTitle="Create Alignment State ">

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
                        {"bSortable": true},
                        {"bSortable": true},
                        {"bSortable": true}
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

        <c:if test="${not empty actionBean.sampleRunData}">
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
                        <th>Sample Key</th>
                        <th>Flowcell</th>
                        <th>Run</th>
                        <th>Lanes</th>
                        <th>Run Date</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.sampleRunData}" var="sampleRunData" varStatus="idx">
                        <tr>
                            <td>
                                    <stripes:checkbox class="sample-checkbox" name="selectedRuns"
                                                      value="${sampleRunData.runName}"/>
                            </td>
                            <td>${sampleRunData.sampleKey}
                                <stripes:hidden name="sampleRunData[${idx.index}].sampleKey" value="${sampleRunData.sampleKey}"/>
                            </td>
                            <td>${sampleRunData.flowcell}
                                <stripes:hidden name="sampleRunData[${idx.index}].flowcell" value="${sampleRunData.flowcell}"/>
                            </td>
                            <td>${sampleRunData.runName}
                                <stripes:hidden name="sampleRunData[${idx.index}].runName" value="${sampleRunData.runName}"/>
                            </td>
                            <td>${sampleRunData.lanesString}
                                <stripes:hidden name="sampleRunData[${idx.index}].lanesString" value="${sampleRunData.lanesString}"/>
                            </td>
                            <td>${sampleRunData.runDate}
                                <stripes:hidden name="sampleRunData[${idx.index}].runDate" value="${sampleRunData.runDate}"/>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>

                <div class="control-group">
                    <stripes:label for="alignmentStateName" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="alignmentStateName" readonly="${!actionBean.creating}"
                                      name="editAlignmentState.stateName"
                                      class="defaultText"
                                      title="Enter the name of the alignment state"/>

                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="referenceGenome" class="control-label"/>
                    <div class="controls">
                        <stripes:select name="referenceGenome" id="eventType">
                            <stripes:options-enumeration enum="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AlignmentActionBean.ReferenceGenome" label="name"/>
                        </stripes:select>
                    </div>
                </div>
                <div class="control-group">
                    <stripes:label for="fastQList" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="fastQList"
                                      name="fastQList"
                                      class="defaultText"
                                      title="FASTQ List"/>

                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <stripes:submit name="create" class="btn btn-primary" value="Submit"/>
                    </div>
                </div>
            </stripes:form>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
