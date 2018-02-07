import io
import os
import shutil
import subprocess
import sys
import typing
import unittest
from pathlib import Path

from pb import message_pb2


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


def read_messages(f: typing.BinaryIO) -> typing.Generator[message_pb2.Variant, None, None]:
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


class MonitorTest(unittest.TestCase):
    LOG_PATH: Path = WORKING_DIR / "definitions.log"
    MESSAGES: typing.List[message_pb2.Variant]

    @classmethod
    def setUpClass(cls):
        run_specimen("TestDefinitions", cls.LOG_PATH)
        if not cls.LOG_PATH.exists():
            raise RuntimeError("Specimen didn't run")

        with io.open(str(cls.LOG_PATH), 'rb') as f:
            cls.MESSAGES = list(read_messages(f))

    @classmethod
    def tearDownClass(cls):
        cls.MESSAGES.clear()
        _delete_working_dir()

    def test_test(self):
        self.assertTrue(True)
