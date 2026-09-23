"""Run smoke-script tests with dependency-free JUnit XML and HTML reporting."""
import html
from pathlib import Path
import sys
import time
import unittest
import xml.etree.ElementTree as ET


class ReportResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.records = []
        self.started = time.monotonic()

    def startTest(self, test):
        self.started = time.monotonic()
        super().startTest(test)

    def record(self, test, status, detail=''):
        self.records.append((test.id(), status, detail, time.monotonic() - self.started))

    def addSuccess(self, test):
        super().addSuccess(test)
        self.record(test, 'passed')

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self.record(test, 'failure', self.failures[-1][1])

    def addError(self, test, err):
        super().addError(test, err)
        self.record(test, 'error', self.errors[-1][1])

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        self.record(test, 'skipped', reason)


def main():
    xml_dir, html_dir = map(Path, sys.argv[1:])
    xml_dir.mkdir(parents=True, exist_ok=True)
    html_dir.mkdir(parents=True, exist_ok=True)
    suite = unittest.defaultTestLoader.discover(str(Path(__file__).parent), 'test_logstash_smoke.py')
    result = unittest.TextTestRunner(verbosity=2, resultclass=ReportResult).run(suite)
    report = ET.Element('testsuite', name='LogstashSmokeScriptTests', tests=str(len(result.records)),
                        failures=str(len(result.failures)), errors=str(len(result.errors)),
                        skipped=str(len(result.skipped)),
                        time=f'{sum(row[3] for row in result.records):.3f}')
    rows = []
    for identifier, status, detail, elapsed in result.records:
        classname, _, name = identifier.rpartition('.')
        case = ET.SubElement(report, 'testcase', classname=classname, name=name, time=f'{elapsed:.3f}')
        if status != 'passed':
            ET.SubElement(case, status).text = detail
        rows.append(f'<tr><td>{html.escape(identifier)}</td><td>{status}</td>'
                    f'<td>{elapsed:.3f}s</td><td><pre>{html.escape(detail)}</pre></td></tr>')
    ET.ElementTree(report).write(xml_dir / 'TEST-logstash-smoke.xml', encoding='utf-8', xml_declaration=True)
    summary = (f'{len(result.records)} tests; {len(result.failures)} failures; '
               f'{len(result.errors)} errors; {len(result.skipped)} skipped')
    (html_dir / 'index.html').write_text(
        '<!doctype html><html lang="en"><meta charset="utf-8">'
        '<title>Logstash smoke-script tests</title><h1>Logstash smoke-script tests</h1>'
        f'<p>{summary}</p><table><tr><th>Test</th><th>Result</th><th>Time</th><th>Details</th></tr>'
        + ''.join(rows) + '</table></html>', encoding='utf-8')
    return 0 if result.wasSuccessful() and result.testsRun > 0 and not result.skipped else 1


if __name__ == '__main__':
    sys.exit(main())
