<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="sample" type="org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>

    <div id="headerId" class="fourcolumn">
        <div>Sample Name: ${sample.sampleKey}</div>
        <div>
            Material Type:
            <c:choose>
                <c:when test="${sample.sampleData == null}">
                    <td>Sample not found in BSP</td>
                </c:when>
                <c:otherwise>
                    <td>${sample.sampleData.materialType}</td>
                </c:otherwise>
            </c:choose>

        </div>
    </div>
</stripes:layout-definition>

