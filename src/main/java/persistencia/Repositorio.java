package persistencia;

import dominio.acessoAgenda.Aula;
import dominio.cadastroRede.*;
import dominio.planosAdesao.Adesao;
import dominio.planosAdesao.Plano;
import dominio.planosAdesao.SituacaoAdesao;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada de persistência da arquitetura lógica.
 *
 * Nesta iteração o armazenamento é em memória: os dados vivem enquanto o
 * servidor estiver rodando. A substituição por banco de dados não afeta as
 * camadas acima, que só enxergam esta classe.
 */
@Component
public class Repositorio {

    private final List<Estabelecimento> estabelecimentos = new ArrayList<>();
    private final List<Empresa> empresas = new ArrayList<>();
    private final List<Plano> planos = new ArrayList<>();
    private final List<Aula> aulas = new ArrayList<>();
    private final List<Adesao> adesoes = new ArrayList<>();

    public Repositorio() { semear(); }

    public List<Estabelecimento> getEstabelecimentos() { return estabelecimentos; }
    public List<Empresa> getEmpresas() { return empresas; }
    public List<Plano> getPlanos() { return planos; }
    public List<Aula> getAulas() { return aulas; }
    public List<Adesao> getAdesoes() { return adesoes; }

    /** Adesão vigente de um aluno, ou null se não houver. */
    public Adesao adesaoDe(Aluno aluno) {
        return adesoes.stream()
            .filter(a -> a.getAluno() == aluno)
            .filter(a -> a.getSituacao() != SituacaoAdesao.ENCERRADA)
            .findFirst().orElse(null);
    }

    private void semear() {
        Empresa tech = new Empresa("Tech Ltda", "11.111.111/0001-11",
                LocalDate.of(2026, 1, 10), 2, 10);
        Empresa norte = new Empresa("Norte Log", "33.333.333/0001-33",
                LocalDate.of(2026, 2, 3), 1, 5);
        empresas.add(tech);
        empresas.add(norte);

        Estudio studio = new Estudio("Studio Movimento", "22.222.222/0001-22", 12.5, 15);
        studio.aprovarCredenciamento();
        Academia forte = new Academia("Academia Forte", "44.444.444/0001-44", 9.9, true);
        forte.aprovarCredenciamento();
        Quadra areia = new Quadra("Quadra Areia", "55.555.555/0001-55", 15, false);
        estabelecimentos.add(studio);
        estabelecimentos.add(forte);
        estabelecimentos.add(areia);

        Modalidade pilates = new Modalidade("Pilates", "Aulas em grupo com aparelhos");
        studio.adicionarModalidade(pilates);
        studio.adicionarModalidade(new Modalidade("Yoga", "Hatha e Vinyasa"));
        Instrutor ana = studio.contratarInstrutor("Ana Reis", "CREF-1234", "Pilates");
        studio.contratarInstrutor("Caio Melo", "CREF-5678", "Yoga");

        forte.adicionarModalidade(new Modalidade("Musculacao", "Acesso livre a sala"));
        forte.contratarInstrutor("Rui Barros", "CREF-9999", "Musculacao");

        Plano premium = new Plano("Premium", 3, 189.9, 8, true);
        Plano basico = new Plano("Basico", 1, 79.9, 4, false);
        for (Plano p : List.of(premium, basico)) {
            p.oferecerPara(tech);
            p.oferecerPara(norte);
            planos.add(p);
        }

        Funcionario lara = new Funcionario("Lara Souza", "000.000.000-00", "lara@tech.com",
                "F-001", LocalDate.of(2025, 3, 1), tech);
        Funcionario bruno = new Funcionario("Bruno Dias", "222.222.222-22", "bruno@tech.com",
                "F-002", LocalDate.of(2024, 8, 15), tech);
        Adesao adLara = Adesao.cadastrarBeneficiario(lara, premium);
        adesoes.add(adLara);
        adesoes.add(Adesao.cadastrarBeneficiario(bruno, basico));

        Dependente miguel = new Dependente("Miguel Souza", "111.111.111-11",
                "miguel@mail.com", "Filho", lara);
        adesoes.add(adLara.incluirDependente(miguel));

        LocalDateTime amanha = LocalDateTime.now().plusDays(1)
                .withHour(7).withMinute(0).withSecond(0).withNano(0);
        aulas.add(new Aula(amanha, Duration.ofMinutes(50), 8, studio, pilates, ana));
    }
}
