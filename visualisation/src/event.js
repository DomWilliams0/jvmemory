function generateRandomPersistentColour(className, isSystem) {
    const classNameWithoutArray = className.replace(/\[\]/g, "");
    let rand = new Math.seedrandom(classNameWithoutArray)() * 360;
    if (!isSystem)
        return "hsl(" + rand + ", 70%, 70%)";
    else
        return "hsl(" + rand + ", 60%, 40%)";
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

const playPauseButton = d3.select("#play-pause");

const MIN_SPEED = 500; // ms between events
const MAX_SPEED = 1; // ms between events

const SERVER = "http://localhost:52933";

const speedSlider = {
    element: document.getElementById("speed-slider"),
    set: function (speed) {
        this.element.value = MIN_SPEED - speed;
    },
    get: function () {
        return MIN_SPEED - this.element.value;
    }
};

function startTicking(events, definitions) {
    let ticker = new EventTicker(events, references = {
            definitions,
            heapObjects,
            heapLinks,
            callstack,
            stackFrames,
        },
        callbacks = {
            onPlayOrPause: setPlayButtonState,
            onSetSimState: (state) => state ? sim.restart() : sim.stop(),
            highlightLocalVar: highlightLocalVar,
            highlightHeapObj: highlightHeapObj,
        });
    // TODO args:
    //      onPlayOrPause(state) -> { set pause button state; }
    //      onStopSim() -> sim.stop
    //      onRestartSim(graphChanged) -> restart
    //

    // toggle -> play/pause -> will emit both events
    // scrub to event X -> ticker.scrub(x) which will simulate up/back to that event and update sim at end
    //                     (always just onRestartSim, will unpause)

    // play/pause
    playPauseButton.on("click", () => ticker.toggle());
    d3.select("body").on("keydown", () => {
        // space when not on button
        if (d3.event.target !== playPauseButton && d3.event.keyCode === 32)
            ticker.toggle();
    });

    // speed slider
    speedSlider.element.min = MAX_SPEED;
    speedSlider.element.max = MIN_SPEED;
    speedSlider.element.onchange = () => ticker.speed = speedSlider.get();
    speedSlider.set(ticker.speed);

    // speed buttons
    d3.select("#slow-down").on("click", ticker.slowDown);
    d3.select("#speed-up").on("click", ticker.speedUp);

    // and awaay we go
    ticker.resume();
}

fetch(SERVER + "/definitions").then(resp => resp.json()).then(defs => {
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

    fetch(SERVER + "/thread").then(resp => resp.json()).then(threads => {
        console.log("%d thread(s) available: %s", threads.length, threads);
        if (threads.length === 0) throw "no events";
        return threads[0];
    }).then(thread_id => {
        fetch(SERVER + "/thread/" + thread_id).then(resp => resp.json())
            .then((events) => startTicking(events, defs));
    });
});

