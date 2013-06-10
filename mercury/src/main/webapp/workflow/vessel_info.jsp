<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>

<script type="text/javascript">
//    $(document).ready(function () {
//        $j('#reworkCandidates').dataTable();
//    });
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
                <table id="reworkCandidates" class="table simple">
                    <thead>
                    <tr>
                        <th></th>
                        <th>Barcode</th>
                        <th>Sample</th>
                        <th>PDO</th>
                        <th>Batches</th>
                        <th>Workflow</th>
                        <th>Sample Count</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${actionBean.reworkCandidates}" var="candidate">
                        <tr>
                            <td>
                                <stripes:radio name="reworkCandidate" value="${candidate}"/>
                            </td>
                            <td>${candidate.tubeBarcode}</td>
                            <td>${candidate.sampleKey}</td>
                            <td>${candidate.productOrderKey}</td>
                            <td>
                                <c:forEach items="${candidate.labVessel.nearestWorkflowLabBatches}" var="batch">
                                    <stripes:link target="_new"
                                                  href="${batch.jiraTicket.browserUrl}"
                                                  class="external">${batch.batchName}</stripes:link>
                                </c:forEach>
                            </td>
                            <td>${candidate.productOrder.product.workflowName}</td>
                            <td>${candidate.labVessel.sampleInstanceCount}</td>
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