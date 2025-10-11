package org.tuna.zoopzoop.backend.domain.member.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "member")
@Setting(settingPath = "ngram.json")
@Getter
@Setter
public class MemberDocument {
    @Id
    private int id;

    @Field(type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "standard")
    private String name;

    private String profileImageUrl;
}
