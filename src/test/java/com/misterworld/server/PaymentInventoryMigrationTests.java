package com.misterworld.server;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentInventoryMigrationTests {
    @Test
    void convertsLegacyStockForPaidBookingsAndReleasesUnpaidStock() throws Exception {
        String url = "jdbc:h2:mem:payment_" + UUID.randomUUID()
            + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("13").load().migrate();

        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("""
                INSERT INTO `회원` (`회원_ID`, `이메일`, `비밀번호`, `이름`, `주소`, `연락처`, Field, `회원구분`)
                VALUES (1, 'legacy@example.test', 'fixture', '고객', '주소', '01012345678', 0, 'MEMBER')
                """);
            sql.execute("""
                INSERT INTO `여행상품` (`여행상품_ID`, `상품명`, `테마`, `그랜드_가격`, `프리미엄_가격`)
                VALUES (1, '골프', '골프', 200, 300)
                """);
            sql.execute("""
                INSERT INTO `여행일정` (`여행일정_ID`, `여행상품_ID`, `출발일`, `종료일`, `여행상태`, `현재신청인원`)
                VALUES (1, 1, '2030-01-01', '2030-01-03', 'CONFIRMED_SCHEDULE', 3)
                """);
            sql.execute("""
                INSERT INTO `여행신청` (`여행신청_ID`, `회원_ID`, `여행일정_ID`, `투어등급`, `호텔옵션`, `교통옵션`,
                    `식사옵션`, `신청인원`, `기본가격`, `옵션추가금액`, `할인율`, `할인금액`, `최종가격`,
                    `신청상태`, `신청일시`, `결제상태`, `결제금액`, `결제완료일시`)
                VALUES (1, 1, 1, 'GRAND', '호텔', '교통', '식사', 2, 400, 0, 0, 0, 400,
                    'CONFIRMED', CURRENT_TIMESTAMP, 'PAID', 400, CURRENT_TIMESTAMP),
                    (2, 1, 1, 'GRAND', '호텔', '교통', '식사', 1, 200, 0, 0, 0, 200,
                    'CONFIRMED', CURRENT_TIMESTAMP, 'UNPAID', NULL, NULL)
                """);
            sql.execute("""
                INSERT INTO `물품재고` (`물품재고_ID`, `여행상품_ID`, `물품명`, `재고수량`, `재고상태`, `수정일시`)
                VALUES (1, 1, '기념품', 4, 'LOW_STOCK', CURRENT_TIMESTAMP)
                """);
            sql.execute("""
                INSERT INTO `재고차감내역` (`여행일정_ID`, `물품재고_ID`, `차감수량`) VALUES (1, 1, 3)
                """);
            Flyway.configure().dataSource(url, "sa", "").load().migrate();

            try (var rows = sql.executeQuery("SELECT `현재신청인원` FROM `여행일정`")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(2);
            }
            try (var rows = sql.executeQuery("SELECT `재고수량`, `재고상태` FROM `물품재고`")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(5);
                assertThat(rows.getString(2)).isEqualTo("NORMAL");
            }
            try (var rows = sql.executeQuery("""
                SELECT `여행신청_ID`, `차감수량` FROM `재고차감내역` WHERE `복구일시` IS NULL
                """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(1);
                assertThat(rows.getInt(2)).isEqualTo(2);
                assertThat(rows.next()).isFalse();
            }
        }
    }
}
