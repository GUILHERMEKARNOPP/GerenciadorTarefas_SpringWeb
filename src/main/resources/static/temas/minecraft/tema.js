/*
  Tema Minecraft: sons do jogo (sons/*.ogg, tocados pela Web Audio com o volume e o tom que o jogo usa em cada
  evento), experiência (XP), hotbar, panorama da tela de título e um minério para quebrar.
  Tudo aqui é enfeite: sem este script o sistema funciona igual.
*/
(() => {
    'use strict';

    const CHAVE_SOM = 'minecraft.som';
    const CHAVE_XP = 'minecraft.xp';
    const MAXIMO_XP = 99999;
    const XP_POR_TAREFA = 5;
    const XP_POR_MINERIO = 3;
    const GOLPES_PARA_QUEBRAR = 6;
    const ESTAGIOS_DE_RACHADURA = 10; // destroy_stage_0 a 9
    // Tempo para o som começar antes de a página trocar (senão a navegação corta o som).
    const ATRASO_NAVEGACAO_MS = 180;
    // Os volumes abaixo são os do jogo (0 a 1); este ganho só compensa o volume geral do navegador.
    const GANHO = 2;
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

    // Nomes oficiais em português (lang/pt_br.json do jogo).
    const ITENS = [
        { nome: 'Bloco de Grama', imagem: 'bloco-grama.png' },
        { nome: 'Terra', imagem: 'bloco-terra.png' },
        { nome: 'Pedra', imagem: 'bloco-pedra.png' },
        { nome: 'Tábuas de Carvalho', imagem: 'bloco-tabuas.png' },
        { nome: 'Tronco de Carvalho', imagem: 'bloco-tronco.png' },
        { nome: 'Folhas de Carvalho', imagem: 'bloco-folhas.png' },
        { nome: 'Minério de Diamante', imagem: 'bloco-diamante.png' },
        { nome: 'Picareta de Diamante', imagem: 'picareta-diamante.png' },
        { nome: 'Papoula', imagem: 'papoula.png' },
    ];

    // As imagens e os sons ficam ao lado deste script; a URL dele já traz o caminho de contexto da aplicação.
    const pastaImagens = new URL('img/', document.currentScript.src).href;
    const pastaSons = new URL('sons/', document.currentScript.src).href;

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

    const sorteio = () => Math.random() - Math.random();

    /** Eventos de som do jogo (sounds.json): arquivos, volume e tom. */
    const EVENTOS = {
        // ui.button.click: SimpleSoundInstance.forUI, volume 0.25
        clique: { arquivos: ['clique'], volume: 0.25, tom: () => 1 },
        // block.grass.step: SoundType.GRASS, passos com volume 0.15
        passo: { arquivos: ['passo-1', 'passo-2', 'passo-3', 'passo-4', 'passo-5', 'passo-6'], volume: 0.15, tom: () => 1 },
        // entity.experience_orb.pickup: volume 0.1, tom (r1 - r2) * 0.35 + 0.9
        xp: { arquivos: ['orbe'], volume: 0.1, tom: () => sorteio() * 0.35 + 0.9 },
        // entity.player.levelup: volume 0.75
        nivel: { arquivos: ['nivel'], volume: 0.75, tom: () => 1 },
        // block.stone.hit: volume (1 + 1) / 8, tom 0.5
        golpe: { arquivos: ['golpe-1', 'golpe-2', 'golpe-3', 'golpe-4', 'golpe-5', 'golpe-6'], volume: 0.25, tom: () => 0.5 },
        // block.stone.break: volume (1 + 1) / 2, tom 0.8
        quebrar: { arquivos: ['quebrar-1', 'quebrar-2', 'quebrar-3', 'quebrar-4'], volume: 1, tom: () => 0.8 },
        // entity.creeper.primed: o pavio aceso do creeper, tom 0.5
        sibilo: { arquivos: ['pavio'], volume: 1, tom: () => 0.5 },
        // entity.generic.explode: tom (1 + (r1 - r2) * 0.2) * 0.7
        explosao: { arquivos: ['explosao-1', 'explosao-2', 'explosao-3', 'explosao-4'], volume: 1, tom: () => (1 + sorteio() * 0.2) * 0.7 },
    };

    let audio = null;
    const buffers = new Map();

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

    /** Baixa e decodifica um arquivo uma vez só; falha (ex.: navegador sem Ogg Vorbis) só deixa sem som. */
    function carregar(arquivo) {
        if (!buffers.has(arquivo)) {
            const ctx = contextoAudio();
            const promessa = ctx
                ? fetch(`${pastaSons}${arquivo}.ogg`)
                    .then((resposta) => (resposta.ok ? resposta.arrayBuffer() : Promise.reject(new Error(resposta.statusText))))
                    .then((dados) => ctx.decodeAudioData(dados))
                    .catch(() => null)
                : Promise.resolve(null);
            buffers.set(arquivo, promessa);
        }
        return buffers.get(arquivo);
    }

    function tocar(nome) {
        if (!somLigado()) {
            return;
        }
        const evento = EVENTOS[nome];
        const arquivo = evento.arquivos[Math.floor(Math.random() * evento.arquivos.length)];
        carregar(arquivo).then((buffer) => {
            const ctx = contextoAudio();
            if (!buffer || !ctx) {
                return;
            }
            const fonte = ctx.createBufferSource();
            const ganho = ctx.createGain();
            fonte.buffer = buffer;
            // No jogo o tom muda a velocidade junto, igual ao playbackRate.
            fonte.playbackRate.value = evento.tom();
            ganho.gain.value = Math.min(1, evento.volume * GANHO);
            fonte.connect(ganho).connect(ctx.destination);
            fonte.start();
        });
    }

    // O navegador só libera o áudio depois de um gesto; nesse primeiro gesto já deixa os sons prontos.
    function prepararSons() {
        if (somLigado()) {
            Object.values(EVENTOS).forEach((evento) => evento.arquivos.forEach(carregar));
        }
    }
    document.addEventListener('pointerdown', prepararSons, { once: true });
    document.addEventListener('keydown', prepararSons, { once: true });

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
            // O jogo recorta o sprite em pixels inteiros: 182 * progresso.
            barra.style.width = `${Math.floor(progresso * 182) * 2}px`;
        });
        document.querySelectorAll('[data-mc-nivel]').forEach((elemento) => {
            elemento.textContent = String(nivel);
            // Nível 0 não aparece no jogo.
            elemento.hidden = nivel === 0;
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
        tocar(link.classList.contains('btn') || link.classList.contains('nav-link') ? somDoBotao(link) : 'passo');
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
        tocar(som);
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
            // Como os botões de opção do jogo: "Nome: Sim/Não" (options.on/off em pt_br).
            texto.textContent = ligado ? 'Sons: Sim' : 'Sons: Não';
            // No celular o texto some (só fica o ícone); o rótulo continua para leitores de tela.
            botao.setAttribute('aria-label', ligado ? 'Sons ligados' : 'Sons desligados');
        };
        botao.addEventListener('click', () => {
            gravar(CHAVE_SOM, somLigado() ? 'desligado' : 'ligado');
            atualizar();
            prepararSons();
            tocar('clique');
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
        const selecao = criarEnfeite('mc-selecao');
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
        hotbar.append(selecao);

        const selecionar = (indice) => {
            selecao.style.setProperty('--mc-indice', String(indice));
            nomeDoItem.textContent = ITENS[indice].nome;
            nomeDoItem.classList.add('visivel');
            window.clearTimeout(esconderNome);
            esconderNome = window.setTimeout(() => nomeDoItem.classList.remove('visivel'), 1500);
        };
        slots.forEach((slot, i) => slot.addEventListener('click', () => {
            selecionar(i);
            tocar('clique');
        }));
        // Teclas 1 a 9 trocam o item, como no jogo (menos quando o foco está num campo).
        document.addEventListener('keydown', (evento) => {
            const numero = Number.parseInt(evento.key, 10);
            if (numero >= 1 && numero <= ITENS.length && !digitando(evento.target)
                && !evento.ctrlKey && !evento.metaKey && !evento.altKey) {
                selecionar(numero - 1);
                tocar('clique');
            }
        });

        hud.append(nomeDoItem, criarBarraDeXp(), hotbar);
        document.body.append(hud);
    }

    /** Pedacinhos do próprio bloco voando, como as partículas de quebra do jogo. */
    function soltarPedacinhos(origem) {
        if (reduzMovimento) {
            return;
        }
        const area = origem.getBoundingClientRect();
        for (let i = 0; i < 10; i += 1) {
            const pedaco = document.createElement('span');
            pedaco.className = 'mc-particula';
            pedaco.style.left = `${area.left + window.scrollX + area.width / 2 - 4}px`;
            pedaco.style.top = `${area.top + window.scrollY + area.height / 2 - 4}px`;
            pedaco.style.setProperty('--px', `${-Math.floor(40 + Math.random() * 170)}px`);
            pedaco.style.setProperty('--py', `${-Math.floor(40 + Math.random() * 170)}px`);
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
        bloco.title = 'Minério de Diamante: clique para minerar';
        bloco.setAttribute('aria-label', 'Minério de Diamante: clique para minerar e ganhar XP');
        bloco.addEventListener('click', () => {
            if (bloco.classList.contains('quebrado')) {
                return;
            }
            golpes += 1;
            if (golpes < GOLPES_PARA_QUEBRAR) {
                // O quadro 0 é o bloco inteiro; os quadros 1 a 10 são as rachaduras.
                bloco.style.setProperty('--estagio', String(Math.round(golpes * ESTAGIOS_DE_RACHADURA / GOLPES_PARA_QUEBRAR)));
                tocar('golpe');
                return;
            }
            golpes = 0;
            bloco.style.setProperty('--estagio', '0');
            soltarPedacinhos(bloco);
            tocar('quebrar');
            const subiu = ganharXp(XP_POR_MINERIO, bloco);
            window.setTimeout(() => tocar(subiu ? 'nivel' : 'xp'), 200);
            bloco.classList.add('quebrado');
            // O minério "nasce" de novo para poder minerar outra vez.
            window.setTimeout(() => bloco.classList.remove('quebrado'), 1500);
        });
        document.body.append(bloco);
    }

    /** Cubo com as 6 faces do panorama; o giro e a câmera ficam no tema.css. */
    function criarPanorama() {
        const panorama = criarEnfeite('mc-panorama', 'div');
        const cubo = criarEnfeite('mc-cubo', 'div');
        for (let i = 0; i < 6; i += 1) {
            cubo.append(criarEnfeite('mc-face'));
        }
        panorama.append(cubo);
        document.body.prepend(panorama);
        document.body.classList.add('mc-titulo');
    }

    /** A frase amarela: o tamanho depende do comprimento do texto, como no SplashRenderer. */
    function criarFrase(cabecalho) {
        const frase = criarEnfeite('mc-frase');
        frase.textContent = FRASES[Math.floor(Math.random() * FRASES.length)];
        cabecalho.append(frase);
        const ajustar = () => {
            // offsetWidth ignora o transform; 2px por pixel do jogo.
            const larguraNoJogo = frase.offsetWidth / 2;
            frase.style.setProperty('--mc-escala', String(180 / (larguraNoJogo + 32)));
        };
        ajustar();
        document.fonts?.ready.then(ajustar);
    }

    function criarRodape() {
        const versao = criarEnfeite('mc-rodape', 'p');
        const aviso = criarEnfeite('mc-rodape direita', 'p');
        versao.textContent = 'Gerenciador de Tarefas';
        aviso.textContent = 'Arte do jogo © Mojang. Tema de fã.';
        document.body.append(versao, aviso);
    }

    /** Login e cadastro viram a tela de título; a página de erro vira a tela "Você morreu!". */
    function montarTelaDeAcesso() {
        const cartao = document.querySelector('.cartao-acesso');
        if (!cartao || document.querySelector('.barra-app')) {
            return;
        }
        document.body.classList.add('mc-acesso');
        if (cartao.classList.contains('text-center')) {
            const pontuacao = criarEnfeite('mc-pontuacao', 'p');
            const valor = document.createElement('span');
            valor.textContent = String(xp());
            // deathScreen.score.value em pt_br: "Pontuação: %s", com o número em amarelo.
            pontuacao.append('Pontuação: ', valor);
            cartao.querySelector('p')?.after(pontuacao);
            return;
        }
        criarPanorama();
        const cabecalho = cartao.querySelector(':scope > .text-center.mb-4');
        if (cabecalho) {
            criarFrase(cabecalho);
        }
        criarRodape();
        criarHotbar();
        criarBlocoParaMinerar();
    }

    criarBotaoSom();
    barraDeXpNoMenu();
    montarTelaDeAcesso();
    atualizarBarrasDeXp();
})();
