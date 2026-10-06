import dominio.RegraDeNegocioException;
import dominio.acessoAgenda.Aula;
import dominio.acessoAgenda.CheckIn;
import dominio.acessoAgenda.Reserva;
import dominio.cadastroRede.*;
import dominio.planosAdesao.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Teste manual do domínio (iterações 1 e 2). Não faz parte da camada de domínio. */
public class Main {

    public static void main(String[] args) {

        // ===== HU4 - admin cadastra parceiros e planos =====
        Empresa empresa = new Empresa("Tech Ltda", "11.111.111/0001-11",
                LocalDate.of(2026, 1, 10), 2, 10);

        Endereco endStudio = new Endereco("Rua Lauro Linhares", "1200", "Trindade",
                "Florianópolis", "88036-002", -27.5893, -48.5196);
        Estudio estudio = new Estudio("Studio Movimento", "22.222.222/0001-22", 12.50,
                endStudio, 15);
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
        Funcionario carla = new Funcionario("Carla", "444.444.444-44", "carla@tech.com",
                "F-003", LocalDate.of(2025, 6, 2), empresa);
        Adesao adesaoCarla = Adesao.cadastrarBeneficiario(carla, basico);
        Dependente filhaCarla = new Dependente("Nina", "333.333.333-33", "nina@mail.com",
                "Filha", carla);
        tentar("RN3", () -> adesaoCarla.incluirDependente(filhaCarla));

        // ===== HU1 - estudio gerencia aulas =====
        Aula aula = estudio.agendarAula(LocalDateTime.now().plusDays(2),
                Duration.ofMinutes(50), 10, pilates, ana);
        System.out.println("Aula criada: " + aula);

        aula.atribuirInstrutor(caio);
        System.out.println("Novo instrutor: " + aula.getInstrutor());

        aula.alterarCapacidade(12);
        System.out.println("Capacidade: " + aula.getCapacidadeMaxima());

        // HU1 - instrutor de outro estabelecimento nao pode ser atribuido
        Endereco endForte = new Endereco("Rua Deputado Antônio Edu Vieira", "800", "Pantanal",
                "Florianópolis", "88040-001", -27.6036, -48.5210);
        Academia forte = new Academia("Academia Forte", "44.444.444/0001-44", 9.90, endForte, true);
        forte.aprovarCredenciamento();
        Instrutor externo = forte.contratarInstrutor("Rui", "CREF-9999", "Musculacao");
        tentar("instrutor externo", () -> aula.atribuirInstrutor(externo));

        // ===== RF5 - cobertura dos planos =====
        premium.liberar(estudio);
        premium.liberar(pilates);
        premium.liberar(forte);
        basico.liberar(forte);
        System.out.println("Premium cobre o Studio? " + premium.libera(estudio));
        System.out.println("Basico cobre o Studio? " + basico.libera(estudio));

        double latPorta = endStudio.getLatitude();
        double lngPorta = endStudio.getLongitude();

        // ===== UC03 - Realizar Check-in =====
        CheckIn checkInLara = adesaoLara.registrarCheckIn(estudio, latPorta, lngPorta, null);
        System.out.println("Check-in: " + checkInLara);

        // RN2 - segundo check-in no mesmo dia
        tentar("RN2", () -> adesaoLara.registrarCheckIn(forte,
                endForte.getLatitude(), endForte.getLongitude(), checkInLara));

        // RN4 - cerca de 1 km ao norte do estudio
        tentar("RN4", () -> adesaoMiguel.registrarCheckIn(estudio,
                latPorta + 0.009, lngPorta, null));

        // RF5 - plano Basico nao cobre o estudio
        tentar("RF5 check-in", () -> adesaoCarla.registrarCheckIn(estudio,
                latPorta, lngPorta, null));

        // Basico cobre a academia
        CheckIn checkInCarla = adesaoCarla.registrarCheckIn(forte,
                endForte.getLatitude(), endForte.getLongitude(), null);
        System.out.println("Check-in: " + checkInCarla);

        // ===== UC04a - Reservar Vaga em Aula =====
        Aula turmaPequena = estudio.agendarAula(LocalDateTime.now().plusDays(1),
                Duration.ofMinutes(50), 1, pilates, ana);
        Reserva reserva = adesaoLara.reservarVaga(turmaPequena, 0);
        System.out.println(reserva);
        System.out.println("Vagas livres: " + turmaPequena.vagasDisponiveis());

        // 3a - aula lotada
        tentar("lotada", () -> adesaoMiguel.reservarVaga(turmaPequena, 0));

        // limite mensal - o servico passa quantas reservas o aluno ja fez no mes
        tentar("limite mensal", () -> adesaoMiguel.reservarVaga(aula,
                premium.getLimiteAulasMes()));

        // RF5 - plano Basico nao cobre o estudio
        tentar("RF5 reserva", () -> adesaoCarla.reservarVaga(aula, 0));

        // capacidade nao pode ficar abaixo das vagas reservadas
        adesaoMiguel.reservarVaga(aula, 0);
        adesaoBruno.reservarVaga(aula, 0);
        tentar("capacidade", () -> aula.alterarCapacidade(1));

        // aula cancelada nao recebe reserva
        Aula cancelada = estudio.agendarAula(LocalDateTime.now().plusDays(3),
                Duration.ofMinutes(50), 5, pilates, ana);
        cancelada.cancelar();
        tentar("cancelada", () -> adesaoLara.reservarVaga(cancelada, 1));

        // ===== UC04b - Cancelar Reserva =====
        // no prazo: sem penalidade, e a vaga volta para outro aluno
        System.out.println("Vagas antes: " + turmaPequena.vagasDisponiveis());
        reserva.cancelar(LocalDateTime.now());
        System.out.println("Cancelada no prazo: " + reserva + " | vagas: "
                + turmaPequena.vagasDisponiveis());
        System.out.println("Vaga reaproveitada: " + adesaoMiguel.reservarVaga(turmaPequena, 1));

        // menos de 2 h antes da aula (RN5): penalizada
        Aula daquiAPouco = estudio.agendarAula(LocalDateTime.now().plusMinutes(90),
                Duration.ofMinutes(50), 5, pilates, ana);
        Reserva tardia = adesaoLara.reservarVaga(daquiAPouco, 1);
        System.out.println("Seria penalizado? " + tardia.cancelamentoSeriaPenalizado(LocalDateTime.now()));
        tardia.cancelar(LocalDateTime.now());
        System.out.println("Cancelada tarde: " + tardia);

        // 4b - aula ja comecou (o momento e passado de fora)
        Reserva atrasada = adesaoLara.reservarVaga(daquiAPouco, 2);
        tentar("4b aula comecou", () -> atrasada.cancelar(daquiAPouco.getDataHoraInicio().plusMinutes(5)));

        // 4c - aula cancelada pelo estabelecimento: sem penalidade mesmo em cima da hora
        daquiAPouco.cancelar();
        atrasada.cancelar(LocalDateTime.now());
        System.out.println("Aula cancelada pelo estabelecimento: " + atrasada);

        // ja cancelada
        tentar("cancelar duas vezes", () -> tardia.cancelar(LocalDateTime.now()));

        // ===== UC07 - Validar Check-in na Recepcao =====
        System.out.println("Codigo do comprovante: " + checkInLara.getCodigo());
        tentar("3c outro estabelecimento", () -> checkInLara.conferirNaRecepcao(forte));
        checkInLara.conferirNaRecepcao(estudio);
        checkInLara.validar();
        System.out.println("Validado: " + checkInLara);
        tentar("validar duas vezes", checkInLara::validar);

        tentar("recusa sem motivo", () -> checkInCarla.recusar("  "));
        checkInCarla.recusar("Documento nao confere");
        System.out.println("Recusado: " + checkInCarla + " - " + checkInCarla.getMotivoRecusa());

        // ===== HU2 - remocao do quadro derruba a elegibilidade em cascata (RN1) =====
        empresa.removerFuncionario(lara);
        System.out.println("Funcionarios apos remocao: " + empresa.getTotalFuncionarios());
        System.out.println("Lara elegivel? " + lara.ehElegivel());
        System.out.println("Miguel elegivel? " + miguel.ehElegivel());
        System.out.println("Adesao da Lara ativa? " + adesaoLara.estaAtiva());
        System.out.println("Adesao do Miguel ativa? " + adesaoMiguel.estaAtiva());

        tentar("RN1 check-in", () -> adesaoMiguel.registrarCheckIn(estudio,
                latPorta, lngPorta, null));
        tentar("RN1 reserva", () -> adesaoLara.reservarVaga(aula, 1));
    }

    private static void tentar(String regra, Runnable acao) {
        try {
            acao.run();
            System.out.println("ERRO: deveria ter bloqueado (" + regra + ")");
        } catch (RegraDeNegocioException e) {
            System.out.println("Bloqueado (" + regra + "): " + e.getMessage());
        }
    }
}
