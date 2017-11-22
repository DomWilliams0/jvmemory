#!/usr/bin/env python3.6
import sys

import networkx

from proto import message_pb2


def _null_handler(_self, _msg):
    pass


class Processor:
    HANDLERS = {}  # initialised after class definition

    def __init__(self, thread_id):
        self.thread_id = thread_id
        self.callstack = []

        self.graph = networkx.DiGraph()
        self.current = "root"

    def handle_message(self, msg):
        payload = getattr(msg, msg.WhichOneof("payload"))
        self.HANDLERS[msg.type](self, payload)

    def handle_method_enter(self, msg):
        top = f"{getattr(msg, 'class')}:{msg.method}"
        self.callstack.append(top)
        print(f"{self.thread_id} >>> {top}")

        count = self.graph.get_edge_data(self.current, top, default={}).get("count", 0)
        self.graph.add_edge(self.current, top, count=count + 1)
        self.current = top

    def handle_method_exit(self, _msg):
        print(f"{self.thread_id} <<< {self.callstack[-1]}")
        self.callstack.pop()
        try:
            self.current = self.callstack[-1]
        except IndexError:
            self.current = "root"

    def on_end(self):
        # with open("testgraph.dot", "w") as f:
        #     f.write("digraph {")
        #     for ((u, v), data) in self.graph.edges.items():
        #         f.write(f"\"{u}\" -> \"{v}\" [label=\"{data['count']}\"]\n")
        #     f.write("}")
        pass


Processor.HANDLERS.update({val: getattr(Processor, f"handle_{key.lower()}", _null_handler)
                           for (key, val) in message_pb2.MessageType.items()})

# thread id -> processor
processors = {}

PORT = 48771
HOST = "localhost"


def serve_callgraph():
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
    proc = processors[1]

    from tornado import web, ioloop
    import json

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


def main():
    def _exit(msg):
        sys.stderr.write(msg + "\n")
        sys.exit(1)

    def read_messages(f):
        from google.protobuf.internal.decoder import _DecodeVarint32 as decoder

        while True:
            len_raw = f.peek()
            if not len_raw:
                break

            len_int, to_consume = decoder(len_raw, 0)
            f.read(to_consume)

            msg_raw = f.read(len_int)
            if not msg_raw:
                break

            msg = message_pb2.Variant()
            msg.ParseFromString(msg_raw)
            yield msg

    try:
        in_file = sys.argv[1]
    except IndexError:
        return _exit(f"Usage: {sys.argv[0]} <monitor output log>")

    try:
        with open(in_file, "rb") as f:
            for msg in read_messages(f):
                try:
                    proc = processors[msg.threadId]
                except KeyError:
                    proc = processors[msg.threadId] = Processor(msg.threadId)

                proc.handle_message(msg)

        for proc in processors.values():
            proc.on_end()

        serve_callgraph()

    except OSError as e:
        return _exit("Bad input file: " + e.strerror)


if __name__ == '__main__':
    main()
