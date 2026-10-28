/*
  Tema Mario Bros: sons 8-bit gerados na hora (Web Audio, sem arquivos de áudio), moedas, HUD e personagens.
  Tudo aqui é enfeite: sem este script o sistema funciona igual.
*/
(() => {
    'use strict';

    const CHAVE_SOM = 'mario.som';
    const CHAVE_MOEDAS = 'mario.moedas';
    const MAXIMO_MOEDAS = 999;
    // Tempo para o som começar antes de a página trocar (senão a navegação corta o som).
    const ATRASO_NAVEGACAO_MS = 180;
    const reduzMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // ---------- Preferências do navegador (podem estar bloqueadas, ex.: aba anônima restrita) ----------

    function ler(chave) {
        try {
            return window.localStorage.getItem(chave);
        } catch {
            return null;
        }
    }

    function gravar(chave, valor) {
        try {
            window.localStorage.setItem(chave, valor);
        } catch {
            // Sem armazenamento a preferência só não fica salva.
        }
    }

    const somLigado = () => ler(CHAVE_SOM) !== 'desligado';

    // ---------- Sons ----------

    let audio = null;

    function contextoAudio() {
        const Contexto = window.AudioContext || window.webkitAudioContext;
        if (!Contexto) {
            return null;
        }
        audio ??= new Contexto();
        if (audio.state === 'suspended') {
            audio.resume();
        }
        return audio;
    }

    /** notas: lista de [frequência, duração em segundos, frequência final (opcional, faz um "deslize")]. */
    function tocar(notas, onda = 'square', volume = 0.07) {
        if (!somLigado()) {
            return;
        }
        const ctx = contextoAudio();
        if (!ctx) {
            return;
        }
        let inicio = ctx.currentTime + 0.01;
        for (const [frequencia, duracao, frequenciaFinal] of notas) {
            const oscilador = ctx.createOscillator();
            const ganho = ctx.createGain();
            oscilador.type = onda;
            oscilador.frequency.setValueAtTime(frequencia, inicio);
            if (frequenciaFinal) {
                oscilador.frequency.exponentialRampToValueAtTime(frequenciaFinal, inicio + duracao);
            }
            ganho.gain.setValueAtTime(volume, inicio);
            ganho.gain.exponentialRampToValueAtTime(0.0001, inicio + duracao);
            oscilador.connect(ganho).connect(ctx.destination);
            oscilador.start(inicio);
            oscilador.stop(inicio + duracao + 0.02);
            inicio += duracao;
        }
    }

    const SONS = {
        moeda: () => tocar([[988, 0.08], [1319, 0.4]]),
        pulo: () => tocar([[260, 0.16, 720]], 'square', 0.05),
        cano: () => tocar([[700, 0.09, 350], [600, 0.09, 300], [500, 0.09, 250], [400, 0.16, 120]]),
        vida: () => tocar([[784, 0.07], [988, 0.07], [1175, 0.07], [1568, 0.07], [1976, 0.25]]),
        crescer: () => tocar([[523, 0.05], [659, 0.05], [784, 0.05], [1047, 0.05], [1319, 0.05], [1568, 0.2]]),
        batida: () => tocar([[160, 0.09, 90]], 'triangle', 0.25),
        ligar: () => tocar([[1319, 0.06], [1760, 0.12]]),
    };

    function somDoBotao(elemento) {
        if (!elemento) {
            return 'moeda';
        }
        if (elemento.matches('.mundo')) {
            return 'crescer';
        }
        if (elemento.matches('.btn-outline-success')) {
            return 'vida';
        }
        if (elemento.matches('.btn-danger, .btn-outline-danger, .btn-outline-light')) {
            return 'cano';
        }
        if (elemento.matches('.btn-primary')) {
            return 'moeda';
        }
        return 'pulo';
    }

    // ---------- Moedas ----------

    function moedas() {
        const valor = Number.parseInt(ler(CHAVE_MOEDAS) ?? '0', 10);
        return Number.isInteger(valor) && valor > 0 ? Math.min(valor, MAXIMO_MOEDAS) : 0;
    }

    function atualizarContadores() {
        const total = moedas();
        document.querySelectorAll('[data-mario-moedas]').forEach((elemento) => {
            elemento.textContent = `×${String(total).padStart(2, '0')}`;
        });
        document.querySelectorAll('[data-mario-pontos]').forEach((elemento) => {
            elemento.textContent = String(total * 200).padStart(6, '0');
        });
    }

    function soltarMoeda(origem) {
        if (reduzMovimento || !origem) {
            return;
        }
        const area = origem.getBoundingClientRect();
        const moeda = document.createElement('span');
        moeda.className = 'mario-moeda-pulando';
        moeda.style.left = `${area.left + window.scrollX + area.width / 2}px`;
        moeda.style.top = `${area.top + window.scrollY - 26}px`;
        moeda.addEventListener('animationend', () => moeda.remove());
        document.body.append(moeda);
    }

    function ganharMoeda(origem) {
        gravar(CHAVE_MOEDAS, String(Math.min(moedas() + 1, MAXIMO_MOEDAS)));
        atualizarContadores();
        soltarMoeda(origem);
    }

    // ---------- Sons ao navegar e enviar formulários ----------

    document.addEventListener('click', (evento) => {
        const link = evento.target.closest('a[href]');
        if (!link || evento.defaultPrevented || evento.button !== 0
            || evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.altKey) {
            return;
        }
        const mesmaAba = !link.target || link.target === '_self';
        const internoDaPagina = link.getAttribute('href').startsWith('#');
        if (!mesmaAba || internoDaPagina || link.hasAttribute('download') || link.origin !== window.location.origin) {
            return;
        }
        if (!somLigado()) {
            return;
        }
        evento.preventDefault();
        SONS[link.classList.contains('btn') ? somDoBotao(link) : 'pulo']();
        window.setTimeout(() => window.location.assign(link.href), ATRASO_NAVEGACAO_MS);
    });

    document.addEventListener('submit', (evento) => {
        const formulario = evento.target;
        // "liberado": é o reenvio feito por este script depois do som; segue normalmente.
        if (evento.defaultPrevented || formulario.dataset.marioEnvio === 'liberado') {
            return;
        }
        // Clique repetido enquanto o som toca: ignora, para não enviar duas vezes.
        if (formulario.dataset.marioEnvio === 'aguardando') {
            evento.preventDefault();
            return;
        }
        const botao = evento.submitter ?? null;
        if (botao?.matches('.btn-outline-success')) {
            ganharMoeda(botao);
        } else if (botao?.matches('.btn-primary')) {
            soltarMoeda(botao);
        }
        if (!somLigado()) {
            return;
        }
        evento.preventDefault();
        formulario.dataset.marioEnvio = 'aguardando';
        SONS[somDoBotao(botao)]();
        window.setTimeout(() => {
            formulario.dataset.marioEnvio = 'liberado';
            formulario.requestSubmit(botao ?? undefined);
        }, ATRASO_NAVEGACAO_MS);
    });

    // Voltar pelo histórico pode restaurar a página com o formulário marcado; libera de novo.
    window.addEventListener('pageshow', () => {
        document.querySelectorAll('form[data-mario-envio]').forEach((formulario) => {
            delete formulario.dataset.marioEnvio;
        });
    });

    // ---------- Botão de som ----------

    function criarBotaoSom() {
        const botao = document.createElement('button');
        const icone = document.createElement('i');
        const texto = document.createElement('span');
        botao.type = 'button';
        botao.className = 'mario-som';
        botao.title = 'Ligar ou desligar os sons (tecla M)';
        icone.setAttribute('aria-hidden', 'true');
        botao.append(icone, texto);

        const atualizar = () => {
            const ligado = somLigado();
            icone.className = ligado ? 'bi bi-volume-up-fill' : 'bi bi-volume-mute-fill';
            texto.textContent = ligado ? 'Som ligado' : 'Som desligado';
            // No celular o texto some (só fica o ícone); o rótulo continua para leitores de tela.
            botao.setAttribute('aria-label', texto.textContent);
        };
        botao.addEventListener('click', () => {
            gravar(CHAVE_SOM, somLigado() ? 'desligado' : 'ligado');
            atualizar();
            SONS.ligar();
        });
        document.addEventListener('keydown', (evento) => {
            const digitando = evento.target instanceof Element
                && evento.target.closest('input, textarea, select, [contenteditable]');
            if (evento.key?.toLowerCase() === 'm' && !digitando && !evento.ctrlKey && !evento.metaKey && !evento.altKey) {
                botao.click();
            }
        });
        atualizar();
        document.body.append(botao);
    }

    // ---------- Cenário ----------

    function criarEnfeite(classe) {
        const elemento = document.createElement('span');
        elemento.className = classe;
        elemento.setAttribute('aria-hidden', 'true');
        return elemento;
    }

    function contadorNaBarra() {
        const area = document.querySelector('.barra-app .container > .d-flex.align-items-center');
        if (!area) {
            return;
        }
        const contador = document.createElement('span');
        const rotulo = document.createElement('span');
        const valor = document.createElement('span');
        contador.className = 'mario-contador';
        contador.title = 'Moedas: ganhe uma a cada tarefa concluída';
        rotulo.className = 'visually-hidden';
        rotulo.textContent = 'Moedas';
        valor.dataset.marioMoedas = '';
        contador.append(rotulo, valor);
        area.prepend(contador);
    }

    /** Login, cadastro e erro (telas sem a barra de navegação) ganham HUD, blocos e inimigo. */
    function montarTelaDeAcesso() {
        const cartao = document.querySelector('.cartao-acesso');
        const principal = cartao?.closest('main');
        if (!principal || document.querySelector('.barra-app')) {
            return;
        }
        const ehErro = cartao.classList.contains('text-center');
        const ehCadastro = document.querySelector('form[action$="/cadastro"]') !== null;

        const hud = document.createElement('div');
        hud.className = 'mario-hud';
        hud.setAttribute('aria-hidden', 'true');
        const coluna = (titulo, valor) => {
            const caixa = document.createElement('div');
            const nome = document.createElement('span');
            const conteudo = document.createElement('span');
            nome.textContent = titulo;
            conteudo.textContent = valor;
            caixa.append(nome, conteudo);
            hud.append(caixa);
            return conteudo;
        };
        coluna('Pontos', '').dataset.marioPontos = '';
        coluna('Moedas', '').dataset.marioMoedas = '';
        coluna('Mundo', ehErro ? '?-?' : ehCadastro ? '1-2' : '1-1');
        const tempo = coluna('Tempo', '400');
        principal.before(hud);

        if (!reduzMovimento) {
            let restante = 400;
            const relogio = window.setInterval(() => {
                restante -= 1;
                tempo.textContent = String(restante).padStart(3, '0');
                tempo.classList.toggle('pressa', restante <= 100);
                if (restante <= 0) {
                    window.clearInterval(relogio);
                }
            }, 400);
        }

        const fileira = document.createElement('div');
        const bloco = document.createElement('button');
        fileira.className = 'mario-fileira';
        bloco.type = 'button';
        bloco.className = 'mario-bloco-surpresa';
        bloco.setAttribute('aria-label', 'Bloco surpresa: pegar uma moeda');
        bloco.addEventListener('click', () => {
            bloco.classList.remove('batendo');
            void bloco.offsetWidth; // reinicia a animação a cada clique
            bloco.classList.add('batendo');
            SONS.moeda();
            ganharMoeda(bloco);
        });
        fileira.append(criarEnfeite('tijolo'), bloco, criarEnfeite('tijolo'), criarEnfeite('tijolo'));
        principal.prepend(fileira);

        if (!reduzMovimento) {
            document.body.append(criarEnfeite('mario-cano-decorativo'), criarEnfeite('mario-personagem inimigo'));
        }
    }

    function colocarEncanador() {
        if (!reduzMovimento) {
            document.body.append(criarEnfeite('mario-personagem encanador'));
        }
    }

    criarBotaoSom();
    contadorNaBarra();
    montarTelaDeAcesso();
    colocarEncanador();
    atualizarContadores();
})();
