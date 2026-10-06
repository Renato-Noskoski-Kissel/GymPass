package application.controleDeAcesso;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.CheckIn;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import dominio.planosAdesao.Adesao;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

/**
 * UC03 - camada de aplicação para o check-in.
 *
 * Busca os objetos envolvidos, entrega à Adesao (que valida e cria o check-in)
 * e guarda o resultado. Nenhuma regra de negócio mora aqui.
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

    private Adesao adesaoVigente(Aluno aluno) {
        Adesao adesao = repositorio.adesaoDe(aluno);
        if (adesao == null) {
            throw new RegraDeNegocioException(aluno + " não tem adesão vigente.");
        }
        return adesao;
    }
}
