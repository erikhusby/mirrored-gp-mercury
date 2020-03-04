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
            <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
                <stripes:param name="queueType" value="${actionBean.queueType}"/>
                <stripes:param name="queueGroupingId" value="${actionBean.queueGrouping.queueGroupingId}"/>
                <stripes:param name="downloadGroupingData" />
                Download Details
            </stripes:link>
        </p>

        <table class="table simple dataTable" id="queueGroupingTable">
            <thead>
            <tr>
                <th>Tube Vessel Barcode</th><th>Sample ID</th><th>Mercury Storage Location</th>
                <th>BSP Storage Location</th><th>Collection</th><th>Sample Data Source System</th><th>Queue Status</th>
                <th>Completed On</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${actionBean.queueGrouping.queuedEntities}" var="queueEntity">
                <c:set value="${actionBean.labVesselIdToSampleId[queueEntity.labVessel.labVesselId]}" var="sampleId" />

                <tr>
                    <td>${queueEntity.labVessel.label}</td>
                    <td>${sampleId}</td>
                    <td>${queueEntity.labVessel.storageLocation.buildLocationTrail()}</td>
                    <td>${actionBean.sampleIdToSampleData[sampleId].bspStorageLocation}</td>
                    <td>${actionBean.sampleIdToSampleData[sampleId].collection}</td>
                    <td>${actionBean.labVesselIdToMercurySample[queueEntity.labVessel.labVesselId].metadataSource.displayName}</td>
                    <td>${queueEntity.queueStatus.displayName}</td>
                    <td>${queueEntity.completedOn}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
