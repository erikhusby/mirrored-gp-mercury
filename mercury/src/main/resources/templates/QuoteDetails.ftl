<#-- @ftlvariable name="quoteDetail" type="org.broadinstitute.gpinformatics.athena.presentation.orders.QuoteDetailsHelper.QuoteDetail" -->

<div class="form-value">
    <div id="fundsRemaining"
            <#if quoteDetail.error?has_content>
                class="alert alert-error"
            </#if>

        <#if quoteDetail.error?has_content> class="alert alert-error" </#if>
        <b>Funding Information</b><br/>
        ${quoteDetail.fundsRemaining}
        <ul>
            <#list quoteDetail.fundingDetails as fundingDetail>
                <#assign className='text-info'/>
                <#if fundingDetail.quoteWarning> <#assign className='text-error'/> </#if>
                <li class="${className}">${fundingDetail.fundingInfoString}<br/></li>
            </#list>
        </ul>
        <#if quoteDetail.error?has_content> ${quoteDetail.error} </#if>

    </div>
</div>
