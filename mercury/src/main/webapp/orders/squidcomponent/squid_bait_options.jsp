<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.SquidComponentActionBean"/>

<script type="text/javascript">
    function pullSampleReceptacles() {

        $j("#squidOligioPoolReceptacles").html("");
        $j("#squidOligioPoolReceptacles").html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
        $j("#squidOligioPoolReceptacles").show();
        $j.ajax({
            url: "${ctxpath}/orders/squid_component.action?ajaxSquidBaitReceptacles=&selectedBaits.groupName=" + $j("#oligioGroupName").val(),
            dataType: 'html',
            success: function (html) {
                $j("#squidOligioPoolReceptacles").html(html);
                $j("#squidOligioPoolReceptacles").fadeIn();
            }
        });
    }
</script>
<stripes:form beanclass="${actionBean.class.name}" partial="true">
    <div class="control-group">
        <stripes:label for="oligioGroupName" class="control-label">
            Oligio group names *
        </stripes:label>
        <div class="controls">
            <stripes:select name="selectedBaits.groupName" id="oligioGroupName" onchange="pullSampleReceptacles()">
                <stripes:option label="Select an oligio group.." value="-1" disabled="true" selected="selected"/>
                <stripes:options-collection collection="${actionBean.getBaitGroupNames()}"/>
            </stripes:select>
        </div>
    </div>

    <div id="squidOligioPoolReceptacles">
        <c:choose>
            <c:when test="${actionBean.groupReceptaclesRetrieved}">
                <jsp:include page="<%=SquidComponentActionBean.BAIT_RECEPTACLES_INSERT%>"/>
            </c:when>
        </c:choose>

    </div>

</stripes:form>
