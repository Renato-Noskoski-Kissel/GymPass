# GymPass — Iteração 1

Sistema de benefício corporativo de atividade física.
Domínio em Java puro, API REST com Spring Boot, interface web.

## Rodar

```
mvn spring-boot:run
```

Abrir <http://localhost:8080>. A primeira execução baixa as dependências e demora.

Requisitos: JDK 17 ou superior e Maven.
No IntelliJ: File → Open, apontar para a pasta do projeto, abrir como Maven.

Para rodar apenas o teste de console do domínio (`Main.java`), use o botão de
run do IntelliJ nessa classe.

## Camadas

| Camada do diagrama | Pacote |
|---|---|
| Apresentação | `presentation.web` + `src/main/resources/static` |
| Aplicação | `application.parceiros`, `application.beneficiarios`, `application.estabelecimento`, `application.grade`, `application.rotinas` |
| Domínio | `dominio.cadastroRede`, `dominio.planosAdesao`, `dominio.acessoAgenda`, `dominio.financeiro` |
| Persistência | `persistencia.Repositorio` |

As classes de domínio não têm nenhuma anotação do Spring. A dependência vai
sempre de cima para baixo: apresentação conhece aplicação, aplicação conhece
domínio, domínio não conhece ninguém acima.

## Histórias implementadas

- **HU1** grade de aulas: criar, trocar professor, mudar vagas, cancelar
- **HU2** quadro de beneficiários: incluir com plano, trocar plano, incluir dependente, remover
- **HU4** parceiros e planos: cadastrar, aprovar, suspender, configurar níveis
- **HU5** modalidades e instrutores

HU3 (aluno agenda aulas) fica para a iteração 2.

## Endpoints

| Método | Caminho | HU |
|---|---|---|
| GET | `/api/estado` | leitura |
| POST | `/api/estabelecimentos` | HU4 |
| POST | `/api/estabelecimentos/{id}/aprovar` | HU4 |
| POST | `/api/estabelecimentos/{id}/suspender` | HU4 |
| POST | `/api/empresas` | HU4 |
| POST | `/api/planos` | HU4 |
| POST | `/api/empresas/{id}/funcionarios` | HU2 |
| POST | `/api/empresas/{id}/funcionarios/{fid}/plano` | HU2 |
| POST | `/api/empresas/{id}/funcionarios/{fid}/dependentes` | HU2 |
| DELETE | `/api/empresas/{id}/funcionarios/{fid}` | HU2 |
| POST | `/api/estabelecimentos/{id}/modalidades` | HU5 |
| DELETE | `/api/estabelecimentos/{id}/modalidades/{mid}` | HU5 |
| POST | `/api/estabelecimentos/{id}/instrutores` | HU5 |
| DELETE | `/api/estabelecimentos/{id}/instrutores/{iid}` | HU5 |
| POST | `/api/aulas` | HU1 |
| POST | `/api/aulas/{id}/instrutor` | HU1 |
| POST | `/api/aulas/{id}/capacidade` | HU1 |
| POST | `/api/aulas/{id}/cancelar` | HU1 |

## Limitações conhecidas

- Sem banco de dados: o `Repositorio` guarda tudo em memória e os dados voltam
  ao estado inicial a cada reinício.
- Identificadores são índices de lista. Funciona porque nada é excluído em
  definitivo, mas não sobreviveria a um banco.
- Sem autenticação: o RNF4 não foi implementado e a troca de perfil na tela é
  apenas de demonstração.
- `prompt()` e `confirm()` em algumas ações, no lugar de modais.
- HU3, check-in, financeiro e notificações ficam para as próximas iterações.

## Demonstrações de regra de negócio

1. Remover a Lara do quadro: o dependente dela perde o acesso junto (RN1 em cascata).
2. Tentar incluir dependente para o Bruno, no plano Básico: bloqueia pela RN3.
3. Tentar agendar aula com data no passado: bloqueia.
