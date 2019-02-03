<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <script type="text/javascript">
    $j(document).ready(function() {
        $j('#addRegulatoryInfoDialog').dialog({
            autoOpen: false,
            height: 500,
            width: 700,
            modal: true
        });
    });
    </script>
    <jsp:include page="regulatory_info_search_insert.jsp"/>
</stripes:layout-definition>
