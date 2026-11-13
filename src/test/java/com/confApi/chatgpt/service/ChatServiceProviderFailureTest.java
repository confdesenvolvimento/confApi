package com.confApi.chatgpt.service;

import com.confApi.chatgpt.dto.ChatRequestDTO;
import com.confApi.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatServiceProviderFailureTest {
    @Test void httpErrorsNeverBecomeMissingChoiceNullPointerOrExposeProviderBody() {
        for (int status : List.of(400, 401, 403, 429, 500, 503)) {
            var fixture = new ChatServiceTiTextoTest();
            var service = fixture.service("{\"error\":{\"message\":\"PRIVATE_PROVIDER_DATA\"}}", status);
            var ex = assertThrows(OpenAiUnavailableException.class,
                    () -> service.chat(new ChatRequestDTO(fixture.messages(), null, false, List.of(), Map.of()), List.of(), null));
            assertEquals("OPENAI_HTTP_" + status, ex.getMessage());
            assertEquals(1, fixture.calls.get());
            assertEquals(503, new GlobalExceptionHandler().handleOpenAiUnavailable(ex).getStatusCodeValue());
        }
    }
    @Test void emptyInvalidOrMissingCompletionIsRejectedBeforeExecutingTools() {
        for (String body : List.of("", "not-json", "null", "[]", "{}", "{\"choices\":[]}",
                "{\"error\":{\"message\":\"PRIVATE\"}}", "{\"choices\":[{}]}")) {
            var fixture = new ChatServiceTiTextoTest(); var service = fixture.service(body, 200);
            var ex = assertThrows(OpenAiUnavailableException.class,
                    () -> service.chat(new ChatRequestDTO(fixture.messages(), null, false, List.of(), Map.of()), List.of(), null));
            assertFalse(ex.getMessage().contains("PRIVATE"));
            org.mockito.Mockito.verifyNoInteractions(fixture.router);
        }
    }
}
