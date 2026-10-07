package com.confApi.hub.hotel.mapper;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class HotelPesquisaMapperTest {
    HotelPesquisaModelFront pesquisa(String entrada,String saida) {
        var p=new HotelPesquisaModelFront();p.setDataEntrada(entrada);p.setDataSaida(saida);return p;
    }
    @Test void datasSaoPreservadasEntreFornecedoresEmParalelo() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tarefas=new ArrayList<>();
            for(int thread=0;thread<8;thread++) {
                final int dia=18+thread;
                tarefas.add(()->{for(int i=0;i<1000;i++) {
                    var model=HotelPesquisaMapper.toHub(pesquisa("2026-12-"+dia,"2026-12-"+(dia+1)));
                    assertEquals(LocalDate.of(2026,12,dia),model.getDataEntrada().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    assertEquals(LocalDate.of(2026,12,dia+1),model.getDataSaida().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                }return null;});
            }
            for(var resultado:pool.invokeAll(tarefas))resultado.get(10,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
    }
    @Test void rejeitaDatasInvalidasSemAjustarAnoOuMesSilenciosamente() {
        for(String data:List.of("2026-02-29","2026-13-18","18/12/2026","2026-12-18texto"))
            assertThrows(IllegalArgumentException.class,()->HotelPesquisaMapper.toHub(pesquisa(data,"2026-12-19")));
        assertNotNull(HotelPesquisaMapper.toHub(pesquisa("2028-02-29","2028-03-01")).getDataEntrada());
    }
}
