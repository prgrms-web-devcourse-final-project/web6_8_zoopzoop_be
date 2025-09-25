package org.tuna.zoopzoop.backend.domain.member.dto.res;

public record ResBodyForEditMemberName(
        String name
) {
    public ResBodyForEditMemberName(String name) {
        this.name = name;
    }
}
