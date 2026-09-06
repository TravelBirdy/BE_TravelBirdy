package com.example.demo.service;

import com.example.demo.DTO.UserSummary;

/**
 * Part 2/3가 로그인 사용자 상태 확인, 작성자 표시용 최소 정보 조회에 사용하는 공개 계약.
 * Part 2/3는 이 Interface만 참조하며, User Entity/UserRepository를 직접 사용하지 않는다.
 */
public interface UserReader {

    void validateActiveUser(Long userId);

    UserSummary getUserSummary(Long userId);
}
