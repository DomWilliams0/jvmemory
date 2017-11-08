#!/usr/bin/env python3.6
import sys

from proto import message_pb2


def _null_handler(_self, _msg):
    pass


class Processor:
    HANDLERS = {}  # initialised after class definition

    def __init__(self, thread_id):
        self.thread_id = thread_id
        self.callstack = []

    def handle_message(self, msg):
        payload = getattr(msg, msg.WhichOneof("payload"))
        self.HANDLERS[msg.type](self, payload)

    def handle_method_enter(self, msg):
        self.callstack.append(f"{getattr(msg, 'class')}:{msg.method}")
        print(f"{self.thread_id} >>> {self.callstack[-1]}")

    def handle_method_exit(self, _msg):
        print(f"{self.thread_id} <<< {self.callstack[-1]}")
        self.callstack.pop()


Processor.HANDLERS.update({val: getattr(Processor, f"handle_{key.lower()}", _null_handler)
                           for (key, val) in message_pb2.MessageType.items()})

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
                    proc = processors[msg.threadId] = Processor(msg.threadId)

                proc.handle_message(msg)

    except OSError as e:
        return _exit("Bad input file: " + e.strerror)


if __name__ == '__main__':
    main()
