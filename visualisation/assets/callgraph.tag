<callgraph>
    <div id="cg"></div>

    <script>
        this.on("mount", function () {
            var cy = cytoscape({

                container: document.getElementById('cg'),

                elements: [ // list of graph elements to start with
                    { // node a
                        data: {id: 'a'}
                    },
                    { // node b
                        data: {id: 'b'}
                    },
                    { // edge ab
                        data: {id: 'ab', source: 'a', target: 'b'}
                    }
                ],

                style: [ // the stylesheet for the graph
                    {
                        selector: 'node',
                        style: {
                            'background-color': '#666',
                            'label': 'data(id)'
                        }
                    },

                    {
                        selector: 'edge',
                        style: {
                            'width': 3,
                            'line-color': '#ccc',
                            'target-arrow-color': '#ccc',
                            'target-arrow-shape': 'triangle'
                        }
                    }
                ],

                layout: {
                    name: 'grid',
                    rows: 1
                }

            });
        });
    </script>
</callgraph>
