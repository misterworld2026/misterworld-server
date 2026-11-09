from typing import Any

from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from starlette.concurrency import run_in_threadpool

from app.stt import MAX_AUDIO_BYTES, TranscriptionError, transcribe_audio


app = FastAPI(title="Mr. World Voice Service")


class InterpretRequest(BaseModel):
    text: str
    context: dict[str, Any]
    candidates: dict[str, Any]


def not_implemented(feature: str) -> JSONResponse:
    return JSONResponse(
        status_code=501,
        content={
            "error": {
                "code": "NOT_IMPLEMENTED",
                "message": f"{feature} is not implemented yet.",
            }
        },
    )


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/internal/v1/transcribe")
async def transcribe(audio: UploadFile | None = File(default=None)) -> JSONResponse:
    if audio is None:
        return JSONResponse(
            status_code=400,
            content={"error": {"code": "EMPTY_AUDIO", "message": "Audio file is required."}},
        )
    data = await audio.read(MAX_AUDIO_BYTES + 1)
    try:
        text = await run_in_threadpool(transcribe_audio, data, audio.content_type)
    except TranscriptionError as exc:
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": {"code": exc.code, "message": exc.message}},
        )
    finally:
        await audio.close()
    return JSONResponse(content={"text": text})


@app.post("/internal/v1/interpret")
def interpret(request: InterpretRequest) -> JSONResponse:
    return not_implemented("Interpretation")
