"""Run the text interpretation benchmark with the real Qwen3 model.

From voice-service: .venv/bin/python -m evaluation.evaluate
"""

import argparse
import json
import re
import time
from pathlib import Path

from app.interpreter import InterpretRequest, interpret_text


FIELDS = ("tour", "schedule", "grade", "hotel", "transport", "meal")
CASES = Path(__file__).with_name("cases.json")
CANDIDATES = {
    "products": [
        {"name": "푸껫 허니문 5박 7일"},
        {"name": "다낭 효도·휴양 3박 5일"},
        {"name": "홋카이도 골프 3박 4일"},
        {"name": "호도협 트레킹 4박 5일"},
    ],
    "options": [
        {"name": name} for name in (
            "오션뷰 객실 업그레이드", "커플 스파 1회 추가", "전용 차량 이동",
            "해산물 특식 1회 추가", "골프 18홀 추가", "골프 클럽 렌탈",
            "트레킹 장비 대여", "현지 특식 1회 추가",
        )
    ],
    "schedules": [],
}


def normalized(value: str | None) -> str | None:
    return None if value is None else re.sub(r"[\s·,]+", "", value).lower()


def accepted(actual: str | None, expected: str | list[str]) -> bool:
    alternatives = expected if isinstance(expected, list) else [expected]
    return normalized(actual) in {normalized(value) for value in alternatives}


def assess(case: dict, fields: dict) -> list[str]:
    mismatches = []
    resolved = case.get("resolved", {})
    unresolved = set(case.get("unresolved", []))
    for name in FIELDS:
        expected_state = "RESOLVED" if name in resolved else "UNRESOLVED" if name in unresolved else "NOT_MENTIONED"
        actual = fields[name]
        if actual["state"] != expected_state:
            mismatches.append(f"{name}: state {actual['state']} != {expected_state}")
        elif expected_state == "RESOLVED" and not accepted(actual["value"], resolved[name]):
            mismatches.append(f"{name}: value {actual['value']!r} != {resolved[name]!r}")
    expected_extras = case.get("extras", [])
    remaining = [v["value"] for v in fields["extras"] if v["state"] == "RESOLVED"]
    extras_match = len(remaining) == len(expected_extras) == len(fields["extras"])
    for expected in expected_extras:
        match = next((i for i, actual in enumerate(remaining) if accepted(actual, expected)), None)
        if match is None:
            extras_match = False
        else:
            remaining.pop(match)
    if not extras_match:
        mismatches.append(f"extras: {fields['extras']!r} != {case.get('extras', [])!r}")
    return mismatches


def main() -> None:
    parser = argparse.ArgumentParser(description="Measure exact utterance-level interpretation accuracy")
    parser.add_argument("--cases", type=Path, default=CASES)
    parser.add_argument("--output", type=Path, help="Write a JSON report to this file")
    args = parser.parse_args()
    cases = json.loads(args.cases.read_text(encoding="utf-8"))
    results = []
    started = time.perf_counter()
    for case in cases:
        case_start = time.perf_counter()
        try:
            response = interpret_text(InterpretRequest(text=case["text"], context={}, candidates=CANDIDATES))
            fields = response.model_dump()["fields"]
            mismatches = assess(case, fields)
        except Exception as exc:
            mismatches = [f"request failed: {type(exc).__name__}"]
            fields = None
        results.append({"id": case["id"], "passed": not mismatches, "seconds": round(time.perf_counter() - case_start, 2),
                        "mismatches": mismatches, "fields": fields})
        print(f"{'PASS' if not mismatches else 'FAIL'} {case['id']} ({results[-1]['seconds']}s)", flush=True)
        for issue in mismatches:
            print(f"  {issue}", flush=True)
    passed = sum(item["passed"] for item in results)
    summary = {"cases": len(cases), "passed": passed, "accuracy": round(passed / len(cases), 4),
               "target": 0.8, "target_met": passed / len(cases) >= 0.8,
               "seconds": round(time.perf_counter() - started, 2), "scope": "text interpretation only"}
    print(json.dumps(summary, ensure_ascii=False, indent=2), flush=True)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps({"summary": summary, "results": results}, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
