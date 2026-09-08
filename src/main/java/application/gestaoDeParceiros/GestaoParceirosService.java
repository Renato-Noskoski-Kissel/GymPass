package application.gestaoDeParceiros;

import dominio.cadastroRede.*;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

import java.time.LocalDate;

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

    public void cadastrarEstabelecimento(String nome, String tipo, double valorPorCheckIn) {
        String cnpj = "00.000.000/0001-00";
        Estabelecimento novo = switch (tipo) {
            case "Academia" -> new Academia(nome, cnpj, valorPorCheckIn, false);
            case "Quadra"   -> new Quadra(nome, cnpj, valorPorCheckIn, false);
            default         -> new Estudio(nome, cnpj, valorPorCheckIn, 15);
        };
        repositorio.getEstabelecimentos().add(novo);
    }

    public void aprovar(int id) {
        repositorio.getEstabelecimentos().get(id).aprovarCredenciamento();
    }

    public void suspender(int id) {
        repositorio.getEstabelecimentos().get(id).suspenderCredenciamento();
    }

    public void contratarEmpresa(String razaoSocial, int limiteDependentes, int diaVencimento) {
        repositorio.getEmpresas().add(new Empresa(razaoSocial, "00.000.000/0001-00",
                LocalDate.now(), limiteDependentes, diaVencimento));
    }

    public void criarPlano(String nome, int nivel, double valorMensal,
                           int limiteAulasMes, boolean permiteDependentes) {
        Plano plano = new Plano(nome, nivel, valorMensal, limiteAulasMes, permiteDependentes);
        repositorio.getEmpresas().forEach(plano::oferecerPara);
        repositorio.getPlanos().add(plano);
    }
}
