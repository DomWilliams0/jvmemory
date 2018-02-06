function generateRandomPersistentColour(className, isSystem) {
    const classNameWithoutArray = className.replace(/\[\]/g, "");
    let rand = new Math.seedrandom(classNameWithoutArray)() * 360;
    if (!isSystem)
        return "hsl(" + rand + ", 70%, 70%)";
    else
        return "hsl(" + rand + ", 60%, 40%)";
}

const SPAWN_OFFSET = 20;

const
    DIDNT_CHANGE_SIM = 0x100,
    DIDNT_CHANGE_GRAPH = 0x101,
    CHANGED_GRAPH = 0x102;

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
            dims: clazz.split("[]").length - 1 // counts []s
        };

    heapObjects.push({
        id,
        clazz,
        array,
        fill: colour,
        x: HEAP_CENTRE[0] + (Math.random() - 0.5) * SPAWN_OFFSET,
        y: HEAP_CENTRE[1] + (Math.random() - 0.5) * SPAWN_OFFSET,
    });

    return CHANGED_GRAPH;
}

function delHeapObject({id}) {
    console.log("delete obj %s", id);

    const index = heapObjects.findIndex((x) => x.id === id);
    if (index < 0)
        throw "cant dealloc missing heap object " + id;
    heapObjects.splice(index, 1);
    heapLinks = heapLinks.filter((x) => x.source.id !== id && x.target.id !== id);

    return CHANGED_GRAPH;
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

    return CHANGED_GRAPH;
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
        return DIDNT_CHANGE_GRAPH;
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
    return CHANGED_GRAPH;
}

function highlight(selection, wat, read) {
    let first = selection.node();
    if (!first) return;

    const prefix = wat + "Access";
    const expected = prefix + (read ? "R" : "W");
    const pat = new RegExp("^" + prefix + "[R|W|RW]$");
    let rw = false;
    for (let cls of first.classList) {
        if (cls.match(pat)) {
            rw = cls !== expected;
            break;
        }
    }

    let highlightClass = prefix + (rw ? "RW" : (read ? "R" : "W"));
    selection.classed(highlightClass, true);
    selection.nodes().forEach(n => {
        n.onanimationend = () => n.classList.remove(highlightClass);
    });
}

const highlightNode = (selection, read) => highlight(selection, "node", read);
const highlightLinks = (selection, read) => highlight(selection, "link", read);

function showLocalVarAccess(payload) {
    let varIndex = payload.varIndex || 0;
    console.log("show %s stack access %d", payload.read ? "read" : "write", varIndex);

    const currentFrame = callstack[callstack.length - 1];
    const id = "stack_" + currentFrame.uuid + "_" + varIndex;

    // TODO highlight local var rect in stack too

    const stackNode = node.filter(d => d.id === id).select("circle");
    if (!stackNode.empty()) {
        highlightNode(stackNode, payload.read);

        const links = link.filter(d => d.source.id === id);
        highlightLinks(links, payload.read);
    }

    return DIDNT_CHANGE_SIM;
}

function showHeapObjectAccess(payload) {
    let {objId, fieldName, read} = payload;
    console.log("showing %s heap access from id %d field %s", read ? "read" : "write", objId, fieldName || "none");

    const heapNode = node.filter(d => d.id === objId).select("circle");
    highlightNode(heapNode, read);

    if (fieldName) {
        const links = link.filter(d => d.source.id === objId && (!fieldName || d.name === fieldName));
        highlightLinks(links);
    }

    return DIDNT_CHANGE_SIM;
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

    return DIDNT_CHANGE_GRAPH;
}

function popMethodFrame(_payload) {
    console.log("exiting method");
    const old_frame = callstack.pop();

    heapLinks = heapLinks.filter(d => !d.stack || d.stack.frameUuid !== old_frame.uuid);
    heapObjects = heapObjects.filter(d => !d.stack || d.stack.frameUuid !== old_frame.uuid);

    return CHANGED_GRAPH;
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

let ticker;
const playPauseButton = document.getElementById("playpause");
function startTicking(server, tickSpeed) {

    function Ticker(events, time) {
        this.time = time;
        this.index = 0;
        this.id = null;

        let ticker = this;
        this.pause = function () {
            clearTimeout(ticker.id);
            ticker.id = null;
            sim.stop();
        };

        function isInRange(x) {
            const val = x === undefined ? ticker.index : x;
            return val < events.length && val >= 0;
        }

        this.resume = function () {
            function advance(delta) {
                let advanced = ticker.index += delta;
                if (!isInRange(advanced))
                    return false;
                ticker.index = advanced;
                return true;
            }

            clearTimeout(ticker.id);
            sim.restart();

            this.id = setTimeout(function callback() {
                let totalChangesToGraph = 0;
                let totalChangesToSim = 0;
                let keepGoing = true;
                while (keepGoing) {
                    keepGoing = false;

                    if (!isInRange())
                        return;

                    let evt = events[ticker.index];
                    let handlerTuple = event_handlers[evt.type];
                    if (handlerTuple) {
                        let [handler, payload_name] = handlerTuple;
                        let payload = evt[payload_name];
                        let simChange = handler(payload);
                        keepGoing = evt.continuous;

                        // thanks to true=1, false=0
                        totalChangesToGraph += simChange === CHANGED_GRAPH;
                        totalChangesToSim += simChange !== DIDNT_CHANGE_SIM
                    }

                    // step
                    if (!advance(1)) {
                        // an end has been reached
                        ticker.pause();
                        console.log("end reached");
                        return;
                    }
                }

                if (totalChangesToSim > 0)
                    restart(totalChangesToGraph > 0);

                // schedule next
                // ticker.time = ctx.nextTime === undefined ? time : ctx.nextTime;
                ticker.id = setTimeout(callback, ticker.time);

            }, ticker.time);
        };

        this.toggle = function () {
            togglePlayButton();
            if (ticker.id)
                ticker.pause();
            else
                ticker.resume();
        }
    }


    fetch(server + "/definitions").then(resp => resp.json()).then(defs => {
        let main;
        for (let cls of defs) {
            cls.colour = generateRandomPersistentColour(cls.name);
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

                    ticker = new Ticker(evts, tickSpeed);

                    d3.select("body")
                        .on("keydown", () => {
                            // space when not on button
                            if (d3.event.target !== playPauseButton && d3.event.keyCode === 32) {
                                ticker.toggle();
                            }
                        });


                    ticker.resume();
                });
        });
    });
}

function togglePlayButton() {
    playPauseButton.firstChild.classList.toggle("fa-play");
    playPauseButton.firstChild.classList.toggle("fa-pause");
}
