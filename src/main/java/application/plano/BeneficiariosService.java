package application.plano;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Funcionario;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static application.NaoEncontradoException.exigir;

/** HU2 - camada de aplicação para a gestão do quadro de beneficiários. */
@Service
public class BeneficiariosService {

    private final Repositorio repositorio;

    public BeneficiariosService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    private Empresa empresa(String cnpj) {
        return exigir(repositorio.buscarEmpresa(cnpj), "Empresa não encontrada: " + cnpj);
    }

    private Plano plano(int id) {
        return exigir(repositorio.buscarPlano(id), "Plano não encontrado: " + id);
    }

    /** Funcionário do quadro desta empresa; um CPF de outra empresa não serve. */
    private Funcionario funcionario(String cnpjEmpresa, String cpf) {
        Empresa empresa = empresa(cnpjEmpresa);
        return exigir(empresa.getFuncionarios().stream()
                .filter(f -> f.temCpf(cpf)).findFirst().orElse(null),
                "Funcionário " + cpf + " não está no quadro de " + empresa + ".");
    }

    private String emailDe(String nome) {
        return nome.toLowerCase().replaceAll("\\s+", ".") + "@empresa.com";
    }

    /**
     * Inclusão no quadro e concessão do plano acontecem na mesma operação.
     * O aluno é gravado antes da adesão, que referencia a linha dele (adesoes.aluno_cpf).
     */
    @Transactional
    public void incluirFuncionario(String cnpjEmpresa, String nome, String cpf,
                                   String matricula, int planoId) {
        Empresa empresa = empresa(cnpjEmpresa);
        Plano plano = plano(planoId);
        exigirCpfLivre(cpf);

        Funcionario funcionario = new Funcionario(nome, cpf, emailDe(nome),
                matricula, LocalDate.now(), empresa);
        Adesao adesao = Adesao.cadastrarBeneficiario(funcionario, plano);

        repositorio.adicionarAluno(funcionario);
        repositorio.adicionarAdesao(adesao);
    }

    /**
     * HU2 - troca de plano. O dependente fica sempre no mesmo plano do
     * responsável (RN3), então a troca se estende às adesões vigentes deles.
     * O funcionário é trocado primeiro: se o domínio recusar (plano não
     * oferecido, suspensão RN7, plano sem dependentes RN3), nada muda.
     *
     * A cascata é atômica: se um dependente falhar no meio, a transação
     * desfaz também as trocas que já tinham sido feitas.
     */
    @Transactional
    public void trocarPlano(String cnpjEmpresa, String cpf, int planoId) {
        Adesao atual = adesaoVigente(cnpjEmpresa, cpf);
        Plano novo = plano(planoId);
        List<Adesao> dosDependentes = adesoesVigentesDosDependentes(cnpjEmpresa, cpf);

        repositorio.adicionarAdesao(Adesao.trocarPlano(atual, novo));
        for (Adesao adesaoDependente : dosDependentes) {
            repositorio.adicionarAdesao(Adesao.trocarPlano(adesaoDependente, novo));
        }
    }

    /**
     * RN7 - a empresa contratante está inadimplente: o bloqueio atinge o
     * funcionário e os dependentes dele. Coordenar as várias adesões é papel
     * da aplicação; cada Adesao só sabe da própria transição. Atômica (ver trocarPlano).
     */
    @Transactional
    public void suspenderAdesao(String cnpjEmpresa, String cpf) {
        adesaoVigente(cnpjEmpresa, cpf).suspender();
        adesoesVigentesDosDependentes(cnpjEmpresa, cpf).forEach(Adesao::suspender);
    }

    /** RN7 - a empresa regularizou o pagamento: o funcionário e os dependentes voltam a ter acesso. */
    @Transactional
    public void reativarAdesao(String cnpjEmpresa, String cpf) {
        adesaoVigente(cnpjEmpresa, cpf).reativar();
        adesoesVigentesDosDependentes(cnpjEmpresa, cpf).forEach(Adesao::reativar);
    }

    /** UC01 - a empresa escolhe quais planos os funcionários podem assinar. */
    @Transactional
    public void oferecerPlano(String cnpjEmpresa, int planoId) {
        Empresa empresa = empresa(cnpjEmpresa);
        Plano plano = plano(planoId);
        if (plano.ehOferecidoPor(empresa)) {
            throw new RegraDeNegocioException(empresa + " já oferece o plano " + plano.getNome() + ".");
        }
        plano.oferecerPara(empresa);
    }

    /**
     * Ninguém fica num plano que a empresa não oferece: enquanto houver
     * funcionário com adesão vigente nele, a retirada é recusada. Os
     * dependentes seguem o titular (RN3), então basta olhar os funcionários.
     */
    @Transactional
    public void retirarPlano(String cnpjEmpresa, int planoId) {
        Empresa empresa = empresa(cnpjEmpresa);
        Plano plano = plano(planoId);
        long noPlano = empresa.getFuncionarios().stream()
                .map(repositorio::adesaoDe)
                .filter(a -> a != null && a.getPlano().equals(plano))
                .count();
        if (noPlano > 0) {
            throw new RegraDeNegocioException((noPlano == 1
                    ? "1 funcionário está no plano " + plano.getNome() + ". Troque o plano dele"
                    : noPlano + " funcionários estão no plano " + plano.getNome() + ". Troque o plano deles")
                + " antes de deixar de oferecê-lo.");
        }
        plano.deixarDeOferecerPara(empresa);
    }

    private List<Adesao> adesoesVigentesDosDependentes(String cnpjEmpresa, String cpf) {
        return funcionario(cnpjEmpresa, cpf).getDependentes().stream()
                .map(repositorio::adesaoDe)
                .filter(Objects::nonNull)
                .toList();
    }

    /** O dependente é gravado antes da adesão dele, que referencia a linha (adesoes.aluno_cpf). */
    @Transactional
    public void incluirDependente(String cnpjEmpresa, String cpfFuncionario,
                                  String nome, String cpf, String grauParentesco) {
        Adesao adesao = adesaoVigente(cnpjEmpresa, cpfFuncionario);
        Funcionario responsavel = funcionario(cnpjEmpresa, cpfFuncionario);
        exigirCpfLivre(cpf);

        Dependente dependente = new Dependente(nome, cpf, emailDe(nome),
                grauParentesco, responsavel);
        Adesao adesaoDoDependente = adesao.incluirDependente(dependente);

        repositorio.adicionarAluno(dependente);
        repositorio.adicionarAdesao(adesaoDoDependente);
    }

    @Transactional
    public void removerFuncionario(String cnpjEmpresa, String cpf) {
        Funcionario funcionario = funcionario(cnpjEmpresa, cpf);
        funcionario.getEmpresaVinculada().removerFuncionario(funcionario);
    }

    /**
     * O CPF identifica o aluno na recepção (UC07, 2a), então não pode repetir.
     * Fica aqui porque só o repositório sabe se ele já existe.
     */
    private void exigirCpfLivre(String cpf) {
        Aluno existente = repositorio.buscarAluno(cpf);
        if (existente != null) {
            throw new RegraDeNegocioException(
                    "O CPF " + cpf + " já está cadastrado para " + existente + ".");
        }
    }

    private Adesao adesaoVigente(String cnpjEmpresa, String cpf) {
        Funcionario f = funcionario(cnpjEmpresa, cpf);
        Adesao adesao = repositorio.adesaoDe(f);
        if (adesao == null) {
            throw new RegraDeNegocioException("Funcionário sem adesão vigente: " + f);
        }
        return adesao;
    }
}
