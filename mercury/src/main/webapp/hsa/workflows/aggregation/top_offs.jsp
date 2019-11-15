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

<stripes:layout-render name="/layout.jsp" pageTitle="Triage" dataTablesVersion="1.10" withSelect="true" sectionTitle="Top Offs" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap.min.js"></script>

        <script type="text/javascript">
            $j(document).ready(function() {

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

                // Grab all unique indexes from table to create a color palette
                const indexes = new Set();
                $j.each($j(".myIndex"), function (idx, value) {
                    indexes.add($j(this).html());
                });
                const indexArray = Array.from(indexes);
                var rgbClrs = rgbColors(indexArray.length);
                const mapIdxToColor = {};
                $j.each(indexArray, function (idx, value) {
                    mapIdxToColor[value] = rgbClrs[idx];
                });

                $j('#topOffListTable').DataTable({
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
                    rowCallback: function( nRow, aData) {
                        var rgb = mapIdxToColor[aData[4]];
                        var clr = "rgb(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + ")";
                        $j('td:eq(4)', nRow).css('background-color', clr)
                    }
                });

                $j('.topOffListTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'topOffListTable-checkAll',
                    countDisplayClass:'topOffListTable-checkedCount',
                    checkboxClass:'topOffListTable-checkbox'});
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">

        <ul class="nav nav-tabs" id="specTabs">
            <li><a href="#holdForTopoffTab" data-toggle="tab">Hold For Top Off</a></li>
            <li><a href="#hiSeqXTab" data-toggle="tab">HiSeqX</a></li>
            <li><a href="#novaSeqTab" data-toggle="tab">Nova</a></li>
            <li><a href="#poolGroupsTab" data-toggle="tab">Pool Groups</a></li>
            <li><a href="#reworksTab" data-toggle="tab">Sent To Rework</a></li>
        </ul>

        <div class="tab-content" id="tabContent">
            <div class="tab-pane active" id="holdForTopoffTab">
                <stripes:form beanclass="${actionBean.class.name}" id="topOffForm" class="form-horizontal">
                    <table id="topOffListTable" class="table simple">
                        <thead>
                        <tr>
                            <th width="30px">
                                <input type="checkbox" class="topOffListTable-checkAll" title="Check All"/>
                                <span id="count" class="topOffListTable-checkedCount"></span>
                            </th>
                            <th>Seq Type</th>
                            <th>Library</th>
                            <th>Index</th>
                            <th>PDO</th>
                            <th>PDO Sample</th>
                            <th>LCSET</th>
                            <th>X Needed</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.holdForTopoffsDto}" var="dto" varStatus="status">
                            <tr>
                                <td>
                                    <stripes:checkbox name="selectedSamples" class="machine-checkbox"
                                                      value="${dto.pdoSample}"/>
                                </td>
                                <td>
                                        ${dto.library}
                                        <stripes:hidden name="dto.[${status.index}].library" value="${dto.library}"/>
                                </td>
                                <td>
                                        ${dto.pdo}
                                            <stripes:hidden name="dto.[${status.index}].pdo" value="${dto.pdo}"/>
                                </td>
                                <td>
                                        ${dto.pdoSample}
                                        <stripes:hidden name="dto.[${status.index}].pdoSample" value="${dto.pdoSample}"/>
                                </td>
                                <td class="myIndex">
                                        ${dto.index}
                                            <stripes:hidden name="dto.[${status.index}].index" value="${dto.index}"/>
                                </td>
                                <td>
                                        ${dto.xNeeded}
                                        <stripes:hidden name="dto.[${status.index}].xNeeded" value="${dto.xNeeded}"/>
                                </td>
                                <td>
                                        ${dto.volume}
                                        <stripes:hidden name="dto.[${status.index}].volume" value="${dto.volume}"/>
                                </td>
                                <td>
                                        ${dto.storage}
                                        <stripes:hidden name="dto.[${status.index}].storage" value="${dto.storage}"/>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                    <div class="control-group">
                        <div class="controls">
                            <stripes:submit name="downloadPickList" value="Download Picklist" class="btn btn-primary"/>
                        </div>
                    </div>
                </stripes:form>
            </div>
            <div class="tab-pane" id="hiSeqXTab">
                <stripes:form beanclass="${actionBean.class.name}" id="topOffForm" class="form-horizontal">
                    <table id="topOffListTable" class="table simple">
                        <thead>
                        <tr>
                            <th width="30px">
                                <input type="checkbox" class="topOffListTable-checkAll" title="Check All"/>
                                <span id="count" class="topOffListTable-checkedCount"></span>
                            </th>
                            <th>Library</th>
                            <th>PDO</th>
                            <th>PDO Sample</th>
                            <th>Index</th>
                            <th>X Needed</th>
                            <th>Volume</th>
                            <th>Storage</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.dtos}" var="dto" varStatus="status">
                            <tr>
                                <td>
                                    <stripes:checkbox name="selectedSamples" class="machine-checkbox"
                                                      value="${dto.pdoSample}"/>
                                </td>
                                <td>
                                        ${dto.library}
                                    <stripes:hidden name="dto.[${status.index}].library" value="${dto.library}"/>
                                </td>
                                <td>
                                        ${dto.pdo}
                                    <stripes:hidden name="dto.[${status.index}].pdo" value="${dto.pdo}"/>
                                </td>
                                <td>
                                        ${dto.pdoSample}
                                    <stripes:hidden name="dto.[${status.index}].pdoSample" value="${dto.pdoSample}"/>
                                </td>
                                <td class="myIndex">
                                        ${dto.index}
                                    <stripes:hidden name="dto.[${status.index}].index" value="${dto.index}"/>
                                </td>
                                <td>
                                        ${dto.xNeeded}
                                    <stripes:hidden name="dto.[${status.index}].xNeeded" value="${dto.xNeeded}"/>
                                </td>
                                <td>
                                        ${dto.volume}
                                    <stripes:hidden name="dto.[${status.index}].volume" value="${dto.volume}"/>
                                </td>
                                <td>
                                        ${dto.storage}
                                    <stripes:hidden name="dto.[${status.index}].storage" value="${dto.storage}"/>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                    <div class="control-group">
                        <div class="controls">
                            <stripes:submit name="downloadPickList" value="Download Picklist" class="btn btn-primary"/>
                        </div>
                    </div>
                </stripes:form>
            </div>
            <div class="tab-pane" id="novaSeqTab">
            </div>
            <div class="tab-pane" id="poolGroupsTab">
            </div>
            <div class="tab-pane" id="reworksTab">
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
