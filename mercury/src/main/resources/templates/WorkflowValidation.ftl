<#-- @ftlvariable name="linkToPlastic" type="java.lang.String" -->
<#-- @ftlvariable name="stationEventNames" type="java.lang.String -->
<#-- @ftlvariable name="start" type="java.util.Date" -->
<#-- @ftlvariable name="station" type="java.lang.String" -->
<#-- @ftlvariable name="bspUser" type="org.broadinstitute.bsp.client.users.BspUser" -->
<#-- @ftlvariable name="validationErrors" type="java.util.List<org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator.WorkflowValidationError>" -->
<html xmlns="http://www.w3.org/1999/html">
<body>
<p>
    Mercury regrets to inform you that the following events sent on ${start?datetime} from ${station} on behalf of
    <a href="mailto:${bspUser.email}">${bspUser.fullName}</a>
    has failed validation because ${validationErrors?size} samples are in the wrong state: ${stationEventNames}
</p>
<table>
    <thead>
    <tr>
        <th>Sample</th>
        <th>LCSET</th>
        <th>Expected Events</th>
        <th>Actual Events</th>
        <th>PDO</th>
        <th>RP</th>
    </tr>
    </thead>
    <tbody>
    <#list validationErrors as validationError>
    <tr>
        <td>${validationError.sampleInstance.earliestMercurySampleName}</td>
        <td><a href="${(validationError.sampleInstance.singleBatch.jiraTicket.browserUrl)!}">${(validationError.sampleInstance.singleBatch.batchName)!''}</a></td>
        <td>
            <#list validationError.errors as error>${error.message}
                <#list error.expectedEventNames as expectedEventName>${expectedEventName} </#list>
            </#list>
        </td>
        <td>
            <#list validationError.errors as error>
                <#list error.actualEventNames as actualEventName>${actualEventName} </#list>
            </#list>
        </td>
        <td><a href="${validationError.linkToProductOrder}">${(validationError.productOrder.businessKey)!''}</a></td>
        <td><a href="${validationError.linkToResearchProject}">${(validationError.productOrder.researchProject.businessKey)!''}</a></td>
    </tr>
    </#list>
    </tbody>
</table>

</body>
</html>

