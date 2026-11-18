package dominio.planosAdesao;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Estabelecimento;

/** Estado final: nenhuma operação nem transição é aceita. */
public class Encerrada implements EstadoAdesao {

    private static RegraDeNegocioException encerrada() {
        return new RegraDeNegocioException("A adesão está encerrada.");
    }

    @Override
    public Adesao incluirDependente(Adesao ad, Dependente d) { throw encerrada(); }

    @Override
    public CheckIn registrarCheckIn(Adesao ad, Estabelecimento e,
                                    double lat, double lng, CheckIn ultimo) {
        throw encerrada();
    }

    @Override
    public Reserva reservarVaga(Adesao ad, Aula a, int reservasMes) { throw encerrada(); }

    @Override public Adesao trocarPlano(Adesao ad, Plano novoPlano) { throw encerrada(); }
    @Override public void suspender(Adesao ad) { throw encerrada(); }
    @Override public void reativar(Adesao ad) { throw encerrada(); }
    @Override public void encerrar(Adesao ad) { throw encerrada(); }

    @Override public boolean estaAtiva() { return false; }
    @Override public boolean estaVigente() { return false; }
    @Override public boolean podeSuspender() { return false; }
    @Override public boolean podeReativar() { return false; }
    @Override public SituacaoAdesao situacao() { return SituacaoAdesao.ENCERRADA; }
}
