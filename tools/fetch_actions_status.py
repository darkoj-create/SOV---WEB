from __future__ import annotations

import io
import json
import urllib.request
import zipfile
from pathlib import Path

HEADERS = {
    "Accept": "application/vnd.github+json",
    "User-Agent": "SOV-read-only-actions-inspector",
    "X-GitHub-Api-Version": "2022-11-28",
}
RUNS_URL = "https://api.github.com/repos/darkoj-create/SOV---WEB/actions/runs?branch=fix%2Fpublic-human-pass&per_page=20"
JOB_LOG_URL = "https://api.github.com/repos/darkoj-create/SOV---WEB/actions/jobs/89161193900/logs"


def fetch_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


# Refresh snapshot after the debug-enabled workflow trigger.
payload = json.loads(fetch_bytes(RUNS_URL).decode("utf-8"))
runs = []
for run in payload.get("workflow_runs", []):
    runs.append({
        "id": run.get("id"),
        "name": run.get("name"),
        "event": run.get("event"),
        "status": run.get("status"),
        "conclusion": run.get("conclusion"),
        "head_sha": run.get("head_sha"),
        "run_number": run.get("run_number"),
        "created_at": run.get("created_at"),
        "updated_at": run.get("updated_at"),
        "jobs_url": run.get("jobs_url"),
        "logs_url": run.get("logs_url"),
        "html_url": run.get("html_url"),
    })
Path("actions-runs.json").write_text(json.dumps(runs, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(runs[:5], ensure_ascii=False, indent=2))

try:
    raw = fetch_bytes(JOB_LOG_URL)
    if raw.startswith(b"PK"):
        chunks: list[str] = []
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            for name in archive.namelist():
                chunks.append(archive.read(name).decode("utf-8", errors="replace"))
        log_text = "\n".join(chunks)
    else:
        log_text = raw.decode("utf-8", errors="replace")
    tail = "\n".join(log_text.splitlines()[-140:])
except Exception as exc:
    tail = f"LOG_FETCH_FAILED: {type(exc).__name__}: {exc}"

Path("human-pass-job-tail.txt").write_text(tail, encoding="utf-8")
print("\nJOB LOG TAIL\n" + tail)
