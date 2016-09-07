<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Designate Loading Tubes" sectionTitle="Designate Loading Tubes">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {
                <%@ include file="/run/designation_tube_list_setup_include.jsp" %>

                // Set up fields.
                $j('#lcsetBarcodeText').removeAttr('value');
                $j('#dateRangeDivNaturalLanguageString').val('Custom');

                $j('input.checkAll:checkbox').change(function(){
                    rowSelected();
                });
            });

            function lcsetUpdated() {
                if (!($j('lcsetsBarcodes'.value).trim)) {
                    $j('#loadDenatureBtn').removeAttr("disabled");
                    $j('#loadPoolNormBtn').removeAttr("disabled");
                } else {
                    $j('#loadDenatureBtn').attr("disabled", "disabled");
                    $j('#loadPoolNormBtn').attr("disabled", "disabled");
                }
            };

            <%@ include file="/run/designation_functions_include.jsp" %>

        </script>
        <style type="text/css">
            #tubeList
            .fixedWidthColumn { width: 8em; word-wrap: break-word; }
            .smallerWidthColumn { width: 4em; }
            .tinyWidthColumn { width: 2em; }
            .widerFixedWidthColumn { width: 10em; }
        </style>

    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">

            <div class="control-group" syle="padding-bottom: 50px;">
                <div style="float: left; width: 66%;">
                    <div class="control-group">
                        <div style="float: left; width: 50%;">
                            <textarea style="width: 95%" id="lcsetBarcodeText" type="text" rows="3" name="lcsetsBarcodes"
                                      placeholder='LCSETs, Loading Tube Barcodes, or leave blank for all undesignated tubes.'
                                      onkeypress="lcsetUpdated()" title="LCSET names with or without the LCSET prefix, or loading tube
 barcodes, or both. Only shows tubes created in the date range.

 Alternatively, leave this input blank and click 'Norm Tubes' to
 get all the undesignated loading tubes created in the date range."
                            ></textarea>
                        </div>

                        <div style="float: right; width: 50%">
                            <div id="dateRangeDiv"
                                 rangeSelector="${actionBean.dateRange.rangeSelector}"
                                 startString="${actionBean.dateRange.startStr}"
                                 endString="${actionBean.dateRange.endStr}">
                            </div>
                            <p>Only tubes created in this date range are shown.</p>
                        </div>
                    </div>

                    <div class="control-group"  style="position: absolute; left: 30%; transform: translate(-50%, -50%);">
                        <stripes:submit id="loadNormBtn" name="loadNorm" value="Norm Tubes" class="btn btn-primary"
                                        title="Adds normalization tubes to the designation display."/>
                        <stripes:submit id="loadDenatureBtn" name="loadDenature" value="Denature Tubes" class="btn btn-primary"
                                        title="Adds denature tubes to the designation display." disabled="disabled"/>
                        <stripes:submit id="loadPoolNormBtn" name="loadPoolNorm" value="Pooled Norm Tubes" class="btn btn-primary"
                                        title="Adds pooled normalization tubes to the designation display." disabled="disabled"/>
                    </div>
                </div>

                <div style="float: right; width: 33%;">
                    <stripes:checkbox id="showQueued" name="showQueued"/>
                    <stripes:label for="showQueued">Include Queued</stripes:label>
                    <stripes:checkbox id="showProcessed" name="showProcessed"/>
                    <stripes:label for="showProcessed">Include On FCT</stripes:label>
                    <stripes:checkbox id="showAbandoned" name="showAbandoned"/>
                    <stripes:label for="showAbandoned">Include Abandoned</stripes:label>
                    <stripes:checkbox id="append" name="append"/>
                    <stripes:label for="append" title="Controls whether the current list of designations is cleared
 or not when adding new designations to the display.">Append to the list</stripes:label>

                    <stripes:submit id="pending" name="pending" value="Show Designations" class="btn btn-primary"
                                    title="Shows previously queued designations."/>
                </div>
            </div>

            <c:if test="${not empty actionBean.dtos}">
                <%@ include file="/run/designation_multi_edit_include.jsp" %>
            </c:if>

            <%@ include file="/run/designation_tube_list_include.jsp" %>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
