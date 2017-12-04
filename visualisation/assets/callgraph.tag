<callgraph>
    <div id="cg"></div>

    <script>
        var timer = null;
        var current = null;
        var history = null;
        var graph = null;

        function setTimeScale(timescale) {
            const BASE_SPEED = 1000;

            function stop() {
                if (timer != null) {
                    clearInterval(timer);
                    timer = null;
                }
            }

            stop();

            if (timescale > 0)
                timer = setInterval(tick, BASE_SPEED / timescale);

            function tick() {
                // finished with current
                if (current != null)
                    current.removeClass("highlighted");

                var next = history.pop();

                // end reached
                if (next === undefined) {
                    stop();
                    return;
                }

                current = graph.getElementById(next);
                current.addClass("highlighted");

                console.log(current.data("id"));
            }
        }

        const URL = "http://localhost:48771";

        this.on("mount", function () {
            graph = cytoscape({
                container: $('#cg'),
                style: [ // the stylesheet for the graph
                    {
                        selector: 'node',
                        style: {
                            'background-color': '#223',
                            'width': '1em',
                            'height': '1em',
                            'label': 'data(id)',
                            'font-size': '10'
                        }
                    },
                    {
                        selector: 'node.highlighted',
                        style: {
                            'background-color': '#c0392b',
                            'width': '3em',
                            'height': '3em'
                        }
                    },
                    {
                        selector: 'edge',
                        style: {
                            'width': 2,
                            'font-size': '10',
                            'line-color': '#ccc',
                            'target-arrow-color': '#ccc',
                            'target-arrow-shape': 'triangle',
                            'label': 'data(count)'
                        }
                    }
                ]
            });

            // request graph
            $.get(URL + "/callgraph", {}, function (data) {
                graph.add(data);
                var l = graph.layout({
                    name: "breadthfirst",
                    directed: true,
                    fit: false,
                    spacingFactor: 1,
                    nodeDimensionsIncludeLabels: true,
                    stop: function () {
                        graph.nodes().forEach(function (n) {
                            var y = n.position("y");
                            n.position("y", graph.height()-y)
                        })
                    }
                });
                l.run();

                $.get(URL + "/playback", {}, function (data) {
                    history = data;
                    history.reverse(); // allows pop() to remove from end
                    setTimeScale(1);
                });
            });
        });
    </script>
</callgraph>
