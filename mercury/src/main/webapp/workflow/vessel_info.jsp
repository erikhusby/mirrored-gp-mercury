<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>
<script type="text/javascript">
    $j(document).ready(function () {
        $j('#bucketCandidates').dataTable({
            "sDom": sDomNoTableToolsButtons,
            "bSort": false
        }).rowGrouping({
                    iGroupingColumnIndex: 1,
                    sGroupLabelPrefix: 'Barcode: ',
                    iGroupingColumnIndex2: 2,
                    sGroupLabelPrefix2: 'PDO Sample(s): ',
                    iGroupingColumnIndex4: 4,
                    sGroupLabelPrefix4:  'Vessel Sample(s)'
                });

        $j('.bucketCandidate-checkbox').enableCheckboxRangeSelection({
            checkAllClass: 'bucketCandidate-checkAll',
            countDisplayClass: 'bucketCandidate-checkedCount',
            checkboxClass: 'bucketCandidate-checkbox'
        });

        $j('.rework-checkbox').enableCheckboxRangeSelection({
            checkAllClass: 'rework-checkAll',
            countDisplayClass: 'rework-checkedCount',
            checkboxClass: 'rework-checkbox'
        });

        // Cause checkbox toggle when clicking on the row
        $j('.candidate-row').click(function (event) {
            $j('.bucketCandidate-checkbox:checkbox', this).click();
        });

        // Prevent the above click handler on the row from being invoked and causing another toggle
        $j('.bucketCandidate-checkbox').click(function (event) {
            event.stopPropagation();
        });
        // Prevent the above click handler on the row from being invoked and causing another toggle
        //The checkboxes that this function is checking against is found in vessel_info which is retrieved through
        //the Ajax call defined above
        $j('.rework-checkbox').click(function (event) {
            toggleReworkComponents();
            event.stopPropagation();
        });

        $j('.rework-checkAll').click(function (event) {
            toggleReworkComponents();
        });
    });
</script>

<stripes:form partial="true" beanclass="${actionBean.class}">
    <c:choose>
        <c:when test="${actionBean.bucketCandidates.isEmpty()}">
            <div class="control-group">
                <div class="controls">
                    <div id="error" class="text-error">Mercury does not recognize tube barcode or sample
                        ID: ${actionBean.vesselLabel}.
                    </div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="control-group">
                <div class="controls">
                    <div>
                        Showing results for ${actionBean.numQueryInputs - actionBean.noResultQueryTerms.size()}
                        of ${actionBean.numQueryInputs} terms.
                        <c:if test="${!empty actionBean.noResultQueryTerms}">
                            <br>No results found for: ${actionBean.noResultQueryTerms}
                        </c:if>
                    </div>
                    <table id="bucketCandidates" class="table simple">
                        <thead>
                        <tr>
                            <th width="30px">
                                <input type="checkbox" class="bucketCandidate-checkAll"/>
                                <span id="count" class="bucketCandidate-checkedCount"/>
                            </th>
                            <th>Barcode</th>
                            <th>PDO Sample</th>
                            <th>
                                <input type="checkbox" class="rework-checkAll"/>
                                <span id="reworkcount" class="rework-checkedCount"/> Rework(s)?
                            </th>
                            <th>Vessel Sample</th>
                            <th>PDO</th>
                            <th>Product</th>
                            <th>Batches</th>
                            <th>Workflow</th>
                            <th>Last Workflow Step</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.bucketCandidates}" var="candidate">
                            <tr class="candidate-row">
                                <td>
                                    <stripes:checkbox class="bucketCandidate-checkbox" name="selectedBucketCandidates"
                                                      value="${candidate}"/>
                                </td>
                                <td>${candidate.tubeBarcode}</td>
                                <td>${candidate.sampleKey}</td>
                                <td>
                                    <stripes:checkbox class="rework-checkbox" name="selectedReworkVessels"
                                                      value="${candidate}"/>
                                </td>
                                <td>${candidate.currentSampleKey}</td>
                                <td>${candidate.productOrder.businessKey}</td>
                                <td>${candidate.productOrder.product.productName}</td>
                                <td>
                                    <c:forEach items="${candidate.labVessel.nearestWorkflowLabBatches}" var="batch">
                                        <stripes:link target="_new"
                                                      href="${batch.jiraTicket.browserUrl}"
                                                      class="external">${batch.batchName}</stripes:link>
                                    </c:forEach>
                                </td>
                                <td>${candidate.productOrder.product.workflowName}</td>
                                <td>${candidate.lastEventStep}</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="selectedBucket" class="control-label">
                    Bucket Name
                </stripes:label>
                <div class="controls">

                    <select id="selectedBucket" name="bucketName">
                        <c:forEach items="${actionBean.buckets}" var="bucket">
                            <option>${bucket.name}</option>
                        </c:forEach>
                    </select>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</stripes:form>