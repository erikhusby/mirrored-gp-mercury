<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.vessel.ReworkActionBean"/>


<stripes:form beanclass="${actionBean.class.name}">
    <br/>

    <div class="control-group">
        <stripes:label for="type" class="control-label">
            Type: *
        </stripes:label>

        <div class="controls">
            <stripes:select name="editReagentDesign.reagentType">
                <stripes:option value="">Choose...</stripes:option>
                <stripes:option value="">Machine Error</stripes:option>
                <%--<stripes:options-enumeration--%>
                <%--enum="org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType"/>--%>
            </stripes:select>

        </div>

    </div>

    <div class="control-group">
        <stripes:label for="comments" class="control-label">
            Comments:
        </stripes:label>

        <div class="controls">
            <textarea rows="5" cols="160" id="comments" name="comments"
                      title="comment"
                      style="width:auto;" class="defaultText"/>
        </div>
    </div>

    <div class="control-group">
        <stripes:label for="type" class="control-label">
            Apply to step: *
        </stripes:label>
        <div class="controls">
            <stripes:select name="editReagentDesign.reagentType">
                <stripes:option value="">Choose...</stripes:option>
                <stripes:option value="">Shearing Bucket</stripes:option>
                <%--<stripes:options-enumeration--%>
                <%--enum="org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign.ReagentType"/>--%>
            </stripes:select>

        </div>
    </div>
    <div class="control-group">
        <stripes:label for="type" class="control-label">
            Rework Level: *
        </stripes:label>
        <div class="controls">
            <stripes:radio name="type" value="Level 1"/> Level 1<br/>
            <stripes:radio name="type" value="Level 2"/> Level 2<br/>
            <stripes:radio name="type" value="Level 3"/> Level 3<br/>
        </div>
    </div>
    <div class="control-group">
        <div class="controls" style="margin-left: 80px;">
            <stripes:submit name="Mark for Rework" value="Mark for Rework"/>
        </div>
    </div>


    <%--<div>--%>
    <%--<a href="javascript:applyHeatMap('${actionBean.jqueryClass}', '${actionBean.colorStyle}')">Mark for Rework</a>--%>
    <%--</div>--%>
</stripes:form>
