import getpass
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


def _get_env(name: str) -> str:
    var = os.getenv(name)
    if not var:
        print(f"Missing env var ${name}")
        sys.exit(1)
    return var


INSTALL_DIR: Path = Path(_get_env("JVMEMORY_DIR"))
TESTS_DIR: Path = Path(_get_env("JVMEMORY_TEST_DIR"))

WORKING_DIR: Path = Path(tempfile.gettempdir()) / f"jvmemory-tests-{getpass.getuser()}"
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


class MonitorTest(unittest.TestCase):
    LOG_PATH = WORKING_DIR / "definitions.log"

    @classmethod
    def setUpClass(cls):
        run_specimen("TestDefinitions", MonitorTest.LOG_PATH)
        if not MonitorTest.LOG_PATH.exists():
            raise RuntimeError("Specimen didn't run")

    @classmethod
    def tearDownClass(cls):
        _delete_working_dir()

    def test_test(self):
        self.assertTrue(True)
