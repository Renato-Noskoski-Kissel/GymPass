package application.plano;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Empresa;
import dominio.cadastroRede.Funcionario;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

import java.time.LocalDate;

/** HU2 - camada de aplicação para a gestão do quadro de beneficiários. */
@Service
public class BeneficiariosService {

    private final Repositorio repositorio;

    public BeneficiariosService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    private Empresa empresa(int id) { return repositorio.getEmpresas().get(id); }

    private String emailDe(String nome) {
        return nome.toLowerCase().replaceAll("\\s+", ".") + "@empresa.com";
    }

    /** Inclusão no quadro e concessão do plano acontecem na mesma operação. */
    public void incluirFuncionario(int empresaId, String nome, String cpf,
                                   String matricula, int planoId) {
        Empresa empresa = empresa(empresaId);
        Plano plano = repositorio.getPlanos().get(planoId);
        exigirCpfLivre(cpf);

        Funcionario funcionario = new Funcionario(nome, cpf, emailDe(nome),
                matricula, LocalDate.now(), empresa);

        repositorio.getAdesoes().add(Adesao.cadastrarBeneficiario(funcionario, plano));
        repositorio.getAlunos().add(funcionario);
    }

    public void trocarPlano(int empresaId, int funcionarioId, int planoId) {
        Adesao atual = adesaoVigente(empresaId, funcionarioId);
        Plano novo = repositorio.getPlanos().get(planoId);
        repositorio.getAdesoes().add(Adesao.trocarPlano(atual, novo));
    }

    public void incluirDependente(int empresaId, int funcionarioId,
                                  String nome, String cpf, String grauParentesco) {
        Adesao adesao = adesaoVigente(empresaId, funcionarioId);
        Funcionario responsavel = empresa(empresaId).getFuncionarios().get(funcionarioId);
        exigirCpfLivre(cpf);

        Dependente dependente = new Dependente(nome, cpf, emailDe(nome),
                grauParentesco, responsavel);

        repositorio.getAdesoes().add(adesao.incluirDependente(dependente));
        repositorio.getAlunos().add(dependente);
    }

    public void removerFuncionario(int empresaId, int funcionarioId) {
        Empresa empresa = empresa(empresaId);
        empresa.removerFuncionario(empresa.getFuncionarios().get(funcionarioId));
    }

    /**
     * O CPF identifica o aluno na recepção (UC07, 2a), então não pode repetir.
     * Fica aqui porque só a lista de todos os alunos sabe se ele já existe.
     */
    private void exigirCpfLivre(String cpf) {
        Aluno existente = repositorio.alunoPorCpf(cpf);
        if (existente != null) {
            throw new RegraDeNegocioException(
                    "O CPF " + cpf + " já está cadastrado para " + existente + ".");
        }
    }

    private Adesao adesaoVigente(int empresaId, int funcionarioId) {
        Funcionario f = empresa(empresaId).getFuncionarios().get(funcionarioId);
        Adesao adesao = repositorio.adesaoDe(f);
        if (adesao == null) {
            throw new RegraDeNegocioException("Funcionário sem adesão vigente: " + f);
        }
        return adesao;
    }
}