// data
let heapObjects = [];
let heapLinks = [];
const callstack = [];
const definitions = {};

// mutable constants
let WINDOW_WIDTH;
let WINDOW_HEIGHT;
let HEAP_WIDTH;
let HEAP_CENTRE;

// constants
const STACK_FRACTION = 0.38;
const HEAP_FRACTION = 1.0 - STACK_FRACTION;
const SERVER = "http://localhost:52933";
const CENTRE_PULL = 0.030;
const LINK_LENGTH = 70;
const LINK_STRENGTH = 0.54;
const TICK_SPEED = 500;

const HEAP_NODE_RADIUS = 6;
const STACK_NODE_RADIUS = 4;
const LOCAL_VAR_SLOT_HEIGHT = 18;
const FRAME_BASE_SIZE = 15;
const LOCAL_VAR_SLOT_PRE_PAD = 10;
const FRAME_PADDING = 15;
const LOCAL_VAR_LINK_X = 0;

const [heapSvg, stackSvg] = buildSvgs();
const sim = d3.forceSimulation(heapObjects)
    .force("charge", d3.forceManyBody().strength(-100))
    .force("link", d3.forceLink().id(d => d.id)
        .distance(LINK_LENGTH).strength(LINK_STRENGTH))
    .force("x", d3.forceX(HEAP_CENTRE[0]).strength(CENTRE_PULL))
    .force("y", d3.forceY(HEAP_CENTRE[1]).strength(CENTRE_PULL))
    .on("tick", tickSim)
    .alphaTarget(0.5);

let node = heapSvg.select("#nodes").selectAll(".node");
let link = heapSvg.select("#links").selectAll(".link");
let linkPath = heapSvg.selectAll(".linkPath");
let linkLabel = heapSvg.selectAll(".linkLabel");
let stackFrame = stackSvg.selectAll(".stackFrame");

// frame uuid -> frame
const stackFrames = {};
let nextUniqueFrameId = 1000;

resize();
d3.select(window).on("resize", resize);

restart();
startTicking(SERVER, TICK_SPEED);

function tickSim() {

    let callstackCurrentHeight = 0;
    stackFrame
        .attr("transform", (d, i) => {
            let thisY = FRAME_PADDING + (i * (FRAME_BASE_SIZE + FRAME_PADDING)) + callstackCurrentHeight;
            let yInverse = WINDOW_HEIGHT - FRAME_PADDING - d.localsHeight - thisY;

            stackFrames[d.uuid].y = yInverse;
            callstackCurrentHeight += d.localsHeight;

            return "translate(0, " + yInverse + ")";
        });

    function getStackLinkPos(stackData) {
        let y = stackFrames[stackData.frameUuid].y;
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

function discoverWindowSize() {
    WINDOW_WIDTH = window.innerWidth;
    WINDOW_HEIGHT = window.innerHeight;
    HEAP_WIDTH = WINDOW_WIDTH * HEAP_FRACTION;
    HEAP_CENTRE = [
        HEAP_WIDTH / 2,
        WINDOW_HEIGHT / 2,
    ];
}

function resize() {
    discoverWindowSize();
    stackSvg
        .attr("width", WINDOW_WIDTH * STACK_FRACTION)
        .attr("height", WINDOW_HEIGHT);

    heapSvg
        .attr("width", HEAP_WIDTH)
        .attr("height", WINDOW_HEIGHT);

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

function restart(changedGraph) {
    // sim.stop(); // necessary?
    sim.nodes(heapObjects);
    sim.force("link").links(heapLinks);

    // nodes
    node = node.data(heapObjects, d => d.id);
    node.exit().remove();

    let nodeEnter = node.enter().append("g");
    let nodeEnterObjs = nodeEnter.append("circle");
    nodeEnterObjs
        .attr("class", d => d.stack ? "nodeStack" : "node")
        .attr("r", d => d.stack ? STACK_NODE_RADIUS : HEAP_NODE_RADIUS)
        .attr("fill", d => d.fill ? d.fill : "none");
    nodeEnterObjs.append("title") // hover
        .text(d => d.id + " - " + d.clazz);

    // array nodes
    node.each(function (d) {
        if (!d.array) return; // TODO filter?!
        if (d.array.done) return;
        d.array.done = true;
        let self = d3.select(this);
        for (let i = 0; i < d.array.dims; i++) {
            self.append("circle")
                .attr("class", "nodeArray")
                .attr("r", HEAP_NODE_RADIUS + 2 * (i+1));
        }
    });
    node = node.merge(nodeEnter);


    // links
    link = link.data(heapLinks, d => d.name);
    link.exit().remove();
    let linkEnter = link.enter().append("line")
        .attr("class", d => d.stack ? "stackLink" : "link")
        .attr("marker-end", "url(#arrowhead)");
    link = link.merge(linkEnter);

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

    // stack
    stackFrame = stackFrame.data(callstack);
    stackFrame.exit().remove();

    let stackFrameEnter = stackFrame.enter().append("g")
        .attr("width", "100%");
    stackFrameEnter.append("rect")
        .attr("width", "100%")
        .attr("height", (d) => d.localsHeight + FRAME_BASE_SIZE)
        .attr("fill", () => "steelblue");
    stackFrameEnter.append("text")
        .attr("text-anchor", "middle")
        .attr("x", "50%")
        .attr("y", 10)
        .attr("fill", "white")
        .text(d => d.methodDefinition.clazzShort + ":" + d.methodDefinition.name)
        .append("title")
        .text(d => d.methodDefinition.clazzLong + ":" + d.methodDefinition.name + d.methodDefinition.signature);
    stackFrame = stackFrame.merge(stackFrameEnter);

    stackFrame = stackFrame.each(function (d) {
        if (!d.spankingNew) return;
        d.spankingNew = false;

        let self = d3.select(this);
        d.methodDefinition.localVars.forEach((local, j) => {
            const shortType = shortenClassName(local.type);
            self.append("text")
                .attr("text-anchor", "middle")
                .attr("x", "50%")
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
    discoverWindowSize();

    const stack = d3.select("#stack").append("svg")
        .attr("width", WINDOW_WIDTH * STACK_FRACTION)
        .attr("height", WINDOW_HEIGHT);

    const heap = d3.select("#heap").append("svg")
        .attr("width", WINDOW_WIDTH * HEAP_FRACTION)
        .attr("height", WINDOW_HEIGHT);
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
