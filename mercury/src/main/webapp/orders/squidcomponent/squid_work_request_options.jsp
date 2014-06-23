<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>


    <%--<stripes:layout-component name="workRequestOptHead">--%>
        <script type="text/javascript">
            function pullOligioGroups() {
                if ($j("#workRequestTypeSelect").val() == "<%=SquidComponentActionBean.WORKREQUEST_TYPE_FOR_BAITS%>") {

                    $j("#squidOligioPoolOptions").html("");
                    $j("#squidOligioPoolOptions").html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
                    $j("#squidOligioPoolOptions").show();
                    $j.ajax({
                        url: "${ctxpath}/orders/squid_component.action?ajaxSquidBaitOptions=&",
                        dataType: 'html',
                        success: function (html) {
                            $j("#squidOligioPoolOptions").html(html);
                            $j("#squidOligioPoolOptions").fadeIn();
                        }
                    });
                } else {
                    $j("#squidOligioPoolOptions").html("");
                }
            }
        </script>

        <stripes:form beanclass="${actionBean.class.name}" partial="true">
            <div class="control-group">
                <stripes:label for="lcsetKey" class="control-label">
                    LC Set name
                </stripes:label>
                <div class="controls">
                    <stripes:text name="autoSquidDto.lcsetId" id="lcsetKey"/>
                </div>
            </div>
            <div class="control-group">
                <stripes:label for="workRequestTypeSelect" class="control-label">
                    Work request type *
                </stripes:label>
                <div class="controls">
                    <stripes:select name="autoSquidDto.workRequestType" id="workRequestTypeSelect"
                                    onchange="pullOligioGroups();">
                        <stripes:option label="Select a work request type.." value="-1" selected="selected"
                                        disabled="true"/>
                        <stripes:options-collection collection="${actionBean.workRequestOptions.workRequestTypes}"
                                                    value="id" label="name"/>
                    </stripes:select>
                </div>
            </div>

            <div id="squidOligioPoolOptions">
                <c:choose>
                    <c:when test="${actionBean.oligioPoolsRetrieved}">
                        <jsp:include page="<%=SquidComponentActionBean.BAIT_OPTIONS_INSERT%>"/>
                    </c:when>
                </c:choose>
            </div>

            <div class="control-group">
                <stripes:label for="analysisTypeSelect" class="control-label">
                    Analysis type *
                </stripes:label>
                <div class="controls">
                    <stripes:select name="autoSquidDto.analysisType" id="analysisTypeSelect">
                        <stripes:option label="Select an analysis type.." value="-1" selected="selected"
                                        disabled="true"/>
                        <stripes:options-collection collection="${actionBean.workRequestOptions.analysisTypes}"
                                                    value="id"
                                                    label="name"/>
                    </stripes:select>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="referenceSeqSelect" class="control-label">
                    Reference sequence *
                </stripes:label>
                <div class="controls">
                    <stripes:select name="autoSquidDto.referenceSequence" id="referenceSeqSeelct">
                        <stripes:option label="Select a reference sequence.." value="-1" selected="selected"
                                        disabled="true"/>
                        <stripes:options-collection collection="${actionBean.workRequestOptions.referenceSequences}"
                                                    value="id" label="name"/>
                    </stripes:select>
                </div>
            </div>

            <div class="control-group">
                <stripes:label for="pairedSequenceSelect" class="control-label">
                    Paired sequencing *
                </stripes:label>
                <div class="controls">
                    <stripes:select name="pairedSequence" id="pairedSequenceSelect">
                        <stripes:option value="-1" label="Select your intention for paired sequencing" disabled="true"
                                        selected="selected"/>
                        <stripes:option value="YES" label="Yes"/>
                        <stripes:option value="NO" label="No"/>
                    </stripes:select>
                </div>
            </div>
        </stripes:form>

