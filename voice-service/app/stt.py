import logging
import os
from functools import lru_cache
from io import BytesIO
from pathlib import Path

logger = logging.getLogger(__name__)

SAMPLE_RATE = 16_000
MAX_AUDIO_BYTES = 10 * 1024 * 1024
MAX_AUDIO_SECONDS = 30
SUPPORTED_MEDIA_TYPES = {"audio/webm", "audio/mp4", "audio/wav"}
MODEL_DIR = Path(__file__).resolve().parents[1] / "models"
# Hugging Face reads this setting during import; keep its cache inside the ignored model directory.
os.environ.setdefault("HF_HOME", str(MODEL_DIR / ".hf_cache"))

from faster_whisper import WhisperModel, decode_audio  # noqa: E402


class TranscriptionError(Exception):
    def __init__(self, status_code: int, code: str, message: str) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.code = code
        self.message = message


@lru_cache(maxsize=1)
def get_model() -> WhisperModel:
    return WhisperModel(
        os.getenv("VOICE_STT_MODEL", "small"),
        device=os.getenv("VOICE_STT_DEVICE", "cpu"),
        compute_type=os.getenv("VOICE_STT_COMPUTE_TYPE", "int8"),
        download_root=str(MODEL_DIR),
    )


def transcribe_audio(data: bytes, content_type: str | None) -> str:
    if not data:
        raise TranscriptionError(400, "EMPTY_AUDIO", "Audio file is empty.")
    if len(data) > MAX_AUDIO_BYTES:
        raise TranscriptionError(413, "AUDIO_TOO_LARGE", "Audio file exceeds 10 MiB.")

    media_type = (content_type or "").split(";", 1)[0].strip().lower()
    if media_type not in SUPPORTED_MEDIA_TYPES:
        raise TranscriptionError(415, "UNSUPPORTED_AUDIO", "Unsupported audio format.")

    try:
        waveform = decode_audio(BytesIO(data), sampling_rate=SAMPLE_RATE)
    except Exception as exc:
        raise TranscriptionError(415, "INVALID_AUDIO", "Audio file could not be decoded.") from exc

    if waveform.size == 0:
        raise TranscriptionError(422, "NO_SPEECH", "No speech was recognized.")
    if waveform.size / SAMPLE_RATE > MAX_AUDIO_SECONDS:
        raise TranscriptionError(413, "AUDIO_TOO_LONG", "Audio exceeds 30 seconds.")

    try:
        segments, _ = get_model().transcribe(
            waveform,
            language="ko",
            beam_size=5,
            vad_filter=True,
            condition_on_previous_text=False,
            hotwords=os.getenv(
                "VOICE_STT_HOTWORDS",
                "허니문, 효도 여행, 골프 여행, 트레킹 여행",
            ),
        )
        text = " ".join(segment.text.strip() for segment in segments).strip()
    except Exception as exc:
        logger.exception("Speech transcription failed")
        raise TranscriptionError(503, "TRANSCRIPTION_FAILED", "Speech transcription failed.") from exc

    if not text:
        raise TranscriptionError(422, "NO_SPEECH", "No speech was recognized.")
    return text
