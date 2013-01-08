<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.VesselViewActionBean"/>
<table>
    <c:forEach var="row" items="${actionBean.vessel.vesselGeometry.rowNames}">
        <tr>
            <c:forEach var="column" items="${actionBean.vessel.vesselGeometry.columnNames}">
                <td>
                        ${row}${column}
                </td>
            </c:forEach>
        </tr>
    </c:forEach>
</table>
