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

            $j(document).ready(

                    function () {
                        $j.ajax({
                            url: "${ctxpath}/orders/squid_component.action?ajaxSquidProjectOptions=&",
                            dataType: 'html',
                            success: function(html) {
                                $j("#squidProjectOptions").html(html);
                            }
                        });
                        $j.ajax({
                            url: "${ctxpath}/orders/squid_component.action?ajaxSquidWorkRequestOptions=&",
                            dataType: 'html',
                            success: function(html) {
                                $j("#squidWorkRequestOptions").html(html);
                            }
                        });
                    }
            );

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="buildSquidComponentForm">
            <div class="form-horizontal span6">
                <stripes:hidden name="<%= ProductOrderActionBean.PRODUCT_ORDER_PARAMETER%>"/>
                <stripes:hidden name="submitString"/>

                <div id="squidProjectOptions" ></div>
                <p/>

                <p/>

                <p/>

                <div id="squidWorkRequestOptions"></div>

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