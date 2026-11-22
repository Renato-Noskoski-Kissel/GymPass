package application.gestaoDeParceiros;

import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Instrutor;
import dominio.cadastroRede.Modalidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistencia.Repositorio;

import static application.NaoEncontradoException.exigir;

/**
 * HU5 - camada de aplicação para modalidades e instrutores.
 *
 * Modalidade e Instrutor não têm adicionarX no Repositorio: são partes do
 * Estabelecimento e vão para o banco em cascata quando a transação termina.
 * Remover da lista apaga a linha; se houver aula usando a modalidade ou o
 * instrutor, o banco recusa (FK) e a tela mostra o motivo.
 */
@Service
public class EstabelecimentoService {

    private final Repositorio repositorio;

    public EstabelecimentoService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    private Estabelecimento estabelecimento(String cnpj) {
        return exigir(repositorio.buscarEstabelecimento(cnpj),
                "Estabelecimento não encontrado: " + cnpj);
    }

    @Transactional
    public void adicionarModalidade(String cnpj, String nome, String descricao) {
        estabelecimento(cnpj).adicionarModalidade(new Modalidade(nome, descricao));
    }

    @Transactional
    public void removerModalidade(String cnpj, int modalidadeId) {
        Estabelecimento e = estabelecimento(cnpj);
        Modalidade m = exigir(e.getModalidades().stream()
                .filter(x -> x.getId() == modalidadeId).findFirst().orElse(null),
                e + " não tem a modalidade " + modalidadeId);
        e.removerModalidade(m);
    }

    @Transactional
    public void contratarInstrutor(String cnpj, String nome, String registro, String especialidade) {
        estabelecimento(cnpj).contratarInstrutor(nome, registro, especialidade);
    }

    @Transactional
    public void desligarInstrutor(String cnpj, String registro) {
        Estabelecimento e = estabelecimento(cnpj);
        Instrutor i = exigir(e.getInstrutores().stream()
                .filter(x -> x.getRegistro().equals(registro)).findFirst().orElse(null),
                e + " não tem o instrutor " + registro);
        e.desligarInstrutor(i);
    }
}
