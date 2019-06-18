<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PlateMetadataActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Metadata Upload"
                       sectionTitle="Metadata Upload for Single Cell" showCreate="false">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $j(document).ready(function () {
                $j("#accordion").accordion({  collapsible:true, active:false, heightStyle:"content", autoHeight:false});

                $j("#accordion").show();

                $j('.delete_btn').click(function() {
                    var rowIdx = this.id.split('-')[1];
                    $j('#vesselList-' + rowIdx).remove();
                    $j('#headerGroup-' + rowIdx).remove();
                    $j("#accordion").accordion("refresh");
                });
            });
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <div id="startNewSession">
            <stripes:form beanclass="${actionBean.class.name}" id="startNewSessionForm">
                <div class="form-horizontal">
                    <div class="control-group">
                        <stripes:label for="plateBarcode" class="control-label">Plate Barcode For File</stripes:label>
                        <div class="controls">
                            <stripes:text id="plateBarcode" name="plateBarcode"/><br/>
                        </div>
                    </div>
                    <div class="control-group">
                        <stripes:label for="metadataFile" class="control-label">Metadata File</stripes:label>

                        <div class="controls">
                            <stripes:file id="metadataFile" name="metadataFile" title="Metadata File"/><br/>
                        </div>
                    </div>
                    <div class="actionButtons">
                        <stripes:submit name="uploadMetadata" value="Upload Metadata File" class="btn"/>
                        <stripes:submit name="addToPdo" value="Receive Plates" class="btn"/>
                    </div>
                </div>
                <div id="uploadedPlatesList">
                    <c:if test="${not empty actionBean.uploadedPlates}">
                        <div id="plateMaps">
                            <div id="queuedPlates">
                                <div id="accordion" style="display:none;" class="accordion">
                                    <c:forEach items="${actionBean.uploadedPlates}" var="entry" varStatus="status">
                                        <c:set var="barcode" value="${entry.key}"/>
                                        <c:set var="metadata" value="${entry.value}"/>
                                        <div style="padding-left: 30px;padding-bottom: 2px" id="headerGroup-${status.index}">
                                            <div id="headerId" class="fourcolumn" style="padding: 0">
                                                <div>Plate Label: ${barcode}</div>
                                                <div>Num. Wells: ${metadata.size()}</div>
                                                <input class="delete_btn" id="remove-${status.index}" type="button" value="remove" />
                                            </div>
                                        </div>

                                        <div id="vesselList-${status.index}">
                                            <div>
                                                <stripes:layout-render name="/sample/plate_metadata_info_list.jsp" plateWells="${metadata}"
                                                                       barcode="${barcode}" index="${status.index}" bean="${actionBean}"/>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                        </div>
                    </c:if>
                </div>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>