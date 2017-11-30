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


class DebugProcessor(BaseProcessor):
    def _log(self, what):
        print(f"{self.thread_id} - {what}")

    def handle_putfield(self, msg):
        self._log(f"putfield '{msg.field}' on obj ${msg.id}")

    def handle_class_decl(self, msg):
        self._log(f"{msg.visibility} {msg.class_type} {msg.name} : {msg.super_class or 'java/lang/Object'}")
        if msg.interfaces:
            self._log(f"\tinterfaces: {msg.interfaces}")
        for f in msg.fields:
            self._log(f"\t{f.visibility}{' static ' if f.static else ' '}{f.type} {f.name}")
        for m in msg.methods:
            self._log(f"\t{m.visibility}{' static ' if m.static else ' '}{m.name} - {m.signature}")
            for lv in m.local_vars:
                self._log(f"\t\tlocal var {lv.index} {lv.type} '{lv.name}'")
        print()

    def handle_store(self, msg):
        self._log(f"store {msg.index} of type {msg.type}")

    def handle_dealloc(self, msg):
        self._log(f"dealloc {msg.id}")

    def handle_load(self, msg):
        self._log(f"load {msg.index}")

    def handle_getfield(self, msg):
        self._log(f"getfield '{msg.field}' on obj ${msg.id}")

    def handle_method_exit(self, msg):
        self._log("<<<")

    def handle_alloc(self, msg):
        self._log(f"alloc {msg.id} of type {msg.type}")

    def handle_method_enter(self, msg):
        self._log(f">>> {getattr(msg, 'class')}:{msg.method}")


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
