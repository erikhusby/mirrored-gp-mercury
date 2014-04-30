(function() {
    // Catch clicks on the table, check that it's an "Add" button, and take the button's "name" as the ID to use.
    var table = $j('#addRegulatoryInfoDialogQueryResults tbody');
    table.click(function (event) {
        var target = event.target;
        if (target.nodeName == "INPUT" &&
            target.type == "submit" &&
            target.value == "Add") {
            $j('#regulatoryInfoId').val(target.name);
        }
    });
})();
