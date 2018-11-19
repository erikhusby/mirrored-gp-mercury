<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.queueType.textName} Queue" sectionTitle="${actionBean.queueType.textName} Queue" showCreate="true">

    <stripes:layout-component name="extraHead">

    </stripes:layout-component>
    <stripes:layout-component name="content">
        <p>
            Queue: ${actionBean.queueType.textName}<br />
            Queue Grouping: ${actionBean.queueGrouping.queueGroupingText}
        </p>

        <table class="table simple dataTable" id="queueGroupingTable">
            <thead>
            <tr>
                <th>Vessel Barcode</th><th>Storage Location</th><th>Status</th><th>Completed On</th><th>Completed By</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.queueGrouping.queuedEntities}" var="queueEntity">
                <tr>
                    <td>${queueEntity.labVessel.label}</td>
                    <td>${queueEntity.labVessel.storageLocation.buildLocationTrail()}</td>
                    <td>${queueEntity.queueStatus.name}</td>
                    <td>${queueEntity.completedOn}</td>
                    <td>${actionBean.userIdToUsername[queueEntity.completedBy].fullName}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
