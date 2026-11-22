/* ==========================================================================
   Camada de apresentação — versão integrada.

   Diferente do protótipo, aqui não há nenhuma classe de domínio no navegador.
   Toda regra de negócio roda no servidor Java; este arquivo só desenha a tela
   e envia requisições HTTP.

   Fluxo de cada ação: envia POST/DELETE -> recarrega GET /api/estado ->
   redesenha a vista atual. Erros de regra de negócio voltam como HTTP 400 com
   a mensagem original definida no domínio.

   Cada registro é identificado pela chave do banco: cnpj (estabelecimento e
   empresa), cpf (aluno), registro (instrutor) ou id (plano, modalidade, aula,
   reserva, check-in). CNPJ e CPF chegam e vão só com dígitos; a pontuação é
   posta aqui, apenas para exibir.
   ========================================================================== */

const $ = sel => document.querySelector(sel);

/** Estado devolvido pelo servidor. Nunca é modificado aqui. */
let estado = {
  estabelecimentos: [], empresas: [], planos: [], aulas: [],
  alunos: [], checkIns: [], reservas: []
};

let vistaAtual = 'admin';
let abaAtual = 'cadastro';   // aba dentro do perfil Estabelecimento: cadastro, grade ou recepcao
let empresaCnpj = null;
let estabelecimentoCnpj = null;

/* seleções que precisam sobreviver ao redesenho da tela */
let alunoCpf = null;
let checkinEstabelecimentoCnpj = null;
let coordenadasDe = null;   // cnpj do estabelecimento cujas coordenadas estão nos campos do check-in
let conferencia = null;     // UC07: check-in localizado pela recepção (código, aluno, quando)

/* ---------- comunicação com o servidor ---------- */

async function api(caminho, metodo = 'GET', corpo = null) {
  const opcoes = { method: metodo, headers: {} };
  if (corpo !== null) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }

  const resposta = await fetch('/api' + caminho, opcoes);

  if (!resposta.ok) {
    let mensagem = 'Não foi possível concluir a operação.';
    try {
      const erro = await resposta.json();
      if (erro && erro.mensagem) mensagem = erro.mensagem;
    } catch (_) { /* resposta sem corpo JSON */ }
    throw new Error(mensagem);
  }

  const texto = await resposta.text();
  return texto ? JSON.parse(texto) : null;
}

async function carregarEstado() {
  estado = await api('/estado');
}

/**
 * Executa uma operação no servidor, recarrega o estado e redesenha.
 * Violação de regra de negócio vira aviso vermelho, sem quebrar a tela.
 */
async function executar(operacao, mensagemSucesso) {
  try {
    await operacao();
    await carregarEstado();
    if (mensagemSucesso) avisar(mensagemSucesso);
  } catch (e) {
    avisar(e.message, 'erro');
  }
  render();
}

/* ---------- utilidades de tela ---------- */

function avisar(texto, tipo = '') {
  const el = document.createElement('div');
  el.className = 'aviso' + (tipo ? ' ' + tipo : '');
  el.textContent = texto;
  $('#avisos').append(el);
  setTimeout(() => el.remove(), 4200);
}

function selo(texto, classe) {
  return `<span class="selo ${classe}">${texto}</span>`;
}

/** Selo de beneficiário: a suspensão por inadimplência (RN7) tem precedência. */
function seloBeneficiario(b) {
  if (b.situacaoAdesao === 'SUSPENSA') return selo('Suspenso', 'suspenso');
  return b.elegivel ? selo('Ativo', 'aprovado') : selo('Sem acesso', 'inativo');
}

function moeda(v) {
  return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

/** CPF e CNPJ trafegam só com dígitos; estes dois põem a pontuação para exibir. */
function formatarCpf(cpf) {
  return cpf ? cpf.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4') : '';
}

function formatarCnpj(cnpj) {
  return cnpj ? cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5') : '';
}

const soDigitos = texto => texto.replace(/\D/g, '');

/**
 * chave: que campo do item vai no value da opção (cnpj, cpf, registro ou id).
 * O value de um <option> é sempre texto, então a comparação é feita como texto.
 */
function preencherSelect(el, itens, rotulo, selecionado, chave = it => it.id) {
  el.innerHTML = itens
    .map(it => {
      const valor = String(chave(it));
      const marcado = selecionado != null && valor === String(selecionado);
      return `<option value="${valor}"${marcado ? ' selected' : ''}>${rotulo(it)}</option>`;
    })
    .join('');
}

/** Uma barra por vaga; as reservadas aparecem preenchidas. */
function barraDeVagas(aula) {
  const ocupadas = aula.capacidade - aula.vagasDisponiveis;
  const visiveis = Math.min(aula.capacidade, 24);
  return Array.from({ length: visiveis },
    (_, i) => `<i${i < ocupadas ? ' class="ocupada"' : ''}></i>`).join('');
}

const empresaAtual = () => estado.empresas.find(e => e.cnpj === empresaCnpj) || estado.empresas[0];
const estabelecimentoAtual = () =>
  estado.estabelecimentos.find(e => e.cnpj === estabelecimentoCnpj) || estado.estabelecimentos[0];

/* ---------- HU4: administração ---------- */

function renderAdmin() {
  const ativos = estado.estabelecimentos.filter(e => e.situacao === 'APROVADO').length;
  $('#cont-estabelecimentos').textContent =
    `${ativos} de ${estado.estabelecimentos.length} ativos`;

  $('#lista-estabelecimentos').innerHTML = estado.estabelecimentos.length
    ? estado.estabelecimentos.map(e => {
        const aprovado = e.situacao === 'APROVADO';
        const suspenso = e.situacao === 'SUSPENSO';
        const classe = aprovado ? 'aprovado' : suspenso ? 'suspenso' : 'pendente';
        const rotulo = aprovado ? 'Ativo' : suspenso ? 'Suspenso' : 'Aguardando';
        const botao = aprovado
          ? `<button class="acao discreta risco" data-suspender="${e.cnpj}">Suspender</button>`
          : `<button class="acao discreta" data-aprovar="${e.cnpj}">Aprovar</button>`;
        return `<div class="item">
          <div class="item-corpo">
            <div class="item-nome">${e.nome}</div>
            <div class="item-detalhe">CNPJ ${formatarCnpj(e.cnpj)} · ${e.tipo} · nível ${e.nivelMinimo} · ${moeda(e.valorPorCheckIn)} por check-in</div>
            <div class="item-detalhe">${e.endereco}</div>
          </div>
          ${selo(rotulo, classe)}
          ${botao}
        </div>`;
      }).join('')
    : '<p class="vazio">Nenhum estabelecimento na rede. Cadastre o primeiro abaixo.</p>';

  $('#cont-empresas').textContent = `${estado.empresas.length} contratadas`;
  $('#lista-empresas').innerHTML = estado.empresas.length
    ? estado.empresas.map(e => `<div class="item">
        <div class="item-corpo">
          <div class="item-nome">${e.razaoSocial}</div>
          <div class="item-detalhe">CNPJ ${formatarCnpj(e.cnpj)} · ${e.totalBeneficiarios} beneficiários · até ${e.limiteDependentes} dependentes por funcionário</div>
        </div>
      </div>`).join('')
    : '<p class="vazio">Nenhuma empresa contratada ainda.</p>';

  $('#cont-planos').textContent = `${estado.planos.length} níveis`;
  $('#lista-planos').innerHTML = estado.planos.length
    ? estado.planos.map(p => `<div class="plano">
        <div class="plano-nome">${p.nome}</div>
        <div class="plano-nivel">${[1,2,3,4,5].map(n =>
          `<i class="${n <= p.nivel ? 'cheio' : ''}"></i>`).join('')}</div>
        <div class="plano-valor">${moeda(p.valorMensal)} <small>por mês</small></div>
        <div class="plano-detalhe">${p.limiteAulasMes} aulas por mês</div>
        <div class="plano-detalhe">${p.permiteDependentes ? 'Aceita dependentes' : 'Sem dependentes'}</div>
        <div class="plano-cobre">${p.cobertura.length
          ? 'Cobre: ' + p.cobertura.join(', ')
          : '<p class="vazio">Não cobre nenhum estabelecimento</p>'}</div>
      </div>`).join('')
    : '<p class="vazio">Nenhum plano configurado. Sem plano não é possível incluir beneficiários.</p>';
}

/* ---------- HU2: empresa ---------- */

function renderEmpresa() {
  preencherSelect($('#empresa-atual'), estado.empresas, e => e.razaoSocial, empresaCnpj, e => e.cnpj);
  const empresa = empresaAtual();
  if (!empresa) return;

  $('#indicadores-empresa').innerHTML = `
    <div class="indicador">
      <div class="indicador-valor">${empresa.totalFuncionarios}</div>
      <div class="indicador-rotulo">funcionários no quadro</div>
    </div>
    <div class="indicador">
      <div class="indicador-valor">${empresa.totalDependentes}</div>
      <div class="indicador-rotulo">dependentes incluídos</div>
    </div>
    <div class="indicador">
      <div class="indicador-valor">${empresa.totalBeneficiarios}</div>
      <div class="indicador-rotulo">pessoas com benefício</div>
    </div>`;

  preencherSelect($('#func-plano'), estado.planos, p => `${p.nome} — ${moeda(p.valorMensal)}`);

  $('#cont-funcionarios').textContent = `${empresa.totalFuncionarios} no quadro`;

  $('#lista-funcionarios').innerHTML = empresa.funcionarios.length
    ? empresa.funcionarios.map(f => {
        const deps = f.dependentes.map(d =>
          `<div class="item-detalhe">${d.nome} · ${d.grauParentesco} · CPF ${formatarCpf(d.cpf)} ${seloBeneficiario(d)}</div>`
        ).join('');
        const podeDep = f.planoPermiteDependentes
          && f.dependentes.length < empresa.limiteDependentes;
        return `<div class="item">
          <div class="item-corpo">
            <div class="item-nome">${f.nome}</div>
            <div class="item-detalhe">${f.matricula} · CPF ${formatarCpf(f.cpf)} · plano ${f.plano || 'nenhum'}</div>
            ${deps}
          </div>
          ${seloBeneficiario(f)}
          <button class="acao discreta" data-trocar="${f.cpf}">Trocar plano</button>
          ${f.podeSuspender ? `<button class="acao discreta" data-suspender="${f.cpf}">Suspender benefício</button>` : ''}
          ${f.podeReativar ? `<button class="acao discreta" data-reativar="${f.cpf}">Reativar benefício</button>` : ''}
          ${podeDep ? `<button class="acao discreta" data-dependente="${f.cpf}">Incluir dependente</button>` : ''}
          <button class="acao discreta risco" data-remover="${f.cpf}">Remover</button>
        </div>`;
      }).join('')
    : '<p class="vazio">Ninguém no quadro ainda. Inclua o primeiro funcionário abaixo.</p>';
}

/* ---------- Estabelecimento: um seletor, três abas ---------- */

function renderEstabelecimento() {
  preencherSelect($('#estabelecimento-atual'), estado.estabelecimentos,
    e => e.nome, estabelecimentoCnpj, e => e.cnpj);
  const est = estabelecimentoAtual();

  document.querySelectorAll('.aba').forEach(b =>
    b.classList.toggle('ativa', b.dataset.aba === abaAtual));
  ['cadastro', 'grade', 'recepcao'].forEach(a =>
    $('#aba-' + a).classList.toggle('oculta', a !== abaAtual));

  if (!est) return;
  ({ cadastro: renderCadastro, grade: renderGrade, recepcao: renderRecepcao })[abaAtual](est);
}

/* ---------- HU5: modalidades e instrutores ---------- */

function renderCadastro(est) {
  $('#cont-modalidades').textContent = `${est.modalidades.length} cadastradas`;
  $('#lista-modalidades').innerHTML = est.modalidades.length
    ? est.modalidades.map(m => `<div class="item">
        <div class="item-corpo">
          <div class="item-nome">${m.nome}</div>
          <div class="item-detalhe">${m.descricao || 'Sem descrição'}</div>
        </div>
        <button class="acao discreta risco" data-remover-modalidade="${m.id}">Remover</button>
      </div>`).join('')
    : '<p class="vazio">Nenhuma modalidade. Sem modalidade não é possível criar aulas.</p>';

  $('#cont-instrutores').textContent = `${est.instrutores.length} contratados`;
  $('#lista-instrutores').innerHTML = est.instrutores.length
    ? est.instrutores.map(i => `<div class="item">
        <div class="item-corpo">
          <div class="item-nome">${i.nome}</div>
          <div class="item-detalhe">${i.registro} · ${i.especialidade || 'geral'}</div>
        </div>
        <button class="acao discreta risco" data-desligar="${i.registro}">Desligar</button>
      </div>`).join('')
    : '<p class="vazio">Nenhum instrutor. Sem instrutor não é possível criar aulas.</p>';
}

/* ---------- HU1: grade de aulas ---------- */

function renderGrade(est) {
  preencherSelect($('#aula-modalidade'), est.modalidades, m => m.nome);
  preencherSelect($('#aula-instrutor'), est.instrutores, i => i.nome, null, i => i.registro);

  const aulas = estado.aulas.filter(a => a.estabelecimentoCnpj === est.cnpj);

  $('#lista-aulas').innerHTML = aulas.length
    ? aulas.map(aula => {
        const reservadas = aula.capacidade - aula.vagasDisponiveis;
        const excedente = aula.capacidade > 24 ? ` (mostrando 24)` : '';
        return `<article class="aula${aula.cancelada ? ' cancelada' : ''}">
          <div class="aula-quando">${aula.inicioLegivel}</div>
          <div class="aula-modalidade">${aula.modalidade} com ${aula.instrutor} · ${aula.duracaoMin} min</div>
          <div class="vagas" aria-hidden="true">${barraDeVagas(aula)}</div>
          <div class="vagas-nota">${reservadas} de ${aula.capacidade} vagas reservadas${excedente}${aula.cancelada ? ' · aula cancelada' : ''}</div>
          <div class="aula-acoes">
            <button class="acao discreta" data-instrutor-aula="${aula.id}">Trocar professor</button>
            <button class="acao discreta" data-capacidade="${aula.id}">Mudar vagas</button>
            ${aula.cancelada ? '' : `<button class="acao discreta risco" data-cancelar="${aula.id}">Cancelar</button>`}
          </div>
        </article>`;
      }).join('')
    : '<p class="vazio">Nenhuma aula na grade deste estabelecimento.</p>';
}

/* ---------- UC03 e UC04a: aluno ---------- */

const alunoAtual = () => estado.alunos.find(a => a.cpf === alunoCpf) || estado.alunos[0];
const credenciados = () => estado.estabelecimentos.filter(e => e.situacao === 'APROVADO');
const estabelecimentoDoCheckin = () =>
  credenciados().find(e => e.cnpj === checkinEstabelecimentoCnpj) || credenciados()[0];

function preencherCoordenadas(lat, lng) {
  $('#checkin-lat').value = lat.toFixed(6);
  $('#checkin-lng').value = lng.toFixed(6);
}

function renderAluno() {
  preencherSelect($('#aluno-atual'), estado.alunos, a =>
    `${a.nome}${a.tipo === 'Dependente' ? ' (dependente)' : ''} — ${a.plano || 'sem plano'}`,
    alunoCpf, a => a.cpf);
  const aluno = alunoAtual();
  if (!aluno) return;

  $('#indicadores-aluno').innerHTML = `
    <div class="indicador">
      <div class="indicador-valor">${aluno.plano || '—'}</div>
      <div class="indicador-rotulo">plano${aluno.empresa ? ' concedido por ' + aluno.empresa : ''}</div>
    </div>
    <div class="indicador">
      <div class="indicador-valor">${aluno.reservasNoMes} de ${aluno.limiteAulasMes}</div>
      <div class="indicador-rotulo">aulas do limite usadas neste mês</div>
    </div>
    <div class="indicador">
      <div class="indicador-valor">${aluno.elegivel ? 'Ativo' : 'Sem acesso'}</div>
      <div class="indicador-rotulo">vínculo com o benefício</div>
    </div>`;

  /* check-in */
  const meusCheckIns = estado.checkIns.filter(c => c.alunoCpf === aluno.cpf);
  $('#cont-checkins').textContent = `${meusCheckIns.length} registrados`;
  const deHoje = meusCheckIns.filter(c => c.hoje).pop();
  const situacaoComprovante = {
    PENDENTE: ['', 'Apresente este código na recepção. Vale até o fim de hoje.'],
    VALIDADO: [' validado', 'Entrada liberada pela recepção'],
    RECUSADO: [' recusado', 'Entrada recusada pela recepção'],
    EXPIRADO: [' expirado', 'Código expirado. Faça um novo check-in.']
  };
  if (deHoje) {
    const [classe, texto] = situacaoComprovante[deHoje.situacao];
    $('#comprovante').innerHTML = `<div class="comprovante${classe}">
        <div class="comprovante-titulo">Check-in de hoje</div>
        <div class="comprovante-codigo">${deHoje.codigo}</div>
        <div class="comprovante-linha">${deHoje.estabelecimento}, ${deHoje.quando}</div>
        <div class="comprovante-linha">${texto}${deHoje.motivoRecusa ? ': ' + deHoje.motivoRecusa : ''}</div>
      </div>`;
  } else {
    $('#comprovante').innerHTML = '<p class="vazio">Nenhum check-in hoje.</p>';
  }

  const lista = credenciados();
  const est = estabelecimentoDoCheckin();
  preencherSelect($('#checkin-estabelecimento'), lista, e => e.nome, est ? est.cnpj : null, e => e.cnpj);
  if (est && coordenadasDe !== est.cnpj) {
    preencherCoordenadas(est.latitude, est.longitude);
    coordenadasDe = est.cnpj;
  }

  /* reservas */
  /* UC04b passo 1: confirmadas ainda não iniciadas, por data e hora */
  const minhas = estado.reservas.filter(r => r.alunoCpf === aluno.cpf);
  const cancelavel = r => r.situacao === 'CONFIRMADA' && (r.aulaCancelada || !r.aulaJaComecou);
  const ativas = minhas.filter(cancelavel).sort((a, b) => a.inicio.localeCompare(b.inicio));
  const historico = minhas.filter(r => !cancelavel(r)).sort((a, b) => b.inicio.localeCompare(a.inicio));
  $('#cont-reservas').textContent = `${ativas.length} ativas`;

  /* UC04b passo 2: dados da reserva, prazo, tempo restante e aviso de penalidade */
  const prazo = r => r.aulaCancelada
    ? '<div class="item-detalhe">Aula cancelada pelo estabelecimento. Cancelar não gera penalidade.</div>'
    : r.cancelamentoPenalizado
      ? `<div class="item-detalhe alerta">O prazo sem penalidade terminou (${r.prazoSemPenalidade}). Cancelar agora gera penalidade.</div>`
      : `<div class="item-detalhe">Cancele sem penalidade até ${r.prazoSemPenalidade} (${r.tempoAtePrazo}).</div>`;

  const htmlAtivas = ativas.map(r => `<div class="item empilhado">
      <div class="item-corpo">
        <div class="item-nome">${r.aula}</div>
        <div class="item-detalhe">${r.quando} · ${r.duracaoMin} min · com ${r.instrutor}</div>
        <div class="item-detalhe">${r.endereco}</div>
        ${prazo(r)}
      </div>
      <div class="item-acoes">
        <button class="acao discreta risco" data-cancelar-reserva="${r.id}">Cancelar reserva</button>
        ${r.aulaCancelada ? selo('Aula cancelada', 'suspenso') : ''}
      </div>
    </div>`).join('');

  const seloHistorico = r => r.situacao === 'CANCELADA'
    ? (r.penalizada ? selo('Cancelada com penalidade', 'suspenso') : selo('Cancelada', 'inativo'))
    : selo('Encerrada', 'inativo');
  const htmlHistorico = historico.map(r => `<div class="item">
      <div class="item-corpo">
        <div class="item-nome">${r.aula}</div>
        <div class="item-detalhe">${r.quando}</div>
      </div>
      ${seloHistorico(r)}
    </div>`).join('');

  $('#lista-reservas').innerHTML =
    (ativas.length ? htmlAtivas : '<p class="vazio">Nenhuma reserva ativa. Escolha uma aula abaixo.</p>')
    + (historico.length ? '<p class="lista-titulo">Histórico</p>' + htmlHistorico : '');

  /* UC04a 3b: aula em que o aluno já tem reserva confirmada não oferece "Reservar" */
  const jaReservou = a => minhas.some(r => r.aulaId === a.id && r.situacao === 'CONFIRMADA');
  const abertas = estado.aulas.filter(a => !a.cancelada && !a.jaComecou)
    .sort((a, b) => a.inicio.localeCompare(b.inicio));
  $('#aulas-reserva').innerHTML = abertas.length
    ? abertas.map(a => `<article class="aula">
        <div class="aula-quando">${a.inicioLegivel}</div>
        <div class="aula-modalidade">${a.modalidade} em ${a.estabelecimento}, com ${a.instrutor} · ${a.duracaoMin} min</div>
        <div class="vagas" aria-hidden="true">${barraDeVagas(a)}</div>
        <div class="vagas-nota">${a.vagasDisponiveis} de ${a.capacidade} vagas livres</div>
        <div class="aula-acoes">
          ${jaReservou(a)
            ? selo('Você tem vaga nesta aula', 'aprovado')
            : `<button class="acao discreta" data-reservar="${a.id}">Reservar vaga</button>`}
        </div>
      </article>`).join('')
    : '<p class="vazio">Nenhuma aula aberta na rede.</p>';
}

/* ---------- UC07: recepção ---------- */

const rotuloCheckIn = {
  PENDENTE: ['Aguardando validação', 'pendente'],
  VALIDADO: ['Entrada liberada', 'aprovado'],
  RECUSADO: ['Recusado', 'suspenso'],
  EXPIRADO: ['Expirado', 'inativo']
};

function renderRecepcao(est) {
  /* Só estabelecimento credenciado recebe check-in, então só ele usa a recepção. */
  const credenciado = est.situacao === 'APROVADO';
  $('#recepcao-sem-credenciamento').classList.toggle('oculta', credenciado);
  $('#recepcao-conteudo').classList.toggle('oculta', !credenciado);
  if (!credenciado) return;

  /* passo 4: nome do aluno e situação; a situação vem sempre do estado mais recente */
  if (conferencia) {
    const atual = estado.checkIns.find(c => c.codigo === conferencia.codigo);
    const situacao = atual ? atual.situacao : conferencia.situacao;
    const [rotulo, classe] = rotuloCheckIn[situacao];
    $('#recepcao-resultado').innerHTML = `<div class="conferencia">
        <div class="conferencia-nome">${conferencia.aluno}</div>
        <div class="item-detalhe">Check-in ${conferencia.quando} · código ${conferencia.codigo}</div>
        ${selo(rotulo, classe)}
        ${situacao === 'PENDENTE' ? `<div class="botoes">
          <button class="acao" data-validar>Liberar entrada</button>
          <button class="acao discreta risco" data-recusar>Recusar entrada</button>
        </div>` : ''}
      </div>`;
  } else {
    $('#recepcao-resultado').innerHTML = '';
  }

  const deHoje = estado.checkIns.filter(c => c.estabelecimentoCnpj === est.cnpj && c.hoje);
  const pendentes = deHoje.filter(c => c.situacao === 'PENDENTE').length;
  $('#cont-recepcao').textContent = `${pendentes} aguardando`;
  $('#lista-recepcao').innerHTML = deHoje.length
    ? deHoje.slice().reverse().map(c => {
        const [rotulo, classe] = rotuloCheckIn[c.situacao];
        return `<div class="item">
          <div class="item-corpo">
            <div class="item-nome">${c.aluno}</div>
            <div class="item-detalhe">${c.quando}${c.motivoRecusa ? ' · ' + c.motivoRecusa : ''}</div>
          </div>
          ${selo(rotulo, classe)}
        </div>`;
      }).join('')
    : '<p class="vazio">Nenhum check-in neste estabelecimento hoje.</p>';
}

const vistas = {
  admin: { render: renderAdmin, contexto: 'Administração da plataforma' },
  empresa: { render: renderEmpresa, contexto: 'Visão da empresa contratante' },
  estabelecimento: { render: renderEstabelecimento, contexto: 'Visão do estabelecimento parceiro' },
  aluno: { render: renderAluno, contexto: 'Visão do aluno' }
};

function render() {
  Object.keys(vistas).forEach(v =>
    $('#vista-' + v).classList.toggle('oculta', v !== vistaAtual));
  $('#contexto').textContent = vistas[vistaAtual].contexto;
  vistas[vistaAtual].render();
}

/* ==========================================================================
   Eventos
   ========================================================================== */

document.querySelectorAll('.perfil').forEach(botao => {
  botao.addEventListener('click', () => {
    document.querySelectorAll('.perfil').forEach(b => b.classList.remove('ativo'));
    botao.classList.add('ativo');
    vistaAtual = botao.dataset.vista;
    render();
  });
});

/* --- HU4 --- */

$('#form-estabelecimento').addEventListener('submit', ev => {
  ev.preventDefault();
  const nome = $('#est-nome').value.trim();
  executar(() => api('/estabelecimentos', 'POST', {
    cnpj: soDigitos($('#est-cnpj').value),
    nome,
    tipo: $('#est-tipo').value,
    valorPorCheckIn: parseFloat($('#est-valor').value),
    nivelMinimo: parseInt($('#est-nivel').value),
    logradouro: $('#est-logradouro').value.trim(),
    numero: $('#est-numero').value.trim(),
    bairro: $('#est-bairro').value.trim(),
    cidade: $('#est-cidade').value.trim(),
    cep: $('#est-cep').value.trim(),
    latitude: parseFloat($('#est-lat').value),
    longitude: parseFloat($('#est-lng').value)
  }).then(() => ev.target.reset()),
  `${nome} cadastrado. Aprove o credenciamento para ativar.`);
});

$('#form-empresa').addEventListener('submit', ev => {
  ev.preventDefault();
  const razaoSocial = $('#emp-nome').value.trim();
  executar(() => api('/empresas', 'POST', {
    cnpj: soDigitos($('#emp-cnpj').value),
    razaoSocial,
    limiteDependentes: parseInt($('#emp-dep').value),
    diaVencimento: parseInt($('#emp-venc').value)
  }).then(() => ev.target.reset()),
  `${razaoSocial} contratada.`);
});

$('#form-plano').addEventListener('submit', ev => {
  ev.preventDefault();
  const nome = $('#plano-nome').value.trim();
  executar(() => api('/planos', 'POST', {
    nome,
    nivel: parseInt($('#plano-nivel').value),
    valorMensal: parseFloat($('#plano-valor').value),
    limiteAulasMes: parseInt($('#plano-limite').value),
    permiteDependentes: $('#plano-dep').checked
  }).then(() => ev.target.reset()),
  `Plano ${nome} criado e disponibilizado às empresas.`);
});

$('#vista-admin').addEventListener('click', ev => {
  const { aprovar, suspender } = ev.target.dataset;
  if (aprovar !== undefined) {
    executar(() => api(`/estabelecimentos/${aprovar}/aprovar`, 'POST'), 'Credenciamento aprovado.');
  }
  if (suspender !== undefined) {
    executar(() => api(`/estabelecimentos/${suspender}/suspender`, 'POST'), 'Estabelecimento suspenso.');
  }
});

/* --- HU2 --- */

$('#empresa-atual').addEventListener('change', ev => {
  empresaCnpj = ev.target.value;
  render();
});

$('#form-funcionario').addEventListener('submit', ev => {
  ev.preventDefault();
  const nome = $('#func-nome').value.trim();
  const planoId = parseInt($('#func-plano').value);
  if (Number.isNaN(planoId)) {
    avisar('Crie um plano antes de incluir funcionários.', 'erro');
    return;
  }
  executar(() => api(`/empresas/${empresaAtual().cnpj}/funcionarios`, 'POST', {
    nome,
    cpf: soDigitos($('#func-cpf').value),
    matricula: $('#func-matricula').value.trim(),
    planoId
  }).then(() => ev.target.reset()),
  `${nome} incluído no quadro.`);
});

$('#vista-empresa').addEventListener('click', ev => {
  const { trocar, remover, dependente, suspender, reativar } = ev.target.dataset;
  const empresa = empresaAtual();
  const base = `/empresas/${empresa.cnpj}/funcionarios`;

  if (trocar !== undefined) {
    const nomes = estado.planos.map((p, i) => `${i + 1}. ${p.nome}`).join('\n');
    const escolha = prompt(`Novo plano:\n${nomes}`, '1');
    const plano = estado.planos[parseInt(escolha) - 1];
    if (!plano) return;
    executar(() => api(`${base}/${trocar}/plano`, 'POST',
      { planoId: plano.id }), `Plano alterado para ${plano.nome}.`);
  }

  if (suspender !== undefined) {
    const f = empresa.funcionarios.find(x => x.cpf === suspender);
    if (!confirm(`Suspender o benefício de ${f.nome} e dos dependentes por inadimplência (RN7)?`)) return;
    executar(() => api(`${base}/${suspender}/suspender`, 'POST'),
      `Benefício de ${f.nome} suspenso.`);
  }

  if (reativar !== undefined) {
    const f = empresa.funcionarios.find(x => x.cpf === reativar);
    executar(() => api(`${base}/${reativar}/reativar`, 'POST'),
      `Benefício de ${f.nome} reativado.`);
  }

  if (dependente !== undefined) {
    const nome = prompt('Nome do dependente:');
    if (!nome) return;
    const cpf = prompt('CPF do dependente:');
    if (!cpf) return;
    const grau = prompt('Grau de parentesco:', 'Filho') || 'Dependente';
    executar(() => api(`${base}/${dependente}/dependentes`, 'POST',
      { nome, cpf: soDigitos(cpf), grauParentesco: grau }), `${nome} incluído como dependente.`);
  }

  if (remover !== undefined) {
    const f = empresa.funcionarios.find(x => x.cpf === remover);
    if (!confirm(`Remover ${f.nome} do quadro? Os dependentes dele perdem o acesso junto.`)) return;
    executar(() => api(`${base}/${remover}`, 'DELETE'),
      `${f.nome} removido. O acesso dele e dos dependentes foi encerrado.`);
  }
});

/* --- Estabelecimento: seletor e abas --- */

$('#estabelecimento-atual').addEventListener('change', ev => {
  estabelecimentoCnpj = ev.target.value;
  conferencia = null;
  render();
});

document.querySelectorAll('.aba').forEach(botao => {
  botao.addEventListener('click', () => {
    abaAtual = botao.dataset.aba;
    render();
  });
});

/* --- HU5 --- */

$('#form-modalidade').addEventListener('submit', ev => {
  ev.preventDefault();
  const nome = $('#mod-nome').value.trim();
  executar(() => api(`/estabelecimentos/${estabelecimentoAtual().cnpj}/modalidades`, 'POST', {
    nome, descricao: $('#mod-desc').value.trim()
  }).then(() => ev.target.reset()), `${nome} adicionada.`);
});

$('#form-instrutor').addEventListener('submit', ev => {
  ev.preventDefault();
  const nome = $('#ins-nome').value.trim();
  executar(() => api(`/estabelecimentos/${estabelecimentoAtual().cnpj}/instrutores`, 'POST', {
    nome,
    registro: $('#ins-registro').value.trim(),
    especialidade: $('#ins-esp').value.trim()
  }).then(() => ev.target.reset()), `${nome} contratado.`);
});

$('#vista-estabelecimento').addEventListener('click', ev => {
  const { removerModalidade, desligar } = ev.target.dataset;
  const base = `/estabelecimentos/${estabelecimentoAtual().cnpj}`;
  if (removerModalidade !== undefined) {
    executar(() => api(`${base}/modalidades/${removerModalidade}`, 'DELETE'),
      'Modalidade removida.');
  }
  if (desligar !== undefined) {
    executar(() => api(`${base}/instrutores/${encodeURIComponent(desligar)}`, 'DELETE'),
      'Instrutor desligado.');
  }
});

/* --- HU1 --- */

$('#form-aula').addEventListener('submit', ev => {
  ev.preventDefault();
  const modalidadeId = parseInt($('#aula-modalidade').value);
  const registroInstrutor = $('#aula-instrutor').value;
  if (Number.isNaN(modalidadeId) || !registroInstrutor) {
    avisar('Cadastre uma modalidade e um instrutor antes de criar aulas.', 'erro');
    return;
  }
  executar(() => api('/aulas', 'POST', {
    cnpj: estabelecimentoAtual().cnpj,
    modalidadeId,
    registroInstrutor,
    inicio: $('#aula-inicio').value,          // formato ISO local, aceito por LocalDateTime.parse
    duracaoMin: parseInt($('#aula-duracao').value),
    capacidade: parseInt($('#aula-capacidade').value)
  }).then(() => {
    ev.target.reset();
    $('#aula-duracao').value = 50;
    $('#aula-capacidade').value = 10;
  }), 'Aula agendada.');
});

$('#aba-grade').addEventListener('click', ev => {
  const { instrutorAula, capacidade, cancelar } = ev.target.dataset;
  const est = estabelecimentoAtual();

  if (instrutorAula !== undefined) {
    const nomes = est.instrutores.map((i, n) => `${n + 1}. ${i.nome}`).join('\n');
    const escolha = prompt(`Novo professor:\n${nomes}`, '1');
    const instrutor = est.instrutores[parseInt(escolha) - 1];
    if (!instrutor) return;
    executar(() => api(`/aulas/${instrutorAula}/instrutor`, 'POST',
      { registroInstrutor: instrutor.registro }), `${instrutor.nome} assumiu a aula.`);
  }

  if (capacidade !== undefined) {
    const aula = estado.aulas.find(a => a.id === parseInt(capacidade));
    const nova = prompt('Número de vagas:', aula.capacidade);
    if (!nova) return;
    executar(() => api(`/aulas/${capacidade}/capacidade`, 'POST',
      { capacidade: parseInt(nova) }), 'Vagas atualizadas.');
  }

  if (cancelar !== undefined) {
    if (!confirm('Cancelar esta aula?')) return;
    executar(() => api(`/aulas/${cancelar}/cancelar`, 'POST'), 'Aula cancelada.');
  }
});

/* --- UC03 e UC04a --- */

$('#aluno-atual').addEventListener('change', ev => {
  alunoCpf = ev.target.value;
  render();
});

$('#checkin-estabelecimento').addEventListener('change', ev => {
  checkinEstabelecimentoCnpj = ev.target.value;
  render();   // troca de estabelecimento recoloca as coordenadas da porta dele
});

$('#loc-porta').addEventListener('click', () => {
  const est = estabelecimentoDoCheckin();
  if (est) preencherCoordenadas(est.latitude, est.longitude);
});

$('#loc-longe').addEventListener('click', () => {
  const est = estabelecimentoDoCheckin();
  if (est) preencherCoordenadas(est.latitude + 0.009, est.longitude);   // ~1 km ao norte
});

/* Validar Localização do Dispositivo: no sistema web, quem fornece é o navegador. */
$('#loc-navegador').addEventListener('click', () => {
  if (!navigator.geolocation) {
    avisar('Este navegador não fornece localização.', 'erro');
    return;
  }
  navigator.geolocation.getCurrentPosition(
    pos => {
      preencherCoordenadas(pos.coords.latitude, pos.coords.longitude);
      avisar('Localização do navegador obtida.');
    },
    erro => avisar(erro.code === erro.PERMISSION_DENIED
      ? 'A permissão de localização é obrigatória para o check-in. Libere no navegador e tente de novo.'
      : 'Não foi possível obter a localização do navegador.', 'erro'),
    { enableHighAccuracy: true, timeout: 10000 });
});

$('#form-checkin').addEventListener('submit', ev => {
  ev.preventDefault();
  const aluno = alunoAtual();
  const est = estabelecimentoDoCheckin();
  if (!aluno || !est) {
    avisar('É preciso um aluno e um estabelecimento credenciado para fazer check-in.', 'erro');
    return;
  }
  executar(() => api('/checkins', 'POST', {
    cpf: aluno.cpf,
    cnpj: est.cnpj,
    latitude: parseFloat($('#checkin-lat').value),
    longitude: parseFloat($('#checkin-lng').value)
  }), `Check-in registrado em ${est.nome}.`);
});

$('#vista-aluno').addEventListener('click', ev => {
  const { reservar, cancelarReserva } = ev.target.dataset;
  const aluno = alunoAtual();

  if (reservar !== undefined) {
    const aula = estado.aulas.find(a => a.id === parseInt(reservar));
    executar(() => api('/reservas', 'POST', { cpf: aluno.cpf, aulaId: aula.id }),
      `Vaga reservada: ${aula.modalidade}, ${aula.inicioLegivel}.`);
  }

  /* UC04b: passo 3 (confirmação) e extensão 4a.1 (confirmação adicional) */
  if (cancelarReserva !== undefined) {
    const r = estado.reservas.find(x => x.id === parseInt(cancelarReserva));
    if (!confirm(`Cancelar a reserva de ${r.aula}, ${r.quando}?`)) return;   // 3a: desistiu
    let aceitaPenalidade = false;
    if (r.cancelamentoPenalizado) {
      if (!confirm(`O prazo sem penalidade terminou (${r.prazoSemPenalidade}). `
          + 'O cancelamento vai ficar registrado como penalizado. Cancelar mesmo assim?')) return;
      aceitaPenalidade = true;
    }
    executar(() => api(`/reservas/${r.id}/cancelar`, 'POST', { cpf: aluno.cpf, aceitaPenalidade }),
      'Reserva cancelada. A vaga foi liberada.');
  }
});

/* --- UC07 --- */

const caminhoRecepcao = codigo =>
  `/recepcao/${estabelecimentoAtual().cnpj}/checkins/${encodeURIComponent(codigo)}`;

$('#form-codigo').addEventListener('submit', ev => {
  ev.preventDefault();
  const codigo = $('#recepcao-codigo').value.trim().toUpperCase();
  conferencia = null;
  executar(async () => {
    conferencia = await api(caminhoRecepcao(codigo));
    ev.target.reset();
  });
});

/* extensão 2a: sem o código, busca pelo CPF; validar e recusar seguem pelo código achado */
$('#form-cpf').addEventListener('submit', ev => {
  ev.preventDefault();
  const cpf = soDigitos($('#recepcao-cpf').value);
  conferencia = null;
  executar(async () => {
    conferencia = await api(`/recepcao/${estabelecimentoAtual().cnpj}/cpf/${cpf}`);
    ev.target.reset();
  });
});

$('#aba-recepcao').addEventListener('click', ev => {
  if (!conferencia) return;
  const { validar, recusar } = ev.target.dataset;

  if (validar !== undefined) {
    executar(() => api(caminhoRecepcao(conferencia.codigo) + '/validar', 'POST'),
      `Entrada de ${conferencia.aluno} liberada.`);
  }

  if (recusar !== undefined) {
    const motivo = prompt('Motivo da recusa:');
    if (motivo === null) return;
    executar(() => api(caminhoRecepcao(conferencia.codigo) + '/recusar', 'POST', { motivo }),
      `Entrada de ${conferencia.aluno} recusada.`);
  }
});

/* ---------- início ---------- */

carregarEstado()
  .then(render)
  .catch(() => {
    avisar('Não foi possível falar com o servidor. A aplicação Java está rodando?', 'erro');
  });
