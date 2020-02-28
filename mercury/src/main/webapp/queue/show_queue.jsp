<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.queueType.textName} Queue"
                       sectionTitle="${actionBean.queueType.textName} Queue" showCreate="false" dataTablesVersion="1.10">

    <stripes:layout-component name="extraHead">

        <style type="text/css">

            .barFull {
                height: 15px;
                width: 80px;
            }

            .barRemovedFromQueue {
                height: 15px;
                float: left;
                background-color: #dd2222;
            }

            .barStillInQueue {
                height: 15px;
                float: left;
                background-color: #22bb22;
            }

            .numberInQueue {
                float: left;
                margin-top: -16px;
                font-weight: bold;
                margin-left: 3px;
                color: white;
            }

            .numberRemovedFromQueue {
                float: right;
                margin-top: -16px;
                font-weight: bold;
                margin-right: 3px;
                color: white;
            }

            .positioning-input {
                width: 40px;
            }

            .edit-cell {
                display: inline-block;
                min-width: 18px;
                max-width: 18px;
                min-height: 18px;
                max-height: 18px;
                padding: 0;
                margin-left: 12px;
                border-width: 0;
                background-repeat: no-repeat;
                background-position: -64px -112px;
                background-image: url('/Mercury/images/ui-icons_2e83ff_256x240.png');
            }

            .searchWrapper {
                list-style: none;
                width: 100%;
                height: 100px;
                margin: 0;
                padding: 0;
                overflow: hidden;
            }

            .searchWrapper > li {
                float: left;
                width: 200px;
                height: 100px;
                padding: 15px;
            }
        </style>

        <script type="text/javascript">
            $j(document).ready(function () {
                $j('#queueTable').dataTable({
                    "paging": false,
                    "ordering": false,
                    "info": false,
                    "bSort": false,
                    "iDisplayLength": 25
                });

                // TBD: These dialogs not implemented yet
                // $j(".excludeSamplesDialog").dialog({ autoOpen: false, modal: true, title: "Sample IDs to exclude", resizable: false });
                // $j("#exclude-opener").click(function() {
                //     $j(".excludeSamplesDialog").dialog("open");
                // });
                // $j(".uploadSamplesDialog").dialog({ autoOpen: false, modal: true, title: "Upload Sample IDs", resizable: false });
                // $j("#upload-opener").click(function() {
                //     $j(".uploadSamplesDialog").dialog("open");
                // });

                $j("#dialogRename").dialog({
                    autoOpen: false,
                    height: 220,
                    width: 350,
                    modal: true,
                    buttons: {
                        "Rename": function () {
                            $j("#formRename").submit();
                        },
                        Cancel: function () {
                            $j("#dialogRename").dialog("close");
                        }
                    },
                    close: function () {
                        $j("#formRename")[0].reset();
                        $j("txtRenameGroupingId").val("");
                        $j("dialogRenameError").text("");
                    }
                });

                $j("#formRename").on("submit", function (event) {
                    event.preventDefault();
                    doRename();
                });

                $j(".edit-cell").tooltip();

            });

            let editGroupName = function (src) {
                let groupId = src.data("queueGroupingId");
                // The link text
                $j("#dialogRename").data("linkToUpdate", src.prev());
                $j("#txtRenameGroupingId").val(groupId);
                $j("#dialogRename").dialog("open");
            };

            let doRename = function () {
                let formData = new FormData();
                formData.append("newGroupName", $j("#txtNewGroupName").val());
                formData.append("queueGroupingId", $j("#txtRenameGroupingId").val());
                formData.append("renameGroup", "");
                formData.append("<csrf:tokenname/>", "<csrf:tokenvalue/>");
                $j.ajax({
                    url: "${ctxpath}/queue/Queue.action",
                    type: 'POST',
                    data: formData,
                    async: true,
                    success: function (results) {
                        if (results.hasOwnProperty('errors')) {
                            $j("#dialogRenameError").text(results.errors);
                        } else {
                            let link = $j("#dialogRename").data("linkToUpdate");
                            $j(link).text(results.newGroupName);
                            $j("#dialogRename").dialog("close");
                        }
                    },
                    error: function () {
                        $j("#dialogRenameError").text("A server error occurred");
                    },
                    cache: false,
                    datatype: "json",
                    processData: false,
                    contentType: false
                });
            };

        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">

        <stripes:form id="queueSearchForm" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:param name="queueType" value="${actionBean.queueType}" />
            <div>
                <ul class="searchWrapper">
                    <c:forEach items="${actionBean.allowedDisplaySearchTerms}" var="searchTerm">
                        <li style="width:70px;height:auto;">
                            <input name="selectedSearchTermType" id="selectedSearchTermType" value="${searchTerm.name}" type="radio" />
                            <label for="selectedSearchTermType">${searchTerm.name}</label>
                        </li>
                    </c:forEach>
                </ul>

                <textarea name="selectedSearchTermValues" id="selectedSearchTermValues" rows="4" ></textarea>
                <br/>
                <stripes:submit id="searchQueue" name="searchQueue">Search</stripes:submit>
            </div>
        </stripes:form>

        <div id="searchResults"></div>


        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:hidden name="queueType" />
            <table class="table simple dataTable" id="queueTable">
                <thead>
                    <tr>
                        <th>Readable Text</th><th>Queue Priority Type</th><th>Origin</th><th>Queue Status</th><th>Positioning</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.queueGroupings}" var="queueGrouping" varStatus="status">
                    <tr>
                        <td>
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean"
                                    event="viewGrouping">
                                <stripes:param name="queueGroupingId" value="${queueGrouping.queueGroupingId}"/>
                                <stripes:param name="queueType" value="${actionBean.queueType}"/>

                                ${queueGrouping.queueGroupingText}
                            </stripes:link>
                            <div class="edit-cell" title="Rename Grouping"
                                 data-queue-grouping-id="${queueGrouping.queueGroupingId}"
                                 onclick="editGroupName($j(this));"></div>
                        </td>
                        <td>${queueGrouping.queuePriority.displayName}</td>
                        <td>${queueGrouping.queueOrigin.displayName} <c:if
                                test="${not empty queueGrouping.queueSpecialization}">(${queueGrouping.queueSpecialization.displayName})</c:if></td>

                        <c:set var="removedFromQueue"
                               value="${actionBean.totalEntitiesInQueue[queueGrouping.queueGroupingId] - actionBean.remainingEntitiesInQueue[queueGrouping.queueGroupingId]}"/>
                        <c:set var="stillInQueue"
                               value="${actionBean.remainingEntitiesInQueue[queueGrouping.queueGroupingId]}"/>
                        <c:set var="percentRemovedFromQueue"
                                   value="${removedFromQueue * 100 / (removedFromQueue + stillInQueue)}"/>
                            <c:set var="percentStillInQueue"
                                   value="${stillInQueue * 100 / (removedFromQueue + stillInQueue)}"/>

                            <td title="${stillInQueue} need ${actionBean.queueType.textName}, ${removedFromQueue} do not need ${actionBean.queueType.textName}"
                                class="barFull">
                                <span class="barStillInQueue" style="width:${percentStillInQueue}%"></span>
                                <span class="barRemovedFromQueue" style="width:${percentRemovedFromQueue}%"></span>
                                <span class="numberInQueue">${stillInQueue}</span>
                                <span class="numberRemovedFromQueue">${removedFromQueue}</span>
                            </td>
                            <td>
                                <span class="position">
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean" event="moveToTop">
                                        <stripes:param name="queueGroupingId" value="${queueGrouping.queueGroupingId}" />
                                        <stripes:param name="queueType" value="${actionBean.queueType}" />
                                        <img src="${ctxpath}/images/Double_arrow_green_up.png" title="Move To Top" style="height: 24px;" />
                                    </stripes:link>
                                    <input type="radio" name="queueGroupingId" value="${queueGrouping.queueGroupingId}" />
                                    <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean" event="moveToBottom">
                                        <stripes:param name="queueGroupingId" value="${queueGrouping.queueGroupingId}" />
                                        <stripes:param name="queueType" value="${actionBean.queueType}" />
                                        <img src="${ctxpath}/images/Double_arrow_green_down.png" title="Move To Bottom" style="height: 24px;"/>
                                    </stripes:link>
                                </span>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
                <tfoot>
                <tr><td colspan="5">
                        Total Vessels In Queue: ${actionBean.totalInQueue}<br />
                    <c:forEach items="${actionBean.queuePriorities}" var="priority">
                        <c:if test="${actionBean.entitiesQueuedByPriority[priority] > 0}">
                            ${priority.displayName} Vessels: ${actionBean.entitiesQueuedByPriority[priority]}<br/>
                        </c:if>
                    </c:forEach>
                        Total Vessels in Rework: ${actionBean.totalNeedRework}
                    </td>
                </tr>
                </tfoot>
            </table>

            Position to move Selected Items to: <input type="text" name="positionToMoveTo" value="" />
            <stripes:submit name="updatePositions" value="Update Positions" />
        </stripes:form>
        <p>&#160;</p>
        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:hidden name="queueType" />
            Enter the barcodes of the vessels to Remove. One per line.
            <stripes:textarea name="excludeVessels" /><br />
            <stripes:submit name="excludeLabVessels" value="Exclude Vessels" />
        </stripes:form>
        <p>&#160;</p>
        <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:hidden name="queueType" />
            Enter the barcodes of the vessels to Add to the Queue.  One per line.
            <stripes:textarea name="enqueueSampleIds" /><br />
            <c:if test="${fn:length(actionBean.allowedQueueSpecializations) > 0}">
            Select applicable Queue Specialization:
            <stripes:select name="queueSpecialization">
                <stripes:option label="Select One: " value="" />
                <stripes:options-collection collection="${actionBean.allowedQueueSpecializations}" value="name" label="displayName" />
            </stripes:select><br />
            </c:if>
            <stripes:submit name="enqueueLabVessels" value="Add Vessels" />
        </stripes:form>

        <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.queue.QueueActionBean">
            <stripes:param name="queueType" value="${actionBean.queueType}" />
            <stripes:param name="downloadFullQueueData" />
            Download Data Dump
        </stripes:link>

        <div id="dialogRename" title="Rename Queue Group">
            <div id="dialogRenameError" style="color:red"></div>
            <form id="formRename">
                <label for="txtNewGroupName">Name</label> <input type="text" name="newGroupName" id="txtNewGroupName"
                                                                 style="width:300px">
                <input type="hidden" name="renameGroup" value=""/>
                <input type="hidden" name="queueGroupingId" id="txtRenameGroupingId" value=""/>
                <!-- Allow form submission with keyboard without duplicating the dialog button -->
                <input type="submit" tabindex="-1" style="position:absolute; top:-1000px">
            </form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
