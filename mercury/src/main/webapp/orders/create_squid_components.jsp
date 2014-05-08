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
//                    [1, 'asc']
                    [0, 'asc']
                ],
                "aoColumns": [
//                    {"bSortable": false},                 // Checkbox
                    {"bSortable": true, "sType": "html"}  // ID
                ]
            });

            $j(document).ready(

                    function () {
                        $j('#squidProjectOptions').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
                        $j('#squidProjectOptions').show();
                        $j.ajax({
                            url: "${ctxpath}/orders/squid_component.action?ajaxSquidProjectOptions=&",
                            dataType: 'html',
                            success: function (html) {
                                $j("#squidProjectOptions").html(html);
                            }
                        });
                        $j("#executionTypeSelect").change(function() {
                            var executionType = $j(this).val();
                            pullWorkRequestOptions(executionType);
                        });
                    }
            );
            function pullWorkRequestOptions(executionType) {
                $j("#squidWorkRequestOptions").html("");
                $j('#squidWorkRequestOptions').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
                $j('#squidWorkRequestOptions').show();
                $j.ajax({
                    url: "${ctxpath}/orders/squid_component.action?ajaxSquidWorkRequestOptions=&autoSquidDto.executionType=" + executionType,
                    dataType: 'html',
                    success: function (html) {
                        $j("#squidWorkRequestOptions").html(html);
                        $j("#squidWorkRequestOptions").fadeIn();
                    }
                });
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="buildSquidComponentForm">
            <div class="form-horizontal span6">
                <stripes:hidden name="<%= ProductOrderActionBean.PRODUCT_ORDER_PARAMETER%>"/>
                <stripes:hidden name="submitString"/>

                <div class="control-group">
                    <stripes:label for="executionTypeSelect" class="control-label">
                        Project execution type
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="autoSquidDto.executionType" id="executionTypeSelect" >

                            <stripes:option value="-1" label="Select a project execution type" disabled="true" selected="selected"/>
                            <stripes:option value="Development" label="Development"/>
                            <stripes:option value="Production" label="Production"/>
                        </stripes:select>
                        <%--<stripes:radio value="Development" name="autoSquidDto.executionType"/>Development--%>
                        <%--<stripes:radio value="Production" name="autoSquidDto.executionType"/>Production--%>
                    </div>
                </div>
                <div id="squidProjectOptions"></div>
                <p/>

                <p/>

                <p/>

                <div id="squidWorkRequestOptions">Select a Project Execution Type to drive work request options</div>

                <c:if test="${not empty actionBean.sourceOrder.samples}">
                    <table id="sampleList" class="table simple">
                        <thead>
                        <tr>
                            <%--<th width="20">--%>
                                <%--<input type="checkbox" class="checkAll" for="count" id="checkAllSamples"/>--%>
                                <%--<span id="count" class="checkedCount"></span>--%>
                            <%--</th>--%>
                            <th width="90">ID</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.sourceOrder.samples}" var="sample">
                            <tr>
                                <%--<td>--%>
                                    <%--<stripes:checkbox name="selectedProductOrderSampleIds"--%>
                                                      <%--title="${sample.samplePosition}"--%>
                                                      <%--class="shiftCheckbox" value="${sample.productOrderSampleId}"/>--%>
                                <%--</td>--%>
                                <td id="" sampleId-${sample.productOrderSampleId} class="sampleName">
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
                <stripes:submit name="<%= SquidComponentActionBean.BUILD_SQUID_COMPONENT_ACTION %>"
                                value="Submit work request" style="margin-right: 10px;" class="btn btn-primary"/>
                <stripes:link beanclass="${actionBean.class.name}"
                              event="<%= SquidComponentActionBean.ENTER_COMPONENTS_ACTION %>">
                    <stripes:param name="<%= ProductOrderActionBean.PRODUCT_ORDER_PARAMETER%>"
                                   value="${actionBean.sourceOrder}"/>
                    Cancel
                </stripes:link>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>    