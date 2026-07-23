from __future__ import annotations

import json
import urllib.request
from pathlib import Path

HEADERS = {
    "Accept": "application/vnd.github+json",
    "User-Agent": "SOV-read-only-actions-inspector",
    "X-GitHub-Api-Version": "2022-11-28",
}
RUNS_URL = "https://api.github.com/repos/darkoj-create/SOV---WEB/actions/runs?branch=fix%2Fhome-mobile-runtime-visual&per_page=10"

# Refresh after the explicit gate-diagnosis run.
request = urllib.request.Request(RUNS_URL, headers=HEADERS)
with urllib.request.urlopen(request, timeout=60) as response:
    payload = json.load(response)

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
print(json.dumps(runs, ensure_ascii=False, indent=2))
