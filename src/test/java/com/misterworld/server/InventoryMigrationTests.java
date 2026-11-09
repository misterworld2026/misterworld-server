package com.misterworld.server;

import com.misterworld.server.inventory.entity.InventoryStatus;
import com.misterworld.server.inventory.entity.ItemInventory;
import com.misterworld.server.inventory.repository.ItemInventoryRepository;
import com.misterworld.server.travelproduct.entity.TravelProduct;
import com.misterworld.server.travelproduct.repository.TravelProductRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@org.springframework.test.context.TestPropertySource(properties="app.sms.dispatch-enabled=false")
@Transactional
class InventoryMigrationTests {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TravelProductRepository products;
    @Autowired
    private ItemInventoryRepository inventory;

    @Test
    void reclassifiesExistingFourWithoutChangingQuantityOrTimestamp() {
        var product = new TravelProduct();
        product.setName("재고 기준 변경 검증");
        product.setTheme("골프");
        product.setClassicPrice(100);
        product.setGrandPrice(200);
        product.setPremiumPrice(300);
        products.saveAndFlush(product);

        var item = new ItemInventory();
        item.setTravelProduct(product);
        item.setItemName("골프공");
        item.setQuantity(4);
        item.setStatus(InventoryStatus.LOW_STOCK);
        var timestamp = LocalDateTime.of(2026, 11, 1, 0, 0);
        item.setUpdatedAt(timestamp);
        inventory.saveAndFlush(item);

        jdbc.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection,
                new ClassPathResource("db/migration/V4__inventory_threshold_three.sql"));
            return null;
        });

        var stored = jdbc.queryForMap(
            "SELECT `재고수량`, `재고상태`, `수정일시` FROM `물품재고` WHERE `물품재고_ID` = ?", item.getId());
        assertThat(stored.get("재고수량")).isEqualTo(4);
        assertThat(stored.get("재고상태")).isEqualTo("NORMAL");
        assertThat(((java.sql.Timestamp) stored.get("수정일시")).toLocalDateTime()).isEqualTo(timestamp);
    }
}
