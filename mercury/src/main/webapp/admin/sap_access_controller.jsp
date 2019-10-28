<%@ page import="org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus" %>
<%@ include file="/resources/layout/taglibs.jsp" %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.admin.SAPAccessControlActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="${actionBean.accessController.controlTitle}"
                       sectionTitle="Manage availability of SAP">

    <stripes:layout-component name="extraHead">
    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}">
            <div class="form-horizontal span6">

                <div class="control-group" id="enableAccess">

                    <stripes:label for="access" class="control-label">
                        Enable/Disable usage of SAP
                    </stripes:label>
                    <div class="controls">
                        <stripes:select name="enabledAccess" id="access">
                            <stripes:option value="<%=AccessStatus.DISABLED.name()%>" label="Disable SAP Access"/>
                            <stripes:option value="<%=AccessStatus.ENABLED.name()%>" label="Enable SAP Access"/>
                        </stripes:select>
                    </div>
                </div>

                <%--<fieldset>--%>
                    <%--<legend><h4>Price Items to "Blacklist"</h4></legend>--%>
                <%--<div id="priceItems" class="control-group">--%>

                    <%--<c:forEach items="${actionBean.priceListOptions}" var="priceItem">--%>

                        <%--<div class="controls">--%>
                            <%--<div class="form-value">--%>

                                <%--<stripes:checkbox name="selectedPriceItems" class="shiftCheckbox"--%>
                                                  <%--title="${priceItem.name}"--%>
                                                  <%--value="${priceItem.name}" id="${priceItem.name}-id"/>--%>
                                <%--<stripes:label for="${priceItem}-id">--%>
                                    <%--${priceItem.name} -- ${priceItem.platformName}--%>
                                <%--</stripes:label>--%>
                            <%--</div>--%>
                        <%--</div>--%>
                    <%--</c:forEach>--%>
                <%--</div>--%>
                <%--</fieldset>--%>

                <div class="controls actionButtons">
                    <stripes:submit name="setAccess" value="Set Selected Access options" class="btn"/>
                    <stripes:submit name="resetAccess" value="Reset the Access Settings" class="btn"/>
                </div>
            </div>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>    