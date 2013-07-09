<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create FCT Ticket" sectionTitle="Create FCT Ticket">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            function typeChanged() {
                var numLanesInput = $j('#numLanesText');
                var loadingConcInput = $j('#loadingConcText');
                if ($j('#typeSelect').val() == 'MISEQ') {
                    numLanesInput.prop('readonly', true);
                    numLanesInput.val(1);
                    loadingConcInput.prop('readonly', true);
                    loadingConcInput.val('7');
                }
                else {
                    numLanesInput.prop('readonly', false);
                    numLanesInput.val(0);
                    loadingConcInput.prop('readonly', false);
                    loadingConcInput.val('0');
                }
            }

            $(document).ready(function () {
                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[
                        [2, 'asc']
                    ],
                    "aoColumns":[
                        {"bSortable":false},
                        {"bSortable":true},
                        {"bSortable":true, "sType":"date"}
                    ]
                });

                $j('.tube-checkbox').enableCheckboxRangeSelection({
                    checkAllClass:'tube-checkAll',
                    countDisplayClass:'tube-checkedCount',
                    checkboxClass:'tube-checkbox'});
            });
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">
            <div class="control-group">
                <stripes:label for="lcsetText" name="LCSet Name" class="control-label"/>
                <div class="controls">
                    <stripes:text id="lcsetText" name="lcsetName"/>
                </div>
            </div>
            <div class="control-group">
                <div class="controls actionButtons">
                    <stripes:submit id="loadDenatureBtn" name="load" value="Load Denature Tubes" class="btn btn-mini"/>
                </div>
            </div>
            <c:if test="${not empty actionBean.denatureTubeToEvent}">
                <div class="control-group">
                    <h5 style="margin-left: 50px;">FCT Ticket Info</h5>
                    <hr style="margin: 0; margin-left: 50px"/>
                </div>
                <div class="control-group" style="margin-left: 50px">
                    <div class="controls">
                        <stripes:select id="typeSelect" name="selectedType" onchange="typeChanged()">
                            <stripes:options-collection collection="${actionBean.supportedTypes}"/>
                        </stripes:select>
                    </div>
                </div>
                <div class="control-group" style="margin-left: 50px">
                    <stripes:label for="numLanesText" name="Number of Lanes" class="control-label"/>
                    <div class="controls">
                        <stripes:text id="numLanesText" name="numberOfLanes"/>
                    </div>
                </div>
                <div class="control-group" style="margin-left: 50px">
                    <stripes:label for="loadingConcText" name="Loading Concentration" class="control-label"/>
                    <div class="controls" style="margin-bottom: 20px">
                        <stripes:text id="loadingConcText" name="loadingConc"/>
                    </div>
                    <table id="tubeList" class="table simple">
                        <thead>
                        <tr>
                            <th width="40">
                                <input type="checkbox" class="tube-checkAll"/><span id="count"
                                                                                    class="tube-checkedCount"></span>
                            </th>
                            <th>Tube Barcode</th>
                            <th>Denature Transfer Date</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${actionBean.denatureTubeToEvent}" var="tubeToEvent">
                            <tr>
                                <td>
                                    <stripes:checkbox class="tube-checkbox" name="selectedVesselLabels"
                                                      value="${tubeToEvent.key.label}"/>
                                </td>
                                <td>${tubeToEvent.key.label}</td>
                                <td><fmt:formatDate value="${tubeToEvent.value.eventDate}"
                                                    pattern="${actionBean.dateTimePattern}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="control-group" style="margin-left: 50px">
                    <div class="controls actionButtons">
                        <stripes:submit id="createFctBtn" name="save" value="Create FCT Tickets"
                                        class="btn btn-primary"/>
                    </div>
                </div>
            </c:if>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>