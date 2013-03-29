<#-- @ftlvariable name="linkToPlastic" type="java.lang.String" -->
<#-- @ftlvariable name="stationEvent" type="org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType" -->
<#-- @ftlvariable name="bspUser" type="org.broadinstitute.bsp.client.users.BspUser" -->
<#-- @ftlvariable name="validationErrors" type="java.util.List<org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource.WorkflowValidationError>" -->
<html xmlns="http://www.w3.org/1999/html">
<body>
<p>
    Mercury regrets to inform you that the <a href="${linkToPlastic}">${stationEvent.eventType}</a> message sent
    on ${stationEvent.start.toGregorianCalendar().getTime()?datetime} from ${stationEvent.station} on behalf of
    <a href="mailto:${bspUser.email}">${bspUser.fullName}</a>
    has failed validation because ${validationErrors?size} samples are in the wrong state.
</p>
<table>
    <thead>
    <tr>
        <th>Sample</th>
        <th>LCSET</th>
        <th>Error</th>
        <th>PDO</th>
        <th>RP</th>
    </tr>
    </thead>
    <tbody>
    <#list validationErrors as validationError>
    <tr>
        <td>${validationError.sampleInstance.startingSample.sampleKey}</td>
        <td><a href="${validationError.sampleInstance.labBatch.jiraTicket.browserUrl}">${(validationError.sampleInstance.labBatch.batchName)!''}</a></td>
        <td><#list validationError.errors as error>${error}<br/></#list></td>
        <td><a href="${validationError.linkToProductOrder}">${(validationError.productOrder.businessKey)!''}</a></td>
        <td><a href="${validationError.linkToResearchProject}">${(validationError.productOrder.researchProject.businessKey!'')}</a></td>
    </tr>
    </#list>
    </tbody>
</table>

</body>
</html>

