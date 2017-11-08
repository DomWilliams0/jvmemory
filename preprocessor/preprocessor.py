#!/usr/bin/env python3.6
import sys
from proto import message_pb2


def _exit(msg):
    sys.stderr.write(msg + "\n")
    sys.exit(1)


def read_messages(f):
    # TODO read protobuf messages
    return
    yield


def main():
    try:
        in_file = sys.argv[1]
    except IndexError:
        return _exit(f"Usage: {sys.argv[0]} <monitor output log>")

    try:
        with open(in_file, "rb") as f:
            for msg in read_messages(f):
                # TODO use msg
                print(f"Read {msg}")

    except OSError as e:
        return _exit("Bad input file: " + e.strerror)


if __name__ == '__main__':
    main()
