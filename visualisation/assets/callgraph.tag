<callgraph>
    <div id="cg"></div>

    <script>

        const URL = "http://localhost:48771"

        this.on("mount", function () {
            var cy = cytoscape({
                container: $('#cg'),
                style: [ // the stylesheet for the graph
                    {
                        selector: 'node',
                        style: {
                            'background-color': '#223',
                            'width': '1em',
                            'height': '1em',
                            'shape': 'roundrectangle',
                            'label': 'data(id)',
                            'font-size': '10'
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
                cy.add(JSON.parse(data));
                var l = cy.layout({
                    name: "breadthfirst",
                    directed: true,
                    fit: false,
                    spacingFactor: 1,
                    stop: function () {
                        cy.nodes().forEach(function (n) {
                            var y = n.position("y")
                            n.position("y", cy.height()-y)
                        })
                    }
                });
                l.run()
            });
        });
    </script>
</callgraph>
