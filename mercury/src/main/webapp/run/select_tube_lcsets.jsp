<%-- Prompts user to select the correct lcset for a loading tube, as part of the process of creating designation tubes. --%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.FlowcellDesignationActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Select Loading Tube LCSET" sectionTitle="Select Loading Tube LCSET">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {

                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[],
                    "aoColumns":[
                        {"bSortable":false},  // barcode
                        {"bSortable":false},  // tube date
                        {"bSortable":false},  // lcset
                        {"bSortable":false},  // select
                    ]
                });

            });

        </script>
        <style type="text/css">
            #tubeList
            .fixedWidth { width: 8em; word-wrap: break-word; }
            .wider { width: 20em; word-wrap: break-word; }
        </style>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
            <input type='hidden' name="dateRangeStart" value="${actionBean.dateRange.start}"/>
            <input type='hidden' name="dateRangeEnd" value="${actionBean.dateRange.end}"/>
            <input type='hidden' name="dateRangeSelector" value="14"/> <!-- Set to "custom" otherwise dates are not saved. -->
            <input type='hidden' name="lcsetsBarcodes" value="${actionBean.lcsetsBarcodes}"/>
            <input type='hidden' name="tubeLcsetSelectionCount" value="${actionBean.tubeLcsetAssignemnts.size()}"/>

            <p>These loading tubes were found to have multiple LCSETs.</p>
            <p>Please select which LCSET should be used then click Continue to submit your selections and proceed with creating designations.</p>

            <div class="control-group">
                <div style="float: left; width: 50%;">
                    <table id="tubeList" class="table simple">
                        <thead>
                        <tr>
                            <th class="fixedWidth">Tube Barcode</th>
                            <th class="wider">Tube Create Date</th>
                            <th>LCSET</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.tubeLcsetAssignemnts}" var="dto" varStatus="item">
                            <tr>
                                <td class="fixedWidth">
                                    <stripes:link id="transferVisualizer" event="view" target="_blank"
                                                  beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean">
                                        ${dto.barcode}
                                    </stripes:link>
                                </td>
                                <td class="wider">
                                    <fmt:formatDate value="${dto.tubeDate}" pattern="MM/dd/yyyy HH:mm"/>
                                </td>
                                <td>
                                    <div class="control-group">
                                        <c:forEach items="${dto.lcsetNameUrls}" var="lcsetPair">
                                            <stripes:radio value="${lcsetPair.left}" id="select_${dto.barcode}_${lcsetPair.left}"
                                                           name="tubeLcsetAssignments[${item.index}].selectedLcsetName"/>
                                            <a href="${lcsetPair.right}" target="JIRA">${lcsetPair.left}</a>
                                        </c:forEach>
                                    </div>
                                </td>

                                <input type="hidden" name="tubeLcsetAssignments[${item.index}].barcode" value="${dto.barcode}"/>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div style="float: right; width: 50%;"></div>
            </div>
            <div class="control-group">
                <stripes:submit id="submitTubeLcsets" name="submitTubeLcsets" value="Continue" class="btn btn-primary"/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
