let definitions;

function highlight(selection, wat, read) {
    let first = selection.node();
    if (!first) return;

    const prefix = wat + "Access";
    const expected = prefix + (read ? "R" : "W");
    const pat = new RegExp("^" + prefix + "[R|W|RW]$");
    let rw = false;
    let rwReplaces = null;
    for (let cls of first.classList) {
        if (cls.match(pat)) {
            rw = cls !== expected;
            if (rw)
                rwReplaces = cls;
            break;
        }
    }

    if (rwReplaces) selection.classed(rwReplaces, false);

    let highlightClass = prefix + (rw ? "RW" : (read ? "R" : "W"));
    selection.classed(highlightClass, true);
    selection.nodes().forEach(n => {
        n.onanimationend = () => n.classList.remove(highlightClass);
    });
}

const highlightNode = (selection, read) => highlight(selection, "node", read);
const highlightLinks = (selection, read) => highlight(selection, "link", read);

function highlightLocalVar(varId, read) {
    const stackNode = node.filter(d => d.id === varId).select("circle");
    if (!stackNode.empty()) {
        highlightNode(stackNode, read);

        const links = link.filter(d => d.source.id === varId);
        highlightLinks(links, read);
    }
}

function highlightHeapObj(objId, field, read) {
    const heapNode = node.filter(d => d.id === objId).select("circle");
    highlightNode(heapNode, read);

    if (field) {
        const links = link.filter(d => d.source.id === objId && (!field || d.name === field));
        highlightLinks(links);
    }
}

function setPlayButtonState(playing) {
    const play = "fa-play";
    const pause = "fa-pause";

    let del, add;
    if (!playing) {
        del = pause;
        add = play;
    } else {
        del = play;
        add = pause;
    }
    const icon = playPauseButton.node().firstChild;
    icon.classList.add(add);
    icon.classList.remove(del)
}

function removeStackNodes(frameUuid) {
    heapLinks = heapLinks.filter(d => !d.stack || d.stack.frameUuid !== frameUuid);
    heapObjects = heapObjects.filter(d => !d.stack || d.stack.frameUuid !== frameUuid);
}

function removeHeapNode(id) {
    const index = heapObjects.findIndex((x) => x.id === id);
    if (index < 0) throw "cant dealloc missing heap object " + id;

    heapLinks = heapLinks.filter(x => x.source.id !== id && x.target.id !== id);
    heapObjects.splice(index, 1);
}

const playPauseButton = d3.select("#play-pause");

const SPEED_STEP = 50;
const SCRUB_STEP = 1;
const SERVER = "http://localhost:52933";

const speedSlider = {
    element: document.getElementById("speed-slider"),
    set: function (speed) {
        this.element.value = Constants.MinSpeed - speed;
    },
    get: function () {
        return Constants.MinSpeed - this.element.value;
    }
};

function startTicking(events) {
    const goodyBag = {
        setPlayButtonState,
        restartSim: restart,
        setSimState: (state) => state ? sim.restart() : sim.stop(),
        highlightLocalVar: highlightLocalVar,
        highlightHeapObj: highlightHeapObj,
        removeStackNodes,
        removeHeapNode,
        getHeapCentre: () => HEAP_CENTRE,
        callStack,
        definitions,
        nodes: () => heapObjects,
        links: () => heapLinks
    };
    let ticker = new EventTicker(events, goodyBag);

    // play/pause
    playPauseButton.on("click", () => ticker.toggle());
    d3.select("body").on("keydown", () => {
        // space when not on button
        if (d3.event.target !== playPauseButton && d3.event.keyCode === 32)
            ticker.toggle();
    });

    // speed slider
    speedSlider.element.min = Constants.MaxSpeed;
    speedSlider.element.max = Constants.MinSpeed;
    speedSlider.element.onchange = () => ticker.speed = speedSlider.get();
    speedSlider.set(ticker.speed);

    // speed buttons
    function changeSpeed(delta) {
        return function () {
            speedSlider.element.value = parseInt(speedSlider.element.value) + (delta * SPEED_STEP);
            speedSlider.element.onchange();
        }
    }

    d3.select("#slow-down").on("click", changeSpeed(-1));
    d3.select("#speed-up").on("click", changeSpeed(1));

    // scrub buttons
    d3.select("#scrub-back").on("click", () => ticker.scrubToRelative(-SCRUB_STEP));
    d3.select("#scrub-forward").on("click", () => ticker.scrubToRelative(SCRUB_STEP));

    // and awaay we go
    ticker.resume();
}

fetch(SERVER + "/definitions").then(resp => resp.arrayBuffer()).then(defs => {
    definitions = new Definitions(new Uint8Array(defs));
    let main = definitions.findMainClass();
    if (main)
        document.title = "JVMemory - " + main;

    fetch(SERVER + "/thread").then(resp => resp.json()).then(threads => {
        console.log("%d thread(s) available: %s", threads.length, threads);
        if (threads.length === 0) throw "no events";
        return threads[0];
    }).then(thread_id => {
        fetch(SERVER + "/thread/" + thread_id).then(resp => resp.arrayBuffer())
            .then((events) => startTicking(new Uint8Array(events)));
    });
});

