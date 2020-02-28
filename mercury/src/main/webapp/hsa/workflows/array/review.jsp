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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.ArraysDataReviewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Arrays Data Review" dataTablesVersion="1.10"  sectionTitle="Arrays Data Review" showCreate="false">

    <stripes:layout-component name="extraHead">
        <style>
        </style>
        <script type="text/javascript">
            $j(document).ready(function() {
                let reviewTable = $j('#reviewTable').dataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: false},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                    ],
                });

                $j('.reviewTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'reviewTable-checkAll',
                    countDisplayClass:'reviewTable-checkedCount',
                    checkboxClass:'reviewTable-checkbox'});

                function hideBucketFields() {
                    $j(".bucketGroup").hide();
                }

                $j( "#decision" ).change(function() {
                    let decision = $(this).val();
                    switch (decision) {
                        case "READY_TO_DELIVER":
                            hideBucketFields();
                            break;
                        case "REWORK":
                            $j(".bucketGroup").show();
                            if ($j("#rework-reason-value").val() === 'Other...') {
                                $j("#rework-reason-user-value").show();
                            } else {
                                $j("#rework-reason-user-value").hide();
                            }
                            break;
                    }
                });

                $j("#rework-reason-value").change(function () {
                    if (this.value === 'Other...') {
                        $j("#rework-reason-user-value").show();
                    } else {
                        $j("#rework-reason-user-value").hide();
                    }
                });

                includeAdvancedFilter(reviewTable, "#reviewTable");

                $j( "#decision" ).change();
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" class="form-horizontal" id="searchForm">
            <table id="reviewTable" class="table simple">
                <thead>
                <tr>
                    <th width="30px">
                        <input type="checkbox" class="reviewTable-checkAll" title="Check All"/>
                        <span id="count" class="reviewTable-checkedCount"></span>
                    </th>
                    <th>PDO Sample Name</th>
                    <th>PDO</th>
                    <th>Chip Well Barcode</th>
                    <th>Call Rate</th>
                    <th>Contamination</th>
                    <th>% Het</th>
                    <th>HapMap Concordance</th>
                    <th>Gender Concordance</th>
                    <th>Try Count</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.metricsDtos}" var="dto" varStatus="status">
                    <tr>
                        <td>
                            <stripes:checkbox name="selectedSamples" class="reviewTable-checkbox"
                                              value="${dto.pdoSampleName}"/>
                            <stripes:hidden name="metricsDtos[${status.index}].productOrder" value="${dto.productOrder}"/>
                        </td>
                        <td>${dto.pdoSampleName}
                            <stripes:hidden name="metricsDtos[${status.index}].pdoSampleName" value="${dto.pdoSampleName}"/>
                        </td>
                        <td>${dto.productOrder}
                            <stripes:hidden name="metricsDtos[${status.index}].productOrder" value="${dto.productOrder}"/>
                        </td>
                        <td>${dto.chipWellBarcode}
                            <stripes:hidden name="metricsDtos[${status.index}].chipWellBarcode" value="${dto.chipWellBarcode}"/>
                        </td>
                        <td>${dto.callRate}
                            <stripes:hidden name="metricsDtos[${status.index}].callRate" value="${dto.callRate}"/>
                        </td>
                        <td>${dto.contamination}
                            <stripes:hidden name="metricsDtos[${status.index}].contamination" value="${dto.contamination}"/>
                        </td>
                        <td>${dto.hetPct}
                            <stripes:hidden name="metricsDtos[${status.index}].hetPct" value="${dto.hetPct}"/>
                        </td>
                        <td>${dto.hapMapConcordance}
                            <stripes:hidden name="metricsDtos[${status.index}].hapMapConcordance" value="${dto.hapMapConcordance}"/>
                        </td>
                        <td>${dto.genderConcordance}
                            <stripes:hidden name="metricsDtos[${status.index}].genderConcordance" value="${dto.genderConcordance}"/>
                        </td>
                        <td>${dto.tryCount}
                            <stripes:hidden name="metricsDtos[${status.index}].tryCount" value="${dto.tryCount}"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <div class="control-group">
                <stripes:label for="decision" name="decision" class="control-label"/>
                <div class="controls">
                    <stripes:select name="decision" id="decision">
                        <stripes:options-enumeration label="displayName"
                                                     enum="org.broadinstitute.gpinformatics.mercury.presentation.hsa.ArraysDataReviewActionBean.ArraysReviewDecision"/>
                    </stripes:select>
                </div>
            </div>
            <div class="control-group bucketGroup">
                <stripes:label for="reworkReason" class="control-label" id="rework-reason-label">
                    Reason for Rework
                </stripes:label>
                <div class="controls">
                    <stripes:select name="reworkReason" id="rework-reason-value">
                        <stripes:options-collection collection="${actionBean.getAllReworkReasons()}"
                                                    value="reason" label="reason"/>
                        <stripes:option label="Other..." value="Other..."/>
                    </stripes:select>
                    <stripes:text name="userReworkReason" id="rework-reason-user-value"/>
                </div>
            </div>
            <div class="control-group bucketGroup">
                <stripes:label for="commentText" class="control-label"/>
                <div class="controls">
                    <stripes:textarea id="commentText" name="commentText"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <stripes:submit name="decide" value="Update" class="btn btn-primary"/>
                </div>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
