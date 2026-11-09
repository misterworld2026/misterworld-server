from fastapi import FastAPI, File, UploadFile
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.concurrency import run_in_threadpool

from app.interpreter import InterpretRequest, InterpretationError, interpret_text
from app.stt import MAX_AUDIO_BYTES, TranscriptionError, transcribe_audio


app = FastAPI(title="Mr. World Voice Service")


@app.exception_handler(RequestValidationError)
async def validation_error(_request, _exc: RequestValidationError) -> JSONResponse:
    return JSONResponse(
        status_code=400,
        content={"error": {"code": "INVALID_REQUEST", "message": "Invalid request."}},
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
async def interpret(request: InterpretRequest) -> JSONResponse:
    try:
        result = await run_in_threadpool(interpret_text, request)
    except InterpretationError as exc:
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": {"code": exc.code, "message": exc.message}},
        )
    return JSONResponse(content=result.model_dump())
