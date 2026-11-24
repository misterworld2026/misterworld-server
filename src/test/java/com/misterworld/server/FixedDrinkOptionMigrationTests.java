package com.misterworld.server;

import com.misterworld.server.domain.TravelProduct;
import com.misterworld.server.repository.TravelProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@org.springframework.test.context.TestPropertySource(properties = "app.sms.dispatch-enabled=false")
@Transactional
class FixedDrinkOptionMigrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TravelProductRepository products;

    @Test
    void addsOneFixedCoffeeAndChampagneOptionToExistingHoneymoonProduct() {
        var product = new TravelProduct();
        product.setName("허니문 낭만 투어");
        product.setTheme("허니문");
        product.setGrandPrice(100);
        product.setPremiumPrice(200);
        products.saveAndFlush(product);

        runMigration();
        runMigration();

        var rows = jdbc.queryForList("""
            SELECT `옵션명`, `허용투어등급`, `부가혜택코드`
            FROM `여행옵션` WHERE `여행상품_ID` = ? ORDER BY `옵션명`
            """, product.getId());
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.get("옵션명"))
            .containsExactlyInAnyOrder("커피 1포트", "샴페인 1병 추가");
        assertThat(rows).allSatisfy(row ->
            assertThat(row.get("허용투어등급")).isEqualTo("GRAND,PREMIUM"));
    }

    private void runMigration() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection,
                new ClassPathResource("db/migration/V13__fixed_drink_options.sql"));
            return null;
        });
    }
}
