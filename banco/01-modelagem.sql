-- GymPass. Modelagem relacional (PostgreSQL 16). INE5417, Iteração 3.
--
-- Chaves primárias: chave natural onde o próprio dado identifica a entidade
-- (estabelecimentos.cnpj, empresas.cnpj, alunos.cpf, instrutores.registro) e
-- id SERIAL onde não existe chave natural segura.
--
-- CNPJ e CPF são guardados só com os dígitos, sem ponto, barra e hífen. Assim
-- "12.345.678/0001-90" e "12345678000190" não viram dois cadastros, e a barra
-- não quebra a URL quando o CNPJ for parâmetro. A pontuação fica só na tela.
--
-- Os tipos acompanham os do Java para o ddl-auto=validate aceitar: double vira
-- DOUBLE PRECISION, int vira INTEGER, boolean vira BOOLEAN, LocalDate vira
-- DATE, LocalDateTime vira TIMESTAMP e enum vira VARCHAR com CHECK.
--
-- As tabelas estão na ordem em que precisam ser criadas: quem é referenciado
-- vem antes de quem referencia.


-- CADASTRO E REDE

-- Herança em tabela única: as colunas das três subclasses ficam aqui, vazias
-- quando não se aplicam, e tipo diz qual subclasse é a linha. Endereco é
-- objeto de valor e não tem tabela própria.
CREATE TABLE estabelecimentos (
    cnpj CHAR(14) PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ACADEMIA', 'ESTUDIO', 'QUADRA')),
    nome_fantasia VARCHAR(150) NOT NULL,
    situacao_credenciamento VARCHAR(20) NOT NULL
        CHECK (situacao_credenciamento IN ('PENDENTE', 'APROVADO', 'SUSPENSO')),
    valor_por_check_in DOUBLE PRECISION NOT NULL,
    nivel_minimo INTEGER NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(10) NOT NULL,
    bairro VARCHAR(80) NOT NULL,
    cidade VARCHAR(80) NOT NULL,
    cep VARCHAR(9) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    funciona24_horas BOOLEAN,
    capacidade_padrao_turma INTEGER,
    coberta BOOLEAN
);

-- Cada modalidade pertence a um estabelecimento. "Yoga" pode existir em
-- vários, então o nome não serve de chave.
CREATE TABLE modalidades (
    id SERIAL PRIMARY KEY,
    estabelecimento_cnpj CHAR(14) NOT NULL REFERENCES estabelecimentos(cnpj),
    nome VARCHAR(80) NOT NULL,
    descricao VARCHAR(255),
    UNIQUE (estabelecimento_cnpj, nome)
);

-- A chave é o registro profissional. Cada instrutor trabalha em um
-- estabelecimento.
CREATE TABLE instrutores (
    registro VARCHAR(20) PRIMARY KEY,
    estabelecimento_cnpj CHAR(14) NOT NULL REFERENCES estabelecimentos(cnpj),
    nome VARCHAR(150) NOT NULL,
    especialidade VARCHAR(80)
);

CREATE TABLE empresas (
    cnpj CHAR(14) PRIMARY KEY,
    razao_social VARCHAR(150) NOT NULL,
    data_contrato DATE NOT NULL,
    limite_dependentes INTEGER NOT NULL,
    dia_vencimento_fatura INTEGER NOT NULL
        CHECK (dia_vencimento_fatura BETWEEN 1 AND 31)
);

-- Herança em tabelas separadas: alunos tem o que é comum, funcionarios e
-- dependentes têm só o que é delas, e a chave das duas é o mesmo cpf da linha
-- em alunos, que é chave primária e estrangeira ao mesmo tempo.
CREATE TABLE alunos (
    cpf CHAR(11) PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('FUNCIONARIO', 'DEPENDENTE')),
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    data_cadastro DATE NOT NULL,
    situacao VARCHAR(20) NOT NULL
        CHECK (situacao IN ('ATIVO', 'INATIVO', 'BLOQUEADO'))
);

-- empresa_cnpj aceita NULL porque desvincular() tira a empresa.
CREATE TABLE funcionarios (
    cpf CHAR(11) PRIMARY KEY REFERENCES alunos(cpf),
    empresa_cnpj CHAR(14) REFERENCES empresas(cnpj),
    matricula VARCHAR(30) NOT NULL,
    data_admissao DATE
);

CREATE TABLE dependentes (
    cpf CHAR(11) PRIMARY KEY REFERENCES alunos(cpf),
    responsavel_cpf CHAR(11) NOT NULL REFERENCES funcionarios(cpf),
    grau_parentesco VARCHAR(30) NOT NULL
);


-- PLANOS E ADESÃO

CREATE TABLE planos (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL,
    nivel INTEGER NOT NULL,
    valor_mensal DOUBLE PRECISION NOT NULL,
    limite_aulas_mes INTEGER NOT NULL,
    permite_dependentes BOOLEAN NOT NULL
);

CREATE TABLE planos_empresas (
    plano_id INTEGER NOT NULL REFERENCES planos(id),
    empresa_cnpj CHAR(14) NOT NULL REFERENCES empresas(cnpj),
    PRIMARY KEY (plano_id, empresa_cnpj)
);

-- Um aluno tem várias adesões ao longo do tempo, porque as encerradas ficam
-- guardadas, com no máximo uma vigente. Essa última regra não vira restrição
-- aqui: ela pertence ao State. Por isso aluno_cpf não tem UNIQUE, que
-- quebraria a troca de plano. A coluna situacao guarda o estado como texto, e
-- o conversor JPA reconstrói Ativa, Suspensa ou Encerrada ao ler.
CREATE TABLE adesoes (
    id SERIAL PRIMARY KEY,
    aluno_cpf CHAR(11) NOT NULL REFERENCES alunos(cpf),
    plano_id INTEGER NOT NULL REFERENCES planos(id),
    data_inicio DATE NOT NULL,
    data_fim DATE,
    situacao VARCHAR(20) NOT NULL
        CHECK (situacao IN ('ATIVA', 'SUSPENSA', 'ENCERRADA'))
);


-- ACESSO E AGENDA

-- duracao é Duration no Java e fica em minutos aqui, com um conversor JPA
-- traduzindo os dois.
CREATE TABLE aulas (
    id SERIAL PRIMARY KEY,
    estabelecimento_cnpj CHAR(14) NOT NULL REFERENCES estabelecimentos(cnpj),
    modalidade_id INTEGER NOT NULL REFERENCES modalidades(id),
    instrutor_registro VARCHAR(20) NOT NULL REFERENCES instrutores(registro),
    data_hora_inicio TIMESTAMP NOT NULL,
    duracao_minutos INTEGER NOT NULL CHECK (duracao_minutos > 0),
    capacidade_maxima INTEGER NOT NULL CHECK (capacidade_maxima > 0),
    cancelada BOOLEAN NOT NULL DEFAULT FALSE
);

-- O par aluno e aula pode se repetir, porque dá para reservar de novo depois
-- de cancelar. Por isso não há UNIQUE sobre as duas colunas.
CREATE TABLE reservas (
    id SERIAL PRIMARY KEY,
    aluno_cpf CHAR(11) NOT NULL REFERENCES alunos(cpf),
    aula_id INTEGER NOT NULL REFERENCES aulas(id),
    data_hora_reserva TIMESTAMP NOT NULL,
    situacao VARCHAR(20) NOT NULL
        CHECK (situacao IN ('CONFIRMADA', 'CANCELADA')),
    penalizada BOOLEAN NOT NULL DEFAULT FALSE
);

-- O codigo é sorteado sem checar colisão, então pode repetir e não serve de
-- chave.
CREATE TABLE check_ins (
    id SERIAL PRIMARY KEY,
    aluno_cpf CHAR(11) NOT NULL REFERENCES alunos(cpf),
    estabelecimento_cnpj CHAR(14) NOT NULL REFERENCES estabelecimentos(cnpj),
    data_hora TIMESTAMP NOT NULL,
    latitude_origem DOUBLE PRECISION NOT NULL,
    longitude_origem DOUBLE PRECISION NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    situacao_validacao VARCHAR(20) NOT NULL
        CHECK (situacao_validacao IN ('PENDENTE', 'VALIDADO', 'RECUSADO', 'EXPIRADO')),
    motivo_recusa VARCHAR(255)
);


-- Índices para as consultas que o Repositorio faz com frequência.
CREATE INDEX idx_check_ins_aluno_data ON check_ins (aluno_cpf, data_hora DESC);
CREATE INDEX idx_check_ins_codigo ON check_ins (codigo);
CREATE INDEX idx_adesoes_aluno ON adesoes (aluno_cpf);
CREATE INDEX idx_reservas_aluno ON reservas (aluno_cpf);