/* ==========================================================================
   Camada de apresentação — versão integrada.

   Diferente do protótipo, aqui não há nenhuma classe de domínio no navegador.
   Toda regra de negócio roda no servidor Java; este arquivo só desenha a tela
   e envia requisições HTTP.

   Fluxo de cada ação: envia POST/DELETE -> recarrega GET /api/estado ->
   redesenha a vista atual. Erros de regra de negócio voltam como HTTP 400 com
   a mensagem original definida no domínio.
   ========================================================================== */

const $ = sel => document.querySelector(sel);

/** Estado devolvido pelo servidor. Nunca é modificado aqui. */
let estado = {
  estabelecimentos: [], empresas: [], planos: [], aulas: [],
  alunos: [], checkIns: [], reservas: []
};

let vistaAtual = 'admin';
let empresaId = 0;
let estabelecimentoId = 0;

/* seleções que precisam sobreviver ao redesenho da tela */
let coberturaPlanoId = 0;
let coberturaEstabelecimentoId = 0;
let alunoId = 0;
let checkinEstabelecimentoId = null;
let coordenadasDe = null;   // estabelecimento cujas coordenadas estão nos campos do check-in
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

function moeda(v) {
  return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function preencherSelect(el, itens, rotulo, idSelecionado) {
  el.innerHTML = itens
    .map(it => `<option value="${it.id}"${it.id === idSelecionado ? ' selected' : ''}>${rotulo(it)}</option>`)
    .join('');
}

/** Uma barra por vaga; as reservadas aparecem preenchidas. */
function barraDeVagas(aula) {
  const ocupadas = aula.capacidade - aula.vagasDisponiveis;
  const visiveis = Math.min(aula.capacidade, 24);
  return Array.from({ length: visiveis },
    (_, i) => `<i${i < ocupadas ? ' class="ocupada"' : ''}></i>`).join('');
}

const empresaAtual = () => estado.empresas.find(e => e.id === empresaId) || estado.empresas[0];
const estabelecimentoAtual = () =>
  estado.estabelecimentos.find(e => e.id === estabelecimentoId) || estado.estabelecimentos[0];

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
          ? `<button class="acao discreta risco" data-suspender="${e.id}">Suspender</button>`
          : `<button class="acao discreta" data-aprovar="${e.id}">Aprovar</button>`;
        return `<div class="item">
          <div class="item-corpo">
            <div class="item-nome">${e.nome}</div>
            <div class="item-detalhe">${e.tipo} · ${moeda(e.valorPorCheckIn)} por check-in</div>
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
          <div class="item-detalhe">${e.totalBeneficiarios} beneficiários · até ${e.limiteDependentes} dependentes por funcionário</div>
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
          ? p.cobertura.map(c => `<div>${c.estabelecimento}${
              c.modalidades.length ? ': ' + c.modalidades.join(', ') : ''}${
              c.estabelecimentoLiberado ? '' : ' (estabelecimento não liberado)'}</div>`).join('')
          : '<p class="vazio">Não cobre nenhum estabelecimento</p>'}</div>
      </div>`).join('')
    : '<p class="vazio">Nenhum plano configurado. Sem plano não é possível incluir beneficiários.</p>';

  preencherSelect($('#cob-plano'), estado.planos, p => p.nome, coberturaPlanoId);
  preencherSelect($('#cob-estabelecimento'), estado.estabelecimentos,
    e => e.nome, coberturaEstabelecimentoId);
  const estCob = estado.estabelecimentos.find(e => e.id === coberturaEstabelecimentoId)
    || estado.estabelecimentos[0];
  $('#cob-modalidade').innerHTML = '<option value="">Acesso ao estabelecimento</option>'
    + (estCob ? estCob.modalidades.map(m =>
        `<option value="${m.id}">Modalidade ${m.nome}</option>`).join('') : '');
}

/* ---------- HU2: empresa ---------- */

function renderEmpresa() {
  preencherSelect($('#empresa-atual'), estado.empresas, e => e.razaoSocial, empresaId);
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
          `<div class="item-detalhe">${d.nome} · ${d.grauParentesco} · CPF ${d.cpf}${d.elegivel ? '' : ' (inativo)'}</div>`
        ).join('');
        const podeDep = f.planoPermiteDependentes
          && f.dependentes.length < empresa.limiteDependentes;
        return `<div class="item">
          <div class="item-corpo">
            <div class="item-nome">${f.nome}</div>
            <div class="item-detalhe">${f.matricula} · CPF ${f.cpf} · plano ${f.plano || 'nenhum'}</div>
            ${deps}
          </div>
          ${f.elegivel ? selo('Ativo', 'aprovado') : selo('Sem acesso', 'inativo')}
          <button class="acao discreta" data-trocar="${f.id}">Trocar plano</button>
          ${podeDep ? `<button class="acao discreta" data-dependente="${f.id}">Incluir dependente</button>` : ''}
          <button class="acao discreta risco" data-remover="${f.id}">Remover</button>
        </div>`;
      }).join('')
    : '<p class="vazio">Ninguém no quadro ainda. Inclua o primeiro funcionário abaixo.</p>';
}

/* ---------- HU5: estabelecimento ---------- */

function renderEstabelecimento() {
  preencherSelect($('#estabelecimento-atual'), estado.estabelecimentos,
    e => e.nome, estabelecimentoId);
  const est = estabelecimentoAtual();
  if (!est) return;

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
        <button class="acao discreta risco" data-desligar="${i.id}">Desligar</button>
      </div>`).join('')
    : '<p class="vazio">Nenhum instrutor. Sem instrutor não é possível criar aulas.</p>';

  preencherSelect($('#recepcao-estabelecimento'), estado.estabelecimentos,
    e => e.nome, estabelecimentoId);

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

  const deHoje = estado.checkIns.filter(c => c.estabelecimentoId === est.id && c.hoje);
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

  /* HU1: grade de aulas */
  preencherSelect($('#grade-estabelecimento'), estado.estabelecimentos,
    e => e.nome, estabelecimentoId);

  preencherSelect($('#aula-modalidade'), est.modalidades, m => m.nome);
  preencherSelect($('#aula-instrutor'), est.instrutores, i => i.nome);

  const aulas = estado.aulas.filter(a => a.estabelecimentoId === est.id);

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

const alunoAtual = () => estado.alunos.find(a => a.id === alunoId) || estado.alunos[0];
const credenciados = () => estado.estabelecimentos.filter(e => e.situacao === 'APROVADO');
const estabelecimentoDoCheckin = () =>
  credenciados().find(e => e.id === checkinEstabelecimentoId) || credenciados()[0];

function preencherCoordenadas(lat, lng) {
  $('#checkin-lat').value = lat.toFixed(6);
  $('#checkin-lng').value = lng.toFixed(6);
}

function renderAluno() {
  preencherSelect($('#aluno-atual'), estado.alunos, a =>
    `${a.nome}${a.tipo === 'Dependente' ? ' (dependente)' : ''} — ${a.plano || 'sem plano'}`,
    alunoId);
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
  const meusCheckIns = estado.checkIns.filter(c => c.alunoId === aluno.id);
  $('#cont-checkins').textContent = `${meusCheckIns.length} registrados`;
  const deHoje = meusCheckIns.filter(c => c.hoje).pop();
  const situacaoComprovante = {
    PENDENTE: ['', 'Apresente este código na recepção'],
    VALIDADO: [' validado', 'Entrada liberada pela recepção'],
    RECUSADO: [' recusado', 'Entrada recusada pela recepção']
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
  preencherSelect($('#checkin-estabelecimento'), lista, e => e.nome, est ? est.id : null);
  if (est && coordenadasDe !== est.id) {
    preencherCoordenadas(est.latitude, est.longitude);
    coordenadasDe = est.id;
  }

  /* reservas */
  /* UC04b passo 1: confirmadas ainda não iniciadas, por data e hora */
  const minhas = estado.reservas.filter(r => r.alunoId === aluno.id);
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

  const abertas = estado.aulas.filter(a => !a.cancelada && !a.jaComecou)
    .sort((a, b) => a.inicio.localeCompare(b.inicio));
  $('#aulas-reserva').innerHTML = abertas.length
    ? abertas.map(a => `<article class="aula">
        <div class="aula-quando">${a.inicioLegivel}</div>
        <div class="aula-modalidade">${a.modalidade} em ${a.estabelecimento}, com ${a.instrutor} · ${a.duracaoMin} min</div>
        <div class="vagas" aria-hidden="true">${barraDeVagas(a)}</div>
        <div class="vagas-nota">${a.vagasDisponiveis} de ${a.capacidade} vagas livres</div>
        <div class="aula-acoes">
          <button class="acao discreta" data-reservar="${a.id}">Reservar vaga</button>
        </div>
      </article>`).join('')
    : '<p class="vazio">Nenhuma aula aberta na rede.</p>';
}

/* ---------- UC07: recepção ---------- */

const rotuloCheckIn = {
  PENDENTE: ['Aguardando validação', 'pendente'],
  VALIDADO: ['Entrada liberada', 'aprovado'],
  RECUSADO: ['Recusado', 'suspenso']
};


const vistas = {
  admin: { render: renderAdmin, contexto: 'Administração da plataforma' },
  empresa: { render: renderEmpresa, contexto: 'Visão da empresa contratante' },
  estabelecimento: { render: renderEstabelecimento, contexto: 'Visão do estabelecimento parceiro' },
  aluno: { render: renderAluno, contexto: 'Visão do aluno' },
};

function render() {
  Object.keys(vistas).forEach(v =>
    $('#vista-' + v).classList.toggle('oculta', v !== vistaAtual));
  /* grade e recepção agora fazem parte da visão do estabelecimento */
  ['grade', 'recepcao'].forEach(v =>
    $('#vista-' + v).classList.toggle('oculta', vistaAtual !== 'estabelecimento'));
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
    nome,
    tipo: $('#est-tipo').value,
    valorPorCheckIn: parseFloat($('#est-valor').value),
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

$('#cob-plano').addEventListener('change', ev => {
  coberturaPlanoId = parseInt(ev.target.value);
});

$('#cob-estabelecimento').addEventListener('change', ev => {
  coberturaEstabelecimentoId = parseInt(ev.target.value);
  render();
});

$('#form-cobertura').addEventListener('submit', ev => {
  ev.preventDefault();
  const plano = estado.planos.find(p => p.id === parseInt($('#cob-plano').value));
  const est = estado.estabelecimentos.find(e => e.id === parseInt($('#cob-estabelecimento').value));
  if (!plano || !est) {
    avisar('Crie um plano e um estabelecimento antes de configurar a cobertura.', 'erro');
    return;
  }
  coberturaPlanoId = plano.id;
  coberturaEstabelecimentoId = est.id;
  const valor = $('#cob-modalidade').value;

  if (valor === '') {
    executar(() => api(`/planos/${plano.id}/estabelecimentos`, 'POST',
      { estabelecimentoId: est.id }), `${est.nome} liberado no plano ${plano.nome}.`);
  } else {
    const mod = est.modalidades.find(m => m.id === parseInt(valor));
    executar(() => api(`/planos/${plano.id}/modalidades`, 'POST',
      { estabelecimentoId: est.id, modalidadeId: mod.id }),
      `${mod.nome} de ${est.nome} liberada no plano ${plano.nome}.`);
  }
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
  empresaId = parseInt(ev.target.value);
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
  executar(() => api(`/empresas/${empresaId}/funcionarios`, 'POST', {
    nome,
    cpf: $('#func-cpf').value.trim(),
    matricula: $('#func-matricula').value.trim(),
    planoId
  }).then(() => ev.target.reset()),
  `${nome} incluído no quadro.`);
});

$('#vista-empresa').addEventListener('click', ev => {
  const { trocar, remover, dependente } = ev.target.dataset;
  const empresa = empresaAtual();

  if (trocar !== undefined) {
    const nomes = estado.planos.map((p, i) => `${i + 1}. ${p.nome}`).join('\n');
    const escolha = prompt(`Novo plano:\n${nomes}`, '1');
    const plano = estado.planos[parseInt(escolha) - 1];
    if (!plano) return;
    executar(() => api(`/empresas/${empresaId}/funcionarios/${trocar}/plano`, 'POST',
      { planoId: plano.id }), `Plano alterado para ${plano.nome}.`);
  }

  if (dependente !== undefined) {
    const nome = prompt('Nome do dependente:');
    if (!nome) return;
    const cpf = prompt('CPF do dependente:');
    if (!cpf) return;
    const grau = prompt('Grau de parentesco:', 'Filho') || 'Dependente';
    executar(() => api(`/empresas/${empresaId}/funcionarios/${dependente}/dependentes`, 'POST',
      { nome, cpf, grauParentesco: grau }), `${nome} incluído como dependente.`);
  }

  if (remover !== undefined) {
    const f = empresa.funcionarios.find(x => x.id === parseInt(remover));
    if (!confirm(`Remover ${f.nome} do quadro? Os dependentes dele perdem o acesso junto.`)) return;
    executar(() => api(`/empresas/${empresaId}/funcionarios/${remover}`, 'DELETE'),
      `${f.nome} removido. O acesso dele e dos dependentes foi encerrado.`);
  }
});

/* --- HU5 --- */

$('#estabelecimento-atual').addEventListener('change', ev => {
  estabelecimentoId = parseInt(ev.target.value);
  render();
});

$('#form-modalidade').addEventListener('submit', ev => {
  ev.preventDefault();
  const nome = $('#mod-nome').value.trim();
  executar(() => api(`/estabelecimentos/${estabelecimentoId}/modalidades`, 'POST', {
    nome, descricao: $('#mod-desc').value.trim()
  }).then(() => ev.target.reset()), `${nome} adicionada.`);
});

$('#form-instrutor').addEventListener('submit', ev => {
  ev.preventDefault();
  const nome = $('#ins-nome').value.trim();
  executar(() => api(`/estabelecimentos/${estabelecimentoId}/instrutores`, 'POST', {
    nome,
    registro: $('#ins-registro').value.trim(),
    especialidade: $('#ins-esp').value.trim()
  }).then(() => ev.target.reset()), `${nome} contratado.`);
});

$('#vista-estabelecimento').addEventListener('click', ev => {
  const { removerModalidade, desligar } = ev.target.dataset;
  if (removerModalidade !== undefined) {
    executar(() => api(
      `/estabelecimentos/${estabelecimentoId}/modalidades/${removerModalidade}`, 'DELETE'),
      'Modalidade removida.');
  }
  if (desligar !== undefined) {
    executar(() => api(
      `/estabelecimentos/${estabelecimentoId}/instrutores/${desligar}`, 'DELETE'),
      'Instrutor desligado.');
  }
});

/* --- HU1 --- */

$('#grade-estabelecimento').addEventListener('change', ev => {
  estabelecimentoId = parseInt(ev.target.value);
  render();
});

$('#form-aula').addEventListener('submit', ev => {
  ev.preventDefault();
  const modalidadeId = parseInt($('#aula-modalidade').value);
  const instrutorId = parseInt($('#aula-instrutor').value);
  if (Number.isNaN(modalidadeId) || Number.isNaN(instrutorId)) {
    avisar('Cadastre uma modalidade e um instrutor antes de criar aulas.', 'erro');
    return;
  }
  executar(() => api('/aulas', 'POST', {
    estabelecimentoId,
    modalidadeId,
    instrutorId,
    inicio: $('#aula-inicio').value,          // formato ISO local, aceito por LocalDateTime.parse
    duracaoMin: parseInt($('#aula-duracao').value),
    capacidade: parseInt($('#aula-capacidade').value)
  }).then(() => {
    ev.target.reset();
    $('#aula-duracao').value = 50;
    $('#aula-capacidade').value = 10;
  }), 'Aula agendada.');
});

$('#vista-grade').addEventListener('click', ev => {
  const { instrutorAula, capacidade, cancelar } = ev.target.dataset;
  const est = estabelecimentoAtual();

  if (instrutorAula !== undefined) {
    const nomes = est.instrutores.map((i, n) => `${n + 1}. ${i.nome}`).join('\n');
    const escolha = prompt(`Novo professor:\n${nomes}`, '1');
    const instrutor = est.instrutores[parseInt(escolha) - 1];
    if (!instrutor) return;
    executar(() => api(`/aulas/${instrutorAula}/instrutor`, 'POST',
      { instrutorId: instrutor.id }), `${instrutor.nome} assumiu a aula.`);
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
  alunoId = parseInt(ev.target.value);
  render();
});

$('#checkin-estabelecimento').addEventListener('change', ev => {
  checkinEstabelecimentoId = parseInt(ev.target.value);
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
    alunoId: aluno.id,
    estabelecimentoId: est.id,
    latitude: parseFloat($('#checkin-lat').value),
    longitude: parseFloat($('#checkin-lng').value)
  }), `Check-in registrado em ${est.nome}.`);
});

$('#vista-aluno').addEventListener('click', ev => {
  const { reservar, cancelarReserva } = ev.target.dataset;
  const aluno = alunoAtual();

  if (reservar !== undefined) {
    const aula = estado.aulas.find(a => a.id === parseInt(reservar));
    executar(() => api('/reservas', 'POST', { alunoId: aluno.id, aulaId: aula.id }),
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
    executar(() => api(`/reservas/${r.id}/cancelar`, 'POST', { alunoId: aluno.id, aceitaPenalidade }),
      'Reserva cancelada. A vaga foi liberada.');
  }
});

/* --- UC07 --- */

$('#recepcao-estabelecimento').addEventListener('change', ev => {
  estabelecimentoId = parseInt(ev.target.value);
  conferencia = null;
  render();
});

const caminhoRecepcao = codigo =>
  `/recepcao/${estabelecimentoId}/checkins/${encodeURIComponent(codigo)}`;

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
  const cpf = $('#recepcao-cpf').value.replace(/\D/g, '');
  conferencia = null;
  executar(async () => {
    conferencia = await api(`/recepcao/${estabelecimentoId}/cpf/${cpf}`);
    ev.target.reset();
  });
});

$('#vista-recepcao').addEventListener('click', ev => {
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