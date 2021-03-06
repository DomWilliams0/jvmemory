// data
let heapObjects = []; // [Node]
let heapLinks = []; // [Link]
const callStack = new CallStack();

let WINDOW_HEIGHT;
let HEAP_CENTRE;

// constants
const CENTRE_PULL = 0.030;
const NODE_REPULSION = 40;
const LINK_LENGTH = 60;

const STATIC_NODE_RADIUS = 6;
const HEAP_NODE_RADIUS = 4;
const STACK_NODE_RADIUS = 2;
const LOCAL_VAR_SLOT_HEIGHT = 18;
const FRAME_BASE_SIZE = 20;
const LOCAL_VAR_SLOT_PRE_PAD = 10;
const FRAME_PADDING = 10;
const LOCAL_VAR_LINK_X = 0;

const [heapSvg, stackSvg] = buildSvgs();
const sim = d3.forceSimulation(heapObjects)
    .force("charge", d3.forceManyBody().strength(-NODE_REPULSION))
    .force("link", d3.forceLink().id(d => d.id)
        .distance(LINK_LENGTH)
        .strength(linkStrength))
    .on("tick", tickSim)
    .alphaTarget(0.5);

let node = heapSvg.select("#nodes").selectAll(".node");
let link = heapSvg.select("#links").selectAll(".link");
let linkPath = heapSvg.selectAll(".linkPath");
let linkLabel = heapSvg.selectAll(".linkLabel");
let stackFrame = stackSvg.selectAll(".stackFrame");

let tooltips = d3.select("body").append("div")
    .attr("class", "tooltip")
    .style("opacity", 0);

resize();
d3.select(window).on("resize", resize);

fetchThreads().then(threads => {
    if (!threads) {
        alert("No events!");
        return;
    }
    // add options
    const select = document.getElementById("thread-select");
    threads.forEach(t => {
        const o = document.createElement("option");
        o.appendChild(document.createTextNode(t));
        select.options.add(o);
    });

    const button = document.getElementById("go-button");
    button.onclick = e => {
        let tid = parseInt(select.value);
        if (isNaN(tid)) {
            alert("Choose a thread!");
            return;
        }

        select.parentElement.remove();
        start(tid);
    };

    if (threads.length === 1) {
        select.value = threads[0];
        console.log("selecting only thread automatically");
        button.click();
    }
});

function tickSim() {

    let callstackCurrentHeight = 50;
    stackFrame
        .attr("transform", (d, i) => {
            const localsHeight = d.localVars.length * LOCAL_VAR_SLOT_HEIGHT;
            let thisY = FRAME_PADDING + (i * (FRAME_BASE_SIZE + FRAME_PADDING)) + callstackCurrentHeight;
            let yInverse = WINDOW_HEIGHT - FRAME_PADDING - localsHeight - thisY;

            callStack.getFrame(d.uuid).y = yInverse;
            callstackCurrentHeight += localsHeight;

            return "translate(0, " + yInverse + ")";
        });

    function getStackLinkPos(stackData) {
        let y = callStack.getFrame(stackData.frameUuid).y;
        y += (FRAME_BASE_SIZE + LOCAL_VAR_SLOT_PRE_PAD + (stackData.index * LOCAL_VAR_SLOT_HEIGHT));
        y -= STACK_NODE_RADIUS;
        return y;
    }

    let realSourceX = d => d.stack ? LOCAL_VAR_LINK_X : d.source.x;
    let realSourceY = d => d.stack ? getStackLinkPos(d.stack) : d.source.y;

    node
        .attr("transform", d => {
            if (d.stack) {
                return "translate(" + LOCAL_VAR_LINK_X + ", " + getStackLinkPos(d.stack) + ")";
            }
            return "translate(" + d.x + ", " + d.y + ")";
        });
    link
        .attr("x1", realSourceX)
        .attr("y1", realSourceY)
        .attr("x2", d => d.target.x)
        .attr("y2", d => d.target.y);

    linkPath
        .attr("d", d => "M " + realSourceX(d) + " " + realSourceY(d) + " L " + d.target.x + " " + d.target.y);

    linkLabel
        .attr("transform", function (d) { // for some reason this cannot be a lambda
            if (d.target.x < realSourceX(d)) {
                const bbox = this.getBBox();
                const rx = bbox.x + bbox.width / 2;
                const ry = bbox.y + bbox.height / 2;
                return "rotate(180 " + rx + " " + ry + ")"
            } else {
                return "rotate(0)"
            }
        });

}

function resize() {
    const stack = document.getElementById("stack");
    const heap = document.getElementById("heap");

    WINDOW_HEIGHT = heap.clientHeight;
    HEAP_CENTRE = [
        heap.clientWidth / 2,
        heap.clientHeight / 2,
    ];
    sim.force("x", d3.forceX(HEAP_CENTRE[0]).strength(CENTRE_PULL))
        .force("y", d3.forceY(HEAP_CENTRE[1]).strength(CENTRE_PULL))
}

function shortenClassName(className) {
    const index = className.lastIndexOf('.');
    if (index >= 0) {
        return className.substring(index + 1);
    }
    return className;
}

function linkStrength(d) {
    if (d.stack)
        return 0.25;

    if (d.static)
        return 0.1;

    return 0.4;
}

function nodeRadius(d) {
    if (d.stack)
        return STACK_NODE_RADIUS;

    if (d.static) {
        return STATIC_NODE_RADIUS;
    }

    return HEAP_NODE_RADIUS;
}

function nodeClass(d) {
    if (d.stack)
        return "stackNode";

    if (d.static)
        return "staticNode";

    return "node";
}

function linkClass(d) {
    if (d.stack)
        return "stackLink";

    if (d.target.static)
        return "staticLink";

    return "link";
}

// thanks https://stackoverflow.com/a/12034334
function escapeToString(str) {
    const escapeHtmlMap = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
        '/': '&#x2F;',
        '`': '&#x60;',
        '=': '&#x3D;'
    };

    if (!str)
        return "";
    return str.replace(/[&<>"'`=\/]/g, s => escapeHtmlMap[s]);
}

function connect(node) {
    node.connected = true;
    link.filter(d => d.source.id === node.id).each(d => {
        d.connected = true;
        connect(d.target);
    });
}

function restart(changedGraph) {
    // sim.stop(); // necessary?
    sim.nodes(heapObjects);
    sim.force("link").links(heapLinks);

    // nodes
    node = node.data(heapObjects, d => d.id);
    node.exit().remove();

    node.select("circle")
        .attr("r", nodeRadius)
        .attr("class", nodeClass);

    let nodeEnter = node.enter().append("g");
    let nodeEnterObjs = nodeEnter.append("circle");
    nodeEnterObjs
        .attr("class", nodeClass)
        .attr("r", nodeRadius)
        .attr("fill", d => d.fill ? d.fill : "none")
        .on("mouseover", d => {
            if (d.stack) return;
            tooltips.transition()
                .duration(200)
                .style("opacity", 1.0);

            const toString = !d.str ? "" : escapeToString(d.str);
            tooltips
                .html(`<b>${d.clazz}</b><br/>${toString}`)
                .style("left", d3.event.pageX + "px")
                .style("top", d3.event.pageY + "px");
        })
        .on("mouseout", d => {
            tooltips.transition()
                .duration(500)
                .style("opacity", 0);
        });

    node = node.merge(nodeEnter);

    node.each(function (d) {
        d.connected = false;

        // array nodes
        if (!d.array) return; // TODO filter?!
        if (d.array.done) return;
        d.array.done = true;
        let self = d3.select(this);
        for (let i = 0; i < d.array.dims; i++) {
            self.append("circle")
                .attr("class", "nodeArray")
                .attr("r", HEAP_NODE_RADIUS + 2 * (i + 1));
        }
    });


    // links
    link = link.data(heapLinks, d => d.name);
    link.exit().remove();
    let linkEnter = link.enter().append("line")
        .attr("class", linkClass)
        .attr("marker-end", "url(#arrowhead)");
    link = link.merge(linkEnter);

    // find connected graph
    link.each(d => d.connected = false);
    node.filter(d => d.stack).each(connect);
    node.classed("unreferenced", d => !d.connected);
    link.classed("unreferenced", d => !d.connected);

    // link paths
    linkPath = linkPath.data(heapLinks, d => d.name);
    linkPath.exit().remove();
    let linkPathEnter = linkPath.enter().append("path")
        .attr("class", "linkPath")
        .attr("id", d => "link_path_" + d.source.id + "" + d.target.id)
        .style("pointer-events", "none")
        .style("text-anchor", "middle");
    linkPath = linkPath.merge(linkPathEnter);

    // link labels
    linkLabel = linkLabel.data(heapLinks, d => d.name);
    linkLabel.exit().remove();
    let labelText = linkLabel.enter().append("text")
        .style("pointer-events", "none")
        .attr("class", "linkLabel")
        .attr("dy", "-3");
    labelText.append("textPath")
        .attr("xlink:href", d => "#link_path_" + d.source.id + "" + d.target.id)
        .style("pointer-events", "none")
        .attr("startOffset", "20%")
        .text(d => d.name);
    linkLabel = linkLabel.merge(labelText);
    linkLabel.classed("unreferenced", d => !d.connected);

    // stack
    stackFrame = stackFrame.data(callStack.callstack);
    stackFrame.exit().remove();

    let stackFrameEnter = stackFrame.enter().append("g")
        .attr("width", "100%");
    stackFrameEnter.append("rect")
        .attr("width", "100%")
        .attr("height", d => (d.localVars.length * LOCAL_VAR_SLOT_HEIGHT) + FRAME_BASE_SIZE)
        .attr("class", "stackFrame");
    stackFrameEnter.append("text")
        .attr("text-anchor", "middle")
        .attr("x", "50%")
        .attr("y", 14)
        .attr("fill", "white")
        .attr("class", "stackFrameClass")
        .text(d => d.clazzShort + ":" + d.name)
        .append("title")
        .text(d => d.clazzLong + ":" + d.name + d.signature);
    stackFrame = stackFrame.merge(stackFrameEnter);

    stackFrame = stackFrame.each(function (d) {
        if (d.rendered) return;
        d.rendered = true;

        let self = d3.select(this);
        d.localVars.forEach((local, j) => {
            const shortType = shortenClassName(local.type);
            self.append("text")
                .attr("text-anchor", "middle")
                .attr("x", "50%")
                .attr("class", "stackFrameLocalVar")
                .attr("y", () => FRAME_BASE_SIZE + LOCAL_VAR_SLOT_PRE_PAD + (LOCAL_VAR_SLOT_HEIGHT * j))
                .text(() => (`${shortType} ${local.name}`))
                .append("title")
                .text(() => (`${local.type} ${local.name}`));
        })
    });

    sim.nodes(heapObjects);
    sim.force("link").links(heapLinks);

    if (changedGraph)
        sim.alpha(0.5).restart();
}

function buildSvgs() {
    const stack = d3.select("#stack").append("svg")
        .attr("width", "100%")
        .attr("height", "100%");

    const heap = d3.select("#heap").append("svg")
        .attr("width", "100%")
        .attr("height", "100%");
    heap.append("g").attr("id", "links");
    heap.append("g").attr("id", "nodes");
    heap.append("marker")
        .attr("id", "arrowhead")
        .attr("class", "linkArrow")
        .attr("viewBox", "-0 -5 10 10")
        .attr("refX", 17)
        .attr("orient", "auto")
        .attr("markerWidth", 7)
        .attr("markerHeight", 7)
        .attr("xoverflow", "visible")
        .append("svg:path")
        .attr("d", "M 0,-5 L 10 ,0 L 0,5")
        .style("stroke", "none");

    return [heap, stack];
}
