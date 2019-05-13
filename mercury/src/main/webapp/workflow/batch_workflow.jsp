<%@ include file="/resources/layout/taglibs.jsp" %>
<stripes:useActionBean var="actionBean"
        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Batch Workflow" sectionTitle="Batch Workflow">
    <stripes:layout-component name="extraHead">
        <style type="text/css">
            label {
                display: inline;
                font-weight: bold;
            }
            input[type="text"] {
                width: 100px;
                font-size: 12px;
                padding: 2px 2px;
            }
        </style>
        <script src="${ctxpath}/resources/scripts/jquery.validate-1.14.0.min.js"></script>
        <script type="text/javascript">
            $j(document).ready(function () {
                <c:if test="${not empty actionBean.anchorIndex}">
                // We're returning from the manual transfer page, so scroll to the link that took us there
                $j('#anchor${actionBean.anchorIndex - 1}')[0].scrollIntoView();
                </c:if>

                $j.validator.addMethod(
                    "expirationDate",
                    function (value, element) {
                        try {
                            var parsedDate = $j.datepicker.parseDate("mm/dd/yy", value, null);
                            return this.optional(element) || parsedDate > new Date();
                        } catch(error) {
                            return false;
                        }
                    },
                    "Enter a valid future date (m/d/yyyy)"
                );
                $j.validator.classRuleSettings.expirationDate = {expirationDate: true};
                $j('.reagentForm').each(function() { $j(this).validate(); });

                $j('.manualTransferPageLink').click(function (event) {
                    var expectedEvent = "${actionBean.expectedWorkflowEvent.workflowStepDef.name}";
                    var addressValue = $(this).attr("href");
                    var params = addressValue.split('&');
                    for (var i = 0; i < params.length; i++) {
                        var param = params[i];
                        if (param.startsWith('workflowStepName')) {
                            var actualEvent = param.split("=")[1];
                            actualEvent = decodeURIComponent(actualEvent);
                            actualEvent = actualEvent.replace(/\+/g,' ');
                            queryUser(actualEvent, expectedEvent, event);
                        }
                    }
                });

                $j('form.reagentForm').submit(function (event) {
                    var expectedEvent = "${actionBean.expectedWorkflowEvent.workflowStepDef.name}";
                    var form = this;
                    var actualEvent = $("input[name='workflowStepName']", form).val();
                    queryUser(actualEvent, expectedEvent, event);
                });

                function queryUser(actualEvent, expectedEvent, formEvent) {
                    if (actualEvent !== expectedEvent) {
                        if (!confirm("Expect next workflow step to be: " + expectedEvent +
                            ". Are you sure you want to proceed?")) {
                            formEvent.preventDefault();
                        }
                    }
                }
            });
            // Some scanners send carriage return, we don't want this to submit the form
            $j(document).on("keypress", ":input:not(textarea)", function (event) {
                return event.keyCode != 13;
            });
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean">
            <label for="batchName">Batch Name</label>
            <input type="text" id="batchName" name="batchName">
            <input type="submit" name="view" value="Find">
        </stripes:form>

        <c:if test="${not empty actionBean.labBatch}"><h3>${actionBean.labBatch.batchName}</h3>
            ${actionBean.effectiveWorkflowDef.productWorkflowDef.name}, version ${actionBean.effectiveWorkflowDef.version},
            ${actionBean.effectiveWorkflowDef.effectiveDate}
        </c:if>
        <table class="table simple">
            <tr>
                <th>Workflow Step</th>
                <th>Events</th>
            </tr>
            <c:forEach items="${actionBean.workflowEvents}" var="workflowEvent" varStatus="workflowEventStatus">
                <tr style="${workflowEvent.skipped ? "background-color:#FFFF99" : (empty workflowEvent.labEvents ? "" : "background-color:LightGray")}">
                    <td>
                        <a name="${workflowEventStatus.index}" id="anchor${workflowEventStatus.index}"></a>
                        <div>
                            <b>${workflowEvent.workflowStepDef.name}</b>
                        </div>
                        <div>
                            ${workflowEvent.workflowStepDef.instructions}
                        </div>
                        <c:forEach items="${workflowEvent.workflowStepDef.labEventTypes}" var="labEventType">
                            <c:choose>
                                <c:when test="${not empty labEventType.manualTransferDetails.messageType or not empty workflowEvent.workflowStepDef.manualTransferDetails}">
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"
                                            event="chooseEventType" class="manualTransferPageLink">
                                        <stripes:param name="stationEvents[0].eventType" value="${labEventType.name}"/>
                                        <stripes:param name="workflowProcessName" value="${workflowEvent.workflowStepDef.processDef.name}"/>
                                        <stripes:param name="workflowStepName" value="${workflowEvent.workflowStepDef.name}"/>
                                        <stripes:param name="workflowEffectiveDate" value="${actionBean.labBatch.createdOn}"/>
                                        <stripes:param name="batchName" value="${actionBean.labBatch.batchName}"/>
                                        <stripes:param name="anchorIndex" value="${workflowEventStatus.index}"/>
                                        Manual Transfer
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean"
                                            class="reagentForm">
                                        <%-- See https://code.google.com/p/chromium/issues/detail?id=468153 --%>
                                        <div style="display: none;">
                                            <input type="text" id="PreventChromeAutocomplete" name="PreventChromeAutocomplete" autocomplete="address-level4" />
                                        </div>
                                        <input type="hidden" name="batchName" value="${actionBean.batchName}"/>
                                        <input type="hidden" name="workflowProcessName" value="${workflowEvent.workflowStepDef.processDef.name}"/>
                                        <input type="hidden" name="workflowStepName" value="${workflowEvent.workflowStepDef.name}"/>
                                        <input type="hidden" name="workflowEffectiveDate" value="${actionBean.labBatch.createdOn}"/>
                                        <input type="hidden" name="labEventType" value="${labEventType}"/>
                                        <input type="hidden" name="workflowQualifier" value="${workflowEvent.workflowStepDef.workflowQualifier}"/>
                                        <stripes:param name="anchorIndex" value="${workflowEventStatus.index}"/>
                                        <c:choose>
                                            <c:when test="${labEventType eq 'ADD_REAGENT'}">
                                                <c:forEach items="${workflowEvent.workflowStepDef.reagentTypes}" var="reagentType" varStatus="loop">
                                                    <div class="control-group">
                                                        <label for="rgtType${loop.index}">Type </label>${reagentType}
                                                        <input type="hidden" id="rgtType${loop.index}"
                                                                name="reagentNames[${loop.index}]" value="${reagentType}">

                                                        <label for="rgtBcd${loop.index}">Barcode </label>
                                                        <input type="text" id="rgtBcd${loop.index}" class="required"
                                                                name="reagentLots[${loop.index}]" autocomplete="off">

                                                        <label for="rgtExp${loop.index}">Expiration mm/dd/yyyy</label>
                                                        <input type="text" id="rgtExp${loop.index}" class="required expirationDate"
                                                                name="reagentExpirations[${loop.index}]" autocomplete="off">

                                                        <label for="rgtVol${loop.index}">Volume </label>
                                                        <input type="text" id="rgtVol${loop.index}" class="required number"
                                                                name="reagentVolumes[${loop.index}]" autocomplete="off"> ul
                                                    </div>
                                                </c:forEach>
                                                <stripes:submit name="${actionBean.batchReagentAction}" value="Add"/>
                                            </c:when>
                                            <c:otherwise>
                                                <stripes:submit name="${actionBean.batchEventAction}" value="Done"/>
                                            </c:otherwise>
                                        </c:choose>
                                    </stripes:form>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </td>
                    <td>
                        <c:forEach items="${workflowEvent.labEvents}" var="labEvent">
                            <div>
                                ${labEvent.labEventType.name} ${labEvent.workflowQualifier} ${actionBean.getUserFullName(labEvent.eventOperator)} ${labEvent.eventDate}
                            </div>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
