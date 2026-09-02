import dominio.RegraDeNegocioException;
import dominio.agenda.Aula;
import dominio.cadastro.*;
import dominio.planos.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Teste manual do domínio da Iteração 1. Não faz parte da camada de domínio. */
public class Main {

    public static void main(String[] args) {

        // ===== HU4 - admin cadastra parceiros e planos =====
        Empresa empresa = new Empresa("Tech Ltda", "11.111.111/0001-11",
                LocalDate.of(2026, 1, 10), 2, 10);

        Estudio estudio = new Estudio("Studio Movimento", "22.222.222/0001-22", 12.50, 15);
        estudio.aprovarCredenciamento();
        System.out.println("Credenciado? " + estudio.estaCredenciado());

        Plano premium = new Plano("Premium", 3, 189.90, 8, true);
        Plano basico  = new Plano("Basico",  1,  79.90, 4, false);
        premium.oferecerPara(empresa);
        basico.oferecerPara(empresa);

        // ===== HU5 - estabelecimento cadastra modalidades e instrutores =====
        Modalidade pilates = new Modalidade("Pilates", "Aulas em grupo com aparelhos");
        estudio.adicionarModalidade(pilates);
        Instrutor ana = estudio.contratarInstrutor("Ana Reis", "CREF-1234", "Pilates");
        Instrutor caio = estudio.contratarInstrutor("Caio Melo", "CREF-5678", "Pilates");
        System.out.println("Instrutores: " + estudio.getInstrutores().size());

        // ===== HU2 - empresa gerencia funcionarios =====
        Funcionario lara = new Funcionario("Lara", "000.000.000-00", "lara@tech.com",
                "F-001", LocalDate.of(2025, 3, 1), empresa);
        Funcionario bruno = new Funcionario("Bruno", "222.222.222-22", "bruno@tech.com",
                "F-002", LocalDate.of(2024, 8, 15), empresa);
        // cadastro e concessao do plano acontecem na mesma operacao
        Adesao adesaoLara = Adesao.cadastrarBeneficiario(lara, premium);
        Adesao adesaoBruno = Adesao.cadastrarBeneficiario(bruno, basico);
        System.out.println("Funcionarios: " + empresa.getTotalFuncionarios());
        System.out.println("Plano da Lara: " + adesaoLara.getPlano());

        // HU2 - troca de plano
        adesaoBruno = Adesao.trocarPlano(adesaoBruno, premium);
        System.out.println("Novo plano do Bruno: " + adesaoBruno.getPlano());

        // RN3 - dependente entra no mesmo plano do responsavel
        Dependente miguel = new Dependente("Miguel", "111.111.111-11", "miguel@mail.com",
                "Filho", lara);
        Adesao adesaoMiguel = adesaoLara.incluirDependente(miguel);
        System.out.println("Dependente no plano: " + adesaoMiguel.getPlano());
        System.out.println("Beneficiarios (func + dep): " + empresa.getTotalBeneficiarios());

        // RN3 - plano Basico nao permite dependentes
        Dependente filhoBruno = new Dependente("Nina", "333.333.333-33", "nina@mail.com",
                "Filha", bruno);
        try {
            adesaoBruno.incluirDependente(filhoBruno);
        } catch (RegraDeNegocioException e) {
            System.out.println("Bloqueado (RN3): " + e.getMessage());
        }

        // ===== HU1 - estudio gerencia aulas =====
        Aula aula = new Aula(LocalDateTime.now().plusDays(2),
                Duration.ofMinutes(50), 10, estudio, pilates, ana);
        System.out.println("Aula criada: " + aula);

        aula.atribuirInstrutor(caio);
        System.out.println("Novo instrutor: " + aula.getInstrutor());

        aula.alterarCapacidade(12);
        System.out.println("Capacidade: " + aula.getCapacidadeMaxima());

        // HU1 - instrutor de outro estabelecimento nao pode ser atribuido
        Academia outra = new Academia("Academia Forte", "44.444.444/0001-44", 9.90, true);
        Instrutor externo = outra.contratarInstrutor("Rui", "CREF-9999", "Musculacao");
        try {
            aula.atribuirInstrutor(externo);
        } catch (RegraDeNegocioException e) {
            System.out.println("Bloqueado (instrutor externo): " + e.getMessage());
        }

        // ===== HU2 - remocao do quadro derruba a elegibilidade em cascata (RN1) =====
        empresa.removerFuncionario(lara);
        System.out.println("Funcionarios apos remocao: " + empresa.getTotalFuncionarios());
        System.out.println("Lara elegivel? " + lara.ehElegivel());
        System.out.println("Miguel elegivel? " + miguel.ehElegivel());
        System.out.println("Adesao da Lara ativa? " + adesaoLara.estaAtiva());
        System.out.println("Adesao do Miguel ativa? " + adesaoMiguel.estaAtiva());
    }
}
