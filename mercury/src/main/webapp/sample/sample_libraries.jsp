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
                        { bSortable:true, sWidth:'100px', sType:'html'},
                        { bSortable:false },
                        { bSortable:true },
                        { bSortable:true },
                        { bSortable:true },
                        { bSortable:true },
                        { bSortable:true },
                        { bSortable:true },
                        { bSortable:true }
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
                    <th>Index</th>
                    <th>Exported Tube</th>
                    <th>Pond Tube</th>
                    <th>Catch Tube</th>
                    <th>Pooled Tube</th>
                    <th>Normalized Tube</th>
                    <th>Denature Tube</th>
                    <th>Flowcell</th>
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
                        <td style="padding: 0;">
                            <table style="padding: 0;">
                                <c:forEach items="${actionBean.getIndexesForSample(sample)}" var="curIndex">
                                    <c:forEach items="${curIndex.molecularIndexingScheme.indexes}"
                                               var="innerIndex">
                                        <tr>
                                            <td style="border: none">
                                                    ${innerIndex.key} - ${innerIndex.value.sequence}

                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:forEach>
                            </table>
                        </td>
                        <td>
                            <c:forEach items="${actionBean.getVesselStringBySampleAndType(sample,'SAMPLE_IMPORT')}"
                                       var="vessel">
                                <stripes:link
                                        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                        event="vesselSearch">
                                    <stripes:param name="searchKey"
                                                   value="${vessel.label}"/>
                                    ${vessel.label}
                                </stripes:link>
                            </c:forEach>
                        </td>
                        <td>
                            <c:forEach items="${actionBean.getVesselStringBySampleAndType(sample,'POND_REGISTRATION')}"
                                       var="vessel">
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                    event="vesselSearch">
                                <stripes:param name="searchKey"
                                               value="${vessel.label}"/>
                                ${vessel.label}
                            </stripes:link>
                            </c:forEach>
                        <td>
                            <c:forEach
                                    items="${actionBean.getVesselStringBySampleAndType(sample,'NORMALIZED_CATCH_REGISTRATION')}"
                                    var="vessel">
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                    event="vesselSearch">
                                <stripes:param name="searchKey"
                                               value="${vessel.label}"/>
                                ${vessel.label}
                            </stripes:link>
                            </c:forEach>
                        <td>
                            <c:forEach items="${actionBean.getVesselStringBySampleAndType(sample,'POOLING_TRANSFER')}"
                                       var="vessel">
                            <stripes:link
                                    beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                    event="vesselSearch">
                                <stripes:param name="searchKey"
                                               value="${vessel.label}"/>
                                ${vessel.label}
                            </stripes:link>
                            </c:forEach>
                        <td>
                            <c:forEach
                                    items="${actionBean.getVesselStringBySampleAndType(sample,'NORMALIZATION_TRANSFER')}"
                                    var="vessel">
                                <stripes:link
                                        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                        event="vesselSearch">
                                    <stripes:param name="searchKey"
                                                   value="${vessel.label}"/>
                                    ${vessel.label}
                                </stripes:link>
                            </c:forEach>
                        </td>
                        <td>
                            <c:forEach items="${actionBean.getVesselStringBySampleAndType(sample,'DENATURE_TRANSFER')}"
                                       var="vessel">
                                <stripes:link
                                        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                        event="vesselSearch">
                                    <stripes:param name="searchKey"
                                                   value="${vessel.label}"/>
                                    ${vessel.label}
                                </stripes:link>
                            </c:forEach>
                        </td>
                        <td>
                            <c:forEach
                                    items="${actionBean.getVesselStringBySampleAndType(sample,'DENATURE_TO_FLOWCELL_TRANSFER')}"
                                    var="vessel">
                                <stripes:link
                                        beanclass="org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean"
                                        event="vesselSearch">
                                    <stripes:param name="searchKey"
                                                   value="${vessel.label}"/>
                                    ${vessel.label}
                                </stripes:link>
                            </c:forEach>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>