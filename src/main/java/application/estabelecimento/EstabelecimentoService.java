package application.estabelecimento;

import dominio.cadastroRede.Estabelecimento;
import dominio.cadastroRede.Modalidade;
import org.springframework.stereotype.Service;
import persistencia.Repositorio;

/** HU5 - camada de aplicação para modalidades e instrutores. */
@Service
public class EstabelecimentoService {

    private final Repositorio repositorio;

    public EstabelecimentoService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    private Estabelecimento estabelecimento(int id) {
        return repositorio.getEstabelecimentos().get(id);
    }

    public void adicionarModalidade(int id, String nome, String descricao) {
        estabelecimento(id).adicionarModalidade(new Modalidade(nome, descricao));
    }

    public void removerModalidade(int id, int modalidadeId) {
        Estabelecimento e = estabelecimento(id);
        e.removerModalidade(e.getModalidades().get(modalidadeId));
    }

    public void contratarInstrutor(int id, String nome, String registro, String especialidade) {
        estabelecimento(id).contratarInstrutor(nome, registro, especialidade);
    }

    public void desligarInstrutor(int id, int instrutorId) {
        Estabelecimento e = estabelecimento(id);
        e.desligarInstrutor(e.getInstrutores().get(instrutorId));
    }
}
