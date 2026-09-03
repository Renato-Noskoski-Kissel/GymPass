package application.beneficiarios;

import dominio.RegraDeNegocioException;
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
    public void incluirFuncionario(int empresaId, String nome, String matricula, int planoId) {
        Empresa empresa = empresa(empresaId);
        Plano plano = repositorio.getPlanos().get(planoId);

        Funcionario funcionario = new Funcionario(nome, "000.000.000-00", emailDe(nome),
                matricula, LocalDate.now(), empresa);

        repositorio.getAdesoes().add(Adesao.cadastrarBeneficiario(funcionario, plano));
    }

    public void trocarPlano(int empresaId, int funcionarioId, int planoId) {
        Adesao atual = adesaoVigente(empresaId, funcionarioId);
        Plano novo = repositorio.getPlanos().get(planoId);
        repositorio.getAdesoes().add(Adesao.trocarPlano(atual, novo));
    }

    public void incluirDependente(int empresaId, int funcionarioId,
                                  String nome, String grauParentesco) {
        Adesao adesao = adesaoVigente(empresaId, funcionarioId);
        Funcionario responsavel = empresa(empresaId).getFuncionarios().get(funcionarioId);

        Dependente dependente = new Dependente(nome, "000.000.000-00", emailDe(nome),
                grauParentesco, responsavel);

        repositorio.getAdesoes().add(adesao.incluirDependente(dependente));
    }

    public void removerFuncionario(int empresaId, int funcionarioId) {
        Empresa empresa = empresa(empresaId);
        empresa.removerFuncionario(empresa.getFuncionarios().get(funcionarioId));
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
