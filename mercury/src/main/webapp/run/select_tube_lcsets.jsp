<%-- Prompts user to select the correct lcset for a loading tube, as part of the process of creating designation tubes. --%>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationLoadingTubeActionBean"/>

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
                        {"bSortable":false}, // select
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
            <input type='hidden' name="tubeLcsetSelectionCount" value="${actionBean.tubeLcsetSelections.size()}"/>

            <div class="control-group">
                <div style="float: left; width: 50%;">
                    <table id="tubeList" class="table simple">
                        <thead>
                        <tr>
                            <th class="fixedWidth">Tube Barcode</th>
                            <th class="wider">Tube Create Date</th>
                            <th>LCSET</th>
                            <th class="fixedWidth" align="center">Select</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.tubeLcsetSelections}" var="dto" varStatus="item">
                            <tr>
                                <c:if test="${dto.rowspan gt 0}">
                                    <td class="fixedWidth" rowspan="${dto.rowspan}">
                                        <stripes:link id="transferVisualizer" event="view" target="_blank"
                                                      beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean">
                                            ${dto.barcode}
                                        </stripes:link>
                                    </td>
                                    <td class="wider" rowspan="${dto.rowspan}">
                                        <fmt:formatDate value="${dto.tubeDate}" pattern="MM/dd/yyyy HH:mm"/>
                                    </td>
                                </c:if>
                                <td class="fixedWidth">
                                    <a href="${dto.lcsetUrl}" target="JIRA">
                                            ${dto.lcsetName}
                                    </a>
                                </td>
                                <td class="fixedWidth">
                                    <stripes:checkbox style="margin-left: 40%" class="shiftCheckbox" checked="${dto.rowspan gt 0}"
                                                      name="tubeLcsetSelections[${item.index}].selected" title=
"Check to indicate that this barcode and LCSET
 pairing is correct for the designation. If the
 barcode has no LCSETs checked it will not be used."/>
                                </td>

                                <input type="hidden" name="tubeLcsetSelections[${item.index}].barcode" value="${dto.barcode}"/>
                                <input type="hidden" name="tubeLcsetSelections[${item.index}].lcsetName" value="${dto.lcsetName}"/>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div style="float: right; width: 50%;"></div>
            </div>
            <div class="control-group">
                <stripes:submit id="submitTubeLcsets" name="submitTubeLcsets" value="Submit" class="btn btn-primary"
                        title="Click to use the checked rows in the designation display."/>
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
