window.onerror = function (errorMsg, url, lineNumber, column, errorObj) {
    alert('Error: ' + errorMsg + ' Script: ' + url + ' Line: ' + lineNumber + ' Column: ' + column + ' StackTrace: ' +  errorObj);
};

function setupSvg() {
    var width = 1200, height = 950;

    // Set up pan and zoom.
    var svg = d3.select("#graphDiv").insert("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .call(d3.behavior.zoom().scaleExtent([0.1, 8]).on("zoom", zoom))
        .append("g");
    svg.append("rect")
        .attr("class", "graphOverlay")
        .attr("width", width * 10)
        .attr("height", height * 10);
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
    return svg;
}

function renderJson(json, svg) {

    // Create a new directed graph
    var g = new dagre.graphlib.Graph();

    // Set an object for the graph label
    g.setGraph({});

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
        });
    node.append("text")
        .attr("class", "graphLabel")
        .attr("dx", 25)
        .attr("dy", 10)
        .text(function (d) {
            return g.node(d).label;
        });
//                .each(function (d) {
//                    var b = this.parentNode.getBBox();
//                    var extra = 2 * margin + 2 * pad;
//                    d.width = b.width + extra;
//                    d.height = b.height + extra;
//                });

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
        });
    nodeChildEnter.append("text")
        .attr("class", "graphLabel")
        .attr("x", function (d) {
            return d.x;
        })
        .attr("y", function (d) {
            return d.y;
        })
        .attr("dx", 25)
        .attr("dy", 10)
        .text(function (d) {
            return d.name;
        });
//                .each(function (d) {
//                    var b = this.parentNode.getBBox();
//                    var extra = 2 * margin + 2 * pad;
//                    d.width = b.width + extra;
//                    d.height = b.height + extra;
//                });

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
            label: l.label
        });
    });
    // Convert Dagre edge control points into pairs of line points.
//        g.edges().forEach(function(e) {
//            var points = g.edge(e).points;
//            for (var i = 0, len = points.length - 1; i < len; i++) {
//                // (note: loop until length - 1 since we're getting the next
//                //  item with i+1)
//                links.push({
//                    sourceX: points[i].x,
//                    sourceY: points[i].y,
//                    targetX: points[i + 1].x,
//                    targetY: points[i + 1].y
//                });
//            }
//        });

    // Render edges.
    var edges = svg.selectAll(".graphEdgeGroup")
        .data(links)
        .enter()
        .append("g")
        .attr("class", "graphEdgeGroup")
        .append("line")
        .attr("class", "graphEdge")
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
        .text(function(d) {
            return d.label;
        });
}