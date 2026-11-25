package application.plano;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Funcionario;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import java.util.List;

import static application.NaoEncontradoException.exigir;

/**
 * UC01 - o próprio aluno escolhe, troca ou cancela o plano.
 *
 * A troca reaproveita BeneficiariosService.trocarPlano, que já leva os
 * dependentes junto (RN3); aqui ficam só os passos que são do aluno: ver os
 * planos, confirmar a perda dos dependentes (5a), assinar de novo e cancelar.
 *
 * Dependente não troca nem cancela: pela RN3 ele segue o plano do responsável.
 */
@Service
public class AssinaturaService {

    private final Repositorio repositorio;
    private final BeneficiariosService beneficiarios;

    public AssinaturaService(Repositorio repositorio, BeneficiariosService beneficiarios) {
        this.repositorio = repositorio;
        this.beneficiarios = beneficiarios;
    }

    /**
     * Passos 1 a 3. Lista vazia é a extensão 3a: a tela orienta a procurar o RH.
     * Extensão 2a: sem vínculo ativo, o caso de uso termina aqui.
     */
    @Transactional(readOnly = true)
    public List<Plano> planosDisponiveis(String cpf) {
        Aluno aluno = elegivel(cpf);
        return repositorio.listarPlanos().stream()
                .filter(p -> p.ehOferecidoPor(aluno.getEmpresaVinculada()))
                .toList();
    }

    /** Adesão vigente (ativa ou suspensa) do aluno, ou null se ele não tiver nenhuma. */
    @Transactional(readOnly = true)
    public Adesao assinaturaVigente(String cpf) {
        return repositorio.adesaoDe(aluno(cpf));
    }

    /**
     * Passos 4 a 7. Sem adesão vigente, assina; com uma, troca.
     *
     * 5a: se o novo plano não permite dependentes, a primeira tentativa é
     * recusada com a lista de quem perde o acesso
     * (DependentesPerdemAcessoException). Com a confirmação, esses dependentes
     * são removidos e as adesões deles encerradas antes da troca. Se a troca
     * falhar depois disso, a transação desfaz a remoção também (*a).
     */
    @Transactional
    public Adesao assinar(String cpf, int planoId, boolean confirmaPerdaDeDependentes) {
        Funcionario funcionario = titular(cpf);
        Plano plano = exigir(repositorio.buscarPlano(planoId), "Plano não encontrado: " + planoId);
        Adesao vigente = repositorio.adesaoDe(funcionario);

        if (vigente == null) {
            Adesao nova = Adesao.assinar(funcionario, plano);
            repositorio.adicionarAdesao(nova);
            return nova;
        }
        if (vigente.getPlano().equals(plano)) {
            throw new RegraDeNegocioException("Você já está no plano " + plano.getNome() + ".");
        }
        if (confirmaPerdaDeDependentes && !plano.permiteDependentes()) {
            removerDependentes(funcionario);
        }
        beneficiarios.trocarPlano(funcionario.getEmpresaVinculada().getCnpj(), cpf, planoId);
        return repositorio.adesaoDe(funcionario);
    }

    /**
     * Extensão 4b. A tela já alertou e o aluno confirmou. Os dependentes perdem
     * o acesso junto, do mesmo jeito que no 5a.
     */
    @Transactional
    public void cancelarAssinatura(String cpf) {
        Funcionario funcionario = titular(cpf);
        Adesao vigente = repositorio.adesaoDe(funcionario);
        if (vigente == null) {
            throw new RegraDeNegocioException("Você não tem assinatura vigente para cancelar.");
        }
        removerDependentes(funcionario);
        vigente.encerrar();
    }

    /** Encerra a adesão vigente de cada dependente e o desliga do responsável. */
    private void removerDependentes(Funcionario funcionario) {
        // cópia: removerDependente altera a lista que está sendo percorrida
        for (Dependente d : List.copyOf(funcionario.getDependentes())) {
            Adesao adesaoDoDependente = repositorio.adesaoDe(d);
            if (adesaoDoDependente != null) {
                adesaoDoDependente.encerrar();
            }
            funcionario.removerDependente(d);
        }
    }

    private Aluno aluno(String cpf) {
        return exigir(repositorio.buscarAluno(cpf), "Aluno não encontrado: " + cpf);
    }

    /** Passo 2 - inclui Validar Elegibilidade (RN1). */
    private Aluno elegivel(String cpf) {
        Aluno aluno = aluno(cpf);
        if (!aluno.ehElegivel()) {
            throw new RegraDeNegocioException(
                "O vínculo de " + aluno + " com o benefício não está ativo (RN1).");
        }
        return aluno;
    }

    /** Só o funcionário assina, troca ou cancela; o dependente segue o responsável (RN3). */
    private Funcionario titular(String cpf) {
        if (!(elegivel(cpf) instanceof Funcionario funcionario)) {
            throw new RegraDeNegocioException(
                "O plano do dependente acompanha o do responsável (RN3). "
                + "Peça ao titular para trocar ou cancelar.");
        }
        return funcionario;
    }
}
