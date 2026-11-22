-- =====================================================================
-- GymPass — Modelagem relacional (PostgreSQL 16)
-- INE5417 · Iteração 3
--
-- Chaves primárias:
--   * chave NATURAL onde o próprio dado identifica a entidade:
--       estabelecimentos.cnpj, empresas.cnpj, alunos.cpf,
--       instrutores.registro
--   * "id" SERIAL só onde não existe chave natural segura:
--       modalidades, planos, adesoes, aulas, reservas, check_ins
--
-- CNPJ e CPF são guardados SÓ COM OS DÍGITOS (sem ponto, barra e
-- hífen): CHAR(14) e CHAR(11). Assim "12.345.678/0001-90" e
-- "12345678000190" não viram dois cadastros, e a barra do CNPJ não
-- quebra a URL quando ele for usado como parâmetro (/estabelecimentos/{cnpj}).
-- A formatação com pontuação fica só na tela.
--
-- Tipos acompanham os do Java para o ddl-auto=validate aceitar:
--   double -> DOUBLE PRECISION · int -> INTEGER · boolean -> BOOLEAN
--   LocalDate -> DATE · LocalDateTime -> TIMESTAMP
--   enum (STRING) -> VARCHAR + CHECK com os valores do enum
--
-- Ordem das tabelas: quem é referenciado vem antes de quem referencia.
-- =====================================================================


-- =====================================================================
-- PACOTE: CADASTRO E REDE  (transcrito da modelagem feita à mão)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Estabelecimento + Academia/Estudio/Quadra
-- Herança em TABELA ÚNICA (como no desenho): as colunas das três
-- subclasses ficam aqui, vazias quando não se aplicam, e "tipo" diz
-- qual subclasse é a linha.
-- Endereco não tem tabela própria: é objeto de valor, embutido aqui.
-- ---------------------------------------------------------------------
CREATE TABLE estabelecimentos (
    cnpj                      CHAR(14)         PRIMARY KEY,
    tipo                      VARCHAR(20)      NOT NULL
                              CHECK (tipo IN ('ACADEMIA', 'ESTUDIO', 'QUADRA')),
    nome_fantasia             VARCHAR(150)     NOT NULL,
    situacao_credenciamento   VARCHAR(20)      NOT NULL
                              CHECK (situacao_credenciamento IN ('PENDENTE', 'APROVADO', 'SUSPENSO')),
    valor_por_check_in        DOUBLE PRECISION NOT NULL,
    nivel_minimo              INTEGER          NOT NULL,

    -- Endereco (embutido)
    logradouro                VARCHAR(150)     NOT NULL,
    numero                    VARCHAR(10)      NOT NULL,
    bairro                    VARCHAR(80)      NOT NULL,
    cidade                    VARCHAR(80)      NOT NULL,
    cep                       VARCHAR(9)       NOT NULL,
    latitude                  DOUBLE PRECISION NOT NULL,
    longitude                 DOUBLE PRECISION NOT NULL,

    -- colunas das subclasses (só uma delas é preenchida por linha)
    funciona24_horas          BOOLEAN,                     -- Academia
    capacidade_padrao_turma   INTEGER,                     -- Estudio
    coberta                   BOOLEAN                      -- Quadra
);

-- ---------------------------------------------------------------------
-- Modalidade: composição de Estabelecimento (cada um tem as suas).
-- "Yoga" pode existir em vários estabelecimentos, então o nome não
-- serve de chave: fica o id, como já estava no desenho (modalidade_id).
-- ---------------------------------------------------------------------
CREATE TABLE modalidades (
    id                    SERIAL       PRIMARY KEY,
    estabelecimento_cnpj  CHAR(14)     NOT NULL REFERENCES estabelecimentos(cnpj),
    nome                  VARCHAR(80)  NOT NULL,
    descricao             VARCHAR(255),
    UNIQUE (estabelecimento_cnpj, nome)                     -- o mesmo estabelecimento não repete modalidade
);

-- ---------------------------------------------------------------------
-- Instrutor: chave é o registro profissional (CREF).
-- Pelo modelo, cada instrutor trabalha em UM estabelecimento.
-- ---------------------------------------------------------------------
CREATE TABLE instrutores (
    registro              VARCHAR(20)  PRIMARY KEY,
    estabelecimento_cnpj  CHAR(14)     NOT NULL REFERENCES estabelecimentos(cnpj),
    nome                  VARCHAR(150) NOT NULL,
    especialidade         VARCHAR(80)
);

-- ---------------------------------------------------------------------
-- Empresa
-- ---------------------------------------------------------------------
CREATE TABLE empresas (
    cnpj                   CHAR(14)     PRIMARY KEY,
    razao_social           VARCHAR(150) NOT NULL,
    data_contrato          DATE         NOT NULL,
    limite_dependentes     INTEGER      NOT NULL,
    dia_vencimento_fatura  INTEGER      NOT NULL
                           CHECK (dia_vencimento_fatura BETWEEN 1 AND 31)
);

-- ---------------------------------------------------------------------
-- Aluno + Funcionario/Dependente
-- Herança em TABELAS SEPARADAS (como no desenho): "alunos" tem o que é
-- comum; "funcionarios" e "dependentes" têm só o que é delas, e a chave
-- delas é o mesmo cpf da linha em "alunos" (PK que também é FK).
-- ---------------------------------------------------------------------
CREATE TABLE alunos (
    cpf            CHAR(11)     PRIMARY KEY,
    tipo           VARCHAR(20)  NOT NULL
                   CHECK (tipo IN ('FUNCIONARIO', 'DEPENDENTE')),
    nome           VARCHAR(150) NOT NULL,
    email          VARCHAR(150),
    data_cadastro  DATE         NOT NULL,
    situacao       VARCHAR(20)  NOT NULL
                   CHECK (situacao IN ('ATIVO', 'INATIVO', 'BLOQUEADO'))
);

CREATE TABLE funcionarios (
    cpf            CHAR(11)     PRIMARY KEY REFERENCES alunos(cpf),
    empresa_cnpj   CHAR(14)     REFERENCES empresas(cnpj),  -- aceita NULL: desvincular() tira a empresa
    matricula      VARCHAR(30)  NOT NULL,
    data_admissao  DATE
);

CREATE TABLE dependentes (
    cpf               CHAR(11)     PRIMARY KEY REFERENCES alunos(cpf),
    responsavel_cpf   CHAR(11)     NOT NULL REFERENCES funcionarios(cpf),
    grau_parentesco   VARCHAR(30)  NOT NULL
);


-- =====================================================================
-- PACOTE: PLANOS E ADESÃO  (PROPOSTA — não estava no desenho)
-- =====================================================================

CREATE TABLE planos (
    id                   SERIAL           PRIMARY KEY,
    nome                 VARCHAR(80)      NOT NULL,
    nivel                INTEGER          NOT NULL,
    valor_mensal         DOUBLE PRECISION NOT NULL,
    limite_aulas_mes     INTEGER          NOT NULL,
    permite_dependentes  BOOLEAN          NOT NULL
);

-- Plano N:N Empresa (Plano.empresasQueOferecem)
CREATE TABLE planos_empresas (
    plano_id      INTEGER  NOT NULL REFERENCES planos(id),
    empresa_cnpj  CHAR(14) NOT NULL REFERENCES empresas(cnpj),
    PRIMARY KEY (plano_id, empresa_cnpj)
);

-- Adesao: um aluno tem 1..* adesões ao longo do tempo (as encerradas
-- ficam guardadas), com no máximo uma vigente. Essa última regra NÃO
-- vira restrição aqui: ela pertence ao State (estaVigente()).
-- "situacao" guarda o estado como texto; o conversor JPA reconstrói
-- Ativa/Suspensa/Encerrada ao ler.
CREATE TABLE adesoes (
    id           SERIAL      PRIMARY KEY,
    aluno_cpf    CHAR(11)    NOT NULL REFERENCES alunos(cpf),  -- sem UNIQUE: quebraria a troca de plano
    plano_id     INTEGER     NOT NULL REFERENCES planos(id),
    data_inicio  DATE        NOT NULL,
    data_fim     DATE,                                          -- NULL enquanto não encerrada
    situacao     VARCHAR(20) NOT NULL
                 CHECK (situacao IN ('ATIVA', 'SUSPENSA', 'ENCERRADA'))
);


-- =====================================================================
-- PACOTE: ACESSO E AGENDA  (PROPOSTA — não estava no desenho)
-- =====================================================================

-- Aula: duracao é Duration no Java; aqui fica em minutos, e um
-- conversor JPA traduz Duration <-> INTEGER.
CREATE TABLE aulas (
    id                    SERIAL      PRIMARY KEY,
    estabelecimento_cnpj  CHAR(14)    NOT NULL REFERENCES estabelecimentos(cnpj),
    modalidade_id         INTEGER     NOT NULL REFERENCES modalidades(id),
    instrutor_registro    VARCHAR(20) NOT NULL REFERENCES instrutores(registro),
    data_hora_inicio      TIMESTAMP   NOT NULL,
    duracao_minutos       INTEGER     NOT NULL CHECK (duracao_minutos > 0),
    capacidade_maxima     INTEGER     NOT NULL CHECK (capacidade_maxima > 0),
    cancelada             BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Reserva: o par aluno-aula pode se repetir (reservar de novo depois
-- de cancelar), então não há UNIQUE(aluno_cpf, aula_id).
CREATE TABLE reservas (
    id                 SERIAL      PRIMARY KEY,
    aluno_cpf          CHAR(11)    NOT NULL REFERENCES alunos(cpf),
    aula_id            INTEGER     NOT NULL REFERENCES aulas(id),
    data_hora_reserva  TIMESTAMP   NOT NULL,
    situacao           VARCHAR(20) NOT NULL,   -- TODO: CHECK com os valores de SituacaoReserva
    penalizada         BOOLEAN     NOT NULL DEFAULT FALSE
);

-- CheckIn: o "codigo" é sorteado sem checar colisão, então pode
-- repetir e não serve de chave.
CREATE TABLE check_ins (
    id                    SERIAL           PRIMARY KEY,
    aluno_cpf             CHAR(11)         NOT NULL REFERENCES alunos(cpf),
    estabelecimento_cnpj  CHAR(14)         NOT NULL REFERENCES estabelecimentos(cnpj),
    data_hora             TIMESTAMP        NOT NULL,
    latitude_origem       DOUBLE PRECISION NOT NULL,
    longitude_origem      DOUBLE PRECISION NOT NULL,
    codigo                VARCHAR(6)       NOT NULL,
    situacao_validacao    VARCHAR(20)      NOT NULL
                          CHECK (situacao_validacao IN ('PENDENTE', 'VALIDADO', 'RECUSADO', 'EXPIRADO')),
    motivo_recusa         VARCHAR(255)
);

-- Índices para as consultas que o Repositorio faz com frequência
CREATE INDEX idx_check_ins_aluno_data ON check_ins (aluno_cpf, data_hora DESC);  -- ultimoCheckInDe
CREATE INDEX idx_check_ins_codigo     ON check_ins (codigo);                     -- checkInPorCodigo
CREATE INDEX idx_adesoes_aluno        ON adesoes   (aluno_cpf);                  -- adesaoDe
CREATE INDEX idx_reservas_aluno       ON reservas  (aluno_cpf);                  -- totalReservasNoMes