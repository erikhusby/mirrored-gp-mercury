                $j('#tubeList').dataTable({
                    "oTableTools":ttExportDefines,
                    "aaSorting":[[2, 'asc'], [3, 'asc'], [4, 'asc']],
                    "aoColumns":[
                        {"bSortable":false}, // selected
                        {"bSortable":true},  // status
                        {"bSortable":true},  // priority
                        {"bSortable":true},  // barcode
                        {"bSortable":true},  // lcset
                        {"bSortable":true},  // tube type
                        {"bSortable":true},  // sequencer model
                        {"bSortable":true},  // number samples
                        {"bSortable":true},  // number lanes
                        {"bSortable":true},  // loading conc
                        {"bSortable":true},  // tube create date
                        {"bSortable":true},  // read length
                        {"bSortable":true},  // index type
                        {"bSortable":true},  // number cycles
                        {"bSortable":true},  // pool test
                        {"bSortable":true},  // regulatory designation
                        {"bSortable":true},  // product
                    ]
                });
