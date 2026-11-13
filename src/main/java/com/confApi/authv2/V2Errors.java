package com.confApi.authv2;
import org.springframework.web.bind.annotation.*;import org.springframework.http.ResponseEntity;import java.util.Map;
@RestControllerAdvice public class V2Errors {
 @ExceptionHandler(V2.Failure.class) public ResponseEntity<?> failure(V2.Failure e,javax.servlet.http.HttpServletRequest request){String correlation=java.util.Objects.toString(request.getAttribute("auth.v2.correlation"),java.util.UUID.randomUUID().toString());return ResponseEntity.status(e.status).header("Cache-Control","no-store").header("X-Correlation-ID",correlation).body(Map.of("code","AUTH_V2_"+e.status,"correlationId",correlation));}
}
