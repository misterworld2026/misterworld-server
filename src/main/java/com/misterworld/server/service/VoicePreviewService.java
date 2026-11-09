package com.misterworld.server.service;

import com.misterworld.server.dto.voice.VoiceDtos.*;
import com.misterworld.server.domain.TravelProduct;
import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.exception.VoiceProcessingException;
import com.misterworld.server.repository.TravelOptionRepository;
import com.misterworld.server.repository.TravelProductRepository;
import com.misterworld.server.repository.TravelScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VoicePreviewService {
    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;
    private static final Set<String> AUDIO_TYPES = Set.of("audio/webm", "audio/mp4", "audio/wav");
    private static final Pattern KOREAN_DATE = Pattern.compile("(?:(20\\d{2})년\\s*)?(\\d{1,2})월\\s*(\\d{1,2})일");
    private final TravelProductRepository products;
    private final TravelScheduleRepository schedules;
    private final TravelOptionRepository options;
    private final VoiceProcessingClient python;

    public Transcription transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "음성 파일이 필요합니다.");
        if (audio.getSize() > MAX_AUDIO_BYTES) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "음성 파일은 10 MiB 이하여야 합니다.");
        String type = audio.getContentType();
        if (type == null || !AUDIO_TYPES.contains(type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT)))
            throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 음성 형식입니다.");
        try {
            return new Transcription(python.transcribe(audio.getBytes(), audio.getOriginalFilename(), MediaType.parseMediaType(type)));
        } catch (IOException ex) {
            throw error(HttpStatus.BAD_REQUEST, "음성 파일을 읽을 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public InterpretationResponse interpret(InterpretationRequest request) {
        if (request == null || request.text() == null || request.text().isBlank() || request.context() == null)
            throw error(HttpStatus.BAD_REQUEST, "문장과 현재 선택 정보가 필요합니다.");
        Candidates candidates = candidates();
        Selection context = new Selection(request.context().productId(), request.context().scheduleId(),
                request.context().grade(), request.context().optionIds() == null ? emptyOptions() : request.context().optionIds());
        validateContext(context, candidates);
        InterpretedFields raw = python.interpret(new InternalRequest(request.text().trim(), context, candidates)).fields();
        validateRaw(raw);

        Match tour = matchTour(raw.tour(), candidates.products());
        Long productId = tour.id() != null ? tour.id() : context.productId();
        boolean productChanged = tour.id() != null && !tour.id().equals(context.productId());
        Match grade = matchGrade(raw.grade(), productId, candidates.products());
        String oldGrade = productChanged ? null : context.grade();
        String selectedGrade = grade.id() != null ? grade.field().value() : oldGrade;
        Match schedule = matchSchedule(raw.schedule(), productId, candidates.schedules());
        Long scheduleId = schedule.id() != null ? schedule.id() : productChanged ? null : context.scheduleId();
        OptionIds previous = productChanged ? emptyOptions() : context.optionIds();

        Match hotel = matchOption(raw.hotel(), productId, selectedGrade, "HOTEL", candidates.options());
        Match transport = matchOption(raw.transport(), productId, selectedGrade, "TRANSPORT", candidates.options());
        Match meal = matchOption(raw.meal(), productId, selectedGrade, "MEAL", candidates.options());
        List<PreviewField> extraFields = new ArrayList<>();
        Set<Long> extraIds = new LinkedHashSet<>(previous.extras());
        for (InterpretedField item : raw.extras()) {
            Match extra = matchOption(item, productId, selectedGrade, "EXTRA", candidates.options());
            extraFields.add(extra.field());
            if (extra.id() != null) extraIds.add(extra.id());
        }
        OptionIds selectedOptions = new OptionIds(
                hotel.id() != null ? hotel.id() : previous.hotel(),
                transport.id() != null ? transport.id() : previous.transport(),
                meal.id() != null ? meal.id() : previous.meal(), List.copyOf(extraIds));
        selectedOptions = keepAllowedOptions(selectedOptions, productId, selectedGrade, candidates.options());
        Selection selection = new Selection(productId, scheduleId, selectedGrade, selectedOptions);
        PreviewFields fields = new PreviewFields(tour.field(), schedule.field(), grade.field(), hotel.field(),
                transport.field(), meal.field(), extraFields);
        int failures = failure(raw.tour(), fields.tour()) + failure(raw.schedule(), fields.schedule())
                + failure(raw.hotel(), fields.hotel()) + failure(raw.meal(), fields.meal());
        return new InterpretationResponse(request.text().trim(), selection, fields, failures,
                failures >= 2 ? NextAction.RETRY_OR_GUI : NextAction.REVIEW);
    }

    private Candidates candidates() {
        List<ProductCandidate> productRows = products.findAll().stream()
                .map(p -> new ProductCandidate(p.getId(), p.getName(), p.getTheme(), availableGrades(p)))
                .filter(p -> !p.availableGrades().isEmpty()).toList();
        Set<Long> productIds = productRows.stream().map(ProductCandidate::id).collect(java.util.stream.Collectors.toSet());
        LocalDate deadline = LocalDate.now().plusDays(7);
        List<ScheduleCandidate> scheduleRows = schedules.findAll().stream()
                .filter(s -> s.getTravelProduct() != null && productIds.contains(s.getTravelProduct().getId()))
                .filter(s -> s.getDepartureDate() != null && !s.getDepartureDate().isBefore(deadline))
                .filter(s -> s.getStatus() == ScheduleStatus.RECRUITING_SCHEDULE
                        || s.getStatus() == ScheduleStatus.CONFIRMED_SCHEDULE)
                .filter(s -> s.getApplicantCount() == null || s.getApplicantCount() < capacity(s.getTravelProduct().getTheme()))
                .map(s -> new ScheduleCandidate(s.getTravelScheduleId(), s.getTravelProduct().getId(), s.getDepartureDate())).toList();
        List<OptionCandidate> optionRows = options.findAll().stream()
                .filter(o -> o.getTravelProduct() != null && productIds.contains(o.getTravelProduct().getId()))
                .filter(o -> o.getType() != null && Set.of("HOTEL", "TRANSPORT", "MEAL", "EXTRA").contains(o.getType()))
                .map(o -> new OptionCandidate(o.getId(), o.getTravelProduct().getId(), o.getType(), o.getName(), o.getGrade())).toList();
        return new Candidates(productRows, scheduleRows, optionRows);
    }

    private List<String> availableGrades(TravelProduct p) {
        List<String> grades = new ArrayList<>();
        if (p.getClassicPrice() != null && !"허니문".equals(p.getTheme()) && !"효도".equals(p.getTheme())) grades.add("CLASSIC");
        if (p.getGrandPrice() != null) grades.add("GRAND");
        if (p.getPremiumPrice() != null) grades.add("PREMIUM");
        return grades;
    }

    private int capacity(String theme) {
        return switch (theme == null ? "" : theme) {
            case "허니문" -> 4;
            case "골프" -> 10;
            default -> 20;
        };
    }

    private OptionIds emptyOptions() { return new OptionIds(null, null, null, List.of()); }
    private VoiceProcessingException error(HttpStatus status, String message) { return new VoiceProcessingException(status, message); }

    private void validateContext(Selection context, Candidates candidates) {
        Long productId = context.productId();
        OptionIds ids = context.optionIds();
        if (productId == null) {
            if (context.scheduleId() != null || context.grade() != null || ids.hotel() != null || ids.transport() != null
                    || ids.meal() != null || !ids.extras().isEmpty()) invalidContext();
            return;
        }
        ProductCandidate product = candidates.products().stream().filter(p -> p.id().equals(productId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "현재 선택 정보가 유효하지 않습니다."));
        if (context.scheduleId() != null && candidates.schedules().stream().noneMatch(s ->
                s.id().equals(context.scheduleId()) && s.productId().equals(productId))) invalidContext();
        if (context.grade() != null && !product.availableGrades().contains(context.grade())) invalidContext();
        validateOption(ids.hotel(), "HOTEL", context, candidates);
        validateOption(ids.transport(), "TRANSPORT", context, candidates);
        validateOption(ids.meal(), "MEAL", context, candidates);
        for (Long id : ids.extras()) validateOption(id, "EXTRA", context, candidates);
    }

    private void validateOption(Long id, String type, Selection context, Candidates candidates) {
        if (id != null && candidates.options().stream().noneMatch(o -> o.id().equals(id)
                && o.productId().equals(context.productId()) && o.type().equals(type) && allowsGrade(o, context.grade()))) invalidContext();
    }

    private void invalidContext() { throw error(HttpStatus.BAD_REQUEST, "현재 선택 정보가 유효하지 않습니다."); }

    private void validateRaw(InterpretedFields raw) {
        if (raw == null || raw.extras() == null) invalidResponse();
        validateField(raw.tour()); validateField(raw.schedule()); validateField(raw.grade());
        validateField(raw.hotel()); validateField(raw.transport()); validateField(raw.meal());
        for (InterpretedField item : raw.extras()) validateField(item);
    }

    private void validateField(InterpretedField field) {
        if (field == null || field.state() == null || (field.state() == State.RESOLVED) !=
                (field.value() != null && !field.value().isBlank())) invalidResponse();
    }

    private void invalidResponse() { throw error(HttpStatus.BAD_GATEWAY, "음성 해석 결과가 올바르지 않습니다."); }
    private record Match(PreviewField field, Long id) {}

    private Match early(InterpretedField input) {
        if (input.state() == State.NOT_MENTIONED) return new Match(new PreviewField(Status.NOT_MENTIONED, null), null);
        if (input.state() == State.UNRESOLVED) return new Match(new PreviewField(Status.UNRESOLVED, null), null);
        return null;
    }

    private Match matchTour(InterpretedField input, List<ProductCandidate> rows) {
        Match early = early(input); if (early != null) return early;
        String spoken = normalize(input.value());
        String withoutTourSuffix = spoken.replaceFirst("(여행|투어)$", "");
        List<Long> nameMatches = rows.stream().filter(p -> {
            String name = normalize(p.name());
            return !spoken.isEmpty() && (name.contains(spoken) || spoken.contains(name)
                    || (!withoutTourSuffix.isEmpty() && name.contains(withoutTourSuffix)));
        }).map(ProductCandidate::id).toList();
        if (!nameMatches.isEmpty()) return matched(input.value(), nameMatches);
        List<Long> themeMatches = rows.stream().filter(p -> {
            String theme = switch (p.theme() == null ? "" : p.theme()) {
                case "허니문", "효도", "골프", "트레킹" -> p.theme();
                default -> "";
            };
            return !theme.isEmpty() && (spoken.equals(theme) || spoken.equals(theme + "여행")
                    || spoken.equals(theme + "투어"));
        }).map(ProductCandidate::id).toList();
        return matched(input.value(), themeMatches);
    }

    private Match matchSchedule(InterpretedField input, Long productId, List<ScheduleCandidate> rows) {
        Match early = early(input); if (early != null) return early;
        Matcher date = KOREAN_DATE.matcher(input.value());
        LocalDate iso = null;
        if (!date.find()) {
            try { iso = LocalDate.parse(input.value()); }
            catch (RuntimeException ex) { return matched(input.value(), List.of()); }
        }
        LocalDate finalIso = iso;
        int year = iso == null && date.group(1) != null ? Integer.parseInt(date.group(1)) : 0;
        int month = iso == null ? Integer.parseInt(date.group(2)) : 0;
        int day = iso == null ? Integer.parseInt(date.group(3)) : 0;
        List<Long> matches = rows.stream().filter(s -> s.productId().equals(productId))
                .filter(s -> finalIso != null ? s.departureDate().equals(finalIso) :
                        (year == 0 || s.departureDate().getYear() == year)
                                && s.departureDate().getMonthValue() == month && s.departureDate().getDayOfMonth() == day)
                .map(ScheduleCandidate::id).toList();
        return matched(input.value(), matches);
    }

    private Match matchGrade(InterpretedField input, Long productId, List<ProductCandidate> rows) {
        Match early = early(input); if (early != null) return early;
        String grade = input.value().toUpperCase(Locale.ROOT);
        boolean allowed = rows.stream().anyMatch(p -> p.id().equals(productId) && p.availableGrades().contains(grade));
        return allowed ? new Match(new PreviewField(Status.MATCHED, grade), 1L) :
                new Match(new PreviewField(Status.INVALID, input.value()), null);
    }

    private Match matchOption(InterpretedField input, Long productId, String grade,
                              String type, List<OptionCandidate> rows) {
        return matchName(input, rows, OptionCandidate::name, OptionCandidate::id,
                o -> o.productId().equals(productId) && o.type().equals(type) && allowsGrade(o, grade));
    }

    private <T> Match matchName(InterpretedField input, List<T> rows, Function<T, String> name,
                                Function<T, Long> id, Predicate<T> allowed) {
        Match early = early(input); if (early != null) return early;
        String spoken = normalize(input.value());
        List<Long> matches = rows.stream().filter(allowed).filter(row -> {
            String candidate = normalize(name.apply(row));
            return !spoken.isEmpty() && (candidate.contains(spoken) || spoken.contains(candidate));
        }).map(id).toList();
        return matched(input.value(), matches);
    }

    private boolean allowsGrade(OptionCandidate option, String grade) {
        return option.grade() == null || Objects.equals(option.grade(), grade);
    }

    private OptionIds keepAllowedOptions(OptionIds ids, Long productId, String grade, List<OptionCandidate> rows) {
        Predicate<Long> valid = id -> id != null && rows.stream().anyMatch(o -> o.id().equals(id)
                && o.productId().equals(productId) && allowsGrade(o, grade));
        return new OptionIds(valid.test(ids.hotel()) ? ids.hotel() : null,
                valid.test(ids.transport()) ? ids.transport() : null,
                valid.test(ids.meal()) ? ids.meal() : null, ids.extras().stream().filter(valid).toList());
    }

    private Match matched(String value, List<Long> ids) {
        if (ids.isEmpty()) return new Match(new PreviewField(Status.INVALID, value), null);
        if (ids.size() > 1) return new Match(new PreviewField(Status.AMBIGUOUS, value), null);
        return new Match(new PreviewField(Status.MATCHED, value), ids.getFirst());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s·,\\-]+", "");
    }

    private int failure(InterpretedField original, PreviewField result) {
        return original.state() != State.NOT_MENTIONED &&
                Set.of(Status.UNRESOLVED, Status.INVALID, Status.AMBIGUOUS).contains(result.status()) ? 1 : 0;
    }
}
