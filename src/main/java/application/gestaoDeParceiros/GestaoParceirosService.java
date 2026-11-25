package application.gestaoDeParceiros;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.*;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import java.time.LocalDate;

import static application.NaoEncontradoException.exigir;

/**
 * HU4 - camada de aplicação para cadastro de parceiros e configuração de planos.
 *
 * Coordena a operação: cria os objetos de domínio na ordem correta e os entrega
 * à persistência. Nenhuma regra de negócio mora aqui — elas estão no domínio.
 */
@Service
public class GestaoParceirosService {

    private final Repositorio repositorio;

    public GestaoParceirosService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void cadastrarEstabelecimento(String cnpj, String nome, String tipo, double valorPorCheckIn,
                                         int nivelMinimo, String logradouro, String numero, String bairro,
                                         String cidade, String cep,
                                         double latitude, double longitude) {
        Estabelecimento existente = repositorio.buscarEstabelecimento(cnpj);
        if (existente != null) {
            throw new RegraDeNegocioException("O CNPJ já está cadastrado para " + existente + ".");
        }
        Endereco endereco = new Endereco(logradouro, numero, bairro, cidade, cep,
                latitude, longitude);
        Estabelecimento novo = switch (tipo) {
            case "Academia" -> new Academia(nome, cnpj, valorPorCheckIn, nivelMinimo, endereco, false);
            case "Quadra"   -> new Quadra(nome, cnpj, valorPorCheckIn, nivelMinimo, endereco, false);
            default         -> new Estudio(nome, cnpj, valorPorCheckIn, nivelMinimo, endereco, 15);
        };
        repositorio.adicionarEstabelecimento(novo);
    }

    @Transactional
    public void aprovar(String cnpj) {
        estabelecimento(cnpj).aprovarCredenciamento();
    }

    @Transactional
    public void suspender(String cnpj) {
        estabelecimento(cnpj).suspenderCredenciamento();
    }

    @Transactional
    public void contratarEmpresa(String cnpj, String razaoSocial, int limiteDependentes, int diaVencimento) {
        Empresa existente = repositorio.buscarEmpresa(cnpj);
        if (existente != null) {
            throw new RegraDeNegocioException("O CNPJ já está cadastrado para " + existente + ".");
        }
        Empresa empresa = new Empresa(razaoSocial, cnpj, LocalDate.now(),
                limiteDependentes, diaVencimento);
        repositorio.adicionarEmpresa(empresa);
        // Começa sem plano: a própria empresa escolhe o que oferece
        // (BeneficiariosService.oferecerPlano). Até lá não inclui funcionários.
    }

    /**
     * O plano nasce sem ser oferecido a ninguém. Cada empresa decide se o
     * oferece aos funcionários (BeneficiariosService.oferecerPlano).
     */
    @Transactional
    public void criarPlano(String nome, int nivel, double valorMensal,
                           int limiteAulasMes, boolean permiteDependentes) {
        repositorio.adicionarPlano(new Plano(nome, nivel, valorMensal, limiteAulasMes, permiteDependentes));
    }

    private Estabelecimento estabelecimento(String cnpj) {
        return exigir(repositorio.buscarEstabelecimento(cnpj),
                "Estabelecimento não encontrado: " + cnpj);
    }
}
