package org.tuna.zoopzoop.backend.domain.datasource.ai.prompt;

public class AiPrompt {
    // 불특정 사이트 메타데이터 추출 프롬프트
    public static final String EXTRACTION = """
        아래 HTML 전문에서 필요한 정보를 JSON 형식으로 추출해 주세요.
        반환 JSON 구조:
        {
          "title": "제목",
          "datacreatedDate": "작성일자 (YYYY-MM-DD)",
          "content": "본문 내용",
          "imageUrl": "썸네일 이미지 URL",
          "source": "출판사 이름 or 서비스 이름 or 도메인 이름"
        }

        HTML 전문:
        %s

        - 반드시 JSON 형식으로만 출력해 주세요.
        - 해당정보가 없으면 반드시 빈 문자열로 출력해 주세요.
        """;

    // 내용 요약, 태그 추출, 카테고리 선정 프롬프트
    public static final String SUMMARY_TAG_CATEGORY = """
        너는 뉴스, 블로그 등 내용 요약 및 분류 AI야. 아래의 규칙에 따라 답변해.
        
        [규칙]
        1. 주어진 content를 50자 이상 100자 이하로 간단히 요약해라.
        2. 아래 Category 목록 중에서 content와 가장 적절한 카테고리 하나를 정확히 선택해라.
           - POLITICS("정치")
           - ECONOMY("경제")
           - SOCIETY("사회")
           - IT("IT")
           - SCIENCE("과학")
           - CULTURE("문화")
           - SPORTS("스포츠")
           - ENVIRONMENT("환경")
           - HISTORY("역사")
           - WORLD("세계")
        3. 내가 제공하는 태그 목록을 참고해서, content와 관련된 태그를 3~5개 생성해라.
           - 제공된 태그와 중복 가능하다.
           - 필요하면 새로운 태그를 만들어도 된다.
        4. 출력은 반드시 아래 JSON 형식으로 해라. Markdown 문법(```)은 쓰지 마라.
           - 해당정보가 없을 시 summary하고 category는 빈 문자열, category는 null로 출력해줘라.
        
        [출력 JSON 형식]
        {
          "summary": "내용 요약 (50~100자)",
          "category": "선택된 카테고리 ENUM 이름",
          "tags": ["태그1", "태그2", "태그3", ...]
        }
        
        [입력 데이터]
        content: %s
        existingTags: %s
        """;
}
