/*
  Tema Minecraft: sons gerados na hora (Web Audio, sem arquivos de áudio), experiência (XP), hotbar,
  bloco para minerar e personagens. Tudo aqui é enfeite: sem este script o sistema funciona igual.
*/
(() => {
    'use strict';

    const CHAVE_SOM = 'minecraft.som';
    const CHAVE_XP = 'minecraft.xp';
    const MAXIMO_XP = 99999;
    const XP_POR_TAREFA = 5;
    const XP_POR_MINERIO = 3;
    const GOLPES_PARA_QUEBRAR = 5;
    // Tempo para o som começar antes de a página trocar (senão a navegação corta o som).
    const ATRASO_NAVEGACAO_MS = 180;
    const reduzMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const FRASES = [
        'Agora com tarefas!',
        'Minere suas metas!',
        '100% sem procrastinação!',
        'Feito de blocos!',
        'Não esqueça o prazo!',
        'Também tente o tema Mario!',
        'Construído com Spring!',
        'Uma tarefa por vez!',
        'Cuidado com o creeper!',
        'Diamantes à vista!',
    ];

    const ITENS = [
        { nome: 'Bloco de grama', imagem: 'bloco-grama.svg' },
        { nome: 'Terra', imagem: 'bloco-terra.svg' },
        { nome: 'Pedra', imagem: 'bloco-pedra.svg' },
        { nome: 'Tábuas de carvalho', imagem: 'bloco-tabuas.svg' },
        { nome: 'Tronco de carvalho', imagem: 'bloco-tronco.svg' },
        { nome: 'Folhas de carvalho', imagem: 'bloco-folhas.svg' },
        { nome: 'Minério de diamante', imagem: 'bloco-diamante.svg' },
        { nome: 'Picareta de diamante', imagem: 'picareta.png' },
        { nome: 'Papoula', imagem: 'papoula.png' },
    ];

    // As imagens ficam ao lado deste script; a URL dele já traz o caminho de contexto da aplicação.
    const pastaImagens = new URL('img/', document.currentScript.src).href;

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
    let ruidoBranco = null;

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
    function tocar(notas, onda = 'square', volume = 0.06) {
        const ctx = somLigado() ? contextoAudio() : null;
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

    /** Ruído filtrado: passos, golpes de picareta, blocos quebrando e explosões. */
    function chiado(duracao, filtro, frequencia, frequenciaFinal, volume) {
        const ctx = somLigado() ? contextoAudio() : null;
        if (!ctx) {
            return;
        }
        if (!ruidoBranco) {
            ruidoBranco = ctx.createBuffer(1, ctx.sampleRate, ctx.sampleRate);
            const dados = ruidoBranco.getChannelData(0);
            for (let i = 0; i < dados.length; i += 1) {
                dados[i] = Math.random() * 2 - 1;
            }
        }
        const inicio = ctx.currentTime + 0.01;
        const fonte = ctx.createBufferSource();
        const passaFaixa = ctx.createBiquadFilter();
        const ganho = ctx.createGain();
        fonte.buffer = ruidoBranco;
        passaFaixa.type = filtro;
        passaFaixa.frequency.setValueAtTime(frequencia, inicio);
        passaFaixa.frequency.exponentialRampToValueAtTime(frequenciaFinal, inicio + duracao);
        ganho.gain.setValueAtTime(volume, inicio);
        ganho.gain.exponentialRampToValueAtTime(0.0001, inicio + duracao);
        fonte.connect(passaFaixa).connect(ganho).connect(ctx.destination);
        fonte.start(inicio);
        fonte.stop(inicio + duracao + 0.02);
    }

    const SONS = {
        clique: () => tocar([[1500, 0.025], [950, 0.035]], 'square', 0.035),
        passo: () => chiado(0.09, 'lowpass', 900, 300, 0.35),
        xp: () => {
            const nota = 1300 + Math.random() * 700;
            tocar([[nota, 0.18]], 'sine', 0.13);
            tocar([[nota * 2, 0.1]], 'triangle', 0.025);
        },
        nivel: () => {
            tocar([[523, 0.08], [659, 0.08], [784, 0.08], [1047, 0.4]], 'triangle', 0.13);
            tocar([[1047, 0.24], [1568, 0.4]], 'sine', 0.04);
        },
        golpe: () => {
            chiado(0.06, 'bandpass', 2200, 900, 0.5);
            tocar([[320, 0.04, 200]], 'square', 0.025);
        },
        quebrar: () => {
            chiado(0.25, 'lowpass', 2600, 350, 0.55);
            tocar([[190, 0.12, 80]], 'triangle', 0.18);
        },
        sibilo: () => chiado(0.35, 'highpass', 2500, 5000, 0.12),
        explosao: () => {
            chiado(0.9, 'lowpass', 1800, 120, 0.7);
            tocar([[90, 0.5, 35]], 'sine', 0.35);
        },
        porta: () => tocar([[150, 0.06, 110], [120, 0.09, 80]], 'square', 0.06),
    };

    function somDoBotao(elemento) {
        if (!elemento) {
            return 'clique';
        }
        if (elemento.matches('.mundo')) {
            return 'nivel';
        }
        if (elemento.matches('.btn-outline-success')) {
            return 'xp';
        }
        if (elemento.matches('.btn-danger')) {
            return 'explosao';
        }
        if (elemento.matches('.btn-outline-danger')) {
            return 'sibilo';
        }
        if (elemento.matches('.btn-outline-light')) {
            return 'porta';
        }
        return 'clique';
    }

    // ---------- Experiência ----------

    function xp() {
        const valor = Number.parseInt(ler(CHAVE_XP) ?? '0', 10);
        return Number.isInteger(valor) && valor > 0 ? Math.min(valor, MAXIMO_XP) : 0;
    }

    /** Como no jogo, cada nível pede um pouco mais de XP que o anterior (7, 9, 11...). */
    function nivelDe(total) {
        let nivel = 0;
        let resto = total;
        while (resto >= 2 * nivel + 7) {
            resto -= 2 * nivel + 7;
            nivel += 1;
        }
        return { nivel, progresso: resto / (2 * nivel + 7) };
    }

    function atualizarBarrasDeXp() {
        const { nivel, progresso } = nivelDe(xp());
        document.querySelectorAll('[data-mc-xp]').forEach((barra) => {
            barra.style.width = `${Math.round(progresso * 100)}%`;
        });
        document.querySelectorAll('[data-mc-nivel]').forEach((elemento) => {
            elemento.textContent = String(nivel);
        });
    }

    function soltarOrbes(origem, quantidade) {
        if (reduzMovimento || !origem) {
            return;
        }
        const area = origem.getBoundingClientRect();
        for (let i = 0; i < quantidade; i += 1) {
            const orbe = document.createElement('span');
            orbe.className = 'mc-orbe-voando';
            orbe.style.left = `${area.left + window.scrollX + area.width / 2}px`;
            orbe.style.top = `${area.top + window.scrollY - 10}px`;
            orbe.style.setProperty('--dx', `${(i - (quantidade - 1) / 2) * 22}px`);
            orbe.style.animationDelay = `${i * 70}ms`;
            orbe.addEventListener('animationend', () => orbe.remove());
            document.body.append(orbe);
        }
    }

    /** Devolve true quando o XP ganho fez subir de nível. */
    function ganharXp(quantidade, origem) {
        const antes = nivelDe(xp()).nivel;
        gravar(CHAVE_XP, String(Math.min(xp() + quantidade, MAXIMO_XP)));
        atualizarBarrasDeXp();
        soltarOrbes(origem, 3);
        return nivelDe(xp()).nivel > antes;
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
        SONS[link.classList.contains('btn') ? somDoBotao(link) : 'passo']();
        window.setTimeout(() => window.location.assign(link.href), ATRASO_NAVEGACAO_MS);
    });

    document.addEventListener('submit', (evento) => {
        const formulario = evento.target;
        // "liberado": é o reenvio feito por este script depois do som; segue normalmente.
        if (evento.defaultPrevented || formulario.dataset.mcEnvio === 'liberado') {
            return;
        }
        // Clique repetido enquanto o som toca: ignora, para não enviar duas vezes.
        if (formulario.dataset.mcEnvio === 'aguardando') {
            evento.preventDefault();
            return;
        }
        const botao = evento.submitter ?? null;
        let som = somDoBotao(botao);
        if (botao?.matches('.btn-outline-success') && ganharXp(XP_POR_TAREFA, botao)) {
            som = 'nivel';
        }
        if (!somLigado()) {
            return;
        }
        evento.preventDefault();
        formulario.dataset.mcEnvio = 'aguardando';
        SONS[som]();
        window.setTimeout(() => {
            formulario.dataset.mcEnvio = 'liberado';
            formulario.requestSubmit(botao ?? undefined);
        }, ATRASO_NAVEGACAO_MS);
    });

    // Voltar pelo histórico pode restaurar a página com o formulário marcado; libera de novo.
    window.addEventListener('pageshow', () => {
        document.querySelectorAll('form[data-mc-envio]').forEach((formulario) => {
            delete formulario.dataset.mcEnvio;
        });
    });

    const digitando = (alvo) => alvo instanceof Element && alvo.closest('input, textarea, select, [contenteditable]');

    // ---------- Botão de som ----------

    function criarBotaoSom() {
        const botao = document.createElement('button');
        const icone = document.createElement('i');
        const texto = document.createElement('span');
        botao.type = 'button';
        botao.className = 'mc-som';
        botao.title = 'Ligar ou desligar os sons (tecla M)';
        icone.setAttribute('aria-hidden', 'true');
        botao.append(icone, texto);

        const atualizar = () => {
            const ligado = somLigado();
            icone.className = ligado ? 'bi bi-volume-up-fill' : 'bi bi-volume-mute-fill';
            texto.textContent = ligado ? 'Som: ligado' : 'Som: desligado';
            // No celular o texto some (só fica o ícone); o rótulo continua para leitores de tela.
            botao.setAttribute('aria-label', texto.textContent);
        };
        botao.addEventListener('click', () => {
            gravar(CHAVE_SOM, somLigado() ? 'desligado' : 'ligado');
            atualizar();
            SONS.clique();
        });
        document.addEventListener('keydown', (evento) => {
            if (evento.key?.toLowerCase() === 'm' && !digitando(evento.target)
                && !evento.ctrlKey && !evento.metaKey && !evento.altKey) {
                botao.click();
            }
        });
        atualizar();
        document.body.append(botao);
    }

    // ---------- Cenário ----------

    function criarEnfeite(classe, tag = 'span') {
        const elemento = document.createElement(tag);
        elemento.className = classe;
        elemento.setAttribute('aria-hidden', 'true');
        return elemento;
    }

    function criarBarraDeXp() {
        const caixa = criarEnfeite('mc-xp', 'div');
        const nivel = criarEnfeite('mc-nivel');
        const barra = criarEnfeite('mc-xp-barra');
        const preenchido = criarEnfeite('mc-xp-preenchido');
        caixa.title = 'Nível de experiência: ganhe XP concluindo tarefas';
        nivel.dataset.mcNivel = '';
        preenchido.dataset.mcXp = '';
        barra.append(preenchido);
        caixa.append(nivel, barra);
        return caixa;
    }

    function barraDeXpNoMenu() {
        document.querySelector('.barra-app')?.after(criarBarraDeXp());
    }

    function criarHotbar() {
        const hud = criarEnfeite('mc-hud', 'div');
        const nomeDoItem = criarEnfeite('mc-item-nome');
        const hotbar = criarEnfeite('mc-hotbar', 'div');
        let esconderNome = 0;

        const slots = ITENS.map((item) => {
            const slot = document.createElement('button');
            slot.type = 'button';
            slot.className = 'mc-slot';
            slot.tabIndex = -1;
            slot.style.backgroundImage = `url("${pastaImagens}${item.imagem}")`;
            hotbar.append(slot);
            return slot;
        });

        const selecionar = (indice) => {
            slots.forEach((slot, i) => slot.classList.toggle('selecionado', i === indice));
            nomeDoItem.textContent = ITENS[indice].nome;
            nomeDoItem.classList.add('visivel');
            window.clearTimeout(esconderNome);
            esconderNome = window.setTimeout(() => nomeDoItem.classList.remove('visivel'), 1500);
        };
        slots.forEach((slot, i) => slot.addEventListener('click', () => {
            selecionar(i);
            SONS.clique();
        }));
        // Teclas 1 a 9 trocam o item, como no jogo (menos quando o foco está num campo).
        document.addEventListener('keydown', (evento) => {
            const numero = Number.parseInt(evento.key, 10);
            if (numero >= 1 && numero <= ITENS.length && !digitando(evento.target)
                && !evento.ctrlKey && !evento.metaKey && !evento.altKey) {
                selecionar(numero - 1);
                SONS.clique();
            }
        });
        slots[0].classList.add('selecionado');

        hud.append(nomeDoItem, criarBarraDeXp(), hotbar);
        document.body.append(hud);
    }

    function soltarPedacinhos(origem) {
        if (reduzMovimento) {
            return;
        }
        const area = origem.getBoundingClientRect();
        const cores = ['#7f7f7f', '#9c9c9c', '#606060', '#4be0e6', '#d5fffa'];
        for (let i = 0; i < 8; i += 1) {
            const pedaco = document.createElement('span');
            pedaco.className = 'mc-particula';
            pedaco.style.background = cores[i % cores.length];
            pedaco.style.left = `${area.left + window.scrollX + area.width / 2 - 4}px`;
            pedaco.style.top = `${area.top + window.scrollY + area.height / 2 - 4}px`;
            pedaco.style.setProperty('--dx', `${(Math.random() - 0.5) * 90}px`);
            pedaco.style.setProperty('--dy', `${-20 - Math.random() * 40}px`);
            pedaco.addEventListener('animationend', () => pedaco.remove());
            document.body.append(pedaco);
        }
    }

    function criarBlocoParaMinerar() {
        const bloco = document.createElement('button');
        let golpes = 0;
        bloco.type = 'button';
        bloco.className = 'mc-bloco-minerar';
        bloco.title = 'Minério de diamante: clique para minerar';
        bloco.setAttribute('aria-label', 'Minério de diamante: clique para minerar e ganhar XP');
        bloco.addEventListener('click', () => {
            if (bloco.classList.contains('quebrado')) {
                return;
            }
            golpes += 1;
            if (golpes < GOLPES_PARA_QUEBRAR) {
                bloco.style.setProperty('--estagio', String(golpes));
                bloco.classList.remove('batendo');
                void bloco.offsetWidth; // reinicia a animação a cada golpe
                bloco.classList.add('batendo');
                SONS.golpe();
                return;
            }
            golpes = 0;
            bloco.style.setProperty('--estagio', '0');
            soltarPedacinhos(bloco);
            SONS.quebrar();
            if (ganharXp(XP_POR_MINERIO, bloco)) {
                window.setTimeout(SONS.nivel, 200);
            } else {
                window.setTimeout(SONS.xp, 200);
            }
            bloco.classList.add('quebrado');
            // O minério "nasce" de novo para poder minerar outra vez.
            window.setTimeout(() => bloco.classList.remove('quebrado'), 1500);
        });
        document.body.append(bloco);
    }

    /** Login, cadastro e erro (telas sem a barra de navegação) viram a tela de título do jogo. */
    function montarTelaDeAcesso() {
        const cartao = document.querySelector('.cartao-acesso');
        if (!cartao || document.querySelector('.barra-app')) {
            return;
        }
        const ehErro = cartao.classList.contains('text-center');
        if (ehErro) {
            const pontuacao = criarEnfeite('mc-pontuacao', 'p');
            const valor = document.createElement('span');
            valor.textContent = String(xp());
            pontuacao.append('Pontuação: ', valor);
            cartao.querySelector('p')?.after(pontuacao);
        } else {
            const frase = criarEnfeite('mc-frase');
            frase.textContent = FRASES[Math.floor(Math.random() * FRASES.length)];
            cartao.querySelector(':scope > .text-center.mb-4')?.append(frase);
        }
        criarHotbar();
        criarBlocoParaMinerar();
        if (!reduzMovimento) {
            document.body.append(criarEnfeite('mc-personagem criatura'));
        }
    }

    function colocarMinerador() {
        if (!reduzMovimento) {
            document.body.append(criarEnfeite('mc-personagem minerador'));
        }
    }

    criarBotaoSom();
    barraDeXpNoMenu();
    montarTelaDeAcesso();
    colocarMinerador();
    atualizarBarrasDeXp();
})();
