import json
import os
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from app.interpreter import InterpretRequest, generate_interpretation, parse_interpretation


class InterpretOutputTest(unittest.TestCase):
    def parse(self, text: str, **overrides: object) -> dict:
        raw = {
            "tour": None,
            "schedule": None,
            "grade": None,
            "hotel": None,
            "transport": None,
            "meal": None,
            "extras": [],
            **overrides,
        }
        return parse_interpretation(json.dumps(raw, ensure_ascii=False) + ">", text).model_dump()["fields"]

    def test_omitted_fields_and_hallucinated_values_are_not_resolved(self) -> None:
        fields = self.parse(
            "일정은 그때로 하고 식사는 해산물 특식, 차량은 전용 차량으로",
            tour="푸껫 허니문",
            schedule="3월 1일",
            grade="GRAND",
            hotel="?",
            transport="전용 차량 이동",
            meal="해산물 특식",
            extras=["골프 18홀 추가"],
        )
        self.assertEqual(fields["tour"], {"state": "NOT_MENTIONED", "value": None})
        self.assertEqual(fields["schedule"], {"state": "UNRESOLVED", "value": None})
        self.assertEqual(fields["grade"], {"state": "NOT_MENTIONED", "value": None})
        self.assertEqual(fields["hotel"], {"state": "NOT_MENTIONED", "value": None})
        self.assertEqual(fields["transport"], {"state": "RESOLVED", "value": "전용 차량"})
        self.assertEqual(fields["meal"], {"state": "RESOLVED", "value": "해산물 특식"})
        self.assertEqual(fields["extras"], [])

    def test_spoken_date_and_theme_override_model_guess(self) -> None:
        fields = self.parse(
            "골프 여행에 4월 26일 출발 프리미엄, 골프 18홀 추가",
            tour="푸껫 허니문",
            schedule="2027-04-26",
            grade="PREMIUM",
            extras=["골프 18홀 추가"],
        )
        self.assertEqual(fields["tour"], {"state": "RESOLVED", "value": "골프 여행"})
        self.assertEqual(fields["schedule"], {"state": "RESOLVED", "value": "4월 26일"})
        self.assertEqual(fields["grade"], {"state": "RESOLVED", "value": "PREMIUM"})
        self.assertEqual(fields["extras"], [{"state": "RESOLVED", "value": "골프 18홀 추가"}])

    def test_option_words_do_not_turn_trekking_equipment_into_a_tour(self) -> None:
        fields = self.parse(
            "트레킹 장비 대여 추가해줘",
            tour="트레킹",
            transport="트레킹 장비 대여",
            extras=["트레킹 장비 대여"],
        )
        self.assertEqual(fields["tour"], {"state": "NOT_MENTIONED", "value": None})
        self.assertEqual(fields["transport"], {"state": "NOT_MENTIONED", "value": None})
        self.assertEqual(fields["extras"], [{"state": "RESOLVED", "value": "트레킹 장비 대여"}])

    def test_extra_quote_after_json_is_ignored(self) -> None:
        raw = '{"tour":null,"schedule":null,"grade":"GRAND","hotel":null,"transport":null,"meal":null,"extras":[]}"'
        fields = parse_interpretation(raw, "그랜드로 바꿔줘").model_dump()["fields"]
        self.assertEqual(fields["grade"], {"state": "RESOLVED", "value": "GRAND"})

    def test_hotel_and_meal_are_not_duplicated_as_extras(self) -> None:
        fields = self.parse(
            "호텔은 오션뷰 객실 업그레이드, 식사는 해산물 특식 추가해줘",
            hotel="오션뷰 객실",
            meal="해산물 특식",
            extras=["오션뷰 객실 업그레이드", "해산물 특식"],
        )
        self.assertEqual(fields["hotel"], {"state": "RESOLVED", "value": "오션뷰 객실"})
        self.assertEqual(fields["meal"], {"state": "RESOLVED", "value": "해산물 특식"})
        self.assertEqual(fields["extras"], [])

    def test_groq_response_uses_existing_output_parser(self) -> None:
        raw = json.dumps({
            "tour": "푸껫 허니문 여행",
            "schedule": "3월 1일 출발",
            "grade": "그랜드",
            "hotel": "오션뷰 객실",
            "transport": None,
            "meal": None,
            "extras": [],
        }, ensure_ascii=False)
        request = InterpretRequest(
            text="푸껫 허니문 여행을 3월 1일 출발로 예약하고 싶어요. 그랜드 등급에 오션뷰 객실로 해주세요.",
            context={},
            candidates={"products": [], "schedules": [], "options": []},
        )
        response = SimpleNamespace(choices=[SimpleNamespace(message=SimpleNamespace(content=raw))])
        with patch.dict(os.environ, {"GROQ_API_KEY": "test-key"}), \
             patch("app.interpreter.Groq") as groq, \
             patch("app.interpreter.get_model") as local_model:
            groq.return_value.chat.completions.create.return_value = response
            result = parse_interpretation(generate_interpretation(request), request.text)

        local_model.assert_not_called()
        self.assertEqual(groq.return_value.chat.completions.create.call_args.kwargs["model"], "qwen/qwen3.8-27b")
        self.assertEqual(result.fields.grade.value, "GRAND")
        self.assertEqual(result.fields.schedule.value, "3월 1일")


if __name__ == "__main__":
    unittest.main()
