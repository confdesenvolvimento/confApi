package com.confApi.mobile;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual HTTP on loopback only: a service credential rejection must never revoke a user. */
class MobileUpstreamAuthenticationTest {
    HttpServer server;
    MobileUpstream upstream;
    final AtomicInteger authCalls=new AtomicInteger(),operationCalls=new AtomicInteger();
    volatile int authStatus=200,operationStatus=200;
    volatile String operationBody="{}",authorization;

    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            exchange.getRequestBody().readAllBytes();
            boolean auth=exchange.getRequestURI().getPath().equals("/user/auth");
            int status=auth?authStatus:operationStatus;
            String body;
            if(auth)body="{\"token\":\"fixture-technical-"+authCalls.incrementAndGet()+"\"}";
            else {operationCalls.incrementAndGet();authorization=exchange.getRequestHeaders().getFirst("Authorization");body=operationBody;}
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status,bytes.length);
            try(var output=exchange.getResponseBody()){output.write(bytes);}
            exchange.close();
        });
        server.start();
        String origin="http://127.0.0.1:"+server.getAddress().getPort()+"/";
        upstream=new MobileUpstream(new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of(
            "development",true,"technicalLogin","fixture","technicalPassword","fixture-password",
            "services",Map.of("hub",origin,"manager",origin,"cache",origin,"email",origin,"confapi",origin,"cservice",origin)))));
    }
    @AfterEach void stop(){if(server!=null)server.stop(0);}

    void unavailable(Runnable action) {
        var failure=assertThrows(MobileFailure.class,action::run);
        assertEquals(503,failure.status);assertEquals("AUTENTICACAO_INTEGRACAO_INDISPONIVEL",failure.code);
    }
    @Test void rejectedTechnicalTokenDoesNotBecomeMobileUnauthorizedAndNextReadRefreshesIt() {
        operationStatus=401;
        unavailable(()->upstream.call("manager","dispositivo/login/fixture","GET",null));
        assertEquals(1,operationCalls.get());assertEquals(1,authCalls.get());
        operationStatus=200;
        upstream.call("manager","dispositivo/login/fixture","GET",null);
        assertEquals(2,authCalls.get());assertEquals("Bearer fixture-technical-2",authorization);
    }
    @Test void technicalIssuerRejectionIsTemporaryAndNeverSendsTheBusinessOperation() {
        authStatus=401;
        unavailable(()->upstream.call("manager","dispositivo/login/fixture","GET",null));
        assertEquals(0,operationCalls.get());
        authStatus=200;
        assertDoesNotThrow(()->upstream.call("manager","dispositivo/login/fixture","GET",null));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"manager","hub","cache","email","confapi","cservice"})
    void forwardedBusinessPostIsNotReplayedAndCannotReportMobileUnauthorized(String service) throws Exception {
        operationStatus=401;
        unavailable(()->upstream.forward(service,"fixture-operation",null,"POST","{}".getBytes(StandardCharsets.UTF_8),"application/json"));
        assertEquals(1,operationCalls.get());assertEquals(1,authCalls.get());
        operationStatus=200;
        var response=upstream.forward(service,"fixture-operation",null,"GET",new byte[0],null);
        try(var body=response.body()){assertEquals(200,response.status());}
        assertEquals(2,authCalls.get());
    }
    @Test void businessPermissionDenialRemainsForbidden() throws Exception {
        operationStatus=403;
        var response=upstream.forward("manager","fixture-operation",null,"GET",new byte[0],null);
        try(var body=response.body()){assertEquals(403,response.status());}
    }
    @Test void incorrectUserCredentialsStillCannotCreateSession() {
        operationBody="{\"mensagemDeErro\":{\"idMensagem\":404}}";
        var sessions=new MobileSessions(upstream);
        assertEquals(401,assertThrows(MobileFailure.class,()->sessions.start("fixture","incorrect","00000000-0000-4000-8000-000000000001","127.0.0.1")).status);
    }
    @Test void internalUnauthorizedDuringRenewalPreservesDurableSessionAndUnconsumedRefresh() throws Exception {
        var b=new MobileSessionPersistenceTest();b.setup();
        var issued=b.active();
        String path="dispositivo/login/"+b.login+"?codigoDispositivo="+b.device;
        doAnswer(call->upstream.call("manager",path,"GET",null)).when(b.upstream).call("manager",path,"GET",null);
        operationStatus=401;
        unavailable(()->b.renew(b.instance(),issued));
        assertEquals(MobileSessionRepository.ACTIVE,b.f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
        assertFalse(b.f.repository.loadRefresh(MobileSessions.hash(issued.get("refreshToken").toString())).orElseThrow().used());
        operationStatus=200;operationBody=b.approved.toString();
        var restarted=b.instance();var renewed=b.renew(restarted,issued);
        assertDoesNotThrow(()->restarted.requireActive(b.require(restarted,renewed)));
    }
    @Test void smsValidatedSessionSurvivesRestartWithoutSendingSmsAgain() throws Exception {
        var b=new MobileSessionPersistenceTest();b.setup();
        var issued=b.start(false);var pending=b.require(b.sessions,issued);String[] code={null};
        doAnswer(call->{code[0]=call.getArgument(2);return null;}).when(b.upstream).deliver(any(),eq("sms"),anyString());
        b.sessions.sendCode(pending,"sms");b.sessions.validateCode(pending,code[0]);b.sessions.requireActive(pending);
        assertTrue(pending.until.isAfter(java.time.Instant.now().plus(java.time.Duration.ofDays(19))));
        clearInvocations(b.upstream);
        var restarted=b.instance();var restored=b.require(restarted,issued);
        assertTrue(restored.primary);assertEquals(42,restarted.profile(restored).path("id").asInt());
        assertEquals(1,restarted.profile(restored).path("senhaTemporaria").asInt());
        restarted.requireActive(restored);
        verify(b.upstream,never()).deliver(any(),anyString(),anyString());
        verify(b.upstream,never()).deliverLoginCode(any(),anyString());
    }
}
