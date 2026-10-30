/*
  Tema Counter-Strike 2: sons gerados na hora (Web Audio, sem arquivos de áudio), dinheiro da partida, HUD com o
  tempo da rodada, killfeed ao concluir tarefas, bomba para desarmar nas telas de acesso e o operador patrulhando.
  Tudo aqui é enfeite: sem este script o sistema funciona igual.
*/
(() => {
    'use strict';

    const CHAVE_SOM = 'cs2.som';
    const CHAVE_DINHEIRO = 'cs2.dinheiro';
    // O killfeed aparece na página seguinte, porque concluir a tarefa recarrega a lista.
    const CHAVE_ABATE = 'cs2.abate';
    // Valores da economia do jogo: começa com $800, o teto é $16.000 e um abate/desarme rende $300.
    const DINHEIRO_INICIAL = 800;
    const DINHEIRO_MAXIMO = 16000;
    const DINHEIRO_POR_TAREFA = 300;
    const DINHEIRO_POR_DESARME = 300;
    // Com o kit, o desarme leva 5 segundos: cada clique vale 1 segundo.
    const CLIQUES_PARA_DESARMAR = 5;
    const TAMANHO_MAXIMO_ALVO = 60;
    const DURACAO_ABATE_MS = 6000;
    const DURACAO_AVISO_MS = 2000;
    // Tempo para o som começar antes de a página trocar (senão a navegação corta o som).
    const ATRASO_NAVEGACAO_MS = 180;
    const reduzMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const formatoDinheiro = new Intl.NumberFormat('pt-BR');

    const RADIO = [
        'Vamos, vamos, vamos!',
        'Setor limpo: nenhuma tarefa atrasada.',
        'Precisamos de reforço: tarefas acumulando!',
        'Eco nesta rodada: só as tarefas essenciais.',
        'Compre colete antes da AWP.',
        'Fumaça na janela, tarefa na mão.',
        'Mire na cabeça: conclua de primeira.',
        'Rush B? Só depois de terminar a lista.',
        'Também tente o tema Stardew Valley!',
        'Segurando o bomb A com o prazo de hoje.',
    ];

    // ---------- Preferências do navegador (podem estar bloqueadas, ex.: aba anônima restrita) ----------

    function ler(armazenamento, chave) {
        try {
            return window[armazenamento].getItem(chave);
        } catch {
            return null;
        }
    }

    function gravar(armazenamento, chave, valor) {
        try {
            window[armazenamento].setItem(chave, valor);
        } catch {
            // Sem armazenamento a preferência só não fica salva.
        }
    }

    function apagar(armazenamento, chave) {
        try {
            window[armazenamento].removeItem(chave);
        } catch {
            // Nada a apagar se o armazenamento está bloqueado.
        }
    }

    const somLigado = () => ler('localStorage', CHAVE_SOM) !== 'desligado';

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
    function tocar(notas, onda = 'triangle', volume = 0.08, atraso = 0) {
        const ctx = somLigado() ? contextoAudio() : null;
        if (!ctx) {
            return;
        }
        let inicio = ctx.currentTime + 0.01 + atraso;
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

    /** Ruído filtrado: passos, rádio, explosão e o "clique" metálico das ferramentas. */
    function chiado(duracao, filtro, frequencia, frequenciaFinal, volume, atraso = 0) {
        const ctx = somLigado() ? contextoAudio() : null;
        if (!ctx) {
            return;
        }
        if (!ruidoBranco) {
            ruidoBranco = ctx.createBuffer(1, ctx.sampleRate * 2, ctx.sampleRate);
            const dados = ruidoBranco.getChannelData(0);
            for (let i = 0; i < dados.length; i += 1) {
                dados[i] = Math.random() * 2 - 1;
            }
        }
        const inicio = ctx.currentTime + 0.01 + atraso;
        const fonte = ctx.createBufferSource();
        const filtroDeFrequencia = ctx.createBiquadFilter();
        const ganho = ctx.createGain();
        fonte.buffer = ruidoBranco;
        filtroDeFrequencia.type = filtro;
        filtroDeFrequencia.frequency.setValueAtTime(frequencia, inicio);
        filtroDeFrequencia.frequency.exponentialRampToValueAtTime(frequenciaFinal, inicio + duracao);
        ganho.gain.setValueAtTime(volume, inicio);
        ganho.gain.exponentialRampToValueAtTime(0.0001, inicio + duracao);
        fonte.connect(filtroDeFrequencia).connect(ganho).connect(ctx.destination);
        fonte.start(inicio);
        fonte.stop(inicio + duracao + 0.02);
    }

    const SONS = {
        clique: () => tocar([[1400, 0.025], [900, 0.03]], 'square', 0.03),
        passo: () => {
            chiado(0.07, 'lowpass', 900, 200, 0.4);
            chiado(0.06, 'lowpass', 700, 180, 0.3, 0.12);
        },
        // Compra no menu: um "tchim" metálico.
        dinheiro: () => {
            chiado(0.05, 'highpass', 4000, 6000, 0.15);
            tocar([[2637, 0.05], [3520, 0.18]], 'square', 0.025, 0.03);
        },
        // Abate na cabeça: um "tink" agudo e curto.
        abate: () => {
            tocar([[3200, 0.18, 2900]], 'sine', 0.12);
            tocar([[4800, 0.12, 4400]], 'sine', 0.04);
        },
        bipe: () => tocar([[2900, 0.07]], 'square', 0.035),
        desarmar: () => {
            chiado(0.04, 'bandpass', 3000, 2500, 0.3);
            tocar([[1800, 0.04, 1200]], 'square', 0.03);
        },
        desarmada: () => tocar([[880, 0.1], [1175, 0.1], [1760, 0.28]], 'triangle', 0.1),
        // Excluir é jogar uma granada: estrondo grave que vai abafando.
        granada: () => {
            chiado(0.6, 'lowpass', 1600, 60, 0.7);
            tocar([[110, 0.4, 40]], 'sine', 0.25);
        },
        // Rádio: chiado curto de transmissão e dois bipes, como no início da rodada.
        radio: () => {
            chiado(0.12, 'bandpass', 2200, 1800, 0.25);
            tocar([[1200, 0.06]], 'square', 0.03, 0.1);
            tocar([[1600, 0.08]], 'square', 0.03, 0.2);
        },
        sair: () => {
            chiado(0.1, 'bandpass', 1800, 900, 0.25);
            tocar([[600, 0.08, 300]], 'square', 0.03);
        },
    };

    function somDoBotao(elemento) {
        if (!elemento) {
            return 'clique';
        }
        if (elemento.matches('.mundo')) {
            return 'radio';
        }
        if (elemento.matches('.btn-outline-success')) {
            return 'abate';
        }
        if (elemento.matches('.btn-danger, .btn-outline-danger')) {
            return 'granada';
        }
        if (elemento.matches('.btn-outline-light')) {
            return 'sair';
        }
        if (elemento.matches('.btn-primary')) {
            return 'dinheiro';
        }
        return 'clique';
    }

    // ---------- Dinheiro ----------

    function dinheiro() {
        const guardado = ler('localStorage', CHAVE_DINHEIRO);
        if (guardado === null) {
            return DINHEIRO_INICIAL;
        }
        const valor = Number.parseInt(guardado, 10);
        return Number.isInteger(valor) && valor > 0 ? Math.min(valor, DINHEIRO_MAXIMO) : 0;
    }

    const textoDinheiro = (valor) => `$${formatoDinheiro.format(valor)}`;

    function atualizarDinheiro() {
        const texto = textoDinheiro(dinheiro());
        document.querySelectorAll('[data-cs-dinheiro]').forEach((elemento) => {
            elemento.textContent = texto;
        });
    }

    function mostrarGanho(origem, valor) {
        if (reduzMovimento || !origem) {
            return;
        }
        const area = origem.getBoundingClientRect();
        const ganho = document.createElement('span');
        ganho.className = 'cs-ganho';
        ganho.textContent = `+${textoDinheiro(valor)}`;
        ganho.style.left = `${area.left + window.scrollX + area.width / 2}px`;
        ganho.style.top = `${area.top + window.scrollY - 30}px`;
        ganho.addEventListener('animationend', () => ganho.remove());
        document.body.append(ganho);
    }

    function ganharDinheiro(quantidade, origem) {
        gravar('localStorage', CHAVE_DINHEIRO, String(Math.min(dinheiro() + quantidade, DINHEIRO_MAXIMO)));
        atualizarDinheiro();
        mostrarGanho(origem, quantidade);
        document.querySelectorAll('.cs-dinheiro').forEach((caixa) => {
            caixa.classList.remove('ganhou');
            void caixa.offsetWidth; // reinicia a animação
            caixa.classList.add('ganhou');
        });
    }

    // ---------- Killfeed ----------

    function criarEnfeite(classe, tag = 'span') {
        const elemento = document.createElement(tag);
        elemento.className = classe;
        elemento.setAttribute('aria-hidden', 'true');
        return elemento;
    }

    function encurtar(texto) {
        const limpo = texto.replace(/\s+/g, ' ').trim();
        return limpo.length > TAMANHO_MAXIMO_ALVO ? `${limpo.slice(0, TAMANHO_MAXIMO_ALVO - 1)}…` : limpo;
    }

    /** Guarda quem concluiu qual tarefa para mostrar no killfeed da próxima página. */
    function guardarAbate(botao) {
        const usuario = document.querySelector('.barra-app [href$="/perfil"] span')?.textContent ?? 'Você';
        const tarefa = botao.closest('tr')?.querySelector('.titulo-tarefa')?.textContent ?? 'tarefa';
        gravar('sessionStorage', CHAVE_ABATE, JSON.stringify({ atacante: encurtar(usuario), vitima: encurtar(tarefa) }));
    }

    function mostrarAbateGuardado() {
        const guardado = ler('sessionStorage', CHAVE_ABATE);
        if (!guardado) {
            return;
        }
        apagar('sessionStorage', CHAVE_ABATE);
        let abate;
        try {
            abate = JSON.parse(guardado);
        } catch {
            return;
        }
        // O valor vem do armazenamento do navegador: só aceita textos, e só entra na página via textContent.
        if (typeof abate?.atacante !== 'string' || typeof abate?.vitima !== 'string') {
            return;
        }
        const feed = criarEnfeite('cs-killfeed', 'div');
        const linha = criarEnfeite('cs-abate');
        const atacante = criarEnfeite('atacante');
        const icone = criarEnfeite('icone');
        const vitima = criarEnfeite('vitima');
        atacante.textContent = encurtar(abate.atacante);
        vitima.textContent = encurtar(abate.vitima);
        linha.append(atacante, icone, vitima);
        feed.append(linha);
        (document.querySelector('.cs-hud') ?? document.body).append(feed);
        window.setTimeout(() => feed.remove(), DURACAO_ABATE_MS);
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
        if (evento.defaultPrevented || formulario.dataset.csEnvio === 'liberado') {
            return;
        }
        // Clique repetido enquanto o som toca: ignora, para não enviar duas vezes.
        if (formulario.dataset.csEnvio === 'aguardando') {
            evento.preventDefault();
            return;
        }
        const botao = evento.submitter ?? null;
        // Concluir tarefa é um abate: rende dinheiro e entra no killfeed.
        if (botao?.matches('.btn-outline-success')) {
            ganharDinheiro(DINHEIRO_POR_TAREFA, botao);
            guardarAbate(botao);
        }
        if (!somLigado()) {
            return;
        }
        evento.preventDefault();
        formulario.dataset.csEnvio = 'aguardando';
        SONS[somDoBotao(botao)]();
        window.setTimeout(() => {
            formulario.dataset.csEnvio = 'liberado';
            formulario.requestSubmit(botao ?? undefined);
        }, ATRASO_NAVEGACAO_MS);
    });

    // Voltar pelo histórico pode restaurar a página com o formulário marcado; libera de novo.
    window.addEventListener('pageshow', () => {
        document.querySelectorAll('form[data-cs-envio]').forEach((formulario) => {
            delete formulario.dataset.csEnvio;
        });
    });

    const digitando = (alvo) => alvo instanceof Element && alvo.closest('input, textarea, select, [contenteditable]');

    // ---------- Botão de som ----------

    function criarBotaoSom() {
        const botao = document.createElement('button');
        const icone = document.createElement('i');
        const texto = document.createElement('span');
        botao.type = 'button';
        botao.className = 'cs-som';
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
            gravar('localStorage', CHAVE_SOM, somLigado() ? 'desligado' : 'ligado');
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

    // ---------- HUD: tempo da rodada e dinheiro ----------

    /** A rodada é o dia: o relógio mostra quanto falta para a meia-noite. */
    function tempoAteMeiaNoite(agora) {
        const meiaNoite = new Date(agora);
        meiaNoite.setHours(24, 0, 0, 0);
        const minutos = Math.max(0, Math.floor((meiaNoite - agora) / 60000));
        return { horas: Math.floor(minutos / 60), minutos: minutos % 60 };
    }

    function criarBloco(classe, rotulo, valor) {
        const bloco = criarEnfeite(classe);
        const titulo = criarEnfeite('cs-rotulo');
        titulo.textContent = rotulo;
        bloco.append(titulo, valor);
        return bloco;
    }

    function criarHud(flutuante) {
        const hud = criarEnfeite(flutuante ? 'cs-hud flutuante' : 'cs-hud container', 'div');
        const caixa = criarEnfeite('cs-caixa-hud');
        const tempo = criarEnfeite('cs-tempo');
        const valor = criarEnfeite('cs-valor');
        const relogio = criarBloco('cs-relogio', 'Rodada', tempo);
        const carteira = criarBloco('cs-dinheiro', 'Dinheiro', valor);
        relogio.title = 'Tempo até o fim do dia';
        carteira.title = 'Dinheiro: ganhe concluindo tarefas';
        valor.dataset.csDinheiro = '';
        caixa.append(relogio, carteira);
        hud.append(caixa);

        const atualizar = () => {
            const restante = tempoAteMeiaNoite(new Date());
            tempo.textContent = `${restante.horas}:${String(restante.minutos).padStart(2, '0')}`;
            tempo.classList.toggle('acabando', restante.horas === 0);
        };
        atualizar();
        window.setInterval(atualizar, 30000);
        return hud;
    }

    function colocarHud() {
        const barra = document.querySelector('.barra-app');
        if (barra) {
            barra.after(criarHud(false));
        } else if (document.querySelector('.cartao-acesso')) {
            document.body.append(criarHud(true));
        }
    }

    // ---------- Bomba das telas de acesso ----------

    function mostrarAviso(texto) {
        const aviso = criarEnfeite('cs-aviso', 'div');
        aviso.textContent = texto;
        document.body.append(aviso);
        window.setTimeout(() => aviso.remove(), DURACAO_AVISO_MS);
    }

    function criarBomba() {
        const bomba = document.createElement('button');
        const barra = criarEnfeite('cs-desarme');
        let cliques = 0;
        bomba.type = 'button';
        bomba.className = 'cs-bomba';
        bomba.append(barra);
        const descrever = () => {
            const acao = bomba.classList.contains('desarmada') ? 'desarmada' : 'clique para desarmar';
            bomba.title = `Bomba plantada: ${acao}`;
            bomba.setAttribute('aria-label', `Bomba plantada: ${acao} e ganhar dinheiro`);
        };
        bomba.addEventListener('click', () => {
            if (bomba.classList.contains('desarmada')) {
                return;
            }
            cliques += 1;
            bomba.classList.add('desarmando');
            bomba.style.setProperty('--progresso', String(cliques / CLIQUES_PARA_DESARMAR));
            bomba.classList.remove('mexendo');
            void bomba.offsetWidth; // reinicia a animação a cada clique
            bomba.classList.add('mexendo');
            if (cliques < CLIQUES_PARA_DESARMAR) {
                SONS.desarmar();
                SONS.bipe();
                return;
            }
            bomba.classList.add('desarmada');
            SONS.desarmada();
            mostrarAviso('Bomba desarmada');
            ganharDinheiro(DINHEIRO_POR_DESARME, bomba);
            descrever();
            // Outra bomba é plantada para desarmar de novo.
            window.setTimeout(() => {
                cliques = 0;
                bomba.style.setProperty('--progresso', '0');
                bomba.classList.remove('desarmada', 'desarmando');
                descrever();
            }, 2500);
        });
        descrever();
        document.body.append(bomba);
    }

    /** Login, cadastro e erro (telas sem a barra de navegação) viram a tela inicial do jogo. */
    function montarTelaDeAcesso() {
        const cartao = document.querySelector('.cartao-acesso');
        if (!cartao || document.querySelector('.barra-app')) {
            return;
        }
        if (cartao.classList.contains('text-center')) {
            const fim = criarEnfeite('cs-fim-rodada', 'p');
            const titulo = document.createElement('strong');
            const valor = document.createElement('span');
            titulo.textContent = 'Os Terroristas venceram';
            valor.textContent = textoDinheiro(dinheiro());
            fim.append(titulo, 'Rodada perdida... mas o seu dinheiro está a salvo: ', valor);
            cartao.querySelector('p')?.after(fim);
        } else {
            const radio = criarEnfeite('cs-radio');
            radio.textContent = RADIO[Math.floor(Math.random() * RADIO.length)];
            cartao.querySelector(':scope > .text-center.mb-4')?.append(radio);
        }
        criarBomba();
    }

    function colocarOperador() {
        if (!reduzMovimento) {
            document.body.append(criarEnfeite('cs-personagem'));
        }
    }

    criarBotaoSom();
    colocarHud();
    montarTelaDeAcesso();
    colocarOperador();
    atualizarDinheiro();
    mostrarAbateGuardado();
})();
