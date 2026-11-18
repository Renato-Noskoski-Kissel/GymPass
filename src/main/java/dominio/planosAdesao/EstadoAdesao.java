package dominio.planosAdesao;

import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.Dependente;
import dominio.cadastroRede.Estabelecimento;

/**
 * Padrão State: cada situação da Adesao é um objeto que decide se as operações
 * de negócio e as transições são permitidas. A Adesao só delega.
 */
public interface EstadoAdesao {

    Adesao incluirDependente(Adesao ad, Dependente d);

    CheckIn registrarCheckIn(Adesao ad, Estabelecimento e, double lat, double lng, CheckIn ultimo);

    Reserva reservarVaga(Adesao ad, Aula a, int reservasMes);

    /** HU2 - troca de plano: encerra a adesão atual e devolve a nova. */
    Adesao trocarPlano(Adesao ad, Plano novoPlano);

    void suspender(Adesao ad);

    void reativar(Adesao ad);

    void encerrar(Adesao ad);

    boolean estaAtiva();

    /** Ativa e Suspensa continuam sendo a adesão corrente do aluno; Encerrada não. */
    boolean estaVigente();

    /** A transição suspender() é aceita neste estado? (para a tela decidir o botão) */
    boolean podeSuspender();

    /** A transição reativar() é aceita neste estado? */
    boolean podeReativar();

    /** Rótulo para exibição; nenhuma regra deve comparar esse valor. */
    SituacaoAdesao situacao();
}
