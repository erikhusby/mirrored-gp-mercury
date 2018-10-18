<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>

<header class="navbar">
    <div class="navbar-inner">
        <ul class="nav" role="navigation">
            <li class="dropdown">
                <a id="projectNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                        class="icon-briefcase"></span> Projects <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li>
                        <stripes:link id="editProject"
                                beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"
                                tabindex="=1" event="list">List</stripes:link>
                    </li>
                    <%-- PMs and sometimes PDMs (and Developers) can create Research Projects. --%>
                    <security:authorizeBlock roles="<%= roles(Developer, GPProjectManager, PM, PDM) %>">
                        <li>
                            <stripes:link id="createProject"
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"
                                    tabindex="=1" event="create">Create</stripes:link>
                        </li>
                    </security:authorizeBlock>
                </ul>
            </li>
            <li class="dropdown">
                <a id="orderNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                        class="icon-shopping-cart"></span> Orders <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li>
                        <stripes:link
                                beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                tabindex="=1" event="list">List</stripes:link>
                    </li>
                    <%-- PMs and PDMs (and Developers) can create Product Orders. --%>
                    <security:authorizeBlock roles="<%= roles(Developer, PDM, GPProjectManager, PM) %>">
                        <li>
                            <stripes:link id="createProductOrder"
                                          beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                          tabindex="=1" event="create">Create</stripes:link>
                        </li>
                    </security:authorizeBlock>

                    <security:authorizeBlock roles="<%= roles(Developer, BillingManager, PDM) %>">
                        <li class="divider"></li>
                        <li>
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"
                                    tabindex="=1" event="list">Billing Sessions</stripes:link>
                        </li>
                    </security:authorizeBlock>
                </ul>
            </li>
            <li class="dropdown">
                <a id="productNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                        class="icon-tags"></span> Products <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li>
                        <stripes:link
                                beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"
                                tabindex="=1" event="list">List</stripes:link>
                    </li>
                    <%-- Only PDMs (and Developers) can create Products. --%>
                    <security:authorizeBlock roles="<%= roles(Developer, PDM) %>">
                        <li>
                            <stripes:link
                                    id="createProduct"
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"
                                    tabindex="=1" event="create">Create</stripes:link>
                        </li>
                    </security:authorizeBlock>
                </ul>
            </li>

            <security:authorizeBlock
                    roles="<%= roles(LabUser, LabManager, PDM, GPProjectManager, PM, Developer)%>">
                <li class="dropdown">

                    <a id="labNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                            class="icon-tasks"></span> Lab <b class="caret"></b></a>
                    <ul class="dropdown-menu" role="menu">
                        <security:authorizeBlock roles="<%= roles(LabUser, LabManager, PDM, GPProjectManager, PM, Developer) %>">
                            <li>
                                <stripes:link id="addToBucket"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"
                                              event="view">Add Sample(s) To Bucket</stripes:link>
                            </li>

                        </security:authorizeBlock>
                        <li>
                            <stripes:link id="viewBuckets"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"
                                          event="view">Buckets</stripes:link>
                        </li>
                        <security:authorizeBlock roles="<%= roles(LabUser, LabManager, PDM, GPProjectManager, PM, Developer) %>">
                            <li>
                                <stripes:link id="controls"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean"
                                              event="list">Controls</stripes:link>
                            </li>
                        </security:authorizeBlock>
                        <li>
                            <stripes:link id="flowcellDesignations"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationActionBean"
                                          event="view">Designate Loading Tubes</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="designationFct"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationFctActionBean"
                                          event="view">Create FCT from Designations</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="createFCT"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean"
                                          event="view">Create FCT Ticket</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="linkDenatureToRB"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LinkDenatureTubeToReagentBlockActionBean"
                                          event="view">Link Denature Tube to Reagent Block</stripes:link>
                        </li>
                        <li>
                            <a tabindex="-1" href="${ctxpath}/reagent/design.action?list">Reagent Designs</a>
                        </li>
                        <li>
                            <stripes:link id="uploadQuants"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadQuantsActionBean"
                                          event="view">Upload Quant</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="nextStepsAfterInitialPico"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean"
                                          event="view">Pico Next Steps</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="fingerprintSpreadsheet"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.FingerprintingSpreadsheetActionBean"
                                          event="view">Create Fingerprinting Spreadsheet</stripes:link>
                        </li>
                        <security:authorizeBlock roles="<%= roles(LabUser, LabManager, PDM, Developer) %>">
                            <li>
                                <stripes:link id="listWorkflows"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.WorkflowActionBean"
                                              event="List">Show Workflows</stripes:link>
                            </li>
                        </security:authorizeBlock>
                        <li>
                            <stripes:link id="uploadSampleVessels"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.SampleVesselActionBean"
                                          event="view">Upload Sample Vessels</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="accessioning"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestAccessioningActionBean">
                                Accession Manifest
                            </stripes:link>
                        </li>
                        <li>
                            <stripes:link id="manifestTubeTransfer"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ManifestTubeTransferActionBean">
                            Transfer Tubes from Manifest
                            </stripes:link>
                        </li>
                        <li>
                            <stripes:link id="uploadReagents"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.ReagentActionBean"
                                          event="view">Upload Reagents</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="manualTransfers"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean"
                                          event="view">Manual Transfers</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="lcsetControls"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LcsetActionBean"
                                          event="view">LCSET Controls</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="batchWorkflow"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BatchWorkflowActionBean"
                                          event="view">Batch Workflow</stripes:link>
                        </li>
                        <security:authorizeBlock roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer) %>">
                            <li>
                                <stripes:link id="molecularIndexUpload"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.reagent.MolecularIndexPlateActionBean"
                                              event="view">Upload Molecular Index Plate(s) from Manifest</stripes:link>
                            </li>
                        </security:authorizeBlock>
                        <li>
                            <stripes:link id="abandonVessels"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AbandonVesselActionBean"
                                          event="view">Abandon Vessels</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="sampleSheet"
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.ArraysReportActionBean"
                                    event="view">Arrays Reports</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="externalLibraryUpload"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean"
                                          event="view">Pooled Tube and External Library Upload</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="tagVessel"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.TagVesselActionBean"
                                          event="view">Add Dev Condition To Vessel</stripes:link>
                        </li>
                        <li>
                            <stripes:link id="singleCellMetadataUpload"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.PlateMetadataActionBean"
                                          event="view">Single Cell Metadata Upload</stripes:link>
                        </li>
                    </ul>
                </li>
            </security:authorizeBlock>

            <security:authorizeBlock roles="<%= roles(Developer, PipelineManager) %>">
                <li class="dropdown">
                    <a id="adminNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                            class="icon-cog"></span> Admin <b class="caret"></b></a>
                    <ul class="dropdown-menu" role="menu">
                        <li><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"
                                event="showAligner">Manage Aligners</stripes:link></li>
                        <li><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"
                                event="showAnalysisType">Manage Analysis Type</stripes:link></li>
                        <li><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"
                                event="showReagentDesign">Manage Reagent Design</stripes:link></li>
                        <li><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.analysis.ManageAnalysisFieldsActionBean"
                                event="showReferenceSequence">Manage Reference Sequence</stripes:link></li>
                        <li><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.GenotypingChipTypeActionBean"
                                event="list">Manage Genotyping Chip Types</stripes:link></li>
                        <security:authorizeBlock roles="<%= roles(Developer) %>">
                            <li><stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.admin.BillingSessionAccessActionBean"
                                    event="list">Manage Billing Session Locks</stripes:link></li>
                            <li><stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.admin.PublicMessageAdminActionBean"
                                    event="view">Manage Public Message</stripes:link></li>
                            <li><stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.admin.SAPAccessControlActionBean"
                                    event="view">Manage Access to SAP Components</stripes:link></li>
                        </security:authorizeBlock>
                    </ul>
                </li>
            </security:authorizeBlock>

            <security:authorizeBlock roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer, LabUser) %>">

                <li class="dropdown">
                    <a id="storageNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                            class="icon-inbox"></span> Storage <b class="caret"></b></a>
                    <ul class="dropdown-menu" role="menu">
                        <security:authorizeBlock
                                roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer, LabUser) %>">
                            <li>

                                <stripes:link id="listStorage"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageLocationActionBean"
                                              event="list">List</stripes:link>
                            </li>
                        </security:authorizeBlock>
                        <security:authorizeBlock roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer) %>">
                            <li>
                                <stripes:link id="createStorage"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.storage.StorageLocationActionBean"
                                              event="create">Create New Storage</stripes:link>
                            </li>
                        </security:authorizeBlock>
                        <li class="divider"></li>
                        <security:authorizeBlock
                                roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer, LabUser) %>">

                            <li>
                                <stripes:link id="findContainer"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"
                                              event="view">
                                    Check In / Search Container
                                </stripes:link>
                            </li>
                        </security:authorizeBlock>

                        <security:authorizeBlock
                                roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer, LabUser) %>">
                            <li>
                                <stripes:link id="createNewContainer"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.container.ContainerActionBean"
                                              event="create">
                                    Create New Container
                                </stripes:link>
                            </li>
                        </security:authorizeBlock>
                        <security:authorizeBlock
                                roles="<%= roles(LabManager, PDM, GPProjectManager, PM, Developer, LabUser) %>">

                            <li>
                                <stripes:link id="pickFromStorage"
                                              beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.PickerActionBean"
                                              event="view">
                                    Create Pick List From Storage
                                </stripes:link>
                            </li>
                        </security:authorizeBlock>
                    </ul>
                </li>
            </security:authorizeBlock>
        </ul>
        <ul class="nav pull-right global-search navbar-search">
            <li style="white-space:nowrap;">
                <stripes:form beanclass="org.broadinstitute.gpinformatics.athena.presentation.search.SearchActionBean"
                              name="quickSearch" method="GET">
                    <input type="search" data-type="search" name="searchKey" placeholder="Search for a RP, PDO or P"
                           class="search-query ui-input-text ui-body-null" autosave="unique" results="10"
                           style="margin-top: 5px;vertical-align: top;height:14px;"/>
                </stripes:form>
            </li>
            <li class="dropdown">
                <a id="searchNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                        class="icon-search"></span> Search <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu">
                    <li>
                        <stripes:link id="vesselSearch"
                                      beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                      event="view">Vessels</stripes:link>
                    </li>
                    <li>
                        <stripes:link id="sampleSearch"
                                      beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"
                                      event="view">Samples</stripes:link>
                    </li>
                    <li>
                        <a href="${ctxpath}/search/ConfigurableSearch.action?fetchSearch=&selectedSearchName=GLOBAL%7CGLOBAL_LAB_VESSEL_SEARCH_INSTANCES%7CLCSET+Data&entityName=LabVessel">LCSets</a>
                    </li>
                    <li>
                        <stripes:link id="userDefSearch"
                                      beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean"
                                      event="entitySelection">User-Defined</stripes:link>
                    </li>
                    <li>
                        <stripes:link id="auditTrailSearch"
                                      beanclass="org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditTrailActionBean"
                                      event="view">Audit Trail</stripes:link>
                    </li>
                    <li>
                        <stripes:link id="transferVisualizer"
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.labevent.TransferVisualizerActionBean"
                                event="view">Transfer Visualizer</stripes:link>
                    </li>
                    <li>
                        <stripes:link id="plateMapView"
                                      beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.MetricsViewActionBean"
                                      event="view">Plate Map Metrics</stripes:link>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</header>
