package presentation.web;

import application.controleDeAcesso.CheckInService;
import org.springframework.web.bind.annotation.*;

/** UC07 - Validar Check-in na Recepção. O estabelecimento é o da recepção. */
@RestController
@RequestMapping("/api/recepcao/{estabelecimentoId}/checkins/{codigo}")
public class RecepcaoController {

    private final CheckInService servico;
    private final MontadorDeEstado montador;

    public RecepcaoController(CheckInService servico, MontadorDeEstado montador) {
        this.servico = servico;
        this.montador = montador;
    }

    public record Recusa(String motivo) {}

    @GetMapping
    public Dtos.ConferenciaDto localizar(@PathVariable int estabelecimentoId,
                                         @PathVariable String codigo) {
        return montador.conferencia(servico.localizarNaRecepcao(estabelecimentoId, codigo));
    }

    @PostMapping("/validar")
    public void validar(@PathVariable int estabelecimentoId, @PathVariable String codigo) {
        servico.validarCheckIn(estabelecimentoId, codigo);
    }

    @PostMapping("/recusar")
    public void recusar(@PathVariable int estabelecimentoId, @PathVariable String codigo,
                        @RequestBody Recusa dados) {
        servico.recusarCheckIn(estabelecimentoId, codigo, dados.motivo());
    }
}
