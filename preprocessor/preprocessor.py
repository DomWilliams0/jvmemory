#!/usr/bin/env python3.6
import sys

from proto import message_pb2


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


def main():
    try:
        in_file = sys.argv[1]
    except IndexError:
        return _exit(f"Usage: {sys.argv[0]} <monitor output log>")

    try:
        with open(in_file, "rb") as f:
            for msg in read_messages(f):
                msg_type = message_pb2.MessageType.Name(msg.type)
                print(f"Read {msg_type}")

    except OSError as e:
        return _exit("Bad input file: " + e.strerror)


if __name__ == '__main__':
    main()
