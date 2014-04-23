<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Define Squid Artifacts: ${actionBean.sourceOrder.title}"
                       sectionTitle="Define Squid Artifacts: ${actionBean.sourceOrder.title}">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j('#sampleList').dataTable({
                "oTableTools": ttExportDefines,
                "aaSorting": [
                    [1, 'asc']
                ],
                "aoColumns": [
                    {"bSortable": false},                           // Checkbox
                    {"bSortable": true, "sType": "html"}           // ID
                ]
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="buildSquidComponentForm">
            <div class="form-horizontal span6">
                <stripes:hidden name="<%= ProductOrderActionBean.PRODUCT_ORDER_PARAMETER%>"/>
                <stripes:hidden name="submitString"/>
                <div class="control-group">
                    <stripes:label for="initiativeSelect" class="control-label">
                        Initiative
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="autoSquidDto.initiative" id="initiativeSelect">
                            <stripes:option label="Select an initiative.." value="-1"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="projectTypeSelect" class="control-label">
                        Project type
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="autoSquidDto.projectType" id="projectTypeSelect">
                            <stripes:option label="Select a project Type.." value="-1"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="fundingSourceSelect" class="control-label">
                        Funding source
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="autoSquidDto.fundingSource" id="fundingSourceSelect">
                            <stripes:option label="Select a fundingSource.." value="-1"/>
                        </stripes:select>
                    </div>
                </div>

                <p/>

                <p/>

                <p/>

                <div class="control-group">
                    <stripes:label for="workRequestTypeSelect" class="control-label">
                        Work request type
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="autoSquidDto.workRequestType" id="workRequestTypeSelect">
                            <stripes:option label="Select a work request type.." value="-1"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="analysisTypeSelect" class="control-label">
                        Analysis type
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="autoSquidDto.analysisType" id="analysisTypeSelect">
                            <stripes:option label="Select an analysis type.." value="-1"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="referenceSeqSelect" class="control-label">
                        Reference sequence
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="autoSquidDto.referenceSequence" id="referenceSeqSeelct">
                            <stripes:option label="Select a reference sequence.." value="-1"/>
                        </stripes:select>
                    </div>
                </div>

                <div class="control-group">
                    <stripes:label for="autoSquidDto.pairedSequence" class="control-label">
                        Paired sequencing
                    </stripes:label>
                    <div class="controls">
                        <stripes:radio value="Yes" name="autoSquidDto.pairedSequence"/>Yes
                        <stripes:radio value="No" name="autoSquidDto.pairedSequence"/>No<br/>
                    </div>
                </div>

                <c:if test="${not empty actionBean.sourceOrder.samples}">
                    <table id="sampleList" class="table simple">
                        <thead>
                        <tr>
                            <th width="20">
                                <input type="checkbox" class="checkAll" for="count" id="checkAllSamples"/>
                                <span id="count" class="checkedCount"></span>
                            </th>
                            <th width="90">ID</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.sourceOrder.samples}" var="sample">
                            <tr>
                                <td>
                                    <stripes:checkbox name="selectedProductOrderSampleIds" title="${sample.samplePosition}"
                                                      class="shiftCheckbox" value="${sample.productOrderSampleId}"/>
                                </td>
                                <td id=""sampleId-${sample.productOrderSampleId} class="sampleName">
                                        <%--@elvariable id="sampleLink" type="org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink"--%>
                                    <c:set var="sampleLink" value="${actionBean.getSampleLink(sample)}"/>

                                    <c:choose>
                                        <c:when test="${sampleLink.hasLink}">
                                            <stripes:link class="external" target="${sampleLink.target}"
                                                          title="${sampleLink.label}" href="${sampleLink.url}">
                                                ${sample.name}
                                            </stripes:link>
                                        </c:when>
                                        <c:otherwise>
                                            ${sample.name}
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:if>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>    