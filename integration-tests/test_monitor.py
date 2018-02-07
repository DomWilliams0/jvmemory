import unittest


class MonitorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        print("set up")

    @classmethod
    def tearDownClass(cls):
        print("tear down")

    def test_test(self):
        self.assertTrue(True)
