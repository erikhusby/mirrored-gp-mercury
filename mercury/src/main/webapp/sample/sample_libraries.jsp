<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions' %>

<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.SampleLibrariesActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Sample Libraries" sectionTitle="Sample Libraries">
    <stripes:layout-component name="extraHead">
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <script type="text/javascript">

            $j(document).ready(function () {
                $j('#sampleLibrariesView').dataTable({
                    oTableTools:ttExportDefines,
                    aaSorting:[
                        [0, 'asc']
                    ],
                    aoColumns:[
                        { bSortable:true, sWidth:'100px', sType:'html'}
                    ],
                    bRetrieve:true,
                    sScrollY:700
                });
            });
        </script>
        <stripes:form
                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.sample.SampleLibrariesActionBean">
            <table id="sampleLibrariesView" class="table simple">
                <thead>
                <tr>
                    <th>Sample</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.selectedSamples}" var="sample">
                    <tr>
                        <td><stripes:link
                                beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.SampleSearchActionBean"
                                event="sampleSearch">
                            <stripes:param name="searchKey" value="${sample}"/>
                            ${sample}
                        </stripes:link></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>