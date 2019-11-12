<#-- @ftlvariable name="returnList" type="java.util.List" -->
<#-- @ftlvariable name="returnListSummary" type="java.util.List" -->
<html xmlns="http://www.w3.org/1999/html">
<head>
    <style type="text/css">
        th,td {text-align: left;padding-right: 1em;} td {border-top: 1px solid black;vertical-align: top;} th {white-space: nowrap;vertical-align: bottom;}
    </style>
</head>
<body>
<p>
    This is a request to reverse a charge against previously billed item(s).
</p>
<table>
    <thead>
    <tr>
        <th>Mercury Order</th>
        <th>Material</th>
        <th>SAP Sales Order</th>
        <th>Delivery Documents<br/>Related to this Item</th>
        <th>Quantity</th>
    </tr>
    </thead>
    <tbody>
    <#list returnList as returnItem>
    <tr>
        <td>${returnItem.mercuryOrder}</td>
        <td>${returnItem.material}</td>
        <td>${returnItem.sapOrderNumber}</td>
        <td>${returnItem.sapDeliveryDocuments}</td>
        <td>${returnItem.quantity}</td>
    </tr>
    </#list>
    </tbody>
</table>

</body>
</html>

