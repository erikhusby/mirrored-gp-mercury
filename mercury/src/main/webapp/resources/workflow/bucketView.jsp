<%@ include file="/resources/layout/taglibs.jsp" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<stripes:useActionBean var="actionBean"
                       beanclass="org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean"/>

<stripes:layout-render name="/layout.jsp" pageTitle="Bucket View" sectionTitle="Select Bucket">
    <stripes:layout-component name="extraHead">
        <script type="text/javascript">
            $(document).ready(function () {
                        $j('#bucketEntryView').dataTable({
                            "oTableTools":ttExportDefines,
                            "aaSorting":[
                                [1, 'asc']
                            ],
                            "aoColumns":[
                                {"bSortable":false},
                                {"bSortable":true},
                                {"bSortable":true},
                                {"bSortable":true},
                                {"bSortable":true},
                                {"bSortable":true, "sType":"date"}
                            ]
                        });

                        $j('.bucket-checkbox').enableCheckboxRangeSelection({
                            checkAllClass:'bucket-checkAll',
                            countDisplayClass:'bucket-checkedCount',
                            checkboxClass:'bucket-checkbox'});
                    }
            )

        </script>
    </stripes:layout-component>
    <stripes:layout-component name="content">
        <stripes:form beanclass="${actionBean.class.name}" id="bucketForm">
            <div class="control-group">
                <div class="control">
                    <stripes:select name="selectedBucket">
                        <stripes:options-collection collection="${actionBean.buckets}" label="name" value="name"/>
                    </stripes:select>
                    <stripes:submit name="viewBucket" value="View Bucket"/>
                </div>
            </div>
        </stripes:form>
        <stripes:form beanclass="${actionBean.class.name}" id="bucketEntryForm">
            <table id="bucketEntryView" class="table simple">
                <thead>
                <tr>
                    <th width="40">
                        <input type="checkbox" class="bucket-checkAll"/><span id="count"
                                                                              class="bucket-checkedCount"></span>
                    </th>
                    <th>Vessel Name</th>
                    <th>PDO</th>
                    <th>Batch Name</th>
                    <th>Sample Type</th>
                    <th>Created Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${actionBean.bucketEntries}" var="entry">
                    <tr>
                        <td>
                            <stripes:checkbox class="bucket-checkbox" name="selectedVesselLabels"
                                              value="${entry.labVessel.label}"/>
                        </td>
                        <td>
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${entry.labVessel.label}">
                                    ${entry.labVessel.label}
                            </a>
                        </td>
                        <td>
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${entry.poBusinessKey}">
                                    ${entry.poBusinessKey}
                            </a>
                        </td>
                        <td>
                            <a href="${ctxpath}/search/all.action?search=&searchKey=${entry.labVessel.nearestLabBatchesString}">
                                    ${entry.labVessel.nearestLabBatchesString}
                            </a>
                        </td>
                        <td>
                            <c:forEach items="${entry.labVessel.mercurySamples}" var="mercurySample">
                                ${mercurySample.bspSampleDTO.materialType}
                            </c:forEach>
                        </td>
                        <td>
                            <fmt:formatDate value="${entry.createdDate}" pattern="MM/dd/yyyy HH:MM:ss"/>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>