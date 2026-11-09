package com.misterworld.server;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.domain.*;
import com.misterworld.server.dto.*;
import com.misterworld.server.repository.*;
import com.misterworld.server.service.TravelProductService;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductSelectionTests {
    @Test
    void restrictedThemesRejectClassicAndAllowGrandAndPremium() {
        var service = new TravelProductService(null, null);
        for (String theme : List.of("허니문", "효도")) {
            assertThatThrownBy(() -> service.validateThemeGradesPricesAndOptions(
                new TravelProductRequest("상품", theme, null, 100, 200, 300, List.of()))).isInstanceOf(
                BusinessException.class);
            service.validateThemeGradesPricesAndOptions(
                new TravelProductRequest("상품", theme, null, null, 200, 300, List.of()));
            assertThatThrownBy(() -> service.validateThemeGradesPricesAndOptions(
                new TravelProductRequest("상품", theme, null, null, 200, 300,
                    List.of(new TravelOptionRequest(null, "HOTEL", "호텔", "CLASSIC", 0, null, null))))).isInstanceOf(
                BusinessException.class);
        }
        for (String theme : List.of("골프", "트레킹"))
            service.validateThemeGradesPricesAndOptions(
                new TravelProductRequest("상품", theme, null, 100, 200, 300, List.of()));
    }

    @Test
    void duplicateUnknownAndOtherProductOptionsAreRejected() throws Exception {
        var products = mock(TravelProductRepository.class);
        var options = mock(TravelOptionRepository.class);
        var product = new TravelProduct();
        product.setTheme("골프");
        var id = TravelProduct.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(product, 1L);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        var service = new TravelProductService(products, options);
        assertThatThrownBy(() -> service.validateSelection(1L, "CLASSIC", List.of(1L, 1L))).isInstanceOf(
            BusinessException.class);
        when(options.findAllById(List.of(999L))).thenReturn(List.of());
        assertThatThrownBy(() -> service.validateSelection(1L, "CLASSIC", List.of(999L))).isInstanceOf(
            BusinessException.class);
        var other = new TravelProduct();
        other.setTheme("골프");
        id.set(other, 2L);
        var option = new TravelOption();
        option.setTravelProduct(other);
        option.setGrade("PREMIUM");
        option.setType("HOTEL");
        when(options.findAllById(List.of(5L))).thenReturn(List.of(option));
        assertThatThrownBy(() -> service.validateSelection(1L, "CLASSIC", List.of(5L))).isInstanceOf(
            BusinessException.class);
    }

    @Test
    void classicTourCanChoosePremiumMeal() throws Exception {
        var products = mock(TravelProductRepository.class);
        var options = mock(TravelOptionRepository.class);
        var product = new TravelProduct();
        product.setTheme("골프");
        var id = TravelProduct.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(product, 1L);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        var selected = new ArrayList<TravelOption>();
        for (String type : List.of("HOTEL", "TRANSPORT", "MEAL")) {
            var option = new TravelOption();
            option.setTravelProduct(product);
            option.setType(type);
            option.setGrade(type.equals("MEAL") ? "PREMIUM" : "CLASSIC");
            selected.add(option);
        }
        when(options.findAllById(List.of(1L, 2L, 3L))).thenReturn(selected);
        assertThat(
            new TravelProductService(products, options).validateSelection(1L, "CLASSIC", List.of(1L, 2L, 3L))).hasSize(
            3);
    }
}
