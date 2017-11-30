import json

from tornado import web, ioloop

PORT = 48771
HOST = "localhost"


def serve_callgraph(processors):
    def convert_to_cytoscapejs(graph):
        cytoscaped = []
        cytoscaped.extend({
                              "data": {
                                  "id": n
                              }
                          } for n in graph.nodes)
        cytoscaped.extend({
                              "data": {
                                  "source": u,
                                  "target": v,
                                  **data
                              }
                          } for ((u, v), data) in graph.edges.items())
        return cytoscaped

    # TODO only care about main thread for now
    proc = processors[0]

    graph = json.dumps(convert_to_cytoscapejs(proc.graph))

    class CallgraphHandler(web.RequestHandler):
        def get(self):
            self.write(graph)

        def set_default_headers(self):
            self.set_header("Access-Control-Allow-Origin", "*")

    app = web.Application([
        (r"/callgraph", CallgraphHandler)
    ])
    app.listen(PORT, address=HOST)
    print(f"Listening on {HOST}:{PORT}")
    ioloop.IOLoop.current().start()
