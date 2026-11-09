from typing import Any

from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel


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
async def transcribe(audio: UploadFile = File(...)) -> JSONResponse:
    return not_implemented("Transcription")


@app.post("/internal/v1/interpret")
def interpret(request: InterpretRequest) -> JSONResponse:
    return not_implemented("Interpretation")
