import json
import logging
import os
import re
from difflib import SequenceMatcher
from functools import lru_cache
from pathlib import Path
from threading import Lock
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


logger = logging.getLogger(__name__)
MODEL_DIR = Path(__file__).resolve().parents[1] / "models"
os.environ.setdefault("HF_HOME", str(MODEL_DIR / ".hf_cache"))

from transformers import AutoModelForCausalLM, AutoTokenizer  # noqa: E402
import torch  # noqa: E402


State = Literal["NOT_MENTIONED", "RESOLVED", "UNRESOLVED"]
generation_lock = Lock()


class InterpretRequest(BaseModel):
    text: str = Field(min_length=1, max_length=500)
    context: dict[str, Any]
    candidates: dict[str, Any]

    @field_validator("text")
    @classmethod
    def nonblank_text(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("Text must not be blank.")
        return value


class InterpretedField(BaseModel):
    model_config = ConfigDict(extra="forbid")

    state: State
    value: str | None

    @model_validator(mode="after")
    def normalize_value(self) -> "InterpretedField":
        if self.state == "RESOLVED":
            if not isinstance(self.value, str) or not self.value.strip():
                raise ValueError("Resolved fields require a value.")
            self.value = self.value.strip()
        else:
            self.value = None
        return self


class InterpretedFields(BaseModel):
    model_config = ConfigDict(extra="forbid")

    tour: InterpretedField
    schedule: InterpretedField
    grade: InterpretedField
    hotel: InterpretedField
    transport: InterpretedField
    meal: InterpretedField
    extras: list[InterpretedField]


class InterpretationResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    fields: InterpretedFields


class CompactOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    tour: str | None
    schedule: str | None
    grade: str | None
    hotel: str | None
    transport: str | None
    meal: str | None
    extras: list[str]


class InterpretationError(Exception):
    def __init__(self, status_code: int, code: str, message: str) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.code = code
        self.message = message


SYSTEM_PROMPT = """여행 선택 발화에서 고객이 명시적으로 말한 조건만 추출한다. JSON 객체 한 줄만 출력한다.
키는 tour, schedule, grade, hotel, transport, meal, extras다. 언급하지 않았으면 null,
항목은 언급했지만 구체적 값이 없으면 "?", extras는 문자열 배열이다.
값은 원문 속 짧은 표현을 그대로 쓴다. grade만 CLASSIC/GRAND/PREMIUM으로 정규화한다.
참고 목록과 이전 선택은 발화의 근거가 아니다. 입력 문장에 없는 상품·옵션·날짜를 만들지 마라.
원문에 연도가 없으면 날짜에 연도를 추가하지 마라. 상품 ID, 일정 ID, 옵션 ID, 가격을 출력하지 마라."""

EXAMPLE_MESSAGES = [
    {
        "role": "user",
        "content": "푸껫 허니문 3월 1일 그랜드에 오션뷰 객실로 해줘",
    },
    {
        "role": "assistant",
        "content": '{"tour":"푸껫 허니문","schedule":"3월 1일","grade":"GRAND","hotel":"오션뷰 객실","transport":null,"meal":null,"extras":[]}',
    },
    {
        "role": "user",
        "content": "호텔은 아무거나 좋은 곳으로 해줘",
    },
    {
        "role": "assistant",
        "content": '{"tour":null,"schedule":null,"grade":null,"hotel":"?","transport":null,"meal":null,"extras":[]}',
    },
]

MENTION_MARKERS = {
    "tour": ("여행", "투어", "상품", "허니문", "효도", "트레킹"),
    "schedule": ("일정", "출발", "날짜", "다음 주", "다음 달", "그때"),
    "hotel": ("호텔", "숙소", "객실", "리조트", "오션뷰"),
    "transport": ("교통", "차량", "버스", "택시", "이동", "렌터카"),
    "meal": ("식사", "음식", "식당", "조식", "중식", "석식", "특식", "해산물"),
}
GRADE_MARKERS = {
    "CLASSIC": ("클래식", "classic"),
    "GRAND": ("그랜드", "grand"),
    "PREMIUM": ("프리미엄", "premium"),
}
VAGUE_VALUES = ("아무거나", "적당한", "좋은 곳", "그때", "언젠가", "아무 때")
DATE_PATTERN = re.compile(r"(?:20\d{2}년\s*)?\d{1,2}월\s*\d{1,2}일")
TOUR_PATTERN = re.compile(r"(?:허니문|효도|트레킹)(?:\s*(?:여행|투어))?|골프\s*(?:여행|투어)")


@lru_cache(maxsize=1)
def get_model() -> tuple[Any, Any, str]:
    model_name = os.getenv("VOICE_QWEN_MODEL", "Qwen/Qwen3-1.7B")
    device = os.getenv("VOICE_QWEN_DEVICE", "auto")
    if device == "auto":
        device = "cuda" if torch.cuda.is_available() else (
            "mps" if torch.backends.mps.is_available() else "cpu"
        )
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    model = AutoModelForCausalLM.from_pretrained(model_name, dtype="auto").to(device)
    model.eval()
    return model, tokenizer, device


def candidate_hints(candidates: dict[str, Any]) -> str:
    hints: list[str] = []
    for key in ("products", "options"):
        entries = candidates.get(key, [])
        if not isinstance(entries, list):
            continue
        names = [
            entry["name"][:80]
            for entry in entries[:20]
            if isinstance(entry, dict) and isinstance(entry.get("name"), str)
        ]
        if names:
            hints.append(f"{key}: {', '.join(names)}")
    return "\n참고 가능한 이름(발화에 없는 값은 쓰지 말 것): " + " / ".join(hints) if hints else ""


def generate_interpretation(request: InterpretRequest) -> str:
    model, tokenizer, device = get_model()
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT + candidate_hints(request.candidates)},
        *EXAMPLE_MESSAGES,
        {"role": "user", "content": request.text},
    ]
    prompt = tokenizer.apply_chat_template(
        messages,
        tokenize=False,
        add_generation_prompt=True,
        enable_thinking=False,
    )
    inputs = tokenizer(prompt, return_tensors="pt").to(device)
    with generation_lock, torch.inference_mode():
        generated = model.generate(
            **inputs,
            max_new_tokens=180,
            do_sample=False,
            pad_token_id=tokenizer.eos_token_id,
        )
    output = generated[0][inputs.input_ids.shape[-1]:]
    return tokenizer.decode(output, skip_special_tokens=True).strip()


def _mentioned(name: str, text: str) -> bool:
    if name == "schedule" and DATE_PATTERN.search(text):
        return True
    return any(marker in text for marker in MENTION_MARKERS[name])


def _spoken_value(value: str, text: str) -> str | None:
    if re.sub(r"\s+", "", value).lower() in re.sub(r"\s+", "", text).lower():
        return value
    match = SequenceMatcher(None, value, text).find_longest_match()
    shared = text[match.b:match.b + match.size].strip()
    if len(shared) >= 4 and len(shared) / len(value) >= 0.45:
        return shared
    return None


def _field(value: str | None, name: str, text: str) -> InterpretedField:
    if name == "grade":
        mentioned = [
            grade for grade, markers in GRADE_MARKERS.items()
            if any(marker in text.lower() for marker in markers)
        ]
        if not mentioned:
            return InterpretedField(state="NOT_MENTIONED", value=None)
        if isinstance(value, str) and value.upper() in mentioned:
            return InterpretedField(state="RESOLVED", value=value.upper())
        if len(mentioned) == 1:
            return InterpretedField(state="RESOLVED", value=mentioned[0])
        return InterpretedField(state="UNRESOLVED", value=None)

    if name == "tour" and (spoken_tour := TOUR_PATTERN.search(text)):
        if value is None or value == "?" or _spoken_value(value, text) is None:
            return InterpretedField(state="RESOLVED", value=spoken_tour.group())
    if name == "schedule" and (spoken_date := DATE_PATTERN.search(text)):
        if value is None or value == "?":
            return InterpretedField(state="RESOLVED", value=spoken_date.group())

    mentioned = _mentioned(name, text)
    if value is None or value == "?":
        return InterpretedField(state="UNRESOLVED" if mentioned else "NOT_MENTIONED", value=None)
    if any(vague in value for vague in VAGUE_VALUES):
        return InterpretedField(state="UNRESOLVED", value=None)

    if name == "schedule" and (spoken_date := DATE_PATTERN.search(text)):
        return InterpretedField(state="RESOLVED", value=spoken_date.group())
    spoken_value = _spoken_value(value, text)
    if spoken_value is None:
        return InterpretedField(state="UNRESOLVED" if mentioned else "NOT_MENTIONED", value=None)
    return InterpretedField(state="RESOLVED", value=spoken_value)


def parse_interpretation(raw: str, text: str) -> InterpretationResult:
    clean = re.sub(r"^\s*```(?:json)?\s*|\s*```\s*$", "", raw.strip())
    decoded, index = json.JSONDecoder().raw_decode(clean)
    if re.fullmatch(r"[\s}>]*", clean[index:]) is None:
        raise ValueError("Unexpected model output after JSON.")
    compact = CompactOutput.model_validate(decoded)
    fields: dict[str, Any] = {
        name: _field(getattr(compact, name), name, text)
        for name in ("tour", "schedule", "grade", "hotel", "transport", "meal")
    }
    fields["extras"] = []
    for value in compact.extras:
        spoken_value = _spoken_value(value, text) if value else None
        if spoken_value:
            fields["extras"].append(InterpretedField(state="RESOLVED", value=spoken_value))
        elif value == "?" and "추가" in text:
            fields["extras"].append(InterpretedField(state="UNRESOLVED", value=None))
    return InterpretationResult(fields=InterpretedFields(**fields))


def interpret_text(request: InterpretRequest) -> InterpretationResult:
    try:
        raw = generate_interpretation(request)
    except Exception as exc:
        logger.exception("Qwen3 interpretation failed")
        raise InterpretationError(503, "INTERPRETATION_FAILED", "Interpretation failed.") from exc

    try:
        return parse_interpretation(raw, request.text)
    except Exception as exc:
        logger.warning("Qwen3 returned an invalid interpretation")
        raise InterpretationError(502, "INVALID_MODEL_OUTPUT", "Invalid interpretation result.") from exc
