<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ page import="org.broadinstitute.gpinformatics.athena.presentation.projects.RegulatoryInfoActionBean" %>

<stripes:layout-definition>
    <script type="text/javascript">
        $j(document).ready(function() {
            $j('#addRegulatoryInfoDialog').dialog({
                autoOpen: false,
                height: 500,
                width: 700,
                modal: true
            });

            $j('#regulatoryInfoSearchForm').submit(searchRegulatoryInfo);
        });

        function resetRegulatoryInfoDialog(errorMessage) {
            $j('#regulatoryInfoQuery').val('');
            $j('#addRegulatoryInfoDialogSheet2').html('');
            $j('#regInfoErrors').html('');
            $j('#addRegulatoryInfoDialogSheet1').show();
            $j('#statusMessage').hide();
            if(errorMessage) {
                $j('#statusMessage').show();
                $j('#statusMessage').html(errorMessage);
            }
        }

        var regulatory_info_dialog = {};

        /**
         * Open a dialog box for adding regulatory information to a research project.
         *
         * @param rpKey                 business key of the research project
         * @param rpLabel               label to display in the title bar of the dialog box
         * @param callback              function to call after successful addition of the regulatory information
         * @param autoSearchIdentifier  optional identifier to automatically search, going directly to result display
         */
        function openRegulatoryInfoDialog(rpKey, rpLabel, callback, autoSearchIdentifier) {
            regulatory_info_dialog.rpKey = rpKey;
            regulatory_info_dialog.successCallback = callback;
            resetRegulatoryInfoDialog();
            if (autoSearchIdentifier) {
                $j('#regulatoryInfoQuery').val(autoSearchIdentifier);
                searchRegulatoryInfo();
            }
            var regInfoDialogDiv = $j('#addRegulatoryInfoDialog');
            regInfoDialogDiv.find('input[name=researchProjectKey]').val(rpKey);
            regInfoDialogDiv.dialog({title: 'Add Regulatory Information for ' + rpLabel}).dialog("open");
        }

        /**
         * Open a dialog box for editing the title of a regulatory information record.
         *
         * @param regulatoryInfoId  primary key of the regulatory information to edit
         * @param rpLabel           label to display in the title bar of the dialog box
         * @param callback          function to call after successful edit
         */
        function openRegulatoryInfoEditDialog(regulatoryInfoId, rpLabel, callback) {
            regulatory_info_dialog.successCallback = callback;
            $j('#addRegulatoryInfoDialogSheet2').html('');
            $j('#regInfoErrors').html('');
            $j('#addRegulatoryInfoDialogSheet1').hide();
            var regInfoDialogDiv = $j('#addRegulatoryInfoDialog');
            regInfoDialogDiv.dialog({title: 'Edit Regulatory Information for ' + rpLabel}).dialog("open");

            $j.ajax({
                url: '${ctxpath}/projects/regulatoryInfo.action',
                data: {
                    '<%= RegulatoryInfoActionBean.VIEW_REGULATORY_INFO_ACTION %>': '',
                    regulatoryInfoId: regulatoryInfoId
                },
                dataType: 'html',
                success: function(html) {
                    $j('#statusMessage').hide();
                    $j('#addRegulatoryInfoDialogSheet2').html(html);
                }
            });
        }

        function closeRegulatoryInfoDialog() {
            $j('#addRegulatoryInfoDialog').dialog("close");
        }

        function searchRegulatoryInfo(event) {
            if (event) {
                event.preventDefault();
            }
            $j.ajax({
                url: '${ctxpath}/projects/regulatoryInfo.action',
                data: {
                    '<%= RegulatoryInfoActionBean.REGULATORY_INFO_QUERY_ACTION %>': '',
                    researchProjectKey: regulatory_info_dialog.rpKey,
                    q: $j('#regulatoryInfoQuery').val()
                },
                dataType: 'html',
                success: function(html) {
                    $j('#addRegulatoryInfoDialogSheet2').html(html);
                }
            });
            $j('#addRegulatoryInfoDialogSheet1').hide();
        }

        function successfulAddCallback() {
            if (regulatory_info_dialog.successCallback) {
                regulatory_info_dialog.successCallback();
            }
        }

        function handleRegInfoAjaxError(xhr, status, error) {
            var $alert = $j(xhr.responseText).find('.alert.alert-block');
            $j('#regInfoErrors').html('<p>' + status + ': ' + error + '</p>' + $alert.html());
        }
    </script>
    <div id="addRegulatoryInfoDialog" title="Add Regulatory Information" class="form-horizontal">

        <div id="addRegulatoryInfoDialogSheet1">
            <div id="statusMessage" class="alert alert-block" style="font-weight: bold"></div>
            <p>Enter the Broad ORSP number Determination number to see if the regulatory information is already known to Mercury.</p>
            <stripes:form id="regulatoryInfoSearchForm" beanclass="<%=RegulatoryInfoActionBean.class.getName()%>">
                <stripes:hidden name="researchProjectKey"/>
                <div class="control-group">
                    <stripes:label for="regulatoryInfoQuery" class="control-label">ORSP #</stripes:label>
                    <div class="controls">
                        <input id="regulatoryInfoQuery" type="text" name="q" required>
                        <button id="regulatoryInfoSearchButton" class="btn btn-primary">Search</button>
                    </div>
                </div>
            </stripes:form>
        </div>

        <div id="addRegulatoryInfoDialogSheet2"></div>
        <div id="regInfoErrors" style="color: red; font-weight: bold;"></div>
    </div></stripes:layout-definition>
