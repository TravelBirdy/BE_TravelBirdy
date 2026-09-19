-- =========================================================
-- V2__seed_place_category_mapping_rules.sql
-- backend-functional-spec-v10.md §3.6.1 카테고리 매핑 규칙 초기 시드
-- 담당: Part 3(ksunk29)
-- 규칙:
--   - priority 오름차순으로 첫 매치를 사용한다.
--   - 카테고리 하나당 키워드 하나씩 행을 나눴다(우선순위 컬럼이 Unique라
--     한 행에 여러 키워드를 못 넣음). 10단위로 그룹을 나눠 카테고리 간
--     우선순위(카페>음식점>숙박>...)는 유지하면서, 그룹 내에서 나중에
--     키워드를 추가할 여지를 남겼다.
--   - 병원/학원/행정기관 등 제외(excluded=true) 규칙은 키워드가 아직
--     팀에서 확정되지 않아 이번 시드에서는 뺐다. 확정되면 별도 마이그레이션으로 추가.
-- =========================================================

INSERT INTO place_category_mapping_rules (priority, include_keyword, target_category, excluded) VALUES
(10, '카페', 'CAFE', false),
(11, '다방', 'CAFE', false),
(12, '베이커리', 'CAFE', false),
(20, '음식점', 'FOOD', false),
(30, '숙박', 'ACCOMMODATION', false),
(31, '펜션', 'ACCOMMODATION', false),
(32, '호텔', 'ACCOMMODATION', false),
(33, '모텔', 'ACCOMMODATION', false),
(40, '문화', 'CULTURE_ART', false),
(41, '예술', 'CULTURE_ART', false),
(42, '전시', 'CULTURE_ART', false),
(43, '박물관', 'CULTURE_ART', false),
(50, '스포츠', 'ACTIVITY', false),
(51, '레저', 'ACTIVITY', false),
(52, '액티비티', 'ACTIVITY', false),
(60, '쇼핑', 'SHOPPING', false),
(61, '마트', 'SHOPPING', false),
(62, '시장', 'SHOPPING', false),
(70, '자연', 'NATURE', false),
(71, '산', 'NATURE', false),
(72, '바다', 'NATURE', false),
(73, '계곡', 'NATURE', false),
(80, '관광', 'ATTRACTION', false),
(81, '명소', 'ATTRACTION', false),
(82, '공원', 'ATTRACTION', false);
