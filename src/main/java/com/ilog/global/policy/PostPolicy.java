package com.ilog.global.policy;

import java.time.ZoneId;

/** 게시글·해시태그 입력값 및 검색 규칙. 미결 사항이 확정되면 이 클래스만 수정한다. */
public final class PostPolicy {

    // TODO(D-08): 제목·내용 길이, 필수 여부, URL 최대 개수 확정 시 조정
    public static final int TITLE_MAX = 100;
    public static final int URL_MAX_COUNT = 10;
    public static final int URL_MAX_LENGTH = 2000;
    public static final String URL_REGEX = "^https?://\\S+$";

    // TODO(D-09): 해시태그 표기(#, 대소문자)·최대 개수 확정 시 조정. [잠정] 요청당 최대 개수만 제한
    public static final int HASHTAG_MAX_COUNT = 10;
    public static final int HASHTAG_NAME_MAX = 50;

    // TODO(D-05): 정렬·페이징 확정 시 조정
    public static final int PAGE_SIZE_DEFAULT = 10;
    public static final int PAGE_SIZE_MAX = 50;

    /** [제안] 작성일(date) 검색 시 하루의 경계를 계산하는 서비스 기준 시간대. */
    public static final ZoneId SEARCH_ZONE = ZoneId.of("Asia/Seoul");

    private PostPolicy() {}
}
