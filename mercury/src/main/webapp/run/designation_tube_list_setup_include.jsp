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
                        {"bSortable":false}, // number lanes
                        {"bSortable":false}, // loading conc
                        {"bSortable":true},  // tube create date
                        {"bSortable":false}, // read length
                        {"bSortable":true},  // index type
                        {"bSortable":true},  // number cycles
                        {"bSortable":true},  // paired end
                        {"bSortable":true},  // pool test
                        {"bSortable":true},  // regulatory designation
                        {"bSortable":true},  // product
                    ]
                });
