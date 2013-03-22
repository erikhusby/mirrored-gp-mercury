<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Add Rework" sectionTitle="Add Rework">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function showVesselInfo() {
                $j('#vesselInfo').html("<img src=\"${ctxpath}/images/spinner.gif\"/>");
                var barcode = $j("#vesselBarcode").val();
                $j.ajax({
                    url:"${ctxpath}/workflow/AddRework.action?vesselInfo=&vesselLabel=" + barcode,
                    dataType:'html',
                    success:updateDetails
                });
            }
            function updateDetails(data) {
                $j("#vesselInfo").html(data);
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}"
                      id="reworkEntryForm">
            <div class="fourcolumn">
                <stripes:label name="Barcode" for="vesselLabel"/>
                <stripes:text id="vesselBarcode" name="vesselLabel" onchange="showVesselInfo()"/>

            </div>
            <div id="vesselInfo"></div>
            <div class="fourcolumn">
                <stripes:label name="Rework Reason" for="reworkReason"/>
                <stripes:select name="reworkReason">
                    <stripes:options-enumeration
                            enum="org.broadinstitute.gpinformatics.mercury.entity.rework.ReworkReason" label="value"/>
                </stripes:select>
            </div>
            <div class="fourcolumn">
                <stripes:label name="Comments" for="commentText"/>
                <stripes:textarea name="commentText"/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>