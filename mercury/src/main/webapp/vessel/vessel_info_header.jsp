<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:layout-definition>
    <%--@elvariable id="vessel" type="org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel"--%>
    <%--@elvariable id="bean" type="org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean"--%>
    <style type="text/css">
        .headerTd, .headerTd td {
            border: 1px solid #cccccc;
            padding: 3px
        }
    </style>
    <table class="headerTd">
        <tr>
            <td width="200"
                style="border: 1px solid #cccccc; border-right: none; border-bottom: none">${vessel.type.name}</td>
            <td style="border: 1px solid #cccccc; border-left: none; border-bottom: none">${vessel.label}</td>
        </tr>
        <tr>
            <td colspan="2" style="border-top: none; border-right: 1px solid #cccccc;"><fmt:formatDate
                    value="${vessel.createdOn}" pattern="yyyy.MM.dd"/></td>
        </tr>
        <tr>
            <td width="200">${vessel.latestEvent.labEventType.name}</td>
            <td width="200"><fmt:formatDate value="${vessel.latestEvent.eventDate}" pattern="yyyy.MM.dd"/></td>
            <td width="80">${vessel.latestEvent.eventLocation}</td>
            <td width="100">${bean.getUserFullName(vessel.latestEvent.eventOperator)}</td>
        </tr>
    </table>

</stripes:layout-definition>
