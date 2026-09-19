package com.travelbird.place.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.place.controller.dto.SavedPlaceCursorPageResponse;
import com.travelbird.place.controller.dto.SavedPlaceListItem;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.SavedPlace;
import com.travelbird.place.domain.SavedPlaceId;
import com.travelbird.place.repository.PlaceRepository;
import com.travelbird.place.repository.SavedPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code /api/users/me/saved-places*} 4개 엔드포인트를 뒷받침하는 서비스.
 * backend-functional-spec-v10.md §3.6.3.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SavedPlaceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MEMO_MAX_LENGTH = 100;

    private final SavedPlaceRepository savedPlaceRepository;
    private final PlaceRepository placeRepository;

    /**
     * 저장은 멱등하다 — 이미 저장돼 있으면 그대로 둔다(기존 memo 보존). 기능명세 §3.6.3은
     * 저장 시점에 memo를 받지 않는다 — memo는 저장 후 {@link #updateMemo}로만 넣는다
     * (oriole0419 리뷰, PR#9).
     */
    public void save(Long userId, Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }

        SavedPlaceId id = new SavedPlaceId(userId, placeId);
        if (!savedPlaceRepository.existsById(id)) {
            savedPlaceRepository.save(SavedPlace.of(userId, placeId, null));
        }
    }

    /**
     * 저장 취소는 멱등하다 — 저장돼 있지 않아도 에러 없이 끝난다. 메모도 행 삭제로 같이
     * 지워진다. 다만 장소 자체가 canonical하게 존재하지 않으면 404를 반환한다(기능명세 §3.6 —
     * 저장/취소/메모 전부 canonical placeId만 입력받도록 규정).
     */
    public void unsave(Long userId, Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
        savedPlaceRepository.deleteById(new SavedPlaceId(userId, placeId));
    }

    public SavedPlace updateMemo(Long userId, Long placeId, String memo) {
        validateMemoLength(memo);
        SavedPlace savedPlace = savedPlaceRepository.findById(new SavedPlaceId(userId, placeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_PLACE_NOT_FOUND));
        savedPlace.updateMemo(memo);
        return savedPlace;
    }

    @Transactional(readOnly = true)
    public SavedPlaceCursorPageResponse list(Long userId, Long cursorPlaceIdOrNull, Integer sizeOrNull) {
        int size = clampSize(sizeOrNull);
        Pageable pageable = PageRequest.of(0, size + 1);

        List<SavedPlace> page = (cursorPlaceIdOrNull == null)
                ? savedPlaceRepository.findFirstPage(userId, pageable)
                : findPageAfterCursor(userId, cursorPlaceIdOrNull, pageable);

        boolean hasNext = page.size() > size;
        List<SavedPlace> pageContent = hasNext ? page.subList(0, size) : page;

        Map<Long, Place> placesById = placeRepository
                .findAllByPlaceIdIn(pageContent.stream().map(sp -> sp.getId().getPlaceId()).toList())
                .stream()
                .collect(Collectors.toMap(Place::getPlaceId, Function.identity()));

        List<SavedPlaceListItem> items = pageContent.stream()
                .map(sp -> toListItem(sp, placesById.get(sp.getId().getPlaceId())))
                .toList();

        Long nextCursor = hasNext ? pageContent.get(pageContent.size() - 1).getId().getPlaceId() : null;
        return new SavedPlaceCursorPageResponse(items, nextCursor);
    }

    /**
     * 커서로 받은 placeId의 저장 관계가 그 사이 취소되어 더 이상 없으면 {@code 400
     * INVALID_CURSOR}를 던진다 — 조용히 빈 목록(+nextCursor: null)으로 처리하면 실제로는
     * 남은 항목이 있는데도 클라이언트가 "마지막 페이지"로 착각해 데이터가 누락된다(chun9930
     * 리뷰 반영, PR#9). 클라이언트는 이 에러를 받으면 첫 페이지부터 다시 조회해야 한다.
     */
    private List<SavedPlace> findPageAfterCursor(Long userId, Long cursorPlaceId, Pageable pageable) {
        SavedPlace cursorRow = savedPlaceRepository.findById(new SavedPlaceId(userId, cursorPlaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CURSOR));
        return savedPlaceRepository.findPageAfterCursor(
                userId, roundToStoredPrecision(cursorRow.getSavedAt()), cursorPlaceId, pageable);
    }

    /**
     * saved_places.saved_at 컬럼은 MySQL DATETIME(초 단위, fsp 없음)이다. Hibernate 1차
     * 캐시에서 커서 행을 다시 읽으면 삽입 전 메모리상의(초 단위보다 정밀한) LocalDateTime을
     * 그대로 돌려줄 수 있는데, 이 값을 그대로 비교 파라미터로 쓰면 실제 DB 저장값과 안 맞아
     * 동률 비교(=)가 깨진다. MySQL은 DATETIME 저장 시 나노초를 버리는(truncate) 게 아니라
     * 가장 가까운 초로 반올림(round)한다(실측 확인 — 0.665740초 -\> 저장값이 +1초로 올라감).
     * 그래서 단순 truncatedTo(SECONDS)가 아니라 반올림해야 DB 저장값과 정확히 맞는다.
     */
    LocalDateTime roundToStoredPrecision(LocalDateTime savedAt) {
        LocalDateTime truncated = savedAt.truncatedTo(ChronoUnit.SECONDS);
        return savedAt.getNano() >= 500_000_000 ? truncated.plusSeconds(1) : truncated;
    }

    private SavedPlaceListItem toListItem(SavedPlace savedPlace, Place place) {
        return new SavedPlaceListItem(
                savedPlace.getId().getPlaceId(),
                place.getName(),
                place.getCategory(),
                place.getImageUrl(),
                savedPlace.getMemo(),
                savedPlace.getSavedAt()
        );
    }

    private int clampSize(Integer sizeOrNull) {
        if (sizeOrNull == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(sizeOrNull, 1), MAX_PAGE_SIZE);
    }

    private void validateMemoLength(String memo) {
        if (memo != null && memo.length() > MEMO_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SAVED_PLACE_MEMO_TOO_LONG);
        }
    }
}
