// data
let heap_data = [];
let heap_links = [];
const callstack = [];

// constants
const server = "http://localhost:52933"
const width = window.innerWidth;
const height = window.innerHeight;
const stack_fraction = 0.38;
const heap_fraction = 1.0 - stack_fraction;
const center_pull = 0.010;
const link_length = 100;
const link_strength = 0.54;
const tick_speed = 50;

const heap_node_radius = 10;
const stack_node_radius = 4;
const local_var_height = 18;
const frame_base_size = 30;
const local_var_pre_pad = 10;
const frame_padding = 15;
const local_var_link_x = 0;

const [heap_svg, stack_svg] = build_svgs();
const heap_center = [
    (width * heap_fraction) / 2,
    height / 2,
];

const sim = d3.forceSimulation(heap_data)
    .force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(heap_center[0], heap_center[1]))
    .force("link", d3.forceLink().id(d => d.id)
        .distance(link_length).strength(link_strength))
    .force("x", d3.forceX(heap_center[0]).strength(center_pull))
    .force("y", d3.forceY(heap_center[1]).strength(center_pull))
    .on("tick", tick_sim)
    .alphaTarget(1);

let node = heap_svg.select("#nodes").selectAll(".node");
let link = heap_svg.select("#links").selectAll(".link");
let link_path = heap_svg.selectAll(".link_path");
let link_label = heap_svg.selectAll(".link_label");
let stack_frame = stack_svg.selectAll(".stack_frame");

// frame uuid -> frame
const stack_frames = {};
let next_unique_frame_id = 1000;

restart();
startTicking(server, tick_speed);

function tick_sim() {

    let callstack_current_height = 0;
    stack_frame
        .attr("transform", (d, i) => {
            let this_y = frame_padding + (i * (frame_base_size + frame_padding)) + callstack_current_height;
            let y_inverse = height - frame_padding - d.locals_height - this_y;

            stack_frames[d.uuid].y = y_inverse;
            callstack_current_height += d.locals_height;

            return "translate(0, " + y_inverse + ")";
        });

    function get_stack_link_pos(stack_data) {
        let y = stack_frames[stack_data.frame_uuid].y;
        y += (frame_base_size + local_var_pre_pad + (stack_data.index * local_var_height));
        y -= stack_node_radius;
        return y;
    }

    let real_source_x = d => d.stack ? local_var_link_x : d.source.x;
    let real_source_y = d => d.stack ? get_stack_link_pos(d.stack) : d.source.y;

    node
        .attr("transform", d => {
            if (d.stack) {
                return "translate(" + local_var_link_x + ", " + get_stack_link_pos(d.stack) + ")";
            }
            return "translate(" + d.x + ", " + d.y + ")";
        });
    link
        .attr("x1", real_source_x)
        .attr("y1", real_source_y)
        .attr("x2", d => d.target.x)
        .attr("y2", d => d.target.y);

    link_path
        .attr("d", d => "M " + real_source_x(d) + " " + real_source_y(d) + " L " + d.target.x + " " + d.target.y);

    link_label
        .attr("transform", function (d) { // for some reason this cannot be a lambda
            if (d.target.x < real_source_x(d)) {
                const bbox = this.getBBox();
                const rx = bbox.x + bbox.width / 2;
                const ry = bbox.y + bbox.height / 2;
                return "rotate(180 " + rx + " " + ry + ")"
            } else {
                return "rotate(0)"
            }
        });

}

function restart() {
    // sim.stop(); // necessary?
    sim.nodes(heap_data);
    sim.force("link").links(heap_links);

    node = node.data(heap_data);
    node.exit().remove();

    let node_enter = node.enter().append("g");
    node_enter.append("circle")
        .attr("class", d => d.stack ? "node_stack" : "node")
        .attr("r", d => d.stack ? stack_node_radius : heap_node_radius);
    node_enter.append("title") // hover
        .text(d => d.id + " - " + d.clazz);
    node = node.merge(node_enter);

    link = link.data(heap_links);
    link.exit().remove();
    let link_enter = link.enter().append("line")
        .attr("class", "link")
        .attr("marker-end", "url(#arrowhead)");

    link = link.merge(link_enter);

    link_path = link_path.data(heap_links, d => d.name);
    link_path.exit().remove();
    let link_path_enter = link_path.enter().append("path")
        .attr("class", "link_path")
        .attr("id", d => "link_path_" + d.source.id + "" + d.target.id)
        .style("pointer-events", "none")
        .style("text-anchor", "middle");
    link_path = link_path.merge(link_path_enter);

    link_label = link_label.data(heap_links, d => d.name);
    link_label.exit().remove();
    let label_text = link_label.enter().append("text")
        .style("pointer-events", "none")
        .attr("class", "link_label")
        .attr("dy", "-3");
    label_text.append("textPath")
        .attr("xlink:href", d => "#link_path_" + d.source.id + "" + d.target.id)
        .style("pointer-events", "none")
        .attr("startOffset", "20%")
        .text(d => d.name);
    link_label = link_label.merge(label_text);

    sim.nodes(heap_data);
    // sim.restart()
    // link_force.links(heap_links);
    // sim.alpha(1).restart();

    // stack
    stack_frame = stack_frame.data(callstack);
    stack_frame.exit().remove();

    let stack_frame_enter = stack_frame.enter().append("g")
        .attr("width", "100%");
    stack_frame_enter.append("rect")
        .attr("width", "100%")
        .attr("height", (d) => d.locals_height + frame_base_size)
        .attr("fill", () => "steelblue");
    stack_frame_enter.append("text")
        .attr("text-anchor", "middle")
        .attr("x", "50%")
        .attr("y", 10)
        .attr("fill", "white")
        .text((d) => d.clazz.name);
    stack_frame_enter.append("text")
        .attr("text-anchor", "middle")
        .attr("x", "50%")
        .attr("y", 25)
        .attr("fill", "white")
        .text((d) => d.method.name);
    stack_frame = stack_frame.merge(stack_frame_enter);

    stack_frame = stack_frame.each(function (d) {
        if (!d.spanking_new) return;
        d.spanking_new = false;

        let self = d3.select(this);
        d.method.localVars.forEach((local, j) => {
            self.append("text")
                .attr("text-anchor", "middle")
                .attr("x", "50%")
                .attr("y", () => frame_base_size + local_var_pre_pad + (local_var_height * j))
                .text(() => (local.index || 0) + ": " + local.name + " : " + local.type);
        })
    })
}

function build_svgs() {
    const stack = d3.select("#stack").append("svg")
        .attr("width", width * stack_fraction)
        .attr("height", height);

    const heap = d3.select("#heap").append("svg")
        .attr("width", width * heap_fraction)
        .attr("height", height);
    heap.append("g").attr("id", "links");
    heap.append("g").attr("id", "nodes");
    heap.append("marker")
        .attr("id", "arrowhead")
        .attr("class", "link_arrow")
        .attr("viewBox", "-0 -5 10 10")
        .attr("refX", 19)
        .attr("orient", "auto")
        .attr("markerWidth", 5)
        .attr("markerHeight", 5)
        .attr("xoverflow", "visible")
        .append("svg:path")
        .attr("d", "M 0,-5 L 10 ,0 L 0,5")
        .style("stroke", "none");

    return [heap, stack];
}
