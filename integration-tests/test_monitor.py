import io
import os
import shutil
import subprocess
import sys
import unittest
from pathlib import Path
from typing import BinaryIO, Generator, List

from google.protobuf.json_format import MessageToDict

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

    @classmethod
    def setUpClass(cls):
        run_specimen("SimpleTest", cls.LOG_PATH)
        if not cls.LOG_PATH.exists():
            raise RuntimeError("Specimen didn't run")

        with io.open(str(cls.LOG_PATH), 'rb') as f:
            cls.MESSAGES = list(read_messages(f))

    @classmethod
    def tearDownClass(cls):
        cls.MESSAGES.clear()
        _delete_working_dir()

    def filter_messages(self, *types: MessageType) -> Generator[Variant, None, None]:
        yield from filter(lambda m: m.type in types, self.MESSAGES)

    def test_definitions(self):
        messages = self.filter_messages(message_pb2.CLASS_DEF)
        msg: Variant = next(messages)

        # just one
        with self.assertRaises(StopIteration):
            next(messages)

        definition = msg.class_def
        self.assertIsNotNone(definition)

        oracle = {
            'name': 'specimens.SimpleTest',
            'classType': 'class',
            'visibility': 'package',
            'superClass': 'java.lang.Object',
            'fields': [
                {'name': 'anInt', 'type': 'I', 'visibility': 'private', 'static': True},
                {'name': 'aString', 'type': 'Ljava/lang/String;', 'visibility': 'private'},
                {'name': 'anObject', 'type': 'Ljava/lang/Object;', 'visibility': 'public'}
            ],
            'methods': [
                {'name': '<init>', 'signature': '()V', 'visibility': 'package'},
                {'name': 'a', 'signature': '()V', 'visibility': 'private'},
                {'name': 'b', 'signature': '(I)I', 'visibility': 'private'},
                {'name': 'c',
                 'signature': '(Ljava/lang/String;Ljava/lang/Long;[[I)Ljava/lang/String;',
                 'visibility': 'private'
                 },
                {'name': 'd', 'signature': '()V', 'visibility': 'public', 'static': True},
                {'name': 'main', 'signature': '([Ljava/lang/String;)V', 'visibility': 'public',
                 'static': True}
            ]
        }
        self.assertEqual(MessageToDict(definition), oracle)
