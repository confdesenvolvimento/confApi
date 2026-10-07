package com.confApi.hoteis;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class HotelPesquisaProgressivaServiceTest {
    HotelSearchService consulta;
    HotelPesquisaProgressivaService service;
    @BeforeEach void setup() { consulta=mock(HotelSearchService.class); service=new HotelPesquisaProgressivaService(consulta,new ObjectMapper()); }
    @AfterEach void close() { service.encerrar(); }
    List<HotelResponse> hoteis(int n) { List<HotelResponse> lista=new ArrayList<>(); for(int i=0;i<n;i++){var h=new HotelResponse(); h.setCodigoHotelSistema("h"+i);lista.add(h);}return lista; }
    HotelPesquisaProgressivaService.Pagina aguardar(String id, int total, boolean concluida) throws Exception {
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        do {var p=service.pagina("ana",id,0);if(p.totalDisponivel()==total && p.concluida()==concluida)return p;Thread.sleep(5);}while(System.nanoTime()<fim);
        fail("A pesquisa nao atingiu o estado esperado"); return null;
    }
    @Test void devolvePrimeiroFornecedorSemEsperarSegundoEPaginaSemDuplicar() throws Exception {
        CountDownLatch liberar=new CountDownLatch(1);
        when(consulta.pesquisarFornecedor(any(),eq("EZLink"))).thenReturn(hoteis(25));
        when(consulta.pesquisarFornecedor(any(),eq("Omnibees"))).thenAnswer(i->{liberar.await(5,TimeUnit.SECONDS);return hoteis(2);});
        try {
            String id=service.iniciar("ana",new HotelPesquisaModelFront()).id();
            var primeira=aguardar(id,25,false);
            assertEquals(20,primeira.hoteis().size());assertEquals(20,primeira.proximoCursor());
            var segunda=service.pagina("ana",id,20);assertEquals(5,segunda.hoteis().size());assertEquals(25,segunda.proximoCursor());
            assertTrue(service.pagina("ana",id,25).hoteis().isEmpty());
            liberar.countDown();aguardar(id,27,true);
            assertEquals(2,service.pagina("ana",id,25).hoteis().size());
        }finally{liberar.countDown();}
    }
    @Test void falhaDeUmFornecedorPreservaResultadosDoOutro() throws Exception {
        when(consulta.pesquisarFornecedor(any(),eq("EZLink"))).thenThrow(new IllegalStateException("indisponivel"));
        when(consulta.pesquisarFornecedor(any(),eq("Omnibees"))).thenReturn(hoteis(1));
        String id=service.iniciar("ana",new HotelPesquisaModelFront()).id();var p=aguardar(id,1,true);
        assertEquals(1,p.falhas().size());assertTrue(p.falhas().get(0).startsWith("EZLink"));
    }
    @Test void isolaUsuarioValidaCursorECancelamento() throws Exception {
        when(consulta.pesquisarFornecedor(any(),anyString())).thenReturn(List.of());
        String id=service.iniciar("ana",new HotelPesquisaModelFront()).id();aguardar(id,0,true);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.pagina("bia",id,0)).getRawStatusCode());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.pagina("ana",id,-1)).getRawStatusCode());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.pagina("ana",id,1)).getRawStatusCode());
        assertThrows(ResponseStatusException.class,()->service.cancelar("bia",id));
        service.cancelar("ana",id);assertThrows(ResponseStatusException.class,()->service.pagina("ana",id,0));
    }
    @Test void pararMantemResultadosEImpedeEntregaTardia() throws Exception {
        CountDownLatch liberar=new CountDownLatch(1);
        when(consulta.pesquisarFornecedor(any(),eq("EZLink"))).thenReturn(hoteis(1));
        when(consulta.pesquisarFornecedor(any(),eq("Omnibees"))).thenAnswer(i->{liberar.await(5,TimeUnit.SECONDS);return hoteis(2);});
        try {
            String id=service.iniciar("ana",new HotelPesquisaModelFront()).id();aguardar(id,1,false);
            service.parar("ana",id);liberar.countDown();
            var pagina=service.pagina("ana",id,0);assertTrue(pagina.concluida());assertEquals(1,pagina.hoteis().size());
        }finally{liberar.countDown();}
    }
    @Test void limitePorUsuarioLiberaAoCancelar() {
        when(consulta.pesquisarFornecedor(any(),anyString())).thenReturn(List.of());
        String id=service.iniciar("ana",new HotelPesquisaModelFront()).id();
        service.iniciar("ana",new HotelPesquisaModelFront());service.iniciar("ana",new HotelPesquisaModelFront());
        assertEquals(429,assertThrows(ResponseStatusException.class,()->service.iniciar("ana",new HotelPesquisaModelFront())).getRawStatusCode());
        service.cancelar("ana",id);assertNotNull(service.iniciar("ana",new HotelPesquisaModelFront()).id());
    }
}
