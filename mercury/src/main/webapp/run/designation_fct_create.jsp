<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationFctActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Create FCT from Designated Loading Tubes" sectionTitle="Create FCT from Designated Loading Tubes">

    <stripes:layout-component name="extraHead">
        <script type="text/javascript">

            $j(document).ready(function () {
                <%@ include file="/run/designation_tube_list_setup_include.jsp" %>

                $j('input.checkAll:checkbox').change(function(){
                    rowSelected();
                });
            });

            <%@ include file="/run/designation_functions_include.jsp" %>

        </script>
        <style type="text/css">
            .width24 { width: 24em; }
            .width16 { width: 16em; word-wrap: break-word; }
            .width14 { width: 14em; word-wrap: break-word; }
            .width10 { width: 10em; word-wrap: break-word; }
            .width8 { width: 8em; word-wrap: break-word; }
            .width4 { width: 4em; }
            .width2 { width: 2em; }
        </style>

    </stripes:layout-component>

    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="scanForm" class="form-horizontal">

            <c:if test="${not empty actionBean.dtos}">
                <%@ include file="/run/designation_multi_edit_include.jsp" %>
            </c:if>

            <%@ include file="/run/designation_tube_list_include.jsp" %>


            <div style="float: left; width: 25%;">
                <c:if test="${not empty actionBean.dtos}">
                    <stripes:label for="diversifySamplesFlag"
                                   title="Each flowcell gets one lane from each sample; a second lane is taken only if needed to complete the flowcell.">
                        Maximize sample diversity
                        <stripes:checkbox id="diversifySamplesFlag" name="diversifySamplesFlag"/>
                    </stripes:label>
                    <div style="position: relative; top: 20px;">
                        <stripes:submit id="createFctBtn" name="createFct" value="Create FCT"
                                        class="btn btn-primary" onclick="updateHiddenInputs()" disabled="disabled"
                                        title="Creates FCTs from the selected rows."/>
                    </div>
                </c:if>
            </div>
            <div style="float: right; width: 75%;">
                <c:if test="${actionBean.createdFcts.size() > 0}">
                    <div class="control-group">
                        <h5>Created FCT Tickets</h5>
                        <ol>
                            <c:forEach items="${actionBean.createdFcts}" var="fctUrl" varStatus="item">
                                <li>
                                    <a target="JIRA" href=${fctUrl.right} class="external" target="JIRA">${fctUrl.left}</a>
                                </li>

                                <input type="hidden" name="createdFcts[${item.index}].left" value="${fctUrl.left}"/>
                                <input type="hidden" name="createdFcts[${item.index}].right" value="${fctUrl.right}"/>
                            </c:forEach>
                        </ol>
                    </div>
                </c:if>
            </div>

        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>
