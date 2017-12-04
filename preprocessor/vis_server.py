import json

from tornado import web, ioloop

PORT = 48771
HOST = "localhost"


class BaseHandler(web.RequestHandler):
    def data_received(self, chunk):
        pass

    def set_default_headers(self):
        self.set_header("Access-Control-Allow-Origin", "*")
        self.set_header('Content-Type', 'application/json')


class CallgraphHandler(BaseHandler):
    def initialize(self, graph=None):
        self.graph = graph

    def get(self):
        self.write(self.graph)


class PlaybackHandler(BaseHandler):
    def initialize(self, history=None):
        self.history = history

    def get(self):
        self.write(self.history)


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
    history = json.dumps(proc.history)

    app = web.Application([
        (r"/callgraph", CallgraphHandler, {"graph": graph}),
        (r"/playback", PlaybackHandler, {"history": history})
    ])
    app.listen(PORT, address=HOST)
    print(f"Listening on {HOST}:{PORT}")
    ioloop.IOLoop.current().start()
