import io
import os
import shutil
import subprocess
import sys
import unittest
from pathlib import Path
from typing import BinaryIO, Generator, List, Tuple

from google.protobuf.json_format import MessageToDict
from google.protobuf.message import Message

import oracles
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
    LOG_PATH: Path = WORKING_DIR / "monitor_output.log"
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

    def filter_messages(self, *types: MessageType, method: str = None) -> List[Tuple[str, Message]]:
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

                if method:
                    if method not in callstack:
                        continue

                which = variant.WhichOneof("payload")
                self.assertIsNotNone(which)
                payload = getattr(variant, which)
                self.assertIsNotNone(payload)
                yield which, MessageToDict(payload, including_default_value_fields=True)

        return list(gen_messages())

    def test_definitions(self):
        msgs = self.filter_messages(message_pb2.CLASS_DEF)
        self.assertEqual(len(msgs), 1)

        self.assertEqual(msgs[0], oracles.definitions)

    def test_allocations(self):
        msgs = self.filter_messages(
            message_pb2.ALLOC_OBJECT, message_pb2.ALLOC_ARRAY,
            method="testAllocations"
        )
        self.assertEqual(msgs, oracles.allocations)

    def test_deallocations(self):
        msgs = self.filter_messages(message_pb2.DEALLOC)
        # ensure at least some dealloc events were emitted
        self.assertGreater(len(msgs), 16)

    def test_callstack(self):
        msgs = self.filter_messages(message_pb2.METHOD_ENTER, message_pb2.METHOD_EXIT, method="testMethodCalls")
        self.assertEqual(msgs, oracles.callstack)

    def test_getfield(self):
        msgs = self.filter_messages(
            message_pb2.GETFIELD,
            method="testGetField"
        )
        self.assertEqual(msgs, oracles.getfield)

    def test_putfield(self):
        msgs = self.filter_messages(
            message_pb2.PUTFIELD_PRIMITIVE, message_pb2.PUTFIELD_OBJECT,
            method="<init>"
        )
        self.assertEqual(msgs, oracles.putfield)

    def test_local_vars(self):
        msgs = self.filter_messages(
            message_pb2.LOAD, message_pb2.STORE_OBJECT, message_pb2.STORE_PRIMITIVE,
            method="testLocalVars")
        self.assertEqual(msgs, oracles.local_vars)

    def test_arrays(self):
        msgs = self.filter_messages(
            message_pb2.LOAD_ARRAY, message_pb2.STORE_OBJECT_IN_ARRAY, message_pb2.STORE_PRIMITIVE_IN_ARRAY,
            method="testArrays")
        self.assertEqual(msgs, oracles.arrays)
