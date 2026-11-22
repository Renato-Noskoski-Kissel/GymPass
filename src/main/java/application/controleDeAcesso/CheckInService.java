package application.controleDeAcesso;

import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.CheckInExpiradoException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import dominio.planosAdesao.Adesao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import static application.NaoEncontradoException.exigir;

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

    @Transactional
    public CheckIn realizarCheckIn(String cpf, String cnpj, double latitude, double longitude) {
        Aluno aluno = exigir(repositorio.buscarAluno(cpf), "Aluno não encontrado: " + cpf);
        Estabelecimento estabelecimento = estabelecimento(cnpj);
        Adesao adesao = adesaoVigente(aluno);
        CheckIn ultimo = repositorio.ultimoCheckInDe(aluno);

        CheckIn checkIn = adesao.registrarCheckIn(estabelecimento, latitude, longitude, ultimo);
        repositorio.adicionarCheckIn(checkIn);
        return checkIn;
    }

    /**
     * UC07 passos 2 a 4 - a recepção informa o código e o sistema localiza o
     * check-in. Extensões 3a (código inexistente) e 3c (outro estabelecimento).
     */
    @Transactional
    public CheckIn localizarNaRecepcao(String cnpj, String codigo) {
        Estabelecimento recepcao = estabelecimento(cnpj);
        CheckIn checkIn = repositorio.checkInPorCodigo(codigo);
        if (checkIn == null) {
            throw new RegraDeNegocioException(
                    "Nenhum check-in com o código " + codigo
                            + ". Peça ao aluno para refazer o check-in.");
        }
        checkIn.conferirNaRecepcao(recepcao);
        return checkIn;
    }

    /**
     * UC07 extensão 2a - sem o código, a recepção busca pelo CPF. Localiza o
     * check-in mais recente do aluno e segue as mesmas conferências (3a, 3c).
     */
    @Transactional
    public CheckIn localizarPorCpfNaRecepcao(String cnpj, String cpf) {
        Estabelecimento recepcao = estabelecimento(cnpj);
        Aluno aluno = repositorio.buscarAluno(cpf);
        if (aluno == null) {
            throw new RegraDeNegocioException("Nenhum aluno com o CPF " + cpf + ".");
        }
        CheckIn checkIn = repositorio.ultimoCheckInDe(aluno);
        if (checkIn == null) {
            throw new RegraDeNegocioException(
                    aluno + " não tem check-in. Peça ao aluno para fazer o check-in.");
        }
        checkIn.conferirNaRecepcao(recepcao);
        return checkIn;
    }

    /**
     * UC07 passos 5 a 7 - a recepção confirma a entrada.
     *
     * noRollbackFor: um pendente vencido é marcado EXPIRADO e a validação é
     * recusada na mesma chamada (UC07 3b). Sem isto, a exceção desfaria
     * também a expiração. Só a CheckInExpiradoException é poupada; qualquer
     * outra recusa de regra desfaz a transação normalmente.
     */
    @Transactional(noRollbackFor = CheckInExpiradoException.class)
    public void validarCheckIn(String cnpj, String codigo) {
        localizarNaRecepcao(cnpj, codigo).validar();
    }

    /** UC07 extensão 5a - a recepção recusa a entrada, com motivo. noRollbackFor: ver validarCheckIn. */
    @Transactional(noRollbackFor = CheckInExpiradoException.class)
    public void recusarCheckIn(String cnpj, String codigo, String motivo) {
        localizarNaRecepcao(cnpj, codigo).recusar(motivo);
    }

    private Estabelecimento estabelecimento(String cnpj) {
        return exigir(repositorio.buscarEstabelecimento(cnpj),
                "Estabelecimento não encontrado: " + cnpj);
    }

    private Adesao adesaoVigente(Aluno aluno) {
        Adesao adesao = repositorio.adesaoDe(aluno);
        if (adesao == null) {
            throw new RegraDeNegocioException(aluno + " não tem adesão vigente.");
        }
        return adesao;
    }
}
