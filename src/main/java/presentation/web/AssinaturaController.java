package presentation.web;

import application.plano.AssinaturaService;
import dominio.planosAdesao.Adesao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import presentation.web.Dtos.AssinaturaDto;
import presentation.web.Dtos.PlanoDto;

import java.util.List;

/** UC01 - o aluno escolhe, troca ou cancela o próprio plano. CPF vai na URL só com dígitos. */
@RestController
@RequestMapping("/api/alunos/{cpf}")
public class AssinaturaController {

    private final AssinaturaService servico;
    private final MontadorDeEstado montador;

    public AssinaturaController(AssinaturaService servico, MontadorDeEstado montador) {
        this.servico = servico;
        this.montador = montador;
    }

    public record NovaAssinatura(int planoId, boolean confirmaPerdaDeDependentes) {}

    @GetMapping("/planos")
    public List<PlanoDto> planosDisponiveis(@PathVariable String cpf) {
        return montador.planos(servico.planosDisponiveis(cpf));
    }

    /** 204 sem corpo quando o aluno não tem adesão vigente. */
    @GetMapping("/assinatura")
    public ResponseEntity<AssinaturaDto> assinaturaVigente(@PathVariable String cpf) {
        Adesao vigente = servico.assinaturaVigente(cpf);
        return vigente == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(montador.assinatura(vigente));
    }

    @PostMapping("/assinatura")
    public AssinaturaDto assinar(@PathVariable String cpf, @RequestBody NovaAssinatura dados) {
        return montador.assinatura(
                servico.assinar(cpf, dados.planoId(), dados.confirmaPerdaDeDependentes()));
    }

    @DeleteMapping("/assinatura")
    public void cancelarAssinatura(@PathVariable String cpf) {
        servico.cancelarAssinatura(cpf);
    }
}
