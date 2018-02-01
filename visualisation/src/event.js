function generateRandomPersistentColour(className, isSystem) {
    let rand = new Math.seedrandom(className)() * 360;
    if (!isSystem)
        return "hsl(" + rand + ", 70%, 70%)";
    else
        return "hsl(" + rand + ", 60%, 40%)";
}

const FLASH_DURATION = 50;
const SPAWN_OFFSET = 20;

function addHeapObject(payload) {
    let {id, arraySize} = payload,
        clazz = payload["class"];

    let classDef = definitions[clazz];
    let colour;
    if (!classDef) {
        // TODO arrays of different dimensions should share the same colour
        //      i.e., remove [[[[[
        const clazzName = clazz.name || clazz;
        colour = generateRandomPersistentColour(clazzName, true);
        definitions[clazz] = {colour};
    }
    else
        colour = classDef.colour;

    console.log("add obj %s %s%s", id, clazz, arraySize ? " of size " + arraySize : "");

    let array = undefined;
    if (arraySize)
        array = {
            size: arraySize,
        };

    heapObjects.push({
        id,
        clazz,
        array,
        fill: colour,
        x: HEAP_CENTRE[0] + (Math.random() - 0.5) * SPAWN_OFFSET,
        y: HEAP_CENTRE[1] + (Math.random() - 0.5) * SPAWN_OFFSET,
    });
    restart(true);
}

function delHeapObject({id}) {
    console.log("delete obj %s", id);

    const index = heapObjects.findIndex((x) => x.id === id);
    if (index < 0)
        throw "cant dealloc missing heap object " + id;
    heapObjects.splice(index, 1);
    heapLinks = heapLinks.filter((x) => x.source.id !== id && x.target.id !== id);

    restart(true);
}

function setInterHeapLink(payload) {
    let {srcId, dstId, fieldName} = payload;
    const rm = dstId === undefined;

    console.log("set link %s from %s to %s", fieldName, srcId, dstId || "null");

    let existingIndex = heapLinks.findIndex((x) => x.source.id === srcId && x.name === fieldName);
    if (existingIndex >= 0) {
        if (rm) {
            heapLinks.splice(existingIndex, 1);
        } else {
            heapLinks[existingIndex].target = dstId;
        }
    } else if (!rm)
        heapLinks.push({source: srcId, target: dstId, name: fieldName});

    restart(true);
}

function getStackNodeId(frame, index) {
    return "stack_" + frame.uuid + "_" + index;
}

function setLocalVarLink(payload) {
    let varIndex = payload.varIndex || 0,
        {dstId} = payload;
    const rm = dstId === undefined;
    const currentFrame = callstack[callstack.length - 1];

    const localVar = currentFrame.methodDefinition.localVars.find(l => l.index === varIndex);
    if (!localVar) {
        console.log("error: invalid local var %d, skipping setLocalVarLink", varIndex);
        return 0;
    }
    const name = localVar.name;
    const stackData = {
        frameUuid: currentFrame.uuid,
        index: varIndex,
    };
    console.log("setting stack link from var %d to %d (%s)", varIndex, dstId, name);

    const id = getStackNodeId(currentFrame, varIndex);

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
            heapLinks.splice(existingLinkIndex, 1);
        } else {
            heapLinks[existingLinkIndex].target = dstId;
        }
    } else if (!rm) {
        heapLinks.push({
            source: id,
            target: dstId,
            name: name,
            stack: stackData,
        });
    }
    restart(true);
}

function showLocalVarAccess(payload) {
    let varIndex = payload.varIndex || 0;
    console.log("show stack access %d", varIndex);

    const currentFrame = callstack[callstack.length - 1];
    const id = "stack_" + currentFrame.uuid + "_" + varIndex;

    // TODO highlight local var rect in stack too

    // highlight stack node and link, if possible
    const stackNode = node.filter(d => d.id === id).select("circle");
    if (stackNode) {
        const links = link.filter(d => d.source.id === id);

        // stackNode.attr("class", "flash");
        links.classed("linkFlash", true);
        stackNode.classed("nodeFlash", true);
        setTimeout(() => {
            links.classed("linkFlash", false);
            stackNode.classed("nodeFlash", false);
        }, FLASH_DURATION);
    }

    return FLASH_DURATION;
}

function showHeapObjectAccess(payload) {
    let {objId, fieldName} = payload;
    console.log("showing heap access from id %d field %s", objId, fieldName || "none");

    const heapNode = node.filter(d => d.id === objId).select("circle");
    heapNode.classed("nodeFlash", true);

    let links;
    if (fieldName) {
        links = link.filter(d => d.source.id === objId && (!fieldName || d.name === fieldName));
        links.classed("linkFlash", true);
    }
    setTimeout(() => {
        heapNode.classed("nodeFlash", false);
        if (links)
            links.classed("linkFlash", false);
    }, FLASH_DURATION);


    return FLASH_DURATION;
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
        y: 0,
    };
    stackFrames[frame.uuid] = frame;
    callstack.push(frame);
    restart();
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

    function Ticker(events, time) {
        this.time = time;
        this.index = 0;
        this.id = null;
        this.lastTick = new Date();

        let ticker = this;
        this.pause = function () {
            clearTimeout(ticker.id);
            ticker.id = null;

            ticker.time -= new Date() - ticker.lastTick;
        };

        this.resume = function () {
            clearTimeout(ticker.id);

            ticker.lastTick = new Date();
            this.id = setTimeout(function callback() {
                ticker.lastTick = new Date();

                // end reached
                if (ticker.index >= events.length) {
                    console.log("all done");
                    ticker.pause();

                    console.log("stopping simulation in 5 seconds");
                    setTimeout(sim.stop, 5000);

                    return;
                }

                let nextTime;
                let evt = events[ticker.index];
                let handlerTuple = event_handlers[evt.type];
                if (handlerTuple) {
                    let [handler, payload_name] = event_handlers[evt.type];
                    let payload = evt[payload_name];
                    nextTime = handler(payload);
                }

                ticker.index += 1;

                if (nextTime === undefined)
                    nextTime = time;
                ticker.time = nextTime;

                ticker.id = setTimeout(callback, ticker.time);

            }, ticker.time);
        };

        this.toggle = function () {
            if (ticker.id)
                ticker.pause();
            else
                ticker.resume();
        }
    }


    fetch(server + "/definitions").then(resp => resp.json()).then(defs => {
        let main;
        for (let cls of defs) {
            cls.colour = generateRandomPersistentColour(cls);
            definitions[cls.name] = cls;

            // bit of a hack
            if (!main &&
                cls.methods.find(m => m.static && m.name === "main" && m.signature === "([Ljava/lang/String;)V")) {
                main = cls.name;
            }
        }

        if (main) {
            document.title = "JVMemory - " + main;
        }

        fetch(server + "/thread").then(resp => resp.json()).then(threads => {
            console.log("%d thread(s) available: %s", threads.length, threads);
            if (threads.length === 0) throw "no events";
            return threads[0];
        }).then(thread_id => {
            fetch(server + "/thread/" + thread_id).then(resp => resp.json())
                .then(evts => {

                    let ticker = new Ticker(evts, tickSpeed);

                    d3.select("body")
                        .on("keydown", () => {
                            // space
                            if (d3.event.keyCode === 32) {
                                ticker.toggle();
                            }
                        });


                    ticker.resume();
                });
        });
    });
}
