<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.HeatMapActionBean"/>

<script type="text/javascript">
    function applyHeatMap(component, colorstyle, reverseOrder) {
        $j(component).heatcolor(
                function () {
                    return $j("div", this).text();
                },
                {
                    lightness:0.50,
                    reverseOrder:$j('#reverseOrder').is(':checked'),
                    colorStyle:$j('input[name=colorStyle]:checked').val()
                }
        );
    }
</script>
<stripes:form beanclass="${actionBean.class.name}">
    <div>
        <table>
            <tr>
                <td>
                    <stripes:radio id="roygbiv" value="roygbiv" name="colorStyle"/>
                </td>
                <td>
                    Roygbiv
                </td>

                <td>
                    <stripes:radio id="redtogreen" value="redtogreen" name="colorStyle"/>
                </td>
                <td>
                    Red to green
                </td>
                <td><stripes:checkbox id="reverseOrder" name="reverseOrder"/>
                    Reverse Color Order
                </td>
            </tr>
        </table>
    </div>

    <div>
        <a href="javascript:applyHeatMap('${actionBean.jqueryClass}', '${actionBean.colorStyle}')">Apply Heat Map</a>
    </div>
</stripes:form>