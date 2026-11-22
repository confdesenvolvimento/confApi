package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class MobileSessionAvailabilityTest {
    static final String DEVICE="00000000-0000-4000-8000-000000000001";
    static class Gate {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        void block(){entered.countDown();try{if(!release.await(8,TimeUnit.SECONDS))throw new AssertionError("Gate timed out");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
        void awaitEntry() throws Exception {assertTrue(entered.await(2,TimeUnit.SECONDS));}
    }
    static class Upstream extends MobileUpstream {
        volatile Gate sending,validating;volatile String code;
        Upstream(){super(null);}
        @Override JsonNode call(String service,String path,String method,JsonNode body){
            if(path.startsWith("usuario/findByLogin/")){if(validating!=null)validating.block();return JSON.valueToTree(Map.of("codgUsuario",42,"status",1,"codigoWooba",42));}
            if(path.startsWith("dispositivo/"))return JSON.valueToTree(Map.of("autorizado",false));
            return JSON.valueToTree(Map.of("id",42,"celular","65999990000","email","fixture@example.test"));
        }
        @Override void deliver(JsonNode profile,String channel,String value){code=value;if(sending!=null)sending.block();}
    }
    static Map<String,Object> start(MobileSessions sessions,String login){return sessions.start(login,null,DEVICE,"192.0.2.1");}
    static MobileSessions.Session session(MobileSessions sessions,Map<String,Object> issued){return sessions.require("Bearer "+issued.get("accessToken"));}
    static void stop(ExecutorService workers,Gate gate) throws Exception {gate.release.countDown();workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));}

    @Test void slowSmsDoesNotBlockAnotherUsersLoginOrRenewal() throws Exception {
        var upstream=new Upstream();var sessions=new MobileSessions(upstream);
        var a=session(sessions,start(sessions,"fixture.a"));var b=start(sessions,"fixture.b");
        var gate=new Gate();upstream.sending=gate;var workers=Executors.newFixedThreadPool(3);
        try {
            var sending=workers.submit(()->sessions.sendCode(a,"sms"));gate.awaitEntry();
            var renewal=workers.submit(()->sessions.renew(b.get("refreshToken").toString()));
            var login=workers.submit(()->start(sessions,"fixture.c"));
            assertNotNull(session(sessions,renewal.get(1,TimeUnit.SECONDS)));
            assertNotNull(session(sessions,login.get(1,TimeUnit.SECONDS)));
            assertFalse(sending.isDone());gate.release.countDown();sending.get(2,TimeUnit.SECONDS);
        }finally{stop(workers,gate);}
    }
    @Test void slowValidationDoesNotBlockAnotherUsersRenewal() throws Exception {
        var upstream=new Upstream();var sessions=new MobileSessions(upstream);
        var a=session(sessions,start(sessions,"fixture.a"));var b=start(sessions,"fixture.b");
        sessions.sendCode(a,"sms");var gate=new Gate();upstream.validating=gate;var workers=Executors.newFixedThreadPool(2);
        try {
            var validation=workers.submit(()->sessions.validateCode(a,upstream.code));gate.awaitEntry();
            var renewal=workers.submit(()->sessions.renew(b.get("refreshToken").toString()));
            assertNotNull(session(sessions,renewal.get(1,TimeUnit.SECONDS)));
            assertFalse(validation.isDone());gate.release.countDown();validation.get(2,TimeUnit.SECONDS);
        }finally{stop(workers,gate);}
    }
    @Test void logoutDuringSlowSmsCannotRestoreOtpOrSession() throws Exception {
        var upstream=new Upstream();var sessions=new MobileSessions(upstream);
        var issued=start(sessions,"fixture.a");var a=session(sessions,issued);
        var gate=new Gate();upstream.sending=gate;var workers=Executors.newFixedThreadPool(2);
        try {
            var sending=workers.submit(()->sessions.sendCode(a,"sms"));gate.awaitEntry();
            workers.submit(()->sessions.close(a)).get(1,TimeUnit.SECONDS);gate.release.countDown();
            var failure=assertThrows(ExecutionException.class,()->sending.get(2,TimeUnit.SECONDS));
            assertInstanceOf(MobileFailure.class,failure.getCause());assertNull(a.otpHash);
            assertThrows(MobileFailure.class,()->session(sessions,issued));
        }finally{stop(workers,gate);}
    }
    @Test void logoutDuringSlowValidationCannotUpgradeSession() throws Exception {
        var upstream=new Upstream();var sessions=new MobileSessions(upstream);
        var issued=start(sessions,"fixture.a");var a=session(sessions,issued);sessions.sendCode(a,"sms");
        var gate=new Gate();upstream.validating=gate;var workers=Executors.newFixedThreadPool(2);
        try {
            var validation=workers.submit(()->sessions.validateCode(a,upstream.code));gate.awaitEntry();
            workers.submit(()->sessions.close(a)).get(1,TimeUnit.SECONDS);gate.release.countDown();
            var failure=assertThrows(ExecutionException.class,()->validation.get(2,TimeUnit.SECONDS));
            assertInstanceOf(MobileFailure.class,failure.getCause());assertFalse(a.primary);assertFalse(a.contact);
            assertThrows(MobileFailure.class,()->session(sessions,issued));
        }finally{stop(workers,gate);}
    }
    @Test void slowConfigurationDoesNotBlockAPinnedDestinationWithCachedTechnicalToken() throws Exception {
        var gate=new Gate();
        var settings=new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of("development",false,
            "services",Map.of("manager","https://manager.test/","hub","https://manager.test/"),"productionServices",Map.of("manager","https://manager.test/"),
            "allowedProductionHosts",List.of("manager.test"),"technicalLogin","fixture-user","technicalPassword","fixture-password")));
        var upstream=new MobileUpstream(settings){
            @Override protected JsonNode request(String url,String method,JsonNode body,String authorization){
                if(url.endsWith("user/auth"))return JSON.valueToTree(Map.of("token","fixture-token"));
                if(url.endsWith("propertiesapp")){gate.block();return JSON.createArrayNode();}
                return JSON.valueToTree(Map.of("ok",true));
            }
        };
        upstream.call("manager","warmup","GET",null);
        var workers=Executors.newFixedThreadPool(2);
        try {
            var config=workers.submit(upstream::publicProperties);gate.awaitEntry();
            var lookup=workers.submit(()->upstream.call("manager","wooba/turUsuarios/fixture","POST",null));
            assertTrue(lookup.get(1,TimeUnit.SECONDS).path("ok").asBoolean());assertFalse(config.isDone());
            gate.release.countDown();config.get(2,TimeUnit.SECONDS);
        }finally{stop(workers,gate);}
    }

    @Test void concurrentBurstOfOneThousandSessionsProgressesWhileAnotherUserWaitsForSms() throws Exception {
        var upstream=new Upstream();var sessions=new MobileSessions(upstream);
        var blocked=session(sessions,start(sessions,"fixture.blocked"));
        var gate=new Gate();upstream.sending=gate;var workers=Executors.newFixedThreadPool(33);
        try {
            var sending=workers.submit(()->sessions.sendCode(blocked,"sms"));gate.awaitEntry();
            var tasks=new ArrayList<Callable<String>>();
            for(int i=0;i<1000;i++){
                final int index=i;
                tasks.add(()->{
                    String login="fixture.load."+index;
                    var issued=sessions.start(login,"fixture-password",DEVICE,"198.51."+(index/250)+"."+(index%250+1));
                    assertEquals(login,session(sessions,issued).login);
                    var renewed=sessions.renew(issued.get("refreshToken").toString());
                    assertThrows(MobileFailure.class,()->session(sessions,issued));
                    var active=session(sessions,renewed);assertEquals(login,active.login);
                    sessions.close(active);assertThrows(MobileFailure.class,()->session(sessions,renewed));
                    return renewed.get("accessToken").toString();
                });
            }
            long started=System.nanoTime();
            var futures=workers.invokeAll(tasks,6,TimeUnit.SECONDS);var tokens=new HashSet<String>();
            for(var future:futures){assertFalse(future.isCancelled(),"Burst stalled");assertTrue(tokens.add(future.get()));}
            assertEquals(1000,tokens.size());assertFalse(sending.isDone());
            System.out.println("SIMULATED_BURST users=1000 workers=32 login+renew+logoutMs="+TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
            gate.release.countDown();sending.get(2,TimeUnit.SECONDS);
        }finally{stop(workers,gate);}
    }

    @Test void concurrentRateLimitRemainsAtomic() throws Exception {
        var sessions=new MobileSessions(new Upstream());var workers=Executors.newFixedThreadPool(16);
        try {
            var tasks=new ArrayList<Callable<Boolean>>();
            for(int i=0;i<300;i++)tasks.add(()->{try{sessions.limit("fixture-rate",25,300);return true;}catch(MobileFailure failure){assertEquals(429,failure.status);return false;}});
            int accepted=0;for(var result:workers.invokeAll(tasks,5,TimeUnit.SECONDS))if(result.get())accepted++;
            assertEquals(25,accepted);
        }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(2,TimeUnit.SECONDS));}
    }

    @Test void abandonedIdentificationsExpireSoonAndSuccessfulValidationGetsFullSessionLifetime() {
        var upstream=new Upstream();var sessions=new MobileSessions(upstream);
        var pending=session(sessions,start(sessions,"fixture.pending"));
        assertFalse(pending.primary);
        assertTrue(pending.until.isBefore(java.time.Instant.now().plusSeconds(1801)));
        assertTrue(pending.until.isAfter(java.time.Instant.now().plusSeconds(1790)));
        sessions.sendCode(pending,"sms");sessions.validateCode(pending,upstream.code);
        assertTrue(pending.primary);
        assertTrue(pending.until.isAfter(java.time.Instant.now().plus(java.time.Duration.ofDays(19))));
    }
}
