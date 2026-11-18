package com.confApi.chatgpt.controller;

import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationControllerValidationTest {

    @ParameterizedTest
    @CsvSource({
            "/chat, codgAgencia, true",
            "/chat, codgAgencia, false",
            "/chat, codgUsuario, true",
            "/chat, codgUsuario, false",
            "/stream, codgAgencia, true",
            "/stream, codgAgencia, false",
            "/stream, codgUsuario, true",
            "/stream, codgUsuario, false"
    })
    void identidadeNulaOuAusenteRetorna400SemConsultarServicos(
            String endpoint, String campo, boolean nuloExplicito) throws Exception {
        ChatService chatService = mock(ChatService.class);
        ProfilePromptRegistry profiles = mock(ProfilePromptRegistry.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ConversationController(chatService, profiles)).build();
        ObjectNode request = new ObjectMapper().createObjectNode()
                .put("identificacao", "confia")
                .put("unidade", "CGB")
                .put("idErp", "321")
                .put("codgAgencia", 51L)
                .put("codgUsuario", 71L)
                .put("input", "Quais sao meus proximos check-ins?");
        if (nuloExplicito) {
            request.putNull(campo);
        } else {
            request.remove(campo);
        }

        MvcResult result = mvc.perform(post("/v1/ai/conversation" + endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isBadRequest())
                .andReturn();

        MethodArgumentNotValidException error = assertInstanceOf(
                MethodArgumentNotValidException.class, result.getResolvedException());
        assertEquals(1, error.getBindingResult().getErrorCount());
        assertNotNull(error.getBindingResult().getFieldError(campo));
        assertEquals("NotNull", error.getBindingResult().getFieldError(campo).getCode());
        verifyNoInteractions(chatService, profiles);
    }
}
