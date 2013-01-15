<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.HeatMapActionBean"/>

<script type="text/javascript">
    function applyHeatMap(component) {
        $j(component).heatcolor(
                function () {
                    return $j("div", this).text();
                },
                {
                    lightness:0.50,
                    reverseOrder:false,
                    colorStyle:'roygbiv',
                    maxval:96,
                    minval:0
                }
        );
    }
</script>
<stripes:form beanclass="${actionBean.class.name}">

    Settings will go here for now just press the link for pretty colors

    <a href="javascript:applyHeatMap('${actionBean.jqueryClass}', '${actionBean.colorStyle}')">Apply Heat Map</a>
</stripes:form>

