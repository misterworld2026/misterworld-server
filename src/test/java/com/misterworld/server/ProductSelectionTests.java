package com.misterworld.server;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.travelproduct.dto.TravelOptionRequest;
import com.misterworld.server.travelproduct.dto.TravelProductRequest;
import com.misterworld.server.domain.TravelOption;
import com.misterworld.server.domain.TravelProduct;
import com.misterworld.server.repository.TravelOptionRepository;
import com.misterworld.server.repository.TravelProductRepository;
import com.misterworld.server.travelproduct.service.TravelProductService;

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
        product.setClassicPrice(100);
        product.setGrandPrice(200);
        product.setPremiumPrice(300);
        var id = TravelProduct.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(product, 1L);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(products.existsById(1L)).thenReturn(true);
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
        product.setClassicPrice(100);
        product.setGrandPrice(200);
        product.setPremiumPrice(300);
        var id = TravelProduct.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(product, 1L);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(products.existsById(1L)).thenReturn(true);
        var selected = new ArrayList<TravelOption>();
        for (String type : List.of("HOTEL", "TRANSPORT", "MEAL")) {
            var option = new TravelOption();
            option.setTravelProduct(product);
            option.setType(type);
            option.setGrade(type.equals("MEAL") ? "PREMIUM" : "CLASSIC");
            option.setDefaultForGrade("CLASSIC");
            option.setAllowedTourGrades("CLASSIC,GRAND,PREMIUM");
            var optionId = TravelOption.class.getDeclaredField("id");
            optionId.setAccessible(true);
            optionId.set(option, (long) selected.size() + 1);
            selected.add(option);
        }
        var defaultMeal = new TravelOption();
        defaultMeal.setTravelProduct(product);
        defaultMeal.setType("MEAL"); defaultMeal.setGrade("CLASSIC"); defaultMeal.setDefaultForGrade("CLASSIC");
        var defaultId = TravelOption.class.getDeclaredField("id");
        defaultId.setAccessible(true); defaultId.set(defaultMeal, 4L);
        selected.get(2).setDefaultForGrade(null);
        when(options.findByTravelProductId(1L)).thenReturn(
            List.of(selected.get(0), selected.get(1), defaultMeal, selected.get(2)));
        when(options.findAllById(List.of(1L, 2L, 3L))).thenReturn(selected);
        assertThat(
            new TravelProductService(products, options).validateSelection(1L, "CLASSIC", List.of(1L, 2L, 3L))).hasSize(
            3);
    }
}
