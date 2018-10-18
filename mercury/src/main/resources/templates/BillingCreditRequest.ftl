<html xmlns="http://www.w3.org/1999/html">
<head>
    <style type="text/css">
        th,td {text-align: left;padding-right: 1em;} td {border-top: 1px solid black;vertical-align: top;} th {white-space: nowrap;vertical-align: bottom;}
    </style>
</head>
<body>
<p>
    This is a request to reverse a charge against a previously billed item.
</p>
<table>
    <thead>
    <tr>
        <th>Mercury Order</th>
        <th>Material</th>
        <th>SAP Sales Order</th>
        <th>Delivery Documents<br/>Related to this Item</th>
        <th>Was there Delivery Discounts?</th>
        <th>Quantity</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>${mercuryOrder}</td>
        <td>${material}</td>
        <td>${sapOrderNumber}</td>
        <td>${sapDeliveryDocuments}</td>
        <td>${deliveryDiscount}</td>
        <td>${quantity}</td>
    </tr>
    </tbody>
</table>

</body>
</html>

