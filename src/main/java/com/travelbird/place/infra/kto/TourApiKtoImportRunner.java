package com.travelbird.place.infra.kto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.place.service.TourApiImportReport;
import com.travelbird.place.service.TourApiPlaceImportBatchService;
import com.travelbird.place.service.TourApiPlaceImportRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * TourAPI(KTO) 초기 장소 Import 진입점. {@code IMPORT_KTO_ENABLED=true}일 때만 서버 기동
 * 시 1회 실행되고, 평소 서버 기동에서는 등록되지 않는다(초기 적재 전용 — public/internal
 * API로는 노출하지 않는다). AI·관광데이터 팀이 넘긴 9-field JSON 파일을 읽어
 * {@link TourApiPlaceImportBatchService#importAll}을 실행하고, 결과(mapping 전체/실패
 * 목록/집계)를 JSON 파일로 남긴다.
 */
@Component
@ConditionalOnProperty(name = "import.kto.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TourApiKtoImportRunner implements ApplicationRunner {

    private final TourApiPlaceImportBatchService tourApiPlaceImportBatchService;
    private final ObjectMapper objectMapper;

    @Value("${import.kto.source-file}")
    private String sourceFilePath;

    @Value("${import.kto.report-file:kto-import-report.json}")
    private String reportFilePath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        File sourceFile = new File(sourceFilePath);
        List<TourApiPlaceImportRow> rows = objectMapper.readValue(sourceFile,
                new TypeReference<List<TourApiPlaceImportRow>>() {
                });
        log.info("TourAPI KTO Import 시작: source={}, {}건", sourceFilePath, rows.size());

        TourApiImportReport report = tourApiPlaceImportBatchService.importAll(rows);

        File reportFile = new File(reportFilePath);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);
        log.info("TourAPI KTO Import 완료: 총 {}건 / 성공 {}건 / 실패 {}건 -> {}",
                report.totalCount(), report.successCount(), report.failureCount(),
                reportFile.getAbsolutePath());
    }
}
