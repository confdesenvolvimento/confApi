package com.confApi.util;
import com.confApi.hub.telegram.TelegramService;
import com.confApi.hub.telegram.dto.MensagemRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TelegramErrorAlertTest {
 @Test void includesCauseAndSourceLine() {
  TelegramService transport=mock(TelegramService.class);
  Exception cause=new IllegalArgumentException("IATA=XYZ");
  cause.setStackTrace(new StackTraceElement[]{new StackTraceElement("com.confApi.Reserva","save","Reserva.java",42)});
  new TelegramErrorAlert(transport).enviar(this,"Localizador=AIFGQH",new RuntimeException("falha",cause));
  ArgumentCaptor<MensagemRequest> sent=ArgumentCaptor.forClass(MensagemRequest.class);
  verify(transport).enviarLogDeErros(sent.capture());
  assertTrue(sent.getValue().getMensagem().contains("IATA=XYZ"));
  assertTrue(sent.getValue().getMensagem().contains("Reserva.java:42"));
  assertTrue(sent.getValue().getMensagem().contains("Localizador=AIFGQH"));
 }
 @Test void transportFailureDoesNotEscape() {
  TelegramService transport=mock(TelegramService.class);
  when(transport.enviarLogDeErros(any())).thenThrow(new RuntimeException("offline"));
  assertDoesNotThrow(()->new TelegramErrorAlert(transport).enviar(this,"erro",new RuntimeException()));
 }
 @Test void boundsMessagesAndHandlesCyclicCauses() {
  Exception a=new Exception("x".repeat(10000));
  Exception b=new Exception("b",a);a.initCause(b);
  String text=ErrorDiagnostic.format("c".repeat(10000),a);
  assertTrue(text.length()<=3200);assertTrue(text.contains("Causa:"));
 }
}
