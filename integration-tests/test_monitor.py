import io
import os
import shutil
import subprocess
import sys
import unittest
from pathlib import Path
from typing import BinaryIO, Generator, List

from google.protobuf.json_format import MessageToDict
from google.protobuf.message import Message

from pb import message_pb2
from pb.message_pb2 import Variant, MessageType


def _get_env(name: str) -> Path:
    var = os.getenv(name)
    if not var:
        print(f"Missing env var ${name}")
        sys.exit(1)
    return Path(var)


INSTALL_DIR: Path = _get_env("JVMEMORY_DIR")
TESTS_DIR: Path = _get_env("JVMEMORY_TEST_DIR")
WORKING_DIR: Path = _get_env("JVMEMORY_WORKING_DIR")
SPECIMEN_DIR: Path = Path(TESTS_DIR) / "specimens"


def _run_cmd(*args: str):
    print("Executing: '{}'".format(' '.join(args)))
    subprocess.check_call(args)


def _delete_working_dir():
    try:
        shutil.rmtree(str(WORKING_DIR))
    except FileNotFoundError:
        pass


def run_specimen(specimen_name: str, out_file: Path):
    # create working directory
    _delete_working_dir()
    WORKING_DIR.mkdir()

    # compile specimen
    specimen_path = SPECIMEN_DIR / f"{specimen_name}.java"
    _run_cmd("javac", "-d", str(WORKING_DIR), str(specimen_path))

    # run monitor agent on it
    agent = INSTALL_DIR / "agent.jar"
    bootstrap = INSTALL_DIR / "bootstrap.jar"
    native = INSTALL_DIR / "libagent.so"
    main = f"specimens.{specimen_name}"
    _run_cmd(
        "java",
        f"-javaagent:{agent}={bootstrap},specimens",
        f"-agentpath:{native}={out_file}",
        "-cp", str(WORKING_DIR),
        "-Xms25m",
        "-Xmx25m",
        main
    )


def read_messages(f: BinaryIO) -> Generator[Variant, None, None]:
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

        msg = Variant()
        msg.ParseFromString(msg_raw)
        yield msg


class MonitorTest(unittest.TestCase):
    LOG_PATH: Path = WORKING_DIR / "definitions.log"
    MESSAGES: List[Variant]

    maxDiff = None

    @classmethod
    def setUpClass(cls):
        run_specimen("FullTest", cls.LOG_PATH)
        if not cls.LOG_PATH.exists():
            raise RuntimeError("Specimen didn't run")

        with io.open(str(cls.LOG_PATH), 'rb') as f:
            cls.MESSAGES = list(read_messages(f))

    @classmethod
    def tearDownClass(cls):
        cls.MESSAGES.clear()
        _delete_working_dir()

    def filter_messages(self, *types: MessageType, methods: List[str] = None) -> List[Message]:
        def gen_messages():
            callstack = []
            # for variant in filter(lambda m: m.type in types, self.MESSAGES):
            for variant in self.MESSAGES:
                if variant.type == message_pb2.METHOD_ENTER:
                    callstack.append(variant.method_enter.method)
                elif variant.type == message_pb2.METHOD_EXIT:
                    callstack.pop()

                if variant.type not in types:
                    continue

                if methods:
                    if not callstack:
                        continue

                    if callstack[-1] not in methods:
                        continue

                which = variant.WhichOneof("payload")
                self.assertIsNotNone(which)
                payload = getattr(variant, which)
                self.assertIsNotNone(payload)
                yield MessageToDict(payload, including_default_value_fields=True)

        return list(gen_messages())

    def test_definitions(self):
        messages = self.filter_messages(message_pb2.CLASS_DEF)
        self.assertEqual(len(messages), 1)
        oracle = {'name': 'specimens.SimpleTest',
                  'classType': 'class', 'visibility': 'package', 'superClass': 'java.lang.Object',
                  'fields': [{'name': 'anInt', 'type': 'I', 'visibility': 'private', 'static': True},
                             {'name': 'aString', 'type': 'Ljava/lang/String;', 'visibility': 'private', 'static': False},
                             {'name': 'anObject', 'type': 'Ljava/lang/Object;', 'visibility': 'public', 'static': False}
                             ],
                  'methods': [
                      {'name': '<init>', 'signature': '()V', 'visibility': 'package', 'static': False, 'localVars': []},
                      {'name': 'a', 'signature': '()V', 'visibility': 'private', 'static': False, 'localVars': []},
                      {'name': 'b', 'signature': '(I)I', 'visibility': 'private', 'static': False, 'localVars': []},
                      {'name': 'c', 'signature': '(Ljava/lang/String;Ljava/lang/Long;[[I)Ljava/lang/String;', 'visibility': 'private', 'static': False, 'localVars': []},
                      {'name': 'd', 'signature': '()V', 'visibility': 'public', 'static': True, 'localVars': []},
                      {'name': 'main', 'signature': '([Ljava/lang/String;)V', 'visibility': 'public', 'static': True, 'localVars': []}
                  ],
                  'interfaces': []
                  }

        # self.assertEqual(messages[0], oracle)

    def test_callstack(self):
        messages = self.filter_messages(message_pb2.METHOD_ENTER, message_pb2.METHOD_EXIT)
        oracle = [
            {'class': 'specimens.SimpleTest', 'method': 'main'}, {'class': 'specimens.SimpleTest', 'method': 'd'},
            {},
            {'class': 'specimens.SimpleTest', 'method': '<init>'},
            {},
            {'class': 'specimens.SimpleTest', 'method': 'c'}, {'class': 'specimens.SimpleTest', 'method': 'b'},
            {'class': 'specimens.SimpleTest', 'method': 'a'},
            {},
            {},
            {},
            {}
        ]

        # self.assertEqual(messages, oracle)

    def test_deallocations(self):
        messages = self.filter_messages(message_pb2.DEALLOC)
        print(messages)
