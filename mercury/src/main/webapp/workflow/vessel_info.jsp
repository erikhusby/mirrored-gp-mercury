<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>
<script type="text/javascript">
    $j(document).ready(function () {
        $j('#reworkCandidates').dataTable({
            "sDom": sDomNoTableToolsButtons,
            "bSort": false
        }).rowGrouping({
            iGroupingColumnIndex: 1,
            sGroupLabelPrefix: 'Barcode: ',
            iGroupingColumnIndex2: 2,
            sGroupLabelPrefix2: 'Sample(s): '
        });

        $j('.rework-checkbox').enableCheckboxRangeSelection({
            checkAllClass: 'rework-checkAll',
            countDisplayClass: 'rework-checkedCount',
            checkboxClass: 'rework-checkbox'
        });

        // Cause checkbox toggle when clicking on the row
        $j('.candidate-row').click(function(event) {
            $j(':checkbox', this).click();
        });

        // Prevent the above click handler on the row from being invoked and causing another toggle
        $j('.rework-checkbox').click(function(event) {
            event.stopPropagation();
        })
    });
</script>

<stripes:form partial="true" beanclass="${actionBean.class}">
<c:choose>
    <c:when test="${actionBean.reworkCandidates.isEmpty()}">
        <div class="control-group">
            <div class="controls">
                <div id="error" class="text-error">Mercury does not recognize tube barcode or sample ID: ${actionBean.vesselLabel}.
                </div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="control-group">
            <div class="controls">
                <div>
                    Showing results for ${actionBean.numQueryInputs - actionBean.noResultQueryTerms.size()} of ${actionBean.numQueryInputs} terms.
                    <c:if test="${!empty actionBean.noResultQueryTerms}">
                        <br>No results found for: ${actionBean.noResultQueryTerms}
                    </c:if>
                </div>
                <table id="reworkCandidates" class="table simple">
                    <thead>
                    <tr>
                        <th width="30px">
                            <input type="checkbox" class="rework-checkAll"/>
                            <span id="count" class="rework-checkedCount"/>
                        </th>
                        <th>Barcode</th>
                        <th>Sample</th>
                        <th>PDO</th>
                        <th>Product</th>
                        <th>Batches</th>
                        <th>Workflow</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.reworkCandidates}" var="candidate">
                        <tr class="candidate-row">
                            <td>
                                <stripes:checkbox class="rework-checkbox" name="selectedReworkCandidates"
                                                  value="${candidate}"/>
                            </td>
                            <td>${candidate.tubeBarcode}</td>
                            <td>${candidate.sampleKey}</td>
                            <td>${candidate.productOrderKey}</td>
                            <td>${candidate.productOrder.product.productName}</td>
                            <td>
                                <c:forEach items="${candidate.labVessel.nearestWorkflowLabBatches}" var="batch">
                                    <stripes:link target="_new"
                                                  href="${batch.jiraTicket.browserUrl}"
                                                  class="external">${batch.batchName}</stripes:link>
                                </c:forEach>
                            </td>
                            <td>${candidate.productOrder.product.workflowName}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="control-group">
            <stripes:label for="selectedBucket" class="control-label">
                Rework To Bucket
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