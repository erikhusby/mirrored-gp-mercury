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
    <table class="headerTd" style="width: 1024px">
        <tr>
            <td>Vessel Type:</td>
            <td>${vessel.type.name}</td>
            <td>Vessel Label:</td>
            <td>${vessel.label}</td>
            <td>Create Date:</td>
            <td><fmt:formatDate
                    value="${vessel.createdOn}" pattern="yyyy.MM.dd"/></td>
        </tr>
        <tr>
            <td>Latest Event:</td>
            <td width="200">${vessel.latestEvent.labEventType.name}</td>
            <td>Event Date:</td>
            <td width="200"><fmt:formatDate value="${vessel.latestEvent.eventDate}" pattern="yyyy.MM.dd"/></td>
            <td>Event Location:</td>
            <td width="80">${vessel.latestEvent.eventLocation}</td>
            <td>Event Operator:</td>
            <td width="100">${bean.getUserFullName(vessel.latestEvent.eventOperator)}</td>
        </tr>
    </table>

</stripes:layout-definition>
