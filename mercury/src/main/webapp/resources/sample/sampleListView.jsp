<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <script type="text/javascript">
        function showPlasticHistoryVisualizer(sampleKey) {
            $j('#plasticViewDiv').load('${ctxpath}/view/plasticHistoryView.action?sampleKey=' + sampleKey);
            $j('#plasticViewDiv').show();
        }
    </script>
    <%--@elvariable id="samples" type="java.util.Collection"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"--%>

    <table id="sampleListView" class="table simple">
        <thead>
        <tr>
            <th width="30">Vessel History</th>
            <th>Sample Name</th>
            <th>PDO</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${samples}" var="sample">
            <tr>
                <td>
                    <a href="javascript:showPlasticHistoryVisualizer('${sample.sampleKey}')">
                        <img width="30" height="30" name="" title="show plastic history view"
                             src="${ctxpath}/images/plate.png"/>
                    </a>
                </td>
                <td>
                        ${sample.sampleKey}
                </td>
                <td>
                        ${sample.productOrderKey}
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div id="plasticViewDiv" style="display:none"></div>
</stripes:layout-definition>