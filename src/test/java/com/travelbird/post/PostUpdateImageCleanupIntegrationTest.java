package com.travelbird.post;

import com.travelbird.file.client.S3FileStorage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 수정으로 제거된 이미지 파일이 <b>커밋 이후</b>에 삭제되는지 확인한다(PR#32 리뷰 반영).
 * {@code @Transactional} 테스트는 커밋되지 않아 AFTER_COMMIT 리스너가 동작하지 않으므로 이 클래스는
 * 트랜잭션 없이 실제로 커밋하고, 만든 행은 {@code @AfterEach}에서 정리한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PostUpdateImageCleanupIntegrationTest {

    private static final Long USER_ID = 9001L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private S3FileStorage s3FileStorage;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> entityManager
                .createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", USER_ID).executeUpdate());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery("delete from posts where trip_id in "
                    + "(select trip_id from trips where user_id = :id)").setParameter("id", USER_ID).executeUpdate();
            entityManager.createNativeQuery("delete from files where user_id = :id")
                    .setParameter("id", USER_ID).executeUpdate();
            entityManager.createNativeQuery("delete from trips where user_id = :id")
                    .setParameter("id", USER_ID).executeUpdate();
            entityManager.createNativeQuery("delete from users where user_id = :id")
                    .setParameter("id", USER_ID).executeUpdate();
        });
    }

    private Long insert(String sql, Object... params) {
        return tx.execute(status -> {
            var query = entityManager.createNativeQuery(sql);
            for (int i = 0; i < params.length; i += 2) {
                query.setParameter((String) params[i], params[i + 1]);
            }
            query.executeUpdate();
            return ((Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult())
                    .longValue();
        });
    }

    private Long createFile(String objectKey) {
        return insert("insert into files (user_id, file_name, object_key, content_type, size_bytes, width, "
                        + "height, purpose, status, expires_at) values (:userId, 'a.jpg', :objectKey, "
                        + "'image/jpeg', 1024, 100, 100, 'POST', 'UPLOADED', :expiresAt)",
                "userId", USER_ID, "objectKey", objectKey, "expiresAt", LocalDateTime.now().plusDays(1));
    }

    private long fileCount(Long fileId) {
        return tx.execute(status -> ((Number) entityManager
                .createNativeQuery("select count(*) from files where file_id = :id")
                .setParameter("id", fileId).getSingleResult()).longValue());
    }

    private Long versionOf(String responseJson) {
        return Long.valueOf(responseJson.split("\"version\":")[1].replaceAll("[^0-9].*", ""));
    }

    /** 이미지 하나가 붙은 DRAFT를 만들고 {postId, version}을 반환한다. */
    private long[] createPostWithImage(Long fileId) throws Exception {
        Long tripId = insert("insert into trips (user_id, source_type, title, sigungu_code, start_date, "
                        + "end_date, companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', '11110', "
                        + ":start, :end, 'SOLO', 'RELAXED')",
                "userId", USER_ID, "start", LocalDate.now(), "end", LocalDate.now().plusDays(1));
        String created = mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                                + "\"imageFileIds\":[" + fileId + "],\"visibility\":\"PUBLIC\",\"publish\":false}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf(created.split("\"postId\":")[1].replaceAll("[^0-9].*", ""));
        return new long[]{postId, versionOf(created)};
    }

    @Test
    void 커밋된_뒤에_제거된_이미지의_S3_객체와_파일_행이_삭제된다() throws Exception {
        String oldKey = "post/" + UUID.randomUUID();
        Long oldFileId = createFile(oldKey);
        long[] created = createPostWithImage(oldFileId);
        Long newFileId = createFile("post/" + UUID.randomUUID());

        mockMvc.perform(patch("/api/posts/{postId}", created[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageFileIds\":[" + newFileId + "],\"version\":" + created[1] + "}"))
                .andExpect(status().isOk());

        verify(s3FileStorage).delete(oldKey);
        assertThat(fileCount(oldFileId)).isZero();
        assertThat(fileCount(newFileId)).isEqualTo(1);
    }

    @Test
    void 이미지_삭제가_실패해도_이미_커밋된_수정은_200이다() throws Exception {
        String oldKey = "post/" + UUID.randomUUID();
        Long oldFileId = createFile(oldKey);
        long[] created = createPostWithImage(oldFileId);
        doThrow(new RuntimeException("S3 장애")).when(s3FileStorage).delete(oldKey);

        mockMvc.perform(patch("/api/posts/{postId}", created[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageFileIds\":[],\"version\":" + created[1] + "}"))
                .andExpect(status().isOk());

        verify(s3FileStorage).delete(oldKey);
        // FileLinkServiceImpl.deleteOwnedFiles가 S3 삭제 실패와 무관하게 DB 행을 정리하도록
        // 바뀌었다(fix/file-link-s3-delete-resilience-eunjin) — 의도된 동작 변경.
        assertThat(fileCount(oldFileId)).isZero();
    }

    @Test
    void 이미지를_바꾸지_않는_수정은_S3를_건드리지_않는다() throws Exception {
        Long fileId = createFile("post/" + UUID.randomUUID());
        long[] created = createPostWithImage(fileId);

        mockMvc.perform(patch("/api/posts/{postId}", created[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정\",\"version\":" + created[1] + "}"))
                .andExpect(status().isOk());

        verify(s3FileStorage, never()).delete(anyString());
    }
}
