package com.misterworld.server.service;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.domain.*;
import com.misterworld.server.dto.*;
import com.misterworld.server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

import java.util.*;

import static com.misterworld.server.common.BusinessException.require;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelProductService {
    private final TravelProductRepository products;
    private final TravelOptionRepository options;

    public Page<TravelProductResponse> getProducts(String theme, Pageable pageable) {
        return (theme == null ? products.findAll(pageable) : products.findByTheme(theme, pageable)).map(
            p -> TravelProductResponse.from(p, List.of()));
    }

    public TravelProductResponse getProduct(Long productId) {
        var p = products.findById(productId).orElseThrow(BusinessException::notFound);
        return TravelProductResponse.from(p, getOptions(productId));
    }

    public List<TravelOptionResponse> getOptions(Long productId) {
        if (!products.existsById(productId))
            throw BusinessException.notFound();
        return options.findByTravelProductId(productId).stream().map(TravelOptionResponse::from).toList();
    }

    @Transactional
    public TravelProductResponse saveProduct(TravelProductRequest request) {
        return saveProduct(null, request);
    }

    @Transactional
    public TravelProductResponse saveProduct(Long productId, TravelProductRequest request) {
        validateThemeGradesPricesAndOptions(request);
        TravelProduct product = productId == null ? new TravelProduct() : products.findByIdForUpdate(productId)
            .orElseThrow(BusinessException::notFound);
        if (productId == null)
            require(request.options().stream().allMatch(o -> o.id() == null), "신규 상품 옵션에 id를 지정할 수 없습니다.");
        var existing = productId == null ? List.<TravelOption>of() : options.findByTravelProductId(productId);
        var ids = new HashSet<Long>();
        for (var o : request.options())
            if (o.id() != null)
                require(ids.add(o.id()), "중복 옵션입니다.");
        require(existing.stream().allMatch(o -> ids.contains(o.getId())), "기존 옵션 삭제는 지원하지 않습니다. 모든 기존 옵션을 포함하세요.");
        product.setName(request.name());
        product.setTheme(request.theme());
        product.setDescription(request.description());
        product.setClassicPrice(request.classicPrice());
        product.setGrandPrice(request.grandPrice());
        product.setPremiumPrice(request.premiumPrice());
        products.saveAndFlush(product);
        for (var requestOption : request.options())
            saveOption(product, requestOption);
        return getProduct(product.getId());
    }

    public void validateThemeGradesPricesAndOptions(TravelProductRequest request) {
        require(Set.of("허니문", "효도", "골프", "트레킹").contains(request.theme()), "허용하지 않는 테마입니다.");
        boolean restricted = restricted(request.theme());
        require(restricted ? request.classicPrice() == null : request.classicPrice() != null,
            "허니문·효도 클래식 가격은 null, 다른 상품 클래식 가격은 필수입니다.");
        Set<String> defaults = new HashSet<>();
        for (var o : request.options()) {
            require(Set.of("HOTEL", "TRANSPORT", "MEAL", "EXTRA").contains(o.type()), "허용하지 않는 옵션 종류입니다.");
            validateGrade(request.theme(), o.grade());
            if (o.defaultForGrade() != null) {
                validateGrade(request.theme(), o.defaultForGrade());
                require(!o.type().equals("EXTRA"), "추가 옵션은 기본 호텔·교통·식사 구성이 아닙니다.");
                require(defaults.add(o.defaultForGrade() + ":" + o.type()), "등급별 같은 종류 기본 옵션은 하나여야 합니다.");
            }
        }
    }

    private boolean restricted(String theme) {
        return "허니문".equals(theme) || "효도".equals(theme);
    }

    private void validateGrade(String theme, String grade) {
        require(Set.of("CLASSIC", "GRAND", "PREMIUM").contains(grade), "허용하지 않는 등급입니다.");
        require(!restricted(theme) || !"CLASSIC".equals(grade), "허니문·효도는 클래식 상품·옵션을 제공하지 않습니다.");
    }

    private void saveOption(TravelProduct product, TravelOptionRequest request) {
        var o = request.id() == null ? new TravelOption() : options.findById(request.id())
            .orElseThrow(BusinessException::notFound);
        require(o.getTravelProduct() == null || o.getTravelProduct().getId().equals(product.getId()), "다른 상품의 옵션입니다.");
        o.setTravelProduct(product);
        o.setType(request.type());
        o.setName(request.name());
        o.setGrade(request.grade());
        o.setAdditionalPrice(request.additionalPrice());
        o.setDescription(request.description());
        o.setDefaultForGrade(request.defaultForGrade());
        options.save(o);
    }

    public List<TravelOption> validateSelection(Long productId, String grade, List<Long> optionIds) {
        var product = products.findById(productId).orElseThrow(BusinessException::notFound);
        validateGrade(product.getTheme(), grade);
        require(optionIds != null && new HashSet<>(optionIds).size() == optionIds.size(), "옵션이 누락되었거나 중복되었습니다.");
        List<TravelOption> selected = options.findAllById(optionIds);
        require(selected.size() == optionIds.size(), "미해결 옵션입니다.");
        Set<String> singleton = new HashSet<>();
        for (var option : selected) {
            require(option.getTravelProduct().getId().equals(productId), "다른 상품의 옵션입니다.");
            validateGrade(product.getTheme(), option.getGrade());
            if (!option.getType().equals("EXTRA"))
                require(singleton.add(option.getType()), "같은 종류의 옵션을 중복 선택할 수 없습니다.");
        }
        require(singleton.containsAll(Set.of("HOTEL", "TRANSPORT", "MEAL")), "호텔·교통·식사 선택이 필요합니다.");
        return selected;
    }
}
