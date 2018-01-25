function addHeapObject(payload) {
    let {id} = payload,
        clazz = payload["class"];

    console.log("add obj %s %s", id, clazz);
    heap_data.push({id, x: width / 2, y: height / 2, clazz});
    restart();
}

function delHeapObject({id}) {
    console.log("delete obj %s", id);
    const index = heap_data.findIndex((x) => x.id === id);
    if (index < 0)
        throw "cant dealloc missing heap object " + id;
    heap_data.splice(index, 1);

    heap_links = heap_links.filter((x) => x.source.id !== id && x.target.id !== id);
    restart()
}

function setInterHeapLink(payload) {
    let {srcId, dstId, fieldName} = payload;
    const rm = dstId === undefined;

    console.log("set link %s from %s to %s", fieldName, srcId, dstId || "null");

    let existingIndex = heap_links.find((x) => x.source === srcId && x.name === fieldName);
    if (existingIndex >= 0) {
        if (rm) {
            heap_links.splice(existingIndex, 1)
        } else {
            heap_links[existingIndex].target = dstId;
        }
    } else if (!rm)
        heap_links.push({source: srcId, target: dstId, name: fieldName});

    restart();
}

function setLocalVarLink(payload) {
    let varIndex = payload.varIndex || 0,
        {dstId} = payload;
    const current_frame = callstack[callstack.length - 1];
    // TODO local vars is not sorted by index?!
    const name = current_frame.method.localVars.find(l => l.index === varIndex).name;
    const id = "stack_" + current_frame.uuid + "_" + varIndex;
    const stack_data = {
        frame_uuid: current_frame.uuid,
        index: varIndex
    };
    console.log("setting stack link from var %d to %d (%s)", varIndex, dstId, name);

    const rm = dstId === undefined;

    // add stack node if not already there
    if (!rm) {
        if (heap_data.find(x => x.stack && x.id === id) === undefined) {
            heap_data.push({
                id: id,
                stack: stack_data,
            });
        }
    }

    let existing_link_index = heap_links.findIndex((x) =>
        x.stack &&
        x.stack.frame_uuid === current_frame.uuid &&
        x.stack.index === varIndex);

    // existing link
    if (existing_link_index >= 0) {
        if (rm) {
            heap_links.splice(existing_link_index, 1)
        } else {
            heap_links[existing_link_index].target = dstId
        }
    } else if (!rm) {
        heap_links.push({
            source: id,
            target: dstId,
            name: name,
            stack: stack_data,
        });
    }
    restart()
}

function showLocalVarAccess(payload) {
    let varIndex = payload.varIndex || 0,
        {edgeName} = payload;
    // TODO is name needed?
    console.log("show stack access %d", varIndex)
}

function showHeapObjectAccess(payload) {
    let {objId, fieldName} = payload;
    console.log("showing heap access from id %d field %s", objId, fieldName)
}

function pushMethodFrame({owningClass, definition}) {
    console.log("entering method %s:%s", owningClass.name, definition.name);
    let frame = {
        clazz: owningClass, method: definition,
        locals_height: local_var_height * definition.localVars.length,
        spanking_new: true,
        uuid: next_unique_frame_id++,
        y: 0
    };
    stack_frames[frame.uuid] = frame;
    callstack.push(frame);
    restart(); // TODO only stack
}

function popMethodFrame() {
    console.log("exiting method");
    const old_frame = callstack.pop();

    heap_links = heap_links.filter(d => !d.stack || d.stack.frame_uuid !== old_frame.uuid);
    heap_data = heap_data.filter(d => !d.stack || d.stack.frame_uuid !== old_frame.uuid);

    restart();
}

const event_handlers = {
    "ADD_HEAP_OBJECT": [addHeapObject, "addHeapObject"],
    "DEL_HEAP_OBJECT": [delHeapObject, "delHeapObject"],
    "SET_INTER_HEAP_LINK": [setInterHeapLink, "setInterHeapLink"],
    "SET_LOCAL_VAR_LINK": [setLocalVarLink, "setLocalVarLink"],
    "SHOW_LOCAL_VAR_ACCESS": [showLocalVarAccess, "showLocalVarAccess"],
    "SHOW_HEAP_OBJECT_ACCESS": [showHeapObjectAccess, "showHeapObjectAccess"],
    "PUSH_METHOD_FRAME": [pushMethodFrame, "pushMethodFrame"],
    "POP_METHOD_FRAME": [popMethodFrame, "pushMethodFrame"], // null placeholder
};

function startTicking(server, tickSpeed) {
    fetch(server + "/thread").then(resp => resp.json()).then(threads => {
        console.log("%d thread(s) available: %s", threads.length, threads);
        if (threads.length === 0) throw "no events";
        return threads[0]
    }).then(thread_id => {
        fetch(server + "/thread/" + thread_id).then(resp => resp.json())
            .then(evts => {
                let i = 0;
                const ticker = setInterval(() => {
                    if (i >= evts.length) {
                        console.log("all done");
                        clearInterval(ticker);
                        setTimeout(sim.stop, 5000);
                        return;
                    }
                    let evt = evts[i];
                    let handler_tup = event_handlers[evt.type];
                    if (handler_tup) {
                        let [handler, payload_name] = event_handlers[evt.type];
                        let payload = evt[payload_name];
                        handler(payload);
                    }

                    i += 1;
                }, tickSpeed);
            })
    });
}
