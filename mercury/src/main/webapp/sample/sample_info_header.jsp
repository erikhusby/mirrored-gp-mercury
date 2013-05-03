<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="sample" type="org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>

    <style type="text/css">
        .headerTd, .headerTd td {
            border: 1px solid #cccccc;
            padding: 3px
        }
    </style>

    <table class="headerTd" style="width: 1024px">
        <tr>
            <td>Sample Name:</td>
            <td>${sample.sampleKey}</td>
            <td>Material Type:</td>
            <td>${sample.bspSampleDTO.materialType}</td>
        </tr>
    </table>

</stripes:layout-definition>
