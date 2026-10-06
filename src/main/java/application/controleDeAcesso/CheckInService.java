package application.controleDeAcesso;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.CheckIn;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import dominio.planosAdesao.Adesao;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

/**
 * UC03 e UC07 - camada de aplicação para o check-in e a validação na recepção.
 *
 * Busca os objetos envolvidos e entrega ao domínio: a Adesao valida e cria o
 * check-in; o próprio CheckIn decide se pode ser validado ou recusado.
 */
@Service
public class CheckInService {

    private final Repositorio repositorio;

    public CheckInService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public CheckIn realizarCheckIn(int alunoId, int estabelecimentoId,
                                   double latitude, double longitude) {
        Aluno aluno = repositorio.getAlunos().get(alunoId);
        Estabelecimento estabelecimento = repositorio.getEstabelecimentos().get(estabelecimentoId);
        Adesao adesao = adesaoVigente(aluno);
        CheckIn ultimo = repositorio.ultimoCheckInDe(aluno);

        CheckIn checkIn = adesao.registrarCheckIn(estabelecimento, latitude, longitude, ultimo);
        repositorio.getCheckIns().add(checkIn);
        return checkIn;
    }

    /**
     * UC07 passos 2 a 4 - a recepção informa o código e o sistema localiza o
     * check-in. Extensões 3a (código inexistente) e 3c (outro estabelecimento).
     */
    public CheckIn localizarNaRecepcao(int estabelecimentoId, String codigo) {
        Estabelecimento recepcao = repositorio.getEstabelecimentos().get(estabelecimentoId);
        CheckIn checkIn = repositorio.checkInPorCodigo(codigo);
        if (checkIn == null) {
            throw new RegraDeNegocioException(
                "Nenhum check-in com o código " + codigo
                + ". Peça ao aluno para refazer o check-in.");
        }
        checkIn.conferirNaRecepcao(recepcao);
        return checkIn;
    }

    /** UC07 passos 5 a 7 - a recepção confirma a entrada. */
    public void validarCheckIn(int estabelecimentoId, String codigo) {
        localizarNaRecepcao(estabelecimentoId, codigo).validar();
    }

    /** UC07 extensão 5a - a recepção recusa a entrada, com motivo. */
    public void recusarCheckIn(int estabelecimentoId, String codigo, String motivo) {
        localizarNaRecepcao(estabelecimentoId, codigo).recusar(motivo);
    }

    private Adesao adesaoVigente(Aluno aluno) {
        Adesao adesao = repositorio.adesaoDe(aluno);
        if (adesao == null) {
            throw new RegraDeNegocioException(aluno + " não tem adesão vigente.");
        }
        return adesao;
    }
}
