package com.misterworld.server.travelproduct.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;
import com.misterworld.server.travelproduct.dto.TravelGradeConfiguration;
import com.misterworld.server.travelproduct.dto.TravelOptionRequest;
import com.misterworld.server.travelproduct.dto.TravelOptionResponse;
import com.misterworld.server.travelproduct.dto.TravelProductRequest;
import com.misterworld.server.travelproduct.dto.TravelProductResponse;
import com.misterworld.server.travelproduct.entity.TravelOption;
import com.misterworld.server.travelproduct.entity.TravelProduct;
import com.misterworld.server.travelproduct.repository.TravelOptionRepository;
import com.misterworld.server.travelproduct.repository.TravelProductRepository;


import lombok.RequiredArgsConstructor;

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
    @org.springframework.beans.factory.annotation.Autowired
    private jakarta.validation.Validator validator;
    @org.springframework.beans.factory.annotation.Autowired
    private TravelApplicationRepository applications;

    public Page<TravelProductResponse> getProducts(String theme, Pageable pageable) {
        return (theme == null ? products.findAll(pageable) : products.findByTheme(theme, pageable)).map(
            p -> TravelProductResponse.from(p, List.of()));
    }

    public TravelProductResponse getProduct(Long productId) {
        var p = products.findById(productId).orElseThrow(BusinessException::notFound);
        var productOptions = getOptions(p);
        return TravelProductResponse.from(p, productOptions, buildAllowedGradesAndDefaults(p, productOptions));
    }

    public List<TravelOptionResponse> getOptions(Long productId) {
        return getOptions(products.findById(productId).orElseThrow(BusinessException::notFound));
    }

    private List<TravelOptionResponse> getOptions(TravelProduct product) {
        return options.findByTravelProductId(product.getId()).stream()
            .filter(o -> !restricted(product.getTheme()) || !"CLASSIC".equals(o.getGrade()))
            .map(TravelOptionResponse::from).toList();
    }

    @Transactional
    public TravelProductResponse saveProduct(TravelProductRequest request) {
        return saveProduct(null, request);
    }

    @Transactional
    public TravelProductResponse saveProduct(Long productId, TravelProductRequest request) {
        validateRequest(request);
        validateThemeGradesPricesAndOptions(request);
        validateDefaults(request);
        TravelProduct product = productId == null ? prepareNewProduct() : products.findByIdForUpdate(productId)
            .orElseThrow(BusinessException::notFound);
        if (productId == null)
            require(request.options().stream().allMatch(o -> o.id() == null), "신규 상품 옵션에 id를 지정할 수 없습니다.");
        var existing = productId == null ? List.<TravelOption>of() : options.findByTravelProductId(productId);
        var ids = new HashSet<Long>();
        for (var o : request.options())
            if (o.id() != null)
                require(ids.add(o.id()), "중복 옵션입니다.");
        require(existing.stream().allMatch(o -> ids.contains(o.getId())), "기존 옵션 삭제는 지원하지 않습니다. 모든 기존 옵션을 포함하세요.");
        if(productId!=null && !product.getTheme().equals(request.theme()) && applications.existsByProductId(productId))
            throw new BusinessException(409,"HISTORY_EXISTS","신청 기록이 있는 상품의 테마는 변경할 수 없습니다.");
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
            require(!"EXTRA".equals(o.type()) || Set.of("CHAMPAGNE","COFFEE","OTHER").contains(o.benefitCode()==null?"":o.benefitCode()),
                "추가 옵션의 benefitCode를 등록해야 합니다.");
            require("EXTRA".equals(o.type()) || o.benefitCode()==null,"혜택 코드는 추가 옵션에만 지정합니다.");
            require(!"CHAMPAGNE".equals(o.benefitCode()) || o.allowedTourGrades()==null || !o.allowedTourGrades().contains("PREMIUM"),
                "프리미엄에는 별도 샴페인 옵션을 제공하지 않습니다.");
            if(o.allowedTourGrades()!=null) {
                require(new HashSet<>(o.allowedTourGrades()).size()==o.allowedTourGrades().size(),"중복 허용 등급입니다.");
                o.allowedTourGrades().forEach(grade -> validateGrade(request.theme(),grade));
            }
            if (o.defaultForGrade() != null) {
                validateGrade(request.theme(), o.defaultForGrade());
                require(o.grade().equals(o.defaultForGrade()),"기본 옵션은 해당 투어 등급의 옵션이어야 합니다.");
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
        o.setBenefitCode(request.benefitCode());
        if(request.allowedTourGrades()!=null)
            o.setAllowedTourGrades(String.join(",",new java.util.TreeSet<>(request.allowedTourGrades())));
        options.save(o);
    }

    public List<TravelOption> validateSelection(Long productId, String grade, List<Long> optionIds) {
        var product = products.findById(productId).orElseThrow(() ->
            new BusinessException(404, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
        validateGrade(product.getTheme(), grade);
        require(optionIds != null && optionIds.stream().noneMatch(Objects::isNull) &&
            new HashSet<>(optionIds).size() == optionIds.size(), "옵션이 누락되었거나 중복되었습니다.");
        List<TravelOption> selected = options.findAllById(optionIds);
        if (selected.size() != optionIds.size())
            throw new BusinessException(404, "OPTION_NOT_FOUND", "옵션을 찾을 수 없습니다.");
        var productOptions = getOptions(product);
        var configuration = buildAllowedGradesAndDefaults(product, productOptions).stream()
            .filter(g -> g.grade().equals(grade)).findFirst().orElseThrow(() ->
                new BusinessException(400, "INVALID_REQUEST", "상품에서 허용하지 않는 등급입니다."));
        if (!configuration.configurationReady())
            throw new BusinessException(409, "CONFIGURATION_INCOMPLETE", "등급별 기본 호텔·교통·식사 등록이 필요합니다.");

        Set<String> singleton = new HashSet<>();
        var allowedIds = configuration.allowedOptions().stream().map(TravelOptionResponse::id).toList();
        for (var option : selected) {
            require(option.getTravelProduct().getId().equals(productId), "다른 상품의 옵션입니다.");
            require(Set.of("HOTEL", "TRANSPORT", "MEAL", "EXTRA").contains(option.getType()), "잘못된 옵션 종류입니다.");
            validateGrade(product.getTheme(), option.getGrade());
            if("EXTRA".equals(option.getType())) {
                if(option.getBenefitCode()==null) throw new BusinessException(409,"CONFIGURATION_INCOMPLETE","추가 옵션 혜택 코드가 필요합니다.");
                require(!"PREMIUM".equals(grade) || !"CHAMPAGNE".equals(option.getBenefitCode()),"프리미엄에는 샴페인이 기본 포함되어 있습니다.");
            }
            require(allowedIds.contains(option.getId()), "해당 투어 등급에서 허용하지 않는 옵션입니다.");
            if (!option.getType().equals("EXTRA"))
                require(singleton.add(option.getType()), "같은 종류의 옵션을 중복 선택할 수 없습니다.");
        }
        require(singleton.containsAll(Set.of("HOTEL", "TRANSPORT", "MEAL")), "호텔·교통·식사 선택이 필요합니다.");
        return selected;
    }

    public static List<TravelGradeConfiguration> buildAllowedGradesAndDefaults(
            TravelProduct product, List<TravelOptionResponse> options) {
        boolean restricted = java.util.Set.of("허니문", "효도").contains(product.getTheme());
        return java.util.stream.Stream.of("CLASSIC", "GRAND", "PREMIUM")
            .filter(grade -> !restricted || !grade.equals("CLASSIC"))
            .filter(grade -> price(product, grade) != null)
            .map(grade -> {
                var defaults = options.stream().filter(o -> grade.equals(o.defaultForGrade())).toList();
                boolean ready = defaults.size() == 3 &&
                    defaults.stream().allMatch(o -> grade.equals(o.grade())) && java.util.Set.copyOf(
                    defaults.stream().map(TravelOptionResponse::type).toList())
                    .equals(java.util.Set.of("HOTEL", "TRANSPORT", "MEAL"));
                var allowed = options.stream()
                    .filter(o -> !"EXTRA".equals(o.type()) || o.benefitCode()!=null)
                    .filter(o -> !"PREMIUM".equals(grade) || !"CHAMPAGNE".equals(o.benefitCode()))
                    .filter(o -> grade.equals(o.defaultForGrade()) || o.allowedTourGrades().contains(grade)).toList();
                return new TravelGradeConfiguration(grade, price(product, grade), defaults, allowed, ready);
            }).toList();
    }

    private static Integer price(TravelProduct product, String grade) {
        return switch (grade) {
            case "CLASSIC" -> product.getClassicPrice();
            case "GRAND" -> product.getGrandPrice();
            default -> product.getPremiumPrice();
        };
    }
    public TravelProduct prepareNewProduct() { return new TravelProduct(); }
    public void validateRequest(TravelProductRequest request) {
        require(request!=null && validator.validate(request).isEmpty(),"상품 입력값을 확인하세요.");
    }
    private void validateDefaults(TravelProductRequest request) {
        for(String grade: restricted(request.theme()) ? List.of("GRAND","PREMIUM") : List.of("CLASSIC","GRAND","PREMIUM"))
            for(String type:List.of("HOTEL","TRANSPORT","MEAL"))
                require(request.options().stream().filter(o -> grade.equals(o.defaultForGrade()) && type.equals(o.type())).count()==1,
                    "판매 등급별 기본 호텔·교통·식사를 각각 등록해야 합니다.");
    }

}
