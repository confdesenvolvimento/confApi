package com.confApi.chatgpt.tools;

import java.util.Map;

public record ToolDefinition(String name, String description, Map<String,Object> jsonSchema) {}