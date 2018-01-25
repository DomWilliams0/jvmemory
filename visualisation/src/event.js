let definitions;

function addHeapObject(payload) {
    let {id} = payload,
        clazz = payload["class"];

    console.log("add obj %s %s", id, clazz);
    heapObjects.push({id, x: WINDOW_WIDTH / 2, y: WINDOW_HEIGHT / 2, clazz});
    restart();
}

function delHeapObject({id}) {
    console.log("delete obj %s", id);

    const index = heapObjects.findIndex((x) => x.id === id);
    if (index < 0)
        throw "cant dealloc missing heap object " + id;
    heapObjects.splice(index, 1);
    heapLinks = heapLinks.filter((x) => x.source.id !== id && x.target.id !== id);

    restart()
}

function setInterHeapLink(payload) {
    let {srcId, dstId, fieldName} = payload;
    const rm = dstId === undefined;

    console.log("set link %s from %s to %s", fieldName, srcId, dstId || "null");

    let existingIndex = heapLinks.find((x) => x.source === srcId && x.name === fieldName);
    if (existingIndex >= 0) {
        if (rm) {
            heapLinks.splice(existingIndex, 1)
        } else {
            heapLinks[existingIndex].target = dstId;
        }
    } else if (!rm)
        heapLinks.push({source: srcId, target: dstId, name: fieldName});

    restart();
}

function setLocalVarLink(payload) {
    let varIndex = payload.varIndex || 0,
        {dstId} = payload;
    const rm = dstId === undefined;
    const currentFrame = callstack[callstack.length - 1];

    // TODO duplicate local vars >:(
    const name = currentFrame.methodDefinition.localVars.find(l => l.index === varIndex).name;
    const id = "stack_" + currentFrame.uuid + "_" + varIndex;
    const stackData = {
        frameUuid: currentFrame.uuid,
        index: varIndex
    };
    console.log("setting stack link from var %d to %d (%s)", varIndex, dstId, name);


    // add stack node if not already there
    if (!rm) {
        if (heapObjects.find(x => x.stack && x.id === id) === undefined) {
            heapObjects.push({
                id: id,
                stack: stackData,
            });
        }
    }

    let existingLinkIndex = heapLinks.findIndex((x) =>
        x.stack &&
        x.stack.frameUuid === currentFrame.uuid &&
        x.stack.index === varIndex);

    // existing link
    if (existingLinkIndex >= 0) {
        if (rm) {
            heapLinks.splice(existingLinkIndex, 1)
        } else {
            heapLinks[existingLinkIndex].target = dstId
        }
    } else if (!rm) {
        heapLinks.push({
            source: id,
            target: dstId,
            name: name,
            stack: stackData,
        });
    }
    restart()
}

function showLocalVarAccess(payload) {
    let varIndex = payload.varIndex || 0;
    console.log("show stack access %d", varIndex)
}

function showHeapObjectAccess(payload) {
    let {objId, fieldName} = payload;
    console.log("showing heap access from id %d field %s", objId, fieldName)
}

function pushMethodFrame({owningClass, name, signature}) {
    console.log("entering method %s:%s", owningClass, name);
    let classDef = definitions[owningClass];
    if (classDef === undefined)
        throw "undefined class " + owningClass;
    let methodDef = classDef.methods.find(m => m.name === name && m.signature === signature);
    if (methodDef === undefined)
        throw "undefined method " + owningClass + ":" + name + " (" + signature + ")";

    let frame = {
        methodDefinition: {
            clazzShort: shortenClassName(owningClass),
            clazzLong: owningClass,
            name: name,
            signature: signature,
            localVars: methodDef.localVars,
        },
        localsHeight: LOCAL_VAR_SLOT_HEIGHT * methodDef.localVars.length,
        spankingNew: true,
        uuid: nextUniqueFrameId++,
        y: 0
    };
    stackFrames[frame.uuid] = frame;
    callstack.push(frame);
    restart(); // TODO only stack
}

function popMethodFrame() {
    console.log("exiting method");
    const old_frame = callstack.pop();

    heapLinks = heapLinks.filter(d => !d.stack || d.stack.frameUuid !== old_frame.uuid);
    heapObjects = heapObjects.filter(d => !d.stack || d.stack.frameUuid !== old_frame.uuid);

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
    fetch(server + "/definitions").then(resp => resp.json()).then(defs => {
        definitions = defs;
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
                        let handlerTuple = event_handlers[evt.type];
                        if (handlerTuple) {
                            let [handler, payload_name] = event_handlers[evt.type];
                            let payload = evt[payload_name];
                            handler(payload);
                        }

                        i += 1;
                    }, tickSpeed);
                })
        });
    });
}
