<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.TopOffActionBean" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<%--
  ~ The Broad Institute
  ~ SOFTWARE COPYRIGHT NOTICE AGREEMENT
  ~ This software and its documentation are copyright 2013 by the
  ~ Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support
  ~ whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  ~ use, misuse, or functionality.
  --%>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.TopOffActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Top Offs" dataTablesVersion="1.10" withSelect="true" sectionTitle="Top Offs" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap.min.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.validate-1.14.0.min.js"></script>
        <script src="${ctxpath}/resources/scripts/jquery.contextMenu.min.js" type="text/javascript"></script>
        <script src="${ctxpath}/resources/scripts/jquery.ui.position.js"></script>
        <link rel="stylesheet" href="${ctxpath}/resources/scripts/jquery.contextMenu.min.css">

        <script type="text/javascript">
            $j(document).ready(function() {

                $j.contextMenu({
                    selector: '.context-menu-hiSeqXTable',
                    callback: function(key, options) {
                        document.getElementById(key).click();
                    },
                    items: {
                        "hiSeqXTable-createTopOffGroup" : { name: "Create Top Off Group"},
                        "hiSeqXTable-sendToRework" : { name: "Send To Rework" },
                        "hiSeqXTable-sendToHolding" : {name: "Send To Holding" },
                        "hiSeqXTable-downloadPickList" : {name: "Download Picklist"}
                    }
                });

                $j.contextMenu({
                    selector: '.context-menu-novaSeqTable',
                    callback: function(key, options) {
                        document.getElementById(key).click();
                    },
                    items: {
                        "novaSeqTable-createTopOffGroup" : { name: "Create Top Off Group"},
                        "novaSeqTable-sendToRework" : { name: "Send To Rework" },
                        "novaSeqTable-sendToHolding" : {name: "Send To Holding" },
                        "novaSeqTable-downloadPickList" : {name: "Download Picklist"}
                    }
                });

                $j.contextMenu({
                    selector: '.context-menu-poolGroupsTable',
                    callback: function(key, options) {
                        document.getElementById(key).click();
                    },
                    items: {
                        "removeFromPoolGroup" : { name: "Remove Selections From Pool Groups"},
                        "markComplete" : { name: "Mark Complete" },
                        "downloadPoolGroups" : {name: "Download Selected Pool Groups" }
                    }
                });

                /**
                 * Generate distinct RGB colors
                 *
                 * t is the total number of colors
                 * you want to generate.
                 */
                function rgbColors(t) {
                    t = parseInt(t);
                    if (t < 2) {
                        return [[255, 49, 0]]; //reddish
                    }
                    if (t < 1) {
                        throw new Error("'t' must be greater than 0.");
                    }

                    // distribute the colors evenly on
                    // the hue range (the 'H' in HSV)
                    var i = 360 / (t - 1);

                    // hold the generated colors
                    var r = [];
                    var sv = 70;
                    for (var x = 0; x < t; x++) {
                        // alternate the s, v for more
                        // contrast between the colors.
                        sv = sv > 90 ? 70 : sv+10;
                        r.push(hsvToRgb(i * x, sv, sv));
                    }
                    return r;
                }

                /**
                 * HSV to RGB color conversion
                 *
                 * H runs from 0 to 360 degrees
                 * S and V run from 0 to 100
                 *
                 * Ported from the excellent java algorithm by Eugene Vishnevsky at:
                 * http://www.cs.rit.edu/~ncs/color/t_convert.html
                 */
                var hsvToRgb = function(h, s, v) {
                    var r, g, b;
                    var i;
                    var f, p, q, t;

                    // Make sure our arguments stay in-range
                    h = Math.max(0, Math.min(360, h));
                    s = Math.max(0, Math.min(100, s));
                    v = Math.max(0, Math.min(100, v));

                    s /= 100;
                    v /= 100;

                    if (s == 0) {
                        // Achromatic (grey)
                        r = g = b = v;
                        return [Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)];
                    }

                    h /= 60; // sector 0 to 5
                    i = Math.floor(h);
                    f = h - i; // factorial part of h
                    p = v * (1 - s);
                    q = v * (1 - s * f);
                    t = v * (1 - s * (1 - f));

                    switch (i) {
                        case 0:
                            r = v;
                            g = t;
                            b = p;
                            break;

                        case 1:
                            r = q;
                            g = v;
                            b = p;
                            break;

                        case 2:
                            r = p;
                            g = v;
                            b = t;
                            break;

                        case 3:
                            r = p;
                            g = q;
                            b = v;
                            break;

                        case 4:
                            r = t;
                            g = p;
                            b = v;
                            break;

                        default: // case 5:
                            r = v;
                            g = p;
                            b = q;
                    }

                    return [Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)];
                };

                const seqTableIds = ['#hiSeqXTable', '#novaSeqTable', '#poolGroupsTable'];

                /**
                 * Initialize Nova And Hiseq Datatables. Color their index cells if a duplicate exists.
                 */
                $j.each(seqTableIds, function (idx, tableId) {

                    const indexes = new Set();
                    const duplicateIndexes = new Set();
                    var idxSelector = $j(tableId + " .myIndex");
                    $j.each(idxSelector, function () {
                        var index = $j(this).text().trim();
                        if (indexes.has(index)) {
                            duplicateIndexes.add(index);
                        } else {
                            indexes.add(index);
                        }
                    });

                    const indexArray = Array.from(duplicateIndexes);
                    var rgbClrs = rgbColors(indexArray.length);
                    const mapIdxToColor = {};
                    $j.each(indexArray, function (idx, value) {
                        mapIdxToColor[value] = rgbClrs[idx];
                    });

                    console.log("Looking at " + tableId);
                    console.log(mapIdxToColor);
                    if (tableId !== '#poolGroupsTable') {
                        $j(tableId).DataTable({
                            renderer: "bootstrap",
                            columns: [
                                {sortable: false},
                                {sortable: true, "sClass": "nowrap"},
                                {sortable: true, "sClass": "nowrap"},
                                {sortable: true, "sClass": "nowrap"},
                                {sortable: true, "sClass": "nowrap"},
                                {sortable: true, "sClass": "nowrap"},
                                {sortable: true, "sClass": "nowrap"},
                                {sortable: true, "sClass": "nowrap"},
                            ],
                            select: {
                                style: 'os',
                                selector: 'td:first-child'
                            },
                            rowCallback: function (row, data) {
                                const INDEX_COL = 3;
                                var splitData = data[INDEX_COL].split(/\s+/);
                                var rowMolecularIndex = splitData[0].trim();
                                var rgb = mapIdxToColor[rowMolecularIndex];
                                console.log(rowMolecularIndex);
                                if (rgb !== undefined) {
                                    var clr = "rgb(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + ")";
                                    $j('td:eq(' + INDEX_COL + ')', row).css('background-color', clr);
                                }
                            }
                        });
                    } else {
                        $j.each(idxSelector, function(data) {
                            console.log(data);
                            var rowMolecularIndex = $j(this).text().trim();
                            var rgb = mapIdxToColor[rowMolecularIndex];
                            console.log(rowMolecularIndex);
                            if (rgb !== undefined) {
                                var clr = "rgb(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + ")";
                                $j(this).css('background-color', clr);
                            }
                        });
                    }
                });

                $j('#sentToReworkTable').DataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: false},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                    ],
                    select: {
                        style:    'os',
                        selector: 'td:first-child'
                    },
                });


                $j('#holdForTopOffTable').DataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: false},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                    ],
                    select: {
                        style:    'os',
                        selector: 'td:first-child'
                    },
                });

                $j('.poolGroupsTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'poolGroupsTable-checkAll',
                    countDisplayClass:'poolGroupsTable-checkedCount',
                    checkboxClass:'poolGroupsTable-checkbox'});

                $j('.holdForTopOffTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'holdForTopOffTable-checkAll',
                    countDisplayClass:'holdForTopOffTable-checkedCount',
                    checkboxClass:'holdForTopOffTable-checkbox'});

                $j('.hiSeqXTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'hiSeqXTable-checkAll',
                    countDisplayClass:'hiSeqXTable-checkedCount',
                    checkboxClass:'hiSeqXTable-checkbox'});

                $j('.novaSeqTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'novaSeqTable-checkAll',
                    countDisplayClass:'novaSeqTable-checkedCount',
                    checkboxClass:'novaSeqTable-checkbox'});

                function calculateLanesNeeded() {
                    var lanesNeededSelector = $j("#poolGroupsTable .lanesNeeded");
                    var poolingPenalty = 1;
                    if ( $j("#poolingPenalty").val().length > 0 &&
                        !$j("#poolingPenalty").hasClass("error")) {
                        poolingPenalty = $j("#poolingPenalty").val();
                    }
                    $j.each(lanesNeededSelector, function () {
                        var defaultYield = $j(this).data("default-yield");
                        var cnt = $j(this).data("count");
                        var childHidden = $j(this).children();
                        console.log(childHidden);
                        var divTag = childHidden[0];
                        var inputTag = childHidden[1];
                        if ( $j("#expectedYield").val().length > 0 &&
                            !$j("#expectedYield").hasClass("error")) {
                            defaultYield = $j("#expectedYield").val();
                        }
                        var maxX = $j(this).data("max-x");
                        let lanesNeeded = (maxX * cnt * poolingPenalty)/ defaultYield;
                        $j(inputTag).val(lanesNeeded);
                        $j(divTag).html(lanesNeeded)
                    });
                }

                $j("#poolingPenalty").change(function () {
                   if (!$j(this).hasClass("error")) {
                       calculateLanesNeeded();
                   }
                });

                $j("#expectedYield").change(function () {
                    if (!$j(this).hasClass("error")) {
                        calculateLanesNeeded();
                    }
                });

                $j("#topOffGroupsForm").validate(1);

                calculateLanesNeeded();
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <div class="tabbable">
            <ul class="nav nav-tabs" id="topoffTabs">
                <li class="active"><a href="#holdForTopoffTab" data-toggle="tab">Hold For Top Off</a></li>
                <li><a href="#hiSeqXTab" data-toggle="tab">HiSeqX</a></li>
                <li><a href="#novaSeqTab" data-toggle="tab">Nova</a></li>
                <li><a href="#poolGroupsTab" data-toggle="tab">Pool Groups</a></li>
                <li><a href="#reworksTab" data-toggle="tab">Sent To Rework</a></li>
            </ul>

            <div class="tab-content">
                <div class="tab-pane active" id="holdForTopoffTab">
                    <stripes:form beanclass="${actionBean.class.name}" id="holdForTopOffForm" class="form-horizontal" method="POST">
                        <table id="holdForTopOffTable" class="table simple">
                            <thead>
                            <tr>
                                <th width="30px">
                                    <input type="checkbox" class="holdForTopOffTable-checkAll" title="Check All"/>
                                    <span id="count" class="holdForTopOffTable-checkedCount"></span>
                                </th>
                                <th>Seq Type</th>
                                <th>Library</th>
                                <th>PDO</th>
                                <th>PDO Sample</th>
                                <th>Index</th>
                                <th>Is Clinical?</th>
                                <th>LCSET</th>
                                <th>X Needed</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:set var="machineType" value="Hold For Top Off"/>
                            <c:forEach items="${actionBean.getTabData(machineType)}" var="dto" varStatus="status">
                                <tr>
                                    <td>
                                        <stripes:checkbox name="selectedSamples" class="holdForTopOffTable-checkbox"
                                                          value="${dto.pdoSample}"/>
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].bucketEntryId" value="${dto.bucketEntryId}"/>
                                    </td>
                                    <td>
                                            ${dto.seqType}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].seqType" value="${dto.seqType}"/>
                                    </td>
                                    <td>
                                            ${dto.library}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].library" value="${dto.library}"/>
                                    </td>
                                    <td>
                                            ${dto.pdo}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].pdo" value="${dto.pdo}"/>
                                    </td>
                                    <td>
                                            ${dto.pdoSample}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].pdoSample" value="${dto.pdoSample}"/>
                                    </td>
                                    <td class="myIndex">
                                            ${dto.index}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].index" value="${dto.index}"/>
                                    </td>
                                    <td>
                                            ${dto.clinical}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].clinical" value="${dto.clinical}"/>
                                    </td>
                                    <td>
                                            ${dto.lcset}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].lcset" value="${dto.lcset}"/>
                                    </td>
                                    <td>
                                            ${dto.xNeeded}
                                        <stripes:hidden name="mapTabToDto['${machineType}'].[${status.index}].xNeeded" value="${dto.xNeeded}"/>
                                    </td>
                                </tr>
                            </c:forEach>

                            </tbody>
                        </table>
                        <div class="control-group">
                            <stripes:label for="workflowSelect" name="Select Workflow" class="control-label"/>
                            <div class="controls">
                                <stripes:select id="workflowSelect" name="selectedWorkflow">
                                    <stripes:option value="">Select a Workflow</stripes:option>
                                    <stripes:options-collection collection="${actionBean.availableWorkflows}"/>
                                </stripes:select>
                            </div>
                        </div>
                        <div class="control-group">
                            <stripes:label for="lcsetText" name="Batch Name" class="control-label"/>
                            <div class="controls">
                                <stripes:text id="lcsetText" class="defaultText" name="selectedLcset"
                                              title="Enter if you are adding to an existing batch"
                                              value="${actionBean.selectedLcset}"/>
                            </div>
                        </div>
                        <div class="control-group">
                            <stripes:label for="summary" name="Summary" class="control-label"/>
                            <div class="controls">
                                <stripes:text name="summary" class="defaultText"
                                              title="Enter a summary for a new batch ticket" id="summary"
                                              value="${actionBean.summary}"/>
                            </div>
                        </div>
                        <div class="control-group">
                            <div class="controls">
                                <stripes:submit name="createOrAddToBatch" value="Create Or Add To Batch" class="btn btn-primary"/>
                            </div>
                        </div>
                    </stripes:form>
                </div>
                <div class="tab-pane" id="hiSeqXTab">
                    <c:set var="tableId" value="hiSeqXTable" scope="request"/>
                    <c:set var="dtoList" value="${actionBean.getTabData('Illumina HiSeq X 10')}" scope="request"/>
                    <c:set var="machineType" value="Illumina HiSeq X 10" scope="request"/>
                    <jsp:include page="top_offs_machine_table.jsp"/>
                </div>
                <div class="tab-pane" id="novaSeqTab">
                    <c:set var="tableId" value="novaSeqTable" scope="request"/>
                    <c:set var="dtoList" value="${actionBean.getTabData('Illumina NovaSeq 6000')}" scope="request"/>
                    <c:set var="machineType" value="Illumina NovaSeq" scope="request"/>
                    <jsp:include page="top_offs_machine_table.jsp"/>
                </div>
                <div class="tab-pane" id="poolGroupsTab">
                    <jsp:include page="top_offs_group_table.jsp"/>
                </div>
                <div class="tab-pane" id="reworksTab">
                    <c:set var="tableId" value="sentToReworkTable" scope="request"/>
                    <c:set var="dtoList" value="${actionBean.getTabData('Sent To Rework')}" scope="request"/>
                    <c:set var="machineType" value="Sent To Rework" scope="request"/>
                    <jsp:include page="top_offs_machine_table.jsp"/>
                </div>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
