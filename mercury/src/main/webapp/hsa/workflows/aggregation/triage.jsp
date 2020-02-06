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
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Triage" dataTablesVersion="1.10"  sectionTitle="Triage" showCreate="true">

    <stripes:layout-component name="extraHead">
        <script src="${ctxpath}/resources/scripts/Bootstrap/bootstrap.min.js"></script>

        <style>
            td.details-control {
                background: url('${ctxpath}/images/plus.png') no-repeat center center;
                cursor: pointer;
            }
            tr.shown td.details-control {
                background: url('${ctxpath}/images/shown.gif') no-repeat center center;
            }

            tr.spinning td.details-control {
                background: url('${ctxpath}/images/spinner.gif') no-repeat center center;
            }

            .table .text-right {text-align: right}

            .borderless td, .borderless th {
                border: none;
            }
        </style>
        <script type="text/javascript">
            $j(document).ready(function() {
               var inSpecTable = $j('#inSpecTable').DataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: false},
                        {
                            "className":      'details-control',
                            "orderable":      false,
                            "data":           null,
                            "defaultContent": ''
                        },
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        // {sortable: true},
                        // {sortable: true},
                        {sortable: true}
                    ],
                });

                var oosTable = $j('#oosSpecTable').DataTable({
                    renderer: "bootstrap",
                    paging: true,
                    pageLength : 150,
                    search : {
                        fieldtype: 'textarea'
                    },
                    columns: [
                        {sortable: false},
                        {
                            "className":      'details-control',
                            "orderable":      false,
                            "data":           null,
                            "defaultContent": ''
                        },
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        // {sortable: true},
                        // {sortable: true},
                        {sortable: true}
                    ],
                });

                $j('.inSpecTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'inSpecTable-checkAll',
                    countDisplayClass:'inSpecTable-checkedCount',
                    checkboxClass:'inSpecTable-checkbox'});

                $j('.oosSpecTable-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'oosSpecTable-checkAll',
                    countDisplayClass:'oosSpecTable-checkedCount',
                    checkboxClass:'oosSpecTable-checkbox'});

                $('#inSpecTab a').click(function (e) {
                    e.preventDefault();
                    $(this).tab('show');
                });

                $('#oosSpecTab a').click(function (e) {
                    e.preventDefault();
                    $(this).tab('show');
                });

                $('#oosSpecTable tbody').on('click', 'td.details-control', function () {
                    var tr = $(this).closest('tr');
                    var row = oosTable.row( tr );
                    var rowData = row.data();
                    var pdoSample = rowData[2].split(/\s+/)[0]; // Include SM then the table data for some reason?

                    if ( row.child.isShown() ) {
                        // This row is already open - close it
                        row.child.hide();
                        tr.removeClass('shown');
                    }
                    else {
                        tr.addClass("spinning");
                        $j.ajax({
                            url: "${ctxpath}/hsa/workflows/aggregation_triage.action?expandSample=&pdoSample=" + pdoSample,
                            datatype: 'html',
                            success: function (resultHtml) {
                                tr.removeClass("spinning");
                                console.log(resultHtml);
                                var div = $('<div/>');
                                div.html(resultHtml);
                                row.child(div).show();
                                tr.addClass('shown');
                            },
                            error: function(results){
                                tr.removeClass("spinning");
                            },
                            cache: false,
                        });
                    }
                });

                $('.dataTables_filter input[type="search"]').css(
                    {'width':'350px','display':'inline-block'}
                );

                // TODO JW Refactor
                $('.dataTables_filter input[type="search"]').keyup(function() {
                    var inputTxt = $(this).val();
                    var searchTerms = inputTxt.split(/[ ,]+/).filter(Boolean);
                    var asRegex = searchTerms.join("|");
                    console.log(asRegex);
                    oosTable.search(asRegex, true, false).draw() ;
                });

                $j('.flowcellStatusTable').hide();

            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <ul class="nav nav-tabs" id="specTabs">
            <li class="active"><a href="#inSpecTab" data-toggle="tab">In Spec</a></li>
            <li><a href="#oosSpecTab" data-toggle="tab">Out Of Spec</a></li>
        </ul>

        <div class="tab-content" id="specTabsContent">
            <div class="tab-pane active" id="inSpecTab">
                <stripes:form beanclass="${actionBean.class.name}" id="inSpecForm" method="POST">
                    <c:set var="tableId" value="inSpecTable" scope="request"/>
                    <c:set var="inSpecTable" value="${true}" scope="request"/>
                    <c:set var="dtoList" value="${actionBean.passingTriageDtos}" scope="request"/>
                    <c:set var="dtoName" value="passingTriageDtos" scope="request"/>
                    <jsp:include page="triage_table.jsp"/>
                    <stripes:submit name="sendToCloud" value="Send To Cloud" class="btn btn-primary"/>
                </stripes:form>
            </div>
            <div class="tab-pane" id="oosSpecTab">
                <stripes:form beanclass="${actionBean.class.name}" id="oosSpecForm" class="form-horizontal" method="POST">
                    <c:set var="tableId" value="oosSpecTable" scope="request"/>
                    <c:set var="inSpecTable" value="${false}" scope="request"/>
                    <c:set var="dtoList" value="${actionBean.oosTriageDtos}" scope="request"/>
                    <c:set var="dtoName" value="oosTriageDtos" scope="request"/>
                    <jsp:include page="triage_table.jsp"/>
                    <div class="control-group">
                        <stripes:label for="oosDecision" name="decision" class="control-label"/>
                        <div class="controls">
                            <stripes:select name="oosDecision" id="oosDecision">
                                <stripes:options-enumeration label="displayName"
                                                             enum="org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean.OutOfSpecCommands"/>
                            </stripes:select>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="commentText" class="control-label"/>
                        <div class="controls">
                            <stripes:textarea id="commentText" name="commentText"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <div class="controls">
                            <stripes:submit name="updateOutOfSpec" value="Update" class="btn btn-primary"/>
                        </div>
                    </div>
                </stripes:form>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
