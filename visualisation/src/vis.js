// data
// TODO add class to heap nodes
let heap_data = [
    {id: 1000},
    {id: 2000},
    {id: 3033},
];
let heap_links = [
    {source: 1000, target: 2000, name: "a"},
    {source: 1000, target: 3033, name: "b"},
];

// TODO use real data
const events = [
    {
        type: "enter_method", payload: {
            clazz: "com.test.Hiya", method: {
                name: "<init>",
                locals: []
            }
        }
    },
    {
        type: "enter_method", payload: {
            clazz: "com.test.Hiya", method: {
                name: "go",
                locals: [
                    {name: "anInt", clazz: "java.lang.Integer", index: 0},
                    {name: "aString", clazz: "java.lang.String", index: 1},
                    {name: "anObj", clazz: "java.lang.Object", index: 2},
                ]
            }
        }
    },
    {type: "add_obj", payload: {id: 4000, type: "com.test.Muffin"}},
    {type: "set_link", payload: {src: 1000, dst: 4000, name: "c"}},
    {type: "exit_method", payload: {}},
    {
        type: "enter_method", payload: {
            clazz: "com.test.Hiya", method: {
                name: "go",
                locals: [
                    {name: "anInt", clazz: "java.lang.Integer", index: 0},
                    {name: "aString", clazz: "java.lang.String", index: 1},
                    {name: "anObj", clazz: "java.lang.Object", index: 2},
                ]
            }
        }
    },
    {type: "set_stack_link", payload: {index: 1, dst: 2000}},
    {type: "del_obj", payload: {id: 3033}},
    {type: "exit_method", payload: {}},
];

const callstack = [];

// constants
const width = window.innerWidth;
const height = window.innerHeight;
const stack_fraction = 0.18;
const heap_fraction = 1.0 - stack_fraction;
const center_pull = 0.010;
const link_length = 200;
const link_strength = 0.04;
const tick_speed = 1000;

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
    .force("x", d3.forceX(heap_center[0]).strength(center_pull))
    .force("y", d3.forceY(heap_center[1]).strength(center_pull))
    .alphaTarget(1);

const link_force = d3.forceLink(heap_links)
    .id(d => d.id)
    .distance(() => link_length)
    .strength(() => link_strength);

sim.force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(heap_center[0], heap_center[1]))
    .force("links", link_force)
    .on("tick", tick_sim);


let node = heap_svg.select("#nodes").selectAll(".node");
let link = heap_svg.select("#links").selectAll(".link");
let link_path = heap_svg.selectAll(".link_path");
let link_label = heap_svg.selectAll(".link_label");
let stack_frame = stack_svg.selectAll(".stack_frame");

// frame uuid -> frame
const stack_frames = {};
let next_unique_frame_id = 1000;

restart();

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

    node = node.data(heap_data, d => d.id);
    node.exit().remove();

    let node_enter = node.enter().append("g");
    node_enter.append("circle")
        .attr("class", d => d.stack ? "node_stack" : "node")
        .attr("r", d => d.stack ? stack_node_radius : heap_node_radius);
    node_enter.append("title") // hover
        .text(d => "hovering: " + d.id);
    node = node.merge(node_enter);

    link = link.data(heap_links, d => d.id + "." + d.name);
    link.exit().remove();
    let link_enter = link.enter().append("line")
        .attr("class", "link")
        .attr("marker-end", "url(#arrowhead)");

    link = link.merge(link_enter);

    link_path = link_path.data(heap_links);
    link_path.exit().remove();
    let link_path_enter = link_path.enter().append("path")
        .attr("class", "link_path")
        .attr("id", (d, i) => "link_path_" + i)
        .style("pointer-events", "none")
        .style("text-anchor", "middle");
    link_path = link_path.merge(link_path_enter);

    link_label = link_label.data(heap_links);
    link_label.exit().remove();
    let label_text = link_label.enter().append("text")
        .style("pointer-events", "none")
        .attr("class", "link_label")
        .attr("dy", "-3")
        .attr("id", (d, i) => "link_label_" + i);
    label_text.append("textPath")
        .attr("xlink:href", (d, i) => "#link_path_" + i)
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
        .text((d) => d.clazz);
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
        d.method.locals.forEach((local, j) => {
            self.append("text")
                .attr("text-anchor", "middle")
                .attr("x", "50%")
                .attr("y", () => frame_base_size + local_var_pre_pad + (local_var_height * j))
                .text(() => local.index + ": " + local.name + " : " + local.clazz);
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

// event handling

function addObj({id, type}) {
    console.log("add obj %s %s", id, type);
    heap_data.push({id: id, x: width / 2, y: height / 2});
    restart();
}

function delObj({id}) {
    console.log("delete obj %s", id);
    const index = heap_data.findIndex((x) => x.id === id);
    if (index < 0)
        throw "cant dealloc missing heap object " + id;
    heap_data.splice(index, 1);

    heap_links = heap_links.filter((x) => x.source.id !== id && x.target.id !== id);
    restart()
}

function setLink({src, dst, name}) {
    console.log("set link %s from %s to %s", name, src, dst);
    let existing = heap_links.find((x) => x.source.id === src && x.name === name);
    if (existing) {
        const target_obj = heap_data.find((x) => x.id === dst);
        if (target_obj === undefined)
            throw "cant find target heap object " + dst;
        existing.target = target_obj;
    } else {
        heap_links.push({source: src, target: dst, name: name});
    }

    restart();
}

function enterMethod({clazz, method}) {
    console.log("entering method %s:%s", clazz, method.name);
    let frame = {
        clazz: clazz, method: method,
        locals_height: local_var_height * method.locals.length,
        spanking_new: true,
        uuid: next_unique_frame_id++,
        y: 0
    };
    stack_frames[frame.uuid] = frame;
    callstack.push(frame);
    restart(); // TODO only stack
}

function exitMethod() {
    console.log("exiting method");
    const old_frame = callstack.pop();

    heap_links = heap_links.filter(d => !d.stack || d.stack.frame_uuid !== old_frame.uuid);
    heap_data = heap_data.filter(d => !d.stack || d.stack.frame_uuid !== old_frame.uuid);

    restart();
}

function setStackLink({index, dst}) {
    console.log("setting stack link from var %d to %d", index, dst);
    const current_frame = callstack[callstack.length - 1];
    const name = current_frame.method.locals[index].name;
    const id = "stack_" + current_frame.uuid + "_" + index;
    const stack_data = {
        frame_uuid: current_frame.uuid,
        index: index
    };

    // add stack node if not already there
    if (heap_data.find(x => x.stack && x.id === id) === undefined) {
        heap_data.push({
            id: id,
            stack: stack_data,
        });
    }

    // add edge
    let existing_link = heap_links.find((x) =>
        x.stack &&
        x.stack.frame_uuid === current_frame.uuid &&
        x.stack.index === index);

    if (existing_link) {
        existing_link.target = dst;
    } else {
        const target_obj = heap_data.find((x) => x.id === dst);
        if (target_obj === undefined)
            throw "cant find target heap object " + dst;
        heap_links.push({
            source: id,
            target: target_obj,
            name: name,
            stack: stack_data,
        });
    }
    restart()
}

const event_handlers = {
    "add_obj": addObj,
    "del_obj": delObj,
    "set_link": setLink,
    "enter_method": enterMethod,
    "exit_method": exitMethod,
    "set_stack_link": setStackLink,
};

const event_ticker = setInterval(() => {
    let evt = events.shift();
    if (evt === undefined) {
        console.log("all done");
        clearInterval(event_ticker);
        sim.stop();
        return;
    }
    let {type, payload} = evt;
    event_handlers[type](payload)
}, tick_speed);
