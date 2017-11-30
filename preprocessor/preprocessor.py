#!/usr/bin/env python3.6
import sys

import networkx

import vis_server
from proto import message_pb2


class BaseProcessor:
    def __init__(self, thread_id):
        self.thread_id = thread_id

        self._handlers = {val: getattr(self, f"handle_{key.lower()}")
                          for (key, val) in message_pb2.MessageType.items()}

    def on_end(self):
        pass

    def handle_message(self, msg):
        payload = getattr(msg, msg.WhichOneof("payload"))
        self._handlers[msg.type](payload)

    def handle_method_enter(self, msg):
        pass

    def handle_method_exit(self, msg):
        pass

    def handle_class_decl(self, msg):
        pass

    def handle_alloc(self, msg):
        pass

    def handle_dealloc(self, msg):
        pass

    def handle_getfield(self, msg):
        pass

    def handle_putfield(self, msg):
        pass

    def handle_store(self, msg):
        pass

    def handle_load(self, msg):
        pass


class CallGraphProcessor(BaseProcessor):
    def __init__(self, thread_id):
        super().__init__(thread_id)

        self.callstack = []
        self.graph = networkx.DiGraph()
        self.current = "root"

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


# thread id -> processor
processors = {}


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
                    proc = processors[msg.threadId] = DebugProcessor(msg.threadId)

                proc.handle_message(msg)

        for proc in processors.values():
            proc.on_end()

        # lord forgive me for this hack
        if isinstance(processors[1], CallGraphProcessor):
            vis_server.serve_callgraph(list(processors.values()))

    except OSError as e:
        return _exit("Bad input file: " + e.strerror)


if __name__ == '__main__':
    main()
