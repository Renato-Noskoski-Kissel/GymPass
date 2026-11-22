-- =====================================================================
-- GymPass — Dados iniciais (PostgreSQL 16)
-- INE5417 · Iteração 3
--
-- Os mesmos dados que o antigo Repositorio.semear() criava em memória.
-- Roda depois de 01-modelagem.sql (o docker executa os scripts de
-- /docker-entrypoint-initdb.d em ordem alfabética, só na 1ª inicialização).
--
-- CNPJ e CPF só com dígitos. As datas das aulas são relativas ao dia em
-- que o banco é criado, para a grade nunca nascer no passado.
-- =====================================================================

-- O container roda em UTC; o Java grava LocalDateTime no horário de
-- Brasília. Sem isto, "daqui a 90 min" ficaria 3 h adiantado.
SET TIME ZONE 'America/Sao_Paulo';

BEGIN;

-- ---------------------------------------------------------------------
-- CADASTRO E REDE
-- ---------------------------------------------------------------------
INSERT INTO empresas (cnpj, razao_social, data_contrato, limite_dependentes, dia_vencimento_fatura) VALUES
    ('11111111000111', 'Tech Ltda', DATE '2026-01-10', 2, 10),
    ('33333333000133', 'Norte Log', DATE '2026-02-03', 1, 5);

-- RF5 - nível mínimo: Forte 1 (todos os planos), Quadra 2, Studio 3 (só Premium).
-- A Quadra Areia fica PENDENTE: o semear() não aprovava o credenciamento dela.
INSERT INTO estabelecimentos (cnpj, tipo, nome_fantasia, situacao_credenciamento, valor_por_check_in, nivel_minimo,
                              logradouro, numero, bairro, cidade, cep, latitude, longitude,
                              funciona24_horas, capacidade_padrao_turma, coberta) VALUES
    ('22222222000122', 'ESTUDIO',  'Studio Movimento', 'APROVADO', 12.5, 3,
     'Rua Lauro Linhares', '1200', 'Trindade', 'Florianópolis', '88036-002', -27.5893, -48.5196,
     NULL, 15, NULL),
    ('44444444000144', 'ACADEMIA', 'Academia Forte',   'APROVADO',  9.9, 1,
     'Rua Deputado Antônio Edu Vieira', '800', 'Pantanal', 'Florianópolis', '88040-001', -27.6036, -48.5210,
     TRUE, NULL, NULL),
    ('55555555000155', 'QUADRA',   'Quadra Areia',     'PENDENTE', 15,   2,
     'Avenida das Rendeiras', '300', 'Lagoa da Conceição', 'Florianópolis', '88062-400', -27.6037, -48.4657,
     NULL, NULL, FALSE);

INSERT INTO modalidades (id, estabelecimento_cnpj, nome, descricao) VALUES
    (1, '22222222000122', 'Pilates',    'Aulas em grupo com aparelhos'),
    (2, '22222222000122', 'Yoga',       'Hatha e Vinyasa'),
    (3, '44444444000144', 'Musculacao', 'Acesso livre a sala');

INSERT INTO instrutores (registro, estabelecimento_cnpj, nome, especialidade) VALUES
    ('CREF-1234', '22222222000122', 'Ana Reis',   'Pilates'),
    ('CREF-5678', '22222222000122', 'Caio Melo',  'Yoga'),
    ('CREF-9999', '44444444000144', 'Rui Barros', 'Musculacao');

-- Aluno (tabela comum) + Funcionario / Dependente (tabelas da subclasse)
INSERT INTO alunos (cpf, tipo, nome, email, data_cadastro, situacao) VALUES
    ('00000000000', 'FUNCIONARIO', 'Lara Souza',   'lara@tech.com',   CURRENT_DATE, 'ATIVO'),
    ('22222222222', 'FUNCIONARIO', 'Bruno Dias',   'bruno@tech.com',  CURRENT_DATE, 'ATIVO'),
    ('11111111111', 'DEPENDENTE',  'Miguel Souza', 'miguel@mail.com', CURRENT_DATE, 'ATIVO');

INSERT INTO funcionarios (cpf, empresa_cnpj, matricula, data_admissao) VALUES
    ('00000000000', '11111111000111', 'F-001', DATE '2025-03-01'),
    ('22222222222', '11111111000111', 'F-002', DATE '2024-08-15');

INSERT INTO dependentes (cpf, responsavel_cpf, grau_parentesco) VALUES
    ('11111111111', '00000000000', 'Filho');

-- ---------------------------------------------------------------------
-- PLANOS E ADESÃO
-- ---------------------------------------------------------------------
INSERT INTO planos (id, nome, nivel, valor_mensal, limite_aulas_mes, permite_dependentes) VALUES
    (1, 'Premium', 3, 189.9, 8, TRUE),
    (2, 'Basico',  1,  79.9, 4, FALSE);

-- Os dois planos são oferecidos pelas duas empresas.
INSERT INTO planos_empresas (plano_id, empresa_cnpj) VALUES
    (1, '11111111000111'), (1, '33333333000133'),
    (2, '11111111000111'), (2, '33333333000133');

-- Lara no Premium, Bruno no Basico; Miguel (dependente da Lara) no mesmo plano dela (RN3).
INSERT INTO adesoes (id, aluno_cpf, plano_id, data_inicio, data_fim, situacao) VALUES
    (1, '00000000000', 1, CURRENT_DATE, NULL, 'ATIVA'),
    (2, '22222222222', 2, CURRENT_DATE, NULL, 'ATIVA'),
    (3, '11111111111', 1, CURRENT_DATE, NULL, 'ATIVA');

-- ---------------------------------------------------------------------
-- ACESSO E AGENDA  (todas no Studio Movimento)
-- ---------------------------------------------------------------------
INSERT INTO aulas (id, estabelecimento_cnpj, modalidade_id, instrutor_registro,
                   data_hora_inicio, duracao_minutos, capacidade_maxima, cancelada) VALUES
    (1, '22222222000122', 1, 'CREF-1234', CURRENT_DATE + 1 + TIME '07:00', 50,  8, FALSE),  -- Pilates amanhã 07h
    (2, '22222222000122', 1, 'CREF-1234', CURRENT_DATE + 1 + TIME '18:00', 60,  2, FALSE),  -- Pilates amanhã 18h (2 vagas)
    (3, '22222222000122', 2, 'CREF-5678', CURRENT_DATE + 1 + TIME '19:00', 60, 10, FALSE),  -- Yoga amanhã 19h
    -- UC04b - começa em 90 min: o prazo sem penalidade (2 h antes, RN5) já
    -- passou, então reservar e cancelar esta aula gera cancelamento penalizado.
    (4, '22222222000122', 1, 'CREF-1234', date_trunc('minute', LOCALTIMESTAMP + INTERVAL '90 minutes'), 50, 6, FALSE);

-- Ids explícitos acima: avança cada SERIAL para o próximo INSERT da aplicação não colidir.
SELECT setval(pg_get_serial_sequence('modalidades', 'id'), (SELECT max(id) FROM modalidades));
SELECT setval(pg_get_serial_sequence('planos',      'id'), (SELECT max(id) FROM planos));
SELECT setval(pg_get_serial_sequence('adesoes',     'id'), (SELECT max(id) FROM adesoes));
SELECT setval(pg_get_serial_sequence('aulas',       'id'), (SELECT max(id) FROM aulas));

COMMIT;
