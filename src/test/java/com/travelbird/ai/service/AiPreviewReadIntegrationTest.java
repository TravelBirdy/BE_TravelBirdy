package com.travelbird.ai.service;

import com.travelbird.ai.entity.*;
import com.travelbird.ai.repository.*;
import com.travelbird.common.enums.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AiPreviewReadIntegrationTest {
    @Autowired EntityManager em;
    @Autowired AiRecommendationJobRepository jobs;
    @Autowired AiTripPreviewRepository previews;
    @Autowired AiPreviewService service;
    @Autowired com.travelbird.savedroute.service.AiPreviewRouteSaveService saves;
    @Autowired com.travelbird.savedroute.service.SavedRouteListService lists;
    @Test void readsMultipleDaysAndHashtagsAfterPersistenceContextClear() {
        em.createNativeQuery("insert into users (user_id, role) values (71, 'ROLE_USER')").executeUpdate();
        var now=LocalDateTime.now();
        var job=jobs.save(AiRecommendationJob.queued(71L,71L,null,AiRequestType.GENERAL,"11110",
            LocalDate.of(2026,10,10),LocalDate.of(2026,10,11),CompanionType.SOLO,Pace.NORMAL,
            "[\"FOOD\"]","[]","[]","[]",true,now));
        var preview=AiTripPreview.temporary(job,71L,"read regression","summary",now);
        preview.addDay(new AiPreviewDay(2));
        preview.addDay(new AiPreviewDay(1));
        preview.replace(null,null,List.of("food","travel"),null,now);
        Long id=previews.saveAndFlush(preview).getId();
        em.clear();
        var response=service.get(71L,id);
        assertThat(response.tripTitle()).isEqualTo("read regression");
        assertThat(response.days()).extracting(day->day.day()).containsExactly(1,2);
        assertThat(response.hashtags()).containsExactlyInAnyOrder("food","travel");
        saves.save(71L,id);
        em.flush(); em.clear();
        var first=lists.list(null,20,71L).items().getFirst();
        assertThat(first.title()).isEqualTo("read regression");
        assertThat(first.region().sigunguCode()).isEqualTo("11110");
        assertThat(first.themes()).containsExactly(TravelTheme.FOOD);
        previews.findById(id).orElseThrow().replace("updated saved title",null,null,null,now.plusMinutes(1));
        em.flush(); em.clear();
        var edited=lists.list(null,20,71L).items().getFirst();
        assertThat(edited.title()).isEqualTo("updated saved title");
        assertThat(edited.savedAt()).isEqualTo(first.savedAt());
        previews.findById(id).orElseThrow().makeTemporary(now.plusMinutes(2));
        em.flush(); em.clear();
        assertThat(lists.list(null,20,71L).items()).isEmpty();
    }
}
