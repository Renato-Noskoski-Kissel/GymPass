package dominio.planosAdesao;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Estabelecimento;
import java.time.LocalDate;

/** RN7 - benefício bloqueado por inadimplência da empresa contratante. */
public class Suspensa implements EstadoAdesao {

    private static RegraDeNegocioException bloqueada() {
        return new RegraDeNegocioException(
            "Acesso bloqueado: a empresa contratante está inadimplente (RN7).");
    }

    @Override
    public Adesao incluirDependente(Adesao ad, Dependente d) { throw bloqueada(); }

    @Override
    public CheckIn registrarCheckIn(Adesao ad, Estabelecimento e,
                                    double lat, double lng, CheckIn ultimo) {
        throw bloqueada();
    }

    @Override
    public Reserva reservarVaga(Adesao ad, Aula a, int reservasMes) { throw bloqueada(); }

    /** Trocar de plano criaria uma adesão ativa e furaria o bloqueio de RN7. */
    @Override
    public Adesao trocarPlano(Adesao ad, Plano novoPlano) {
        throw new RegraDeNegocioException(
            "O benefício está suspenso por inadimplência da empresa (RN7). "
            + "Reative o benefício antes de trocar de plano.");
    }

    @Override
    public void suspender(Adesao ad) {
        throw new RegraDeNegocioException("A adesão já está suspensa.");
    }

    @Override
    public void reativar(Adesao ad) { ad.mudarPara(new Ativa()); }

    @Override
    public void encerrar(Adesao ad) {
        ad.registrarFim(LocalDate.now());
        ad.mudarPara(new Encerrada());
    }

    @Override public boolean estaAtiva() { return false; }
    @Override public boolean estaVigente() { return true; }
    @Override public boolean podeSuspender() { return false; }
    @Override public boolean podeReativar() { return true; }
    @Override public SituacaoAdesao situacao() { return SituacaoAdesao.SUSPENSA; }
}
