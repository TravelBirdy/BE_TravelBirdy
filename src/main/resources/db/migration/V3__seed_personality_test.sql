-- 성향 테스트 마스터 데이터 시드 (기능명세서 3.3.1 문항 8개 x 선택지 5개)
-- [미식]=GOURMET, [휴식]=REST, [사진]=PHOTO, [액티비티]=ACTIVITY, [문화]=CULTURE
-- 문서의 성향 태그는 내부 표기이며 선택지 텍스트 자체에는 노출하지 않는다.

INSERT INTO personality_tests (test_version, active) VALUES ('v1', TRUE);

INSERT INTO personality_questions (test_version, question_order, text) VALUES
('v1', 1, '여행지에서 정체불명의 초대장을 받았다. 초대장에 적혀있으면 하는 문구는?'),
('v1', 2, '여행지에 도착한 직후, 내 눈앞에 펼쳐졌으면 하는 장면은?'),
('v1', 3, '숙소 근처를 걷다가 다섯 갈래 길을 발견했다. 어느 길로 가볼까?'),
('v1', 4, '이번 여행에서 하루 동안 특별한 능력을 하나 얻는다면?'),
('v1', 5, '이제 이 여행지에서 내게 주어진 시간은 단 3시간. 이 중 제일 포기 못하는 것은?'),
('v1', 6, '내가 꿈꾸는 완벽한 여행의 마무리는?'),
('v1', 7, '여행을 마무리하고 탄 기차 안에서 하는 말은?'),
('v1', 8, '여행을 마치고 열어본 내 갤러리는?');

INSERT INTO personality_options (question_id, text, trait)
SELECT q.question_id, o.text, o.trait
FROM personality_questions q
JOIN (
    SELECT 1 AS question_order, '아무나 맛볼 수 없는, 오늘 단 하루만 열리는 미식의 정점에 당신을 초대합니다.' AS text, 'GOURMET' AS trait
    UNION ALL SELECT 1, '오늘만큼은 서두르지 마세요. 당신만의 완벽한 쉼터로 초대합니다.', 'REST'
    UNION ALL SELECT 1, '셔터를 누르는 순간 영화가 되는 곳, 당신만의 인생 한 장면을 선물해 드립니다.', 'PHOTO'
    UNION ALL SELECT 1, '심장이 터질 듯한 새로운 도파민의 세계가 곧 열립니다.', 'ACTIVITY'
    UNION ALL SELECT 1, '당신의 눈과 귀를 사로잡고 마음에 깊은 영감을 새겨줄 현장으로 초대합니다.', 'CULTURE'

    UNION ALL SELECT 2, '지루한 일상을 단번에 날려버릴 만큼 활기차고, 온몸의 세포를 깨워줄 짜릿한 모험과 즐길 거리가 가득한 핫플레이스', 'ACTIVITY'
    UNION ALL SELECT 2, '창밖으로 수채화처럼 붉게 물들어가는 노을과, 그 실루엣이 액자처럼 담기는 빈티지 카페의 창가', 'PHOTO'
    UNION ALL SELECT 2, '수십 년의 세월을 간직한 골목과 은은한 아날로그 감성이 묻어나는 오래된 문화 거리', 'CULTURE'
    UNION ALL SELECT 2, '잔잔하게 들리는 파도 소리와 은은한 나무 향이 가득한 프라이빗하고 아늑한 숙소', 'REST'
    UNION ALL SELECT 2, '침샘을 자극하는 맛있는 음식 냄새와, 달콤한 디저트 냄새가 뒤섞인 활기찬 거리', 'GOURMET'

    UNION ALL SELECT 3, '스냅사진 속 한 장면처럼 초록 식물들이 하얀 벽을 감싸고 있는, 카메라를 켤 수밖에 없는 예쁜 주택가 길', 'PHOTO'
    UNION ALL SELECT 3, '수십 년의 세월을 간직한 오래된 건축물과 고즈넉한 돌담길을 따라 느긋하게 걷는 역사적인 길', 'CULTURE'
    UNION ALL SELECT 3, '따뜻한 온기와 은은한 음악이 기다리는 숙소로 향하는 잔잔하고 편안한 길', 'REST'
    UNION ALL SELECT 3, '코끝을 강렬하게 자극하는 맛있는 음식 냄새를 따라, 현지인들이 옹기종기 줄 서 있는 유명 먹거리 길', 'GOURMET'
    UNION ALL SELECT 3, '멀리서 웃음소리와 활기찬 에너지가 뿜어져 나오는, 재미있는 즐길 거리가 가득해 보이는 북적이는 길', 'ACTIVITY'

    UNION ALL SELECT 4, '시끄러운 인파 속에서도 머릿속이 비워지며 완벽한 이너피스를 찾는 능력', 'REST'
    UNION ALL SELECT 4, '아침부터 새벽까지 쉬지 않고 돌아다녀도 방전되지 않는 무한 체력', 'ACTIVITY'
    UNION ALL SELECT 4, '오래된 돌담길이나 건물만 봐도 그곳에 얽힌 흥미진진한 이야기를 알아채는 능력', 'CULTURE'
    UNION ALL SELECT 4, '간판만 슥 봐도 실패 확률 0%의 로컬 찐맛집을 단번에 감별하는 능력', 'GOURMET'
    UNION ALL SELECT 4, '평범한 골목길에서도 필터 낀 듯 감성적인 구도를 포착해 내는 스냅 작가의 눈', 'PHOTO'

    UNION ALL SELECT 5, '3시간이면 인생샷 300장은 건진다! 무조건 뷰가 예쁜 핫플레이스로 직진.', 'PHOTO'
    UNION ALL SELECT 5, '웨이팅이 길어도 상관없다. 이 지역에서 가장 유명한 로컬 맛집으로 달려간다.', 'GOURMET'
    UNION ALL SELECT 5, '3시간뿐인데 쫓기듯 다니기 싫다. 여기저기 돌아다니지 않고, 편안한 공간에서 좋아하는 음악 들으며 쉬기.', 'REST'
    UNION ALL SELECT 5, '지루하게 보낼 틈이 없어! 남은 시간을 꽉 채워줄 활기차고 역동적인 놀거리를 찾아 발 빠르게 움직인다.', 'ACTIVITY'
    UNION ALL SELECT 5, '이 지역의 진짜 정취를 채우고 싶어, 로컬 감성이 가득한 문화 공간이나 소품샵 투어하기.', 'CULTURE'

    UNION ALL SELECT 6, '따뜻한 물로 샤워를 마치고, 폭신한 침대 속으로 쏙 들어가, 밀린 피로를 풀며 꿀잠 자기', 'REST'
    UNION ALL SELECT 6, '힙한 펍에서 밤새 다트 게임을 즐기거나, 야간 야외 수영장에서 수영하며 보내기', 'ACTIVITY'
    UNION ALL SELECT 6, '은은한 달빛이 내리는 골목과 문화재 야경을 느긋하게 산책하며, 이 도시의 밤을 온전히 느끼기', 'CULTURE'
    UNION ALL SELECT 6, '숙소 테이블에 지역 유명 야식과 시원한 맥주를 잔뜩 펼쳐놓고 맛있게 먹으며 수다 떨기', 'GOURMET'
    UNION ALL SELECT 6, '스탠드 불빛 아래에서 오늘 찍은 사진들을 쭉 훑어보고, 마음에 드는 인생샷을 골라 SNS나 일기장에 기록하기', 'PHOTO'

    UNION ALL SELECT 7, '몸도 마음도 제대로 비우고 가네. 다시 시작할 힘이 좀 나는 것 같아.', 'REST'
    UNION ALL SELECT 7, '진짜 알차게 놀았다! 다음엔 더 다이나믹하고 재미 넘치는 곳으로 가야지.', 'ACTIVITY'
    UNION ALL SELECT 7, '그때 봤던 작품과 도시의 풍경이 아직도 잊히지 않네.', 'CULTURE'
    UNION ALL SELECT 7, '그 맛을 잊을 수가 없다. 또 먹으러 가고 싶어!', 'GOURMET'
    UNION ALL SELECT 7, '사진 보정하고 있는데 버릴 사진이 하나도 없네. 역시 남는 건 사진뿐이야.', 'PHOTO'

    UNION ALL SELECT 8, '아늑한 숙소와 조용한 카페에서 온전히 쉬어갔던 편안한 순간들의 흔적', 'REST'
    UNION ALL SELECT 8, '온몸을 던져 신나게 뛰어놀며 찍힌, 흔들렸지만 생동감 넘치는 역동적인 순간들', 'ACTIVITY'
    UNION ALL SELECT 8, '이 지역 고유의 매력이 묻어나는 웅장한 건축물과 독특한 예술 작품이 담긴 사진들', 'CULTURE'
    UNION ALL SELECT 8, '비주얼만 봐도 군침이 도는 푸짐한 로컬 음식과 디저트 사진들로 가득한 화면', 'GOURMET'
    UNION ALL SELECT 8, '카메라 셔터만 눌러도 그림이 되는 포토존에서 남긴 나만의 감성 가득한 인생샷들', 'PHOTO'
) o ON o.question_order = q.question_order
WHERE q.test_version = 'v1';
