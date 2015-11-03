window.onerror = function (errorMsg, url, lineNumber, column, errorObj) {
    alert('Error: ' + errorMsg + ' Script: ' + url + ' Line: ' + lineNumber + ' Column: ' + column + ' StackTrace: ' +  errorObj);
};

function renderJson(json) {
    var width = 1200, height = 950;
    var menu = [
        {
            title: 'Add barcode',
            action: function (element, d, i) {
                console.log("adding " + element.__data__);
            }
        }
    ];

    // Set up pan and zoom.
    var zoomBehavior = d3.behavior.zoom().scaleExtent([0.1, 8]).on("zoom", zoom);
    var svg = d3.select("#graphDiv").insert("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .call(zoomBehavior)
        .append("g");
    svg.append("rect")
        .attr("class", "graphOverlay")
        .attr("width", width * 20)
        .attr("height", height * 20);
    function zoom() {
        svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    }

    // Arrows for edges.
    svg.append('svg:defs').append('svg:marker')
        .attr({
            id: 'end-arrow',
            viewBox: '0 -5 10 10',
            refX: 8,
            markerWidth: 6,
            markerHeight: 6,
            orient: 'auto'
        })
        .append('svg:path')
        .attr({
            d: 'M0,-5L10,0L0,5L2,0',
            'stroke-width': '0px',
            fill: '#000'
        });

    // Create a new directed graph
    var g = new dagre.graphlib.Graph();

    // Set an object for the graph label
    g.setGraph({ranksep: 100});

    // Default to assigning a new object as a label for each new edge.
    g.setDefaultEdgeLabel(function() { return {}; });
    // function read(json) {

    // Load json nodes and links into Dagre's model.  Dagre primarily uses width and height.
    for (var i = 0; i < json.nodes.length; i++) {
        g.setNode(json.nodes[i].id, json.nodes[i].values);
    }
    for (i = 0; i < json.links.length; i++) {
        g.setEdge(json.links[i].source, json.links[i].target);
    }

    // Assign x / y coordinates.
    dagre.layout(g);

    // Render racks and plates.
    var margin = 10, pad = 12;
    var node = svg.selectAll(".graphNode")
        .data(g.nodes())
        .enter().append("g")
        .attr("class", "graphNode")
        .attr("transform", function (d) {
            var graphNode = g.node(d);
            var x = graphNode.x - (graphNode.width / 2);
            var y = graphNode.y - (graphNode.height / 2);
            return "translate(" + x + "," + y + ")";
        });
    node.append("rect")
        .attr("class", "graphNodeRect")
        .attr("width", function (d) {
            return g.node(d).width;
        })
        .attr("height", function (d) {
            return g.node(d).height;
        })
        .on("contextmenu", d3.contextMenu(menu));
    node.append("text")
        .attr("class", "graphLabel")
        .attr("text-anchor", "middle")
        .text(function (d) {
            return g.node(d).label;
        })
        .attr("x", function (d) {
            return g.node(d).width / 2;
        })
        .attr("y", function (d) {
            return 14;
        })
        .on("contextmenu", d3.contextMenu(menu));

    // Set the D3 datum to the children.
    var nodeChildEnter = node.selectAll(".nodeChild")
        .data(function (d) {
            return g.node(d).children;
        })
        .enter();

    // Render tubes.
    var nodeChild = nodeChildEnter.append("rect")
        .attr("class", function (d) {
            return "nodeChild" + (d.hasOwnProperty('highlight') ? " nodeHighlight" : "");
        })
        .attr("x", function (d) {
            return d.x;
        })
        .attr("y", function (d) {
            return d.y;
        })
        .attr("width", function (d) {
            return d.w;
        })
        .attr("height", function (d) {
            return d.h;
        })
        .on("contextmenu", d3.contextMenu(menu));
    nodeChildEnter.append("text")
        .attr("class", "graphLabel")
        .attr("x", function (d) {
            return d.x + d.w / 2;
        })
        .attr("y", function (d) {
            return d.y + 14;
        })
        .attr("text-anchor", "middle")
        .text(function (d) {
            return d.name;
        })
        .on("contextmenu", d3.contextMenu(menu));

    // Make list of edges to draw, including between children, if applicable.
    var links = [];
    json.links.forEach(function(l) {
        var sourceNode = g.node(l.source);
        var sourceX = sourceNode.x;
        var sourceY = sourceNode.y;
        var i, len;
        if (l.sourceChild) {
            for (i = 0, len = sourceNode.children.length; i < len; i++) {
                if (sourceNode.children[i].name === l.sourceChild) {
                    sourceX += sourceNode.children[i].x - (sourceNode.width / 2);
                    sourceY += sourceNode.children[i].y - (sourceNode.height / 2);
                    break;
                }
            }
        } else {
            sourceY += sourceNode.height / 2;
        }
        // todo jmt reduce copy / paste
        var targetNode = g.node(l.target);
        var targetX = targetNode.x;
        var targetY = targetNode.y;
        if (l.targetChild) {
            for (i = 0, len = targetNode.children.length; i < len; i++) {
                if (targetNode.children[i].name === l.targetChild) {
                    targetX += targetNode.children[i].x - (targetNode.width / 2);
                    targetY += targetNode.children[i].y - (targetNode.height / 2);
                    break;
                }
            }
        } else {
            targetY -= targetNode.height / 2;
        }
        links.push({
            sourceX: sourceX,
            sourceY: sourceY,
            targetX: targetX,
            targetY: targetY,
            label: l.label,
            class: l.class
        });
    });

    // Render edges.
    var edges = svg.selectAll(".graphEdgeGroup")
        .data(links)
        .enter()
        .append("g")
        .attr("class", "graphEdgeGroup")
        .append("line")
        .attr("class", function (d) {
            return d.class ? d.class : "graphEdge";
        })
        .style("marker-end",  "url(#end-arrow)")
        .attr("x1", function (d) {
            return d.sourceX;
        })
        .attr("y1", function (d) {
            return d.sourceY;
        })
        .attr("x2", function (d) {
            return d.targetX;
        })
        .attr("y2", function (d) {
            return d.targetY;
        });

    var edgeText = svg.selectAll(".graphEdgeGroup")
        .append("text")
        .attr("class", "graphEdgeLabel")
        .attr("text-anchor", "middle")
        .attr("x", function(d) {
            if (d.targetX > d.sourceX) {
                return (d.sourceX + (d.targetX - d.sourceX)/2);
            } else {
                return (d.targetX + (d.sourceX - d.targetX)/2);
            }
        })
        .attr("y", function(d) {
            if (d.targetY > d.sourceY) {
                return (d.sourceY + (d.targetY - d.sourceY)/2);
            } else {
                return (d.targetY + (d.sourceY - d.targetY)/2);
            }
        })
        .attr("dx", 0)
        .attr("dy", 0)
        .text(function(d) {
            return d.label;
        });
    svg.selectAll(".graphEdgeLabel")
        .call(wrap,  150);

    // Pan to starting vessel.
    var scale = 1;
    var graphNode = g.node(json.startId);
    var panX = (graphNode.x * -scale) + (width / 2);
    var panY = (graphNode.y * -scale) + (height / 2);
    zoomBehavior.translate([panX, panY]).scale(1);
    zoomBehavior.event(svg.transition().duration(500));

    function wrap(text, width) {
        text.each(function() {
            var text = d3.select(this),
                words = text.text().split(/\s+/).reverse(),
                word,
                line = [],
                lineNumber = 0,
                lineHeight = 1.1, // ems
                x = text.attr("x"),
                y = text.attr("y"),
                dy = parseFloat(text.attr("dy")),
                tspan = text.text(null).append("tspan").attr("x", x).attr("y", y).attr("dy", dy + "em");
            while (word = words.pop()) {
                line.push(word);
                tspan.text(line.join(" "));
                if (tspan.node().getComputedTextLength() > width) {
                    line.pop();
                    tspan.text(line.join(" "));
                    line = [word];
                    tspan = text.append("tspan").attr("x", x).attr("y", y).attr("dy", ++lineNumber * lineHeight + dy + "em").text(word);
                }
            }
        });
    }
}
