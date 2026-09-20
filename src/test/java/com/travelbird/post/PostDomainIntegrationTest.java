package com.travelbird.post;

import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.api.PostRouteLock;
import com.travelbird.post.api.PostRouteLockReader;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostImage;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.post.service.HomePostReaderImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PostDomainIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("travelbird")
            .withUsername("travelbird")
            .withPassword("travelbird");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    private static final String SIGUNGU_CODE = "11110";
    private static final Long USER_ID = 1L;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostImageRepository postImageRepository;
    @Autowired
    private PostRouteLockReader postRouteLockReader;
    @Autowired
    private HomePostReaderImpl homePostReaderImpl;

    @BeforeEach
    void seedFixtures() {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", USER_ID)
                .executeUpdate();
    }

    private Long createTripFixture() {
        entityManager.createNativeQuery(
                        "insert into trips (user_id, source_type, title, sigungu_code, start_date, end_date, "
                                + "companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', :sigunguCode, "
                                + ":start, :end, 'SOLO', 'RELAXED')")
                .setParameter("userId", USER_ID)
                .setParameter("sigunguCode", SIGUNGU_CODE)
                .setParameter("start", LocalDate.now())
                .setParameter("end", LocalDate.now().plusDays(1))
                .executeUpdate();
        Number tripId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripId.longValue();
    }

    private Long createFileFixture() {
        entityManager.createNativeQuery(
                        "insert into files (user_id, file_name, object_key, content_type, size_bytes, width, "
                                + "height, purpose, status, expires_at) values (:userId, 'a.jpg', :objectKey, "
                                + "'image/jpeg', 1024, 100, 100, 'POST', 'UPLOADED', :expiresAt)")
                .setParameter("userId", USER_ID)
                .setParameter("objectKey", "post/" + java.util.UUID.randomUUID())
                .setParameter("expiresAt", java.time.LocalDateTime.now().plusDays(1))
                .executeUpdate();
        Number fileId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return fileId.longValue();
    }

    @Test
    void 같은_트립에_삭제안된_게시글은_하나만_허용된다() {
        Long tripId = createTripFixture();
        postRepository.saveAndFlush(Post.create(tripId, "제목", "본문", null, PostVisibility.PUBLIC, true));

        assertThatThrownBy(() -> postRepository.saveAndFlush(
                Post.create(tripId, "제목2", "본문2", null, PostVisibility.PUBLIC, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 삭제된_게시글이_있던_트립엔_새_게시글을_만들_수_있다() {
        Long tripId = createTripFixture();
        Post first = postRepository.saveAndFlush(Post.create(tripId, "제목", "본문", null, PostVisibility.PUBLIC, true));
        first.tombstone();
        postRepository.saveAndFlush(first);

        Post second = postRepository.saveAndFlush(
                Post.create(tripId, "제목2", "본문2", null, PostVisibility.PUBLIC, true));

        assertThat(second.getPostId()).isNotEqualTo(first.getPostId());
    }

    @Test
    void 게시글_없는_트립은_잠금이_없다() {
        Long tripId = createTripFixture();

        PostRouteLock lock = postRouteLockReader.findActivePublishedPostByTripId(tripId);

        assertThat(lock.postId()).isNull();
        assertThat(lock.routeLocked()).isFalse();
    }

    @Test
    void DRAFT만_있으면_잠금이_없다() {
        Long tripId = createTripFixture();
        postRepository.save(Post.create(tripId, null, null, null, PostVisibility.PRIVATE, false));

        assertThat(postRouteLockReader.findActivePublishedPostByTripId(tripId).routeLocked()).isFalse();
    }

    @Test
    void PUBLISHED면_잠긴다() {
        Long tripId = createTripFixture();
        Post post = postRepository.save(Post.create(tripId, "제목", "본문", null, PostVisibility.PUBLIC, true));

        PostRouteLock lock = postRouteLockReader.findActivePublishedPostByTripId(tripId);

        assertThat(lock.postId()).isEqualTo(post.getPostId());
        assertThat(lock.routeLocked()).isTrue();
    }

    @Test
    void 삭제되면_잠금이_풀린다() {
        Long tripId = createTripFixture();
        Post post = postRepository.save(Post.create(tripId, "제목", "본문", null, PostVisibility.PUBLIC, true));
        post.tombstone();
        postRepository.save(post);

        assertThat(postRouteLockReader.findActivePublishedPostByTripId(tripId).routeLocked()).isFalse();
    }

    @Test
    void PRIVATE나_BLOCKED여도_삭제_안됐으면_잠금이_유지된다() {
        Long tripId = createTripFixture();
        Post post = postRepository.save(Post.create(tripId, "제목", "본문", null, PostVisibility.PUBLIC, true));
        post.changeVisibility(PostVisibility.PRIVATE);
        postRepository.save(post);

        assertThat(postRouteLockReader.findActivePublishedPostByTripId(tripId).routeLocked()).isTrue();
    }

    @Test
    void 같은_파일ID는_다른_게시글에_중복_첨부할_수_없다() {
        Long tripId1 = createTripFixture();
        Long tripId2 = createTripFixture();
        Post post1 = postRepository.saveAndFlush(Post.create(tripId1, "제목", "본문", null, PostVisibility.PUBLIC, true));
        Post post2 = postRepository.saveAndFlush(Post.create(tripId2, "제목2", "본문2", null, PostVisibility.PUBLIC, true));
        Long fileId = createFileFixture();
        postImageRepository.saveAndFlush(PostImage.of(post1.getPostId(), fileId, 0));

        assertThatThrownBy(() -> postImageRepository.saveAndFlush(PostImage.of(post2.getPostId(), fileId, 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 홈_추천_기록은_공개_게시글만_반환한다() {
        Long tripId1 = createTripFixture();
        Long tripId2 = createTripFixture();
        Long tripId3 = createTripFixture();
        postRepository.save(Post.create(tripId1, "공개글", "본문", null, PostVisibility.PUBLIC, true));
        postRepository.save(Post.create(tripId2, "비공개글", "본문", null, PostVisibility.PRIVATE, true));
        postRepository.save(Post.create(tripId3, "임시저장", null, null, PostVisibility.PRIVATE, false));

        List<CommunityPostCard> cards = homePostReaderImpl.getHomeRecommendedPosts(10, null);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).title()).isEqualTo("공개글");
        assertThat(cards.get(0).savedRoute()).isFalse();
    }
}
