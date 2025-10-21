package com.confApi.chatgpt.dto;

import java.util.Map;

public record ToolCallDTO(String name, Map<String,Object> arguments) {

}
