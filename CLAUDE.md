# GymPass — INE5417 (UFSC)

Sistema de assinatura corporativa de academias, estilo Gympass.
Java + Spring Boot. Migrando a persistência de memória para PostgreSQL.

## Arquitetura (4 camadas — respeitar as fronteiras)
Apresentação (static/app.js + Controllers) → Aplicação (Services, fachada)
→ Domínio (classes de negócio) → Persistência (Repositorio)

## Fontes da verdade
- Banco: banco/01-modelagem.sql. As tabelas NÃO mudam; o código se adapta.
- Projeto: docs/dcp-oficial.puml. Assinaturas de Services, Controllers e da
  interface Repositorio devem ficar iguais às do diagrama.

## Regras
- Chaves naturais: Estabelecimento e Empresa por cnpj, Aluno por cpf,
  Instrutor por registro. CNPJ e CPF guardados só com dígitos.
  Modalidade, Plano, Adesao, Aula, Reserva e CheckIn usam id SERIAL.
- Padrões que não podem ser descaracterizados: Fachada (Services) e
  State (Adesao com Ativa/Suspensa/Encerrada). estaVigente() fica em Java.
- spring.jpa.hibernate.ddl-auto=validate, nunca update/create.
- Banco local: docker compose up -d (porta 5433).
- Trabalhe em etapas pequenas e suba a aplicação ao final de cada uma.
