package presentation.web;

import application.busca.BuscaService;
import application.plano.AssinaturaService;
import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Estabelecimento;
import dominio.planosAdesao.Plano;
import org.springframework.web.bind.annotation.*;
import presentation.web.Dtos.DetalheEstabelecimentoDto;
import presentation.web.Dtos.ResultadoBuscaDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * UC02 - Buscar Estabelecimentos. Só leitura: os filtros vão na query string.
 * A referência (latitude, longitude) é a do dispositivo ou a que o aluno digitou (2a).
 */
@RestController
@RequestMapping("/api/busca/estabelecimentos")
public class BuscaController {

    private final BuscaService servico;
    private final AssinaturaService assinaturas;
    private final MontadorDeEstado montador;

    public BuscaController(BuscaService servico, AssinaturaService assinaturas,
                           MontadorDeEstado montador) {
        this.servico = servico;
        this.assinaturas = assinaturas;
        this.montador = montador;
    }

    @GetMapping
    public List<ResultadoBuscaDto> buscar(@RequestParam String cpf,
                                          @RequestParam double latitude,
                                          @RequestParam double longitude,
                                          @RequestParam double raioKm,
                                          @RequestParam(required = false) String modalidade,
                                          @RequestParam(required = false) String tipo,
                                          @RequestParam(required = false) String inicio,
                                          @RequestParam(required = false) String fim,
                                          @RequestParam(defaultValue = "false") boolean somenteCobertos) {
        var encontrados = servico.buscarEstabelecimentos(cpf, latitude, longitude, raioKm,
                modalidade, tipo, inicio, fim, somenteCobertos);
        Plano plano = assinaturas.assinaturaVigente(cpf).getPlano();
        return encontrados.stream()
                .map(e -> montador.resultadoBusca(e, plano, latitude, longitude))
                .toList();
    }

    /**
     * Passo 7. A grade é a do período filtrado na busca; sem filtro de horário,
     * mostra os próximos 7 dias.
     */
    @GetMapping("/{cnpj}")
    public DetalheEstabelecimentoDto detalhar(@PathVariable String cnpj,
                                              @RequestParam String cpf,
                                              @RequestParam double latitude,
                                              @RequestParam double longitude,
                                              @RequestParam(required = false) String inicio,
                                              @RequestParam(required = false) String fim) {
        Estabelecimento e = servico.detalharEstabelecimento(cpf, cnpj);
        Plano plano = assinaturas.assinaturaVigente(cpf).getPlano();
        LocalDateTime de = LocalDateTime.now();
        LocalDateTime ate = de.plusDays(GRADE_PADRAO_DIAS);
        if (inicio != null && !inicio.isBlank() && fim != null && !fim.isBlank()) {
            try {
                de = LocalDateTime.parse(inicio.trim());
                ate = LocalDateTime.parse(fim.trim());
            } catch (DateTimeParseException ex) {
                throw new RegraDeNegocioException("Data ou hora inválida na faixa de horário.");
            }
        }
        return montador.detalhe(e, plano, latitude, longitude, de, ate);
    }

    private static final int GRADE_PADRAO_DIAS = 7;
}
