package com.confApi.authv2;
import java.util.Map;
public final class LegacyPolicy {
 public static void check(String login){if(!V2.enabled())return;if(login==null||login.isBlank())throw new V2.Failure(400);var policy=V2.request(V2.setting("AUTH_V2_MANAGER_URL"),"/internal/v2/dispositivos/politica/login","POST",Map.of("login",login),V2.token("AUTH_V2_MANAGER_TOKEN_FILE"),null);if(policy.path("minimumVersion").asInt(2)>=2)throw new V2.Failure(403);}
 private LegacyPolicy(){}
}
