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

        <script type="text/javascript">
            $j(document).ready(function() {
                $j('#inSpecTable').DataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: false},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true}
                    ],
                });

                $j('#oosSpecTable').DataTable({
                    renderer: "bootstrap",
                    columns: [
                        {sortable: false},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true, "sClass": "nowrap"},
                        {sortable: true},
                        {sortable: true},
                        {sortable: true},
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
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <ul class="nav nav-tabs" id="specTabs">
            <li><a href="#inSpecTab" data-toggle="tab">In Spec</a></li>
            <li><a href="#oosSpecTab" data-toggle="tab">Out Of Spec</a></li>
        </ul>

        <div class="tab-content" id="specTabsContent">
            <div class="tab-pane active" id="inSpecTab">
                <stripes:form beanclass="${actionBean.class.name}" id="inSpecForm">
                    <c:set var="tableId" value="inSpecTable" scope="request"/>
                    <c:set var="inSpecTable" value="${true}" scope="request"/>
                    <c:set var="dtoList" value="${actionBean.passingTriageDtos}" scope="request"/>
                    <c:set var="dtoName" value="passingTriageDtos" scope="request"/>
                    <jsp:include page="triage_table.jsp"/>
                    <stripes:submit name="sendToCloud" value="Send To Cloud" class="btn btn-primary"/>
                </stripes:form>
            </div>
            <div class="tab-pane" id="oosSpecTab">
                <stripes:form beanclass="${actionBean.class.name}" id="oosSpecForm" class="form-horizontal">
                    <c:set var="tableId" value="oosSpecTable" scope="request"/>
                    <c:set var="inSpecTable" value="${false}" scope="request"/>
                    <c:set var="dtoList" value="${actionBean.oosTriageDtos}" scope="request"/>
                    <c:set var="dtoName" value="oosTriageDtos" scope="request"/>
                    <jsp:include page="triage_table.jsp"/>
                    <div class="control-group">
                        <stripes:label for="decision" name="decision" class="control-label"/>
                        <div class="controls">
                            <stripes:select name="decision" id="oosDecision">
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
