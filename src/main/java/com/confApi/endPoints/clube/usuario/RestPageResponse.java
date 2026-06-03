package com.confApi.endPoints.clube.usuario;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class RestPageResponse<T> extends PageImpl<T> {

    @JsonCreator
    public RestPageResponse(
            @JsonProperty("content") List<T> content,
            @JsonProperty("pageable") JsonNode pageable,
            @JsonProperty("totalElements") long totalElements) {
        super(content,
                PageRequest.of(
                        pageable.get("pageNumber").asInt(),
                        pageable.get("pageSize").asInt()),
                totalElements);
    }
}
