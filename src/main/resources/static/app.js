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
let estado = { estabelecimentos: [], empresas: [], planos: [], aulas: [] };

let vistaAtual = 'admin';
let empresaId = 0;
let estabelecimentoId = 0;

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
      </div>`).join('')
    : '<p class="vazio">Nenhum plano configurado. Sem plano não é possível incluir beneficiários.</p>';
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
          `<div class="item-detalhe">${d.nome} · ${d.grauParentesco}${d.elegivel ? '' : ' (inativo)'}</div>`
        ).join('');
        const podeDep = f.planoPermiteDependentes
          && f.dependentes.length < empresa.limiteDependentes;
        return `<div class="item">
          <div class="item-corpo">
            <div class="item-nome">${f.nome}</div>
            <div class="item-detalhe">${f.matricula} · plano ${f.plano || 'nenhum'}</div>
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
}

/* ---------- HU1: grade de aulas ---------- */

function renderGrade() {
  preencherSelect($('#grade-estabelecimento'), estado.estabelecimentos,
    e => e.nome, estabelecimentoId);
  const est = estabelecimentoAtual();
  if (!est) return;

  preencherSelect($('#aula-modalidade'), est.modalidades, m => m.nome);
  preencherSelect($('#aula-instrutor'), est.instrutores, i => i.nome);

  const aulas = estado.aulas.filter(a => a.estabelecimentoId === est.id);

  $('#lista-aulas').innerHTML = aulas.length
    ? aulas.map(aula => {
        const vagas = Array.from({ length: Math.min(aula.capacidade, 24) },
          () => '<i></i>').join('');
        const excedente = aula.capacidade > 24 ? ` e mais ${aula.capacidade - 24}` : '';
        return `<article class="aula${aula.cancelada ? ' cancelada' : ''}">
          <div class="aula-quando">${aula.inicioLegivel}</div>
          <div class="aula-modalidade">${aula.modalidade} com ${aula.instrutor} · ${aula.duracaoMin} min</div>
          <div class="vagas" aria-hidden="true">${vagas}</div>
          <div class="vagas-nota">${aula.capacidade} vagas${excedente}${aula.cancelada ? ' · aula cancelada' : ''}</div>
          <div class="aula-acoes">
            <button class="acao discreta" data-instrutor-aula="${aula.id}">Trocar professor</button>
            <button class="acao discreta" data-capacidade="${aula.id}">Mudar vagas</button>
            ${aula.cancelada ? '' : `<button class="acao discreta risco" data-cancelar="${aula.id}">Cancelar</button>`}
          </div>
        </article>`;
      }).join('')
    : '<p class="vazio">Nenhuma aula na grade deste estabelecimento.</p>';
}

const vistas = {
  admin: { render: renderAdmin, contexto: 'Administração da plataforma' },
  empresa: { render: renderEmpresa, contexto: 'Visão da empresa contratante' },
  estabelecimento: { render: renderEstabelecimento, contexto: 'Visão do estabelecimento parceiro' },
  grade: { render: renderGrade, contexto: 'Grade de aulas do estabelecimento' }
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
    nome,
    tipo: $('#est-tipo').value,
    valorPorCheckIn: parseFloat($('#est-valor').value)
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
    const grau = prompt('Grau de parentesco:', 'Filho') || 'Dependente';
    executar(() => api(`/empresas/${empresaId}/funcionarios/${dependente}/dependentes`, 'POST',
      { nome, grauParentesco: grau }), `${nome} incluído como dependente.`);
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

/* ---------- início ---------- */

carregarEstado()
  .then(render)
  .catch(() => {
    avisar('Não foi possível falar com o servidor. A aplicação Java está rodando?', 'erro');
  });
