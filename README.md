# GymPass — Iteração 3

Sistema de benefício corporativo de atividade física.
Domínio em Java puro, API REST com Spring Boot, interface web.

## Rodar

Requisitos: Docker com Compose, JDK 17 ou superior e Maven.

### 1. Subir o banco

```
docker compose up -d
```

Na primeira vez, o Docker baixa a imagem do PostgreSQL 16 e cria o banco com
as tabelas e os dados de exemplo. Para conferir se está no ar:

```
docker ps --filter name=gympass-db
```

### 2. Subir a aplicação

```
mvn spring-boot:run
```

Ou, no IntelliJ: File → Open, apontar para a pasta do projeto, abrir como
Maven e rodar a classe `sistema.Aplicacao` (o IntelliJ já traz o Maven embutido).

Abrir <http://localhost:8080>. A primeira execução baixa as dependências e demora.

Para rodar apenas o teste de console do domínio (`Main.java`), use o botão de
run do IntelliJ nessa classe.

### Comandos úteis

| Para quê | Comando |
|---|---|
| Parar o banco sem perder os dados | `docker compose down` |
| Zerar o banco (apaga o volume e roda os `.sql` de novo) | `docker compose down -v && docker compose up -d` |
| Abrir o banco em SQL | `docker exec -it gympass-db psql -U gympass -d gympass` |
| Ver os logs do Postgres | `docker logs gympass-db` |

### Problemas comuns

- **`Connection refused` na porta 5433**: o container não está rodando. Rode
  `docker compose up -d`.
- **`Schema-validation: missing column/table`**: o volume foi criado com uma
  versão antiga do `01-modelagem.sql`. Zere o banco com `down -v`.
- **Dados bagunçados depois de muitos testes**: zere o banco com `down -v`.

## Banco de dados

### Docker

O `docker-compose.yml` sobe um container `gympass-db` com o PostgreSQL 16.
Ninguém precisa instalar o Postgres na máquina, e todo o grupo usa a mesma
versão e as mesmas credenciais.

| Configuração | O que faz |
|---|---|
| `image: postgres:16` | Imagem oficial do PostgreSQL 16 |
| `POSTGRES_DB / USER / PASSWORD` | Cria o banco `gympass`, usuário `gympass`, senha `gympass123` |
| `ports: "5433:5432"` | O Postgres escuta na 5432 dentro do container e aparece como **5433** no computador, para não brigar com um Postgres local na 5432 |
| `./banco:/docker-entrypoint-initdb.d` | Os scripts de `banco/` rodam sozinhos, em ordem alfabética |
| `gympass-dados:/var/lib/postgresql/data` | Volume nomeado: os dados continuam lá mesmo depois de desligar ou apagar o container |

### Scripts

1. **`banco/01-modelagem.sql`**: cria as tabelas. É a fonte da verdade do
   esquema: as tabelas não mudam, o código Java se adapta a elas.
   - Chave natural onde o próprio dado identifica a entidade: `cnpj CHAR(14)`
     (estabelecimentos e empresas), `cpf CHAR(11)` (alunos) e `registro`
     (instrutores). CNPJ e CPF são guardados só com dígitos. As demais tabelas
     usam `id SERIAL`.
   - Academia, Estúdio e Quadra ficam numa tabela única (`estabelecimentos`),
     e a coluna `tipo` diz qual é a subclasse. O endereço fica embutido como colunas.
   - Enums viram `VARCHAR` com `CHECK`.
2. **`banco/02-dados.sql`**: dados iniciais, os mesmos que o antigo
   `Repositorio.semear()` criava em memória. Usa o fuso `America/Sao_Paulo`
   e gera as datas das aulas a partir do dia em que o banco é criado, para a
   grade não começar no passado.

**Os scripts rodam só na primeira inicialização**, quando o volume está vazio.
Depois de alterar um `.sql`, é preciso zerar o banco (`docker compose down -v`).

### Como a aplicação usa o banco

```
Controllers → Services (@Transactional) → Domínio → Repositorio (interface)
                                                        ↓
                                              RepositorioPostgres
                                                        ↓
                                     persistencia/jpa/*Jpa (Spring Data)
                                                        ↓
                                            PostgreSQL no container
```

- **Conexão**: `application.properties` aponta para
  `jdbc:postgresql://localhost:5433/gympass`. Usuário e senha podem ser
  trocados pelas variáveis de ambiente `DB_USER` e `DB_PASSWORD`; sem elas,
  valem `gympass` / `gympass123`.
- **`ddl-auto=validate`**: o Hibernate não cria nem altera tabelas. Na
  subida, ele só confere se as classes Java batem com o esquema. Se não
  baterem, a aplicação não sobe.
- **`Repositorio`** é a interface que as camadas de cima enxergam.
  **`RepositorioPostgres`** a implementa delegando para interfaces Spring
  Data (`AdesaoJpa`, `AlunoJpa` etc.), que o Spring implementa sozinho.
  - Entidades de chave natural (CNPJ, CPF) são gravadas com
    `entityManager.persist()`, não com `save()`. O `save()` faria um `merge`
    e gravaria uma cópia, quebrando as referências entre objetos.
  - CNPJ e CPF podem chegar com pontuação; a pontuação é removida antes de consultar.
- **Transações**: são abertas pelos Services (fachada). Uma operação de
  negócio inteira usa o mesmo contexto de persistência.
- **Padrão State**: o `ConversorEstadoAdesao` grava o estado da `Adesao`
  (Ativa, Suspensa ou Encerrada) como texto em `adesoes.situacao` e recria o
  objeto de estado na leitura. O domínio não precisa saber disso, e
  `estaVigente()` continua em Java.
- **`open-in-view=true`**: mantém a sessão aberta até o fim da requisição,
  para que coleções preguiçosas possam ser carregadas depois dos Services.

## Camadas

| Camada do diagrama | Pacote |
|---|---|
| Apresentação | `presentation.web` + `src/main/resources/static` |
| Aplicação | `application.gestaoDeParceiros`, `application.plano`, `application.agendamento`, `application.controleDeAcesso`, `application.rotinasDeSistema` |
| Domínio | `dominio.cadastroRede`, `dominio.planosAdesao`, `dominio.acessoAgenda`, `dominio.financeiro` |
| Persistência | `persistencia.Repositorio`, `persistencia.RepositorioPostgres`, `persistencia.jpa` |

As classes de domínio não dependem do Spring: o mapeamento para o banco usa
anotações JPA (`jakarta.persistence`) e, em poucos pontos, do Hibernate. A dependência vai sempre de cima para
baixo: apresentação conhece aplicação, aplicação conhece domínio, domínio não
conhece ninguém acima.

| Service (fachada) | Pacote | Cobre |
|---|---|---|
| `GestaoParceirosService` | `gestaoDeParceiros` | HU4 |
| `EstabelecimentoService` | `gestaoDeParceiros` | HU5 |
| `BeneficiariosService` | `plano` | HU2 |
| `GradeService` | `agendamento` | HU1 |
| `ReservaService` | `agendamento` | UC04a, UC04b |
| `CheckInService` | `controleDeAcesso` | UC03, UC07 |

## Histórias e casos de uso implementados

- **HU1** grade de aulas: criar, trocar professor, mudar vagas, cancelar
- **HU2** quadro de beneficiários: incluir com plano, trocar plano (os
  dependentes acompanham, RN3), incluir dependente, suspender e reativar o
  benefício por inadimplência (RN7), remover
- **HU4** parceiros e planos: cadastrar, aprovar, suspender, configurar níveis
  (a cobertura do plano é por nível, RF5)
- **HU5** modalidades e instrutores
- **UC03** realizar check-in: confere a distância até o estabelecimento (RN4)
  e gera um código de comprovante, que expira
- **UC04a / UC04b** reservar vaga em aula e cancelar reserva: bloqueia reserva
  duplicada, respeita o limite de aulas do plano no mês da aula e penaliza
  cancelamento em cima da hora (RN5)
- **UC07** validar check-in na recepção: localiza pelo código ou pelo CPF do
  aluno e valida ou recusa

## Endpoints

CNPJ e CPF vão na URL só com dígitos.

| Método | Caminho | HU / UC |
|---|---|---|
| GET | `/api/estado` | leitura |
| POST | `/api/estabelecimentos` | HU4 |
| POST | `/api/estabelecimentos/{cnpj}/aprovar` | HU4 |
| POST | `/api/estabelecimentos/{cnpj}/suspender` | HU4 |
| POST | `/api/empresas` | HU4 |
| POST | `/api/planos` | HU4 |
| POST | `/api/empresas/{cnpj}/funcionarios` | HU2 |
| POST | `/api/empresas/{cnpj}/funcionarios/{cpf}/plano` | HU2 |
| POST | `/api/empresas/{cnpj}/funcionarios/{cpf}/suspender` | HU2 |
| POST | `/api/empresas/{cnpj}/funcionarios/{cpf}/reativar` | HU2 |
| POST | `/api/empresas/{cnpj}/funcionarios/{cpf}/dependentes` | HU2 |
| DELETE | `/api/empresas/{cnpj}/funcionarios/{cpf}` | HU2 |
| POST | `/api/estabelecimentos/{cnpj}/modalidades` | HU5 |
| DELETE | `/api/estabelecimentos/{cnpj}/modalidades/{id}` | HU5 |
| POST | `/api/estabelecimentos/{cnpj}/instrutores` | HU5 |
| DELETE | `/api/estabelecimentos/{cnpj}/instrutores/{registro}` | HU5 |
| POST | `/api/aulas` | HU1 |
| POST | `/api/aulas/{id}/instrutor` | HU1 |
| POST | `/api/aulas/{id}/capacidade` | HU1 |
| POST | `/api/aulas/{id}/cancelar` | HU1 |
| POST | `/api/reservas` | UC04a |
| POST | `/api/reservas/{id}/cancelar` | UC04b |
| POST | `/api/checkins` | UC03 |
| GET | `/api/recepcao/{cnpj}/checkins/{codigo}` | UC07 |
| GET | `/api/recepcao/{cnpj}/cpf/{cpf}` | UC07 (ext. 2a) |
| POST | `/api/recepcao/{cnpj}/checkins/{codigo}/validar` | UC07 |
| POST | `/api/recepcao/{cnpj}/checkins/{codigo}/recusar` | UC07 |

## Limitações conhecidas

- Sem autenticação: o RNF4 não foi implementado e a troca de perfil na tela é
  apenas de demonstração.
- `prompt()` e `confirm()` em algumas ações, no lugar de modais.
- Financeiro e notificações ainda não foram implementados (`dominio.financeiro`
  e `application.rotinasDeSistema` têm só um esboço).

## Demonstrações de regra de negócio

1. Remover a Lara do quadro: o dependente dela perde o acesso junto (RN1 em cascata).
2. Tentar incluir dependente para o Bruno, no plano Básico: bloqueia pela RN3.
3. Tentar agendar aula com data no passado: bloqueia.
