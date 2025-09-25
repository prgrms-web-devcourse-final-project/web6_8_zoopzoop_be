package org.tuna.zoopzoop.backend.domain.datasource.ai.prompt;

public class AiPrompt {
    public static final String EXTRACTION = """
        아래 HTML 전문에서 필요한 정보를 JSON 형식으로 추출해 주세요.
        반환 JSON 구조:
        {
          "title": "제목",
          "datacreatedDate": "작성일자 (YYYY-MM-DD)",
          "content": "본문 내용",
          "imageUrl": "썸네일 이미지 URL",
          "sources": "출판사 이름 or 서비스 이름 or 도메인 이름"
        }

        HTML 전문:
        %s

        - 반드시 JSON 형식으로만 출력해 주세요.
        - 해당정보가 없으면 반드시 빈 문자열로 출력해 주세요.
        """;

    public static final String SUMMARY_TAG_CATEGORY = """
        내용 요약, 태그 요약, 카테고리 선정 프롬프트
        """;
}
