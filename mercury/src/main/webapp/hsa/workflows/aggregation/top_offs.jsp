<%@ page import="org.broadinstitute.gpinformatics.mercury.presentation.hsa.SlurmActionBean" %>
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

<stripes:layout-render name="/layout.jsp" pageTitle="Triage" dataTablesVersion="1.10"  sectionTitle="Top Offs" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap.min.js"></script>
        <script src="${ctxpath}/resources/scripts/chroma.min.js"></script>

        <script type="text/javascript">
            $j(document).ready(function() {
                // Grab all unique indexes from table to create a color palette
                const indexes = new Set();
                var chromaColor = chroma.scale(['white', 'yellow', 'red', 'black'])
                    .correctLightness();

                const mapIdxToColor = {};
                $j.each($j(".myIndex"), function (idx, value) {
                    indexes.add($j(this).html());
                });



                var pal_for_queries = palette(['sequential'], 10, 0);
                var hsv6 = palette('hsv_rainbow', indexes.size);

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
                    rowCallback: function( nRow, aData) {
                        if ( aData[5] === 'Illumina_P5-Telef_P7-Yeyey' )
                        {
                            $j(nRow).css('background-color', 'red')
                        } else {
                            $j(nRow).css('background-color', 'red')
                        }
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
                <c:forEach items="${actionBean.dtos}" var="dto">
                    <tr>
                        <td>
                            <stripes:checkbox name="selectedIds" class="machine-checkbox"
                                              value="${dto.pdoSample}"/>
                        </td>
                        <td>${dto.library}</td>
                        <td>${dto.pdo}</td>
                        <td>${dto.pdoSample}</td>
                        <td class="myIndex">${dto.index}</td>
                        <td>${dto.xNeeded}</td>
                        <td>${dto.volume}</td>
                        <td>${dto.storage}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
