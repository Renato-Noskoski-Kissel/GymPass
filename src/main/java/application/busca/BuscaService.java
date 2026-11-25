package application.busca;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Academia;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Estudio;
import dominio.cadastroRede.Quadra;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static application.NaoEncontradoException.exigir;

/**
 * UC02 - Buscar Estabelecimentos.
 *
 * Devolve os próprios Estabelecimentos, já filtrados e em ordem de distância.
 * Distância (Endereco.distanciaAte) e cobertura (Plano.libera) são perguntas
 * que o domínio já responde; quem monta a tela as faz de novo para cada item.
 *
 * Os filtros rodam em memória sobre listarEstabelecimentos(): a rede é pequena
 * e o Repositorio não precisou mudar. Para o pico do RNF7 o filtro do raio
 * teria de descer para uma consulta no banco.
 */
@Service
public class BuscaService {

    private final Repositorio repositorio;

    public BuscaService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Passos 4 a 6.
     *
     * @param modalidade nome da modalidade (sem diferenciar maiúsculas), ou vazio para todas
     * @param tipo       Academia, Estudio ou Quadra, ou vazio para todos
     * @param inicio     início da faixa de horário (ISO, ex.: 2026-11-25T18:00), ou vazio
     * @param fim        fim da faixa; com a faixa informada, só entram os que têm
     *                   aula não cancelada começando nela
     */
    @Transactional(readOnly = true)
    public List<Estabelecimento> buscarEstabelecimentos(String cpf, double latitude, double longitude,
                                                        double raioKm, String modalidade, String tipo,
                                                        String inicio, String fim,
                                                        boolean somenteCobertos) {
        Plano plano = planoVigente(cpf);
        if (raioKm <= 0) {
            throw new RegraDeNegocioException("Informe um raio de busca maior que zero.");
        }
        double raioMetros = raioKm * 1000;
        LocalDateTime[] faixa = faixa(inicio, fim);

        Stream<Estabelecimento> resultado = repositorio.listarEstabelecimentos().stream()
                .filter(Estabelecimento::estaCredenciado)
                .filter(e -> e.getEndereco().distanciaAte(latitude, longitude) <= raioMetros);

        if (informado(modalidade)) {
            resultado = resultado.filter(e -> e.getModalidades().stream()
                    .anyMatch(m -> m.getNome().equalsIgnoreCase(modalidade.trim())));
        }
        if (informado(tipo)) {
            resultado = resultado.filter(e -> ehDoTipo(e, tipo.trim()));
        }
        if (faixa != null) {
            resultado = resultado.filter(e -> !e.aulasEntre(faixa[0], faixa[1]).isEmpty());
        }
        if (somenteCobertos) {
            resultado = resultado.filter(plano::libera);
        }
        return resultado
                .sorted(Comparator.comparingDouble(e -> e.getEndereco().distanciaAte(latitude, longitude)))
                .toList();
    }

    /**
     * Passo 7. Extensão 7a: o estabelecimento pode ter sido suspenso entre a
     * consulta e a seleção; nesse caso a tela informa e volta para a lista.
     */
    @Transactional(readOnly = true)
    public Estabelecimento detalharEstabelecimento(String cpf, String cnpj) {
        planoVigente(cpf);
        Estabelecimento e = exigir(repositorio.buscarEstabelecimento(cnpj),
                "Estabelecimento não encontrado: " + cnpj);
        if (!e.estaCredenciado()) {
            throw new RegraDeNegocioException(
                e + " não está mais disponível na rede. Escolha outro estabelecimento da lista.");
        }
        return e;
    }

    /**
     * Pré-condição do UC02: o aluno tem adesão vigente, porque o nível do plano
     * classifica os resultados.
     */
    private Plano planoVigente(String cpf) {
        Aluno aluno = exigir(repositorio.buscarAluno(cpf), "Aluno não encontrado: " + cpf);
        Adesao adesao = repositorio.adesaoDe(aluno);
        if (adesao == null) {
            throw new RegraDeNegocioException(
                "A busca usa o nível do seu plano, e você não tem assinatura vigente.");
        }
        return adesao.getPlano();
    }

    /** A faixa vem inteira ou não vem; null quando não foi informada. */
    private LocalDateTime[] faixa(String inicio, String fim) {
        if (!informado(inicio) && !informado(fim)) return null;
        if (!informado(inicio) || !informado(fim)) {
            throw new RegraDeNegocioException("Informe o início e o fim da faixa de horário.");
        }
        try {
            LocalDateTime de = LocalDateTime.parse(inicio.trim());
            LocalDateTime ate = LocalDateTime.parse(fim.trim());
            if (!ate.isAfter(de)) {
                throw new RegraDeNegocioException("O fim da faixa de horário deve ser depois do início.");
            }
            return new LocalDateTime[] { de, ate };
        } catch (DateTimeParseException ex) {
            throw new RegraDeNegocioException("Data ou hora inválida na faixa de horário.");
        }
    }

    /** Passo 3c - academia, estúdio ou quadra. */
    private static boolean ehDoTipo(Estabelecimento e, String tipo) {
        return switch (tipo.toLowerCase()) {
            case "academia" -> e instanceof Academia;
            case "estudio", "estúdio" -> e instanceof Estudio;
            case "quadra" -> e instanceof Quadra;
            default -> throw new RegraDeNegocioException(
                "Tipo de estabelecimento desconhecido: " + tipo + ". Use Academia, Estudio ou Quadra.");
        };
    }

    private static boolean informado(String valor) {
        return valor != null && !valor.isBlank();
    }
}
