import json
import unittest

from app.interpreter import parse_interpretation


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


if __name__ == "__main__":
    unittest.main()
