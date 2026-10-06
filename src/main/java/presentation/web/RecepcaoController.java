package presentation.web;

import application.controleDeAcesso.CheckInService;
import org.springframework.web.bind.annotation.*;

/** UC07 - Validar Check-in na Recepção. O estabelecimento é o da recepção. */
@RestController
@RequestMapping("/api/recepcao/{estabelecimentoId}")
public class RecepcaoController {

    private final CheckInService servico;
    private final MontadorDeEstado montador;

    public RecepcaoController(CheckInService servico, MontadorDeEstado montador) {
        this.servico = servico;
        this.montador = montador;
    }

    public record Recusa(String motivo) {}

    /** Passos 2 a 4 - localizar pelo código do comprovante. */
    @GetMapping("/checkins/{codigo}")
    public Dtos.ConferenciaDto localizar(@PathVariable int estabelecimentoId,
                                         @PathVariable String codigo) {
        return montador.conferencia(servico.localizarNaRecepcao(estabelecimentoId, codigo));
    }

    /** Extensão 2a - localizar pelo CPF do aluno (só dígitos na URL). */
    @GetMapping("/cpf/{cpf}")
    public Dtos.ConferenciaDto localizarPorCpf(@PathVariable int estabelecimentoId,
                                               @PathVariable String cpf) {
        return montador.conferencia(servico.localizarPorCpfNaRecepcao(estabelecimentoId, cpf));
    }

    @PostMapping("/checkins/{codigo}/validar")
    public void validar(@PathVariable int estabelecimentoId, @PathVariable String codigo) {
        servico.validarCheckIn(estabelecimentoId, codigo);
    }

    @PostMapping("/checkins/{codigo}/recusar")
    public void recusar(@PathVariable int estabelecimentoId, @PathVariable String codigo,
                        @RequestBody Recusa dados) {
        servico.recusarCheckIn(estabelecimentoId, codigo, dados.motivo());
    }
}