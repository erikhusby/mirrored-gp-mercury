<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.VesselViewActionBean"/>
<table>
    <c:forEach var="row" items="${actionBean.vessel.vesselGeometry.rowNames}" varStatus="rowIndex">
        <tr>
            <c:forEach var="column" items="${actionBean.vessel.vesselGeometry.columnNames}" varStatus="colIndex">
                <td>
                        ${row}${column}<br/>
                    <c:forEach var="sample" items="${actionBean.samplesAtPosition(row, column)}">
                        ${sample.startingSample.sampleKey}<br/>
                    </c:forEach>

                </td>
            </c:forEach>
        </tr>
    </c:forEach>
</table>
