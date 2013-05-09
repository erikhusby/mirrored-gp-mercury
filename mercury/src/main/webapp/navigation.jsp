<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.Role.*" %>
<%@ page import="static org.broadinstitute.gpinformatics.mercury.entity.DB.roles" %>
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

<header class="navbar">
    <div class="navbar-inner">
        <ul class="nav" role="navigation">
            <li class="dropdown">
                <a id="projectNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                        class="icon-briefcase"></span> Projects <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li>
                        <stripes:link
                                beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean"
                                tabindex="=1" event="list">List</stripes:link>
                    </li>
                    <%-- Only PMs (and Developers) can create Research Projects. --%>
                    <security:authorizeBlock roles="<%= roles(Developer, PM) %>">
                        <li>
                            <stripes:link
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
                    <%-- PMs and PDMs (and Developers) can place Product Orders. --%>
                    <security:authorizeBlock roles="<%= roles(Developer, PDM, PM) %>">
                        <li>
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean"
                                    tabindex="=1" event="create">Create</stripes:link>
                        </li>
                    </security:authorizeBlock>
                    <li class="divider"></li>
                    <li>
                        <stripes:link
                                beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean"
                                tabindex="=1" event="list">Billing Sessions</stripes:link>
                    </li>
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
                                    beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean"
                                    tabindex="=1" event="create">Create</stripes:link>
                        </li>
                    </security:authorizeBlock>
                </ul>
            </li>

            <security:authorizeBlock roles="<%= roles(PDM, Developer)%>">
                <li class="dropdown">

                    <a id="labNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                            class="icon-tasks"></span> Lab <b class="caret"></b></a>
                    <ul class="dropdown-menu" role="menu">
                        <li>
                            <stripes:link id="viewBuckets"
                                          beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"
                                          event="view">Buckets</stripes:link>
                        </li>
                        <security:authorizeBlock roles="<%= roles(Developer) %>">
                            <li>
                                <stripes:link
                                        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.CollaboratorControlsActionBean"
                                        event="list">Controls</stripes:link>
                            </li>
                            <li>
                                <stripes:link
                                        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean"
                                        event="view">Add Rework</stripes:link>
                            </li>
                            <li>
                                <stripes:link
                                        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.LinkDenatureTubeToFlowcellActionBean"
                                        event="view">Link Denature Tube to Flowcell</stripes:link>
                            </li>
                            <li><a tabindex="-1" href="${ctxpath}/reagent/design.action?list">Reagent Designs</a></li>
                        </security:authorizeBlock>
                    </ul>
                </li>
            </security:authorizeBlock>

            <security:authorizeBlock roles="<%= roles(Developer) %>">
                <li class="dropdown">
                    <a id="adminNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span
                            class="icon-cog"></span> Admin <b class="caret"></b></a>
                    <ul class="dropdown-menu" role="menu">
                        <li><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean"
                                tabindex="1" event="startBatch">Create Batch</stripes:link></li>
                    </ul>
                </li>
            </security:authorizeBlock>

        </ul>

        <security:authorizeBlock roles="<%= roles(Developer) %>">
            <ul class="nav pull-right global-search navbar-search">
                <li style="white-space:nowrap;">
                    <stripes:form
                            beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean">
                        <input type="search" data-type="search" name="searchKey"
                               placeholder="Enter a PDO, Sample or Barcode"
                               class="search-query ui-input-text ui-body-null"
                               style="margin-top: 5px;vertical-align: top;height:14px;"/>
                        <input type="submit" name="search" value="Search" class="btn btn-mini"/>
                        &#160;
                        <stripes:link style="display: inline; padding: 0px;" title="Click for advanced search options"
                                      beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean"
                                      event="view">Advanced</stripes:link>
                    </stripes:form>
                </li>
            </ul>
        </security:authorizeBlock>
    </div>
</header>
