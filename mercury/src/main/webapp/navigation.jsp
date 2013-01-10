<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.DB" %>
<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security" prefix="security" %>
<%@ page import="org.broadinstitute.gpinformatics.mercury.entity.DB" %>
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
                <a id="projectNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-briefcase"></span> Projects <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" tabindex="=1" event="list">List</stripes:link>
                    </li>
                    <li>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean" tabindex="=1" event="create">Create</stripes:link>
                    </li>
                </ul>
            </li>
            <li class="dropdown">
                <a id="orderNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-shopping-cart"></span> Orders <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" tabindex="=1" event="list">List</stripes:link>
                    </li>
                    <li>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean" tabindex="=1" event="create">Create</stripes:link>
                    </li>
                    <li class="divider"></li>
                    <li>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean" tabindex="=1" event="list">Billing Sessions</stripes:link>
                    </li>
                </ul>
            </li>
            <li class="dropdown">
                <a id="productNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-tags"></span> Products <b class="caret"></b></a>
                <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                    <li>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean" tabindex="=1" event="list">List</stripes:link>
                    </li>
                    <li>
                        <stripes:link beanclass="org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean" tabindex="=1" event="create">Create</stripes:link>
                    </li>
                </ul>
            </li>

            <security:authorizeBlock roles="<%=new String[] {DB.Role.Developer.name}%>">
                <li class="dropdown">
                    <a id="adminNav" href="#" role="button" class="dropdown-toggle" data-toggle="dropdown"><span class="icon-cog"></span> Admin <b class="caret"></b></a>
                    <ul class="dropdown-menu" role="menu" aria-labelledby="drop1">
                        <li><a tabindex="-1" href="${ctxpath}/reagent/design.action?list">Reagent Design</a></li>
                        <li>
                            <stripes:link beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean" tabindex="1" event="view">Create Batch</stripes:link>
                        </li>
                    </ul>
                </li>

            </security:authorizeBlock>
        </ul>

        <security:authorizeBlock roles="<%=new String[] {DB.Role.Developer.name}%>">
            <ul class="nav pull-right global-search">
                <li style="white-space:nowrap;">
                    <stripes:form beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean">
                    <input type="text" name="searchKey" title="enter a barcode to search" class="defaultText" style="margin-top: 5px;vertical-align: top;height:14px;"/>
                    <input type="submit" name="search" value="Search" class="btn btn-mini"/>
                    &#160;
                    <stripes:link style="display: inline; padding: 0px;" title="Click for advanced search options" beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean" event="view">Advanced</stripes:link>
                    </stripes:form>
                </li>
            </ul>
        </security:authorizeBlock>
    </div>
</header>
