package com.travelbird.event.service;

import com.travelbird.event.client.TourApiClient;
import com.travelbird.event.client.TourApiFestivalResponse;
import com.travelbird.event.domain.Event;
import com.travelbird.event.repository.EventRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기능명세서 3.17.1: "TourAPI 국문 관광정보 API에서 매일 새벽 1회 수집·갱신".
 *
 * <p>이 클래스는 TOUR_API_SERVICE_KEY, sigungu_master 시드(Part3), searchFestival2
 * 응답의 정확한 필드 구성(장소명 필드 등)을 실제로 검증하지 못한 상태로 작성됐다 —
 * 실제 서비스키로 최초 실행 후 로그를 보고 {@link #DEFAULT_SYNC_MONTHS}, placeName
 * 처리, 지역 매칭 실패율을 재확인해야 한다.
 */
@Component
public class EventSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventSyncScheduler.class);
    private static final DateTimeFormatter TOUR_API_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;
    // 몇 개월치를 미리 동기화할지 문서에 명시가 없어 3개월로 임의 설정.
    private static final int DEFAULT_SYNC_MONTHS = 3;

    private final TourApiClient tourApiClient;
    private final SigunguCodeResolver sigunguCodeResolver;
    private final EventRepository eventRepository;

    public EventSyncScheduler(
            TourApiClient tourApiClient, SigunguCodeResolver sigunguCodeResolver, EventRepository eventRepository) {
        this.tourApiClient = tourApiClient;
        this.sigunguCodeResolver = sigunguCodeResolver;
        this.eventRepository = eventRepository;
    }

    @Scheduled(cron = "${tour-api.sync-cron}")
    @Transactional
    public void syncFestivals() {
        if (!tourApiClient.isConfigured()) {
            log.warn("TOUR_API_SERVICE_KEY가 설정되지 않아 축제 동기화를 건너뜁니다.");
            return;
        }

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusMonths(DEFAULT_SYNC_MONTHS);
        int upserted = 0;
        int skipped = 0;

        for (int pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
            List<TourApiFestivalResponse.Item> items = tourApiClient.searchFestivals(from, to, pageNo, PAGE_SIZE);
            if (items.isEmpty()) {
                break;
            }
            for (TourApiFestivalResponse.Item item : items) {
                if (upsert(item)) {
                    upserted++;
                } else {
                    skipped++;
                }
            }
            if (items.size() < PAGE_SIZE) {
                break;
            }
        }

        log.info("축제 동기화 완료: upsert {}건, 스킵 {}건", upserted, skipped);
    }

    private boolean upsert(TourApiFestivalResponse.Item item) {
        if (item.contentid() == null || item.title() == null) {
            return false;
        }
        LocalDate startDate = parseDate(item.eventstartdate());
        LocalDate endDate = parseDate(item.eventenddate());
        if (startDate == null || endDate == null) {
            log.warn("날짜 파싱 실패, 건너뜀: contentId={}", item.contentid());
            return false;
        }
        Optional<String> sigunguCode = resolveSigunguCode(item);
        if (sigunguCode.isEmpty()) {
            log.warn("시군구 코드 매칭 실패, 건너뜀: contentId={}, addr1={}", item.contentid(), item.addr1());
            return false;
        }

        // searchFestival2 목록 응답엔 행사장소명 전용 필드가 없어(상세조회 detailIntro2에만 존재),
        // 실제 주소(addr1)를 대신 채운다 — 상세 화면에서 장소명이 꼭 필요해지면 detailIntro2
        // 연동을 별도로 추가해야 한다.
        String placeName = item.addr1() != null && !item.addr1().isBlank() ? item.addr1() : item.title();

        Optional<Event> existing = eventRepository.findByTourApiContentId(item.contentid());
        if (existing.isPresent()) {
            existing.get().updateFrom(item.title(), sigunguCode.get(), placeName, startDate, endDate, item.firstimage());
        } else {
            eventRepository.save(Event.create(
                    item.contentid(), item.title(), sigunguCode.get(), placeName, startDate, endDate, item.firstimage()));
        }
        return true;
    }

    private Optional<String> resolveSigunguCode(TourApiFestivalResponse.Item item) {
        if (item.lDongRegnCd() != null && !item.lDongRegnCd().isBlank()
                && item.lDongSignguCd() != null && !item.lDongSignguCd().isBlank()) {
            String candidate = item.lDongRegnCd() + item.lDongSignguCd();
            if (sigunguCodeResolver.exists(candidate)) {
                return Optional.of(candidate);
            }
        }
        return sigunguCodeResolver.resolveFromAddress(item.addr1());
    }

    private LocalDate parseDate(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(yyyyMMdd, TOUR_API_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
