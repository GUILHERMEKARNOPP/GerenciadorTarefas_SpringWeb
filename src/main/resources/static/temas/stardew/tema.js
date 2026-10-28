/*
  Tema Stardew Valley: sons gerados na hora (Web Audio, sem arquivos de áudio), ouro, caixa de data e hora,
  canteiro para regar e colher e personagens. Tudo aqui é enfeite: sem este script o sistema funciona igual.
*/
(() => {
    'use strict';

    const CHAVE_SOM = 'stardew.som';
    const CHAVE_OURO = 'stardew.ouro';
    const MAXIMO_OURO = 9999999;
    const OURO_POR_TAREFA = 50;
    const OURO_POR_COLHEITA = 35;
    const ESTAGIO_PRONTO = 4;
    // Tempo para o som começar antes de a página trocar (senão a navegação corta o som).
    const ATRASO_NAVEGACAO_MS = 180;
    const reduzMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const formatoOuro = new Intl.NumberFormat('pt-BR');

    const DICAS = [
        'Regue suas tarefas todo dia!',
        'Toda colheita começa com uma semente.',
        'Hoje é um bom dia para plantar!',
        'Spring: estação e framework!',
        'Não esqueça de dar comida às galinhas!',
        'Uma tarefa por dia, uma colheita por estação.',
        'Também tente o tema Minecraft!',
        'Durma cedo: amanhã tem prazo!',
        'Pastinacas valem ouro!',
    ];

    const DIAS = ['Dom.', 'Seg.', 'Ter.', 'Qua.', 'Qui.', 'Sex.', 'Sáb.'];

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
    function tocar(notas, onda = 'triangle', volume = 0.08) {
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

    /** Ruído filtrado: passos na grama, água do regador, enxada e lixeira. */
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
        clique: () => tocar([[880, 0.03, 660], [1320, 0.04]], 'sine', 0.07),
        passo: () => chiado(0.08, 'highpass', 1800, 3500, 0.12),
        moedas: () => {
            tocar([[1568, 0.07], [2093, 0.22]], 'square', 0.035);
            tocar([[3136, 0.05], [4186, 0.18]], 'sine', 0.025);
        },
        regar: () => chiado(0.35, 'bandpass', 900, 2600, 0.3),
        colher: () => tocar([[330, 0.09, 880]], 'sine', 0.16),
        // Arpejo alegre (escala pentatônica), como quando o dia começa.
        jingle: () => {
            tocar([[523, 0.09], [587, 0.09], [659, 0.09], [784, 0.09], [880, 0.3]], 'triangle', 0.11);
            tocar([[1047, 0.27], [1319, 0.3]], 'sine', 0.035);
        },
        lixeira: () => {
            chiado(0.3, 'lowpass', 2200, 300, 0.5);
            tocar([[220, 0.06, 140], [160, 0.12, 90]], 'square', 0.04);
        },
        enxada: () => {
            chiado(0.07, 'lowpass', 1400, 400, 0.45);
            tocar([[140, 0.07, 90]], 'triangle', 0.15);
        },
        porta: () => tocar([[300, 0.05, 240], [200, 0.1, 150]], 'triangle', 0.1),
    };

    function somDoBotao(elemento) {
        if (!elemento) {
            return 'clique';
        }
        if (elemento.matches('.mundo')) {
            return 'jingle';
        }
        if (elemento.matches('.btn-outline-success')) {
            return 'moedas';
        }
        if (elemento.matches('.btn-danger')) {
            return 'lixeira';
        }
        if (elemento.matches('.btn-outline-danger')) {
            return 'enxada';
        }
        if (elemento.matches('.btn-outline-light')) {
            return 'porta';
        }
        return 'clique';
    }

    // ---------- Ouro ----------

    function ouro() {
        const valor = Number.parseInt(ler(CHAVE_OURO) ?? '0', 10);
        return Number.isInteger(valor) && valor > 0 ? Math.min(valor, MAXIMO_OURO) : 0;
    }

    function atualizarOuro() {
        const texto = formatoOuro.format(ouro());
        document.querySelectorAll('[data-sv-ouro]').forEach((elemento) => {
            elemento.textContent = texto;
        });
    }

    function soltarMoedas(origem, quantidade, valor) {
        if (reduzMovimento || !origem) {
            return;
        }
        const area = origem.getBoundingClientRect();
        const x = area.left + window.scrollX + area.width / 2;
        const y = area.top + window.scrollY;
        for (let i = 0; i < quantidade; i += 1) {
            const moeda = document.createElement('span');
            moeda.className = 'sv-moeda-voando';
            moeda.style.left = `${x}px`;
            moeda.style.top = `${y - 10}px`;
            moeda.style.setProperty('--dx', `${(i - (quantidade - 1) / 2) * 22}px`);
            moeda.style.animationDelay = `${i * 70}ms`;
            moeda.addEventListener('animationend', () => moeda.remove());
            document.body.append(moeda);
        }
        const ganho = document.createElement('span');
        ganho.className = 'sv-ganho';
        ganho.textContent = `+${valor}g`;
        ganho.style.left = `${x}px`;
        ganho.style.top = `${y - 34}px`;
        ganho.addEventListener('animationend', () => ganho.remove());
        document.body.append(ganho);
    }

    function ganharOuro(quantidade, origem) {
        gravar(CHAVE_OURO, String(Math.min(ouro() + quantidade, MAXIMO_OURO)));
        atualizarOuro();
        soltarMoedas(origem, 3, quantidade);
        document.querySelectorAll('.sv-ouro').forEach((caixa) => {
            caixa.classList.remove('ganhou');
            void caixa.offsetWidth; // reinicia a animação
            caixa.classList.add('ganhou');
        });
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
        if (evento.defaultPrevented || formulario.dataset.svEnvio === 'liberado') {
            return;
        }
        // Clique repetido enquanto o som toca: ignora, para não enviar duas vezes.
        if (formulario.dataset.svEnvio === 'aguardando') {
            evento.preventDefault();
            return;
        }
        const botao = evento.submitter ?? null;
        // Concluir tarefa é vender a colheita: rende ouro.
        if (botao?.matches('.btn-outline-success')) {
            ganharOuro(OURO_POR_TAREFA, botao);
        }
        if (!somLigado()) {
            return;
        }
        evento.preventDefault();
        formulario.dataset.svEnvio = 'aguardando';
        SONS[somDoBotao(botao)]();
        window.setTimeout(() => {
            formulario.dataset.svEnvio = 'liberado';
            formulario.requestSubmit(botao ?? undefined);
        }, ATRASO_NAVEGACAO_MS);
    });

    // Voltar pelo histórico pode restaurar a página com o formulário marcado; libera de novo.
    window.addEventListener('pageshow', () => {
        document.querySelectorAll('form[data-sv-envio]').forEach((formulario) => {
            delete formulario.dataset.svEnvio;
        });
    });

    const digitando = (alvo) => alvo instanceof Element && alvo.closest('input, textarea, select, [contenteditable]');

    // ---------- Botão de som ----------

    function criarBotaoSom() {
        const botao = document.createElement('button');
        const icone = document.createElement('i');
        const texto = document.createElement('span');
        botao.type = 'button';
        botao.className = 'sv-som';
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

    // ---------- Caixa de data, hora e ouro ----------

    /** Estações do hemisfério sul, pela data real (dia em que cada uma começa, aproximado). */
    function estacaoDe(data) {
        const mesDia = (data.getMonth() + 1) * 100 + data.getDate();
        if (mesDia >= 1221 || mesDia < 320) {
            return { id: 'verao', nome: 'Verão' };
        }
        if (mesDia < 621) {
            return { id: 'outono', nome: 'Outono' };
        }
        if (mesDia < 922) {
            return { id: 'inverno', nome: 'Inverno' };
        }
        return { id: 'primavera', nome: 'Primavera' };
    }

    function criarEnfeite(classe, tag = 'span') {
        const elemento = document.createElement(tag);
        elemento.className = classe;
        elemento.setAttribute('aria-hidden', 'true');
        return elemento;
    }

    function criarHud(flutuante) {
        const hud = criarEnfeite(flutuante ? 'sv-hud flutuante' : 'sv-hud container', 'div');
        const caixa = criarEnfeite('sv-caixa-hud');
        const relogio = criarEnfeite('sv-relogio');
        const data = criarEnfeite('sv-data');
        const estacao = criarEnfeite('sv-estacao');
        const hora = criarEnfeite('sv-hora');
        const ouroCaixa = criarEnfeite('sv-ouro');
        const valor = document.createElement('span');
        ouroCaixa.title = 'Ouro: ganhe concluindo tarefas';
        valor.dataset.svOuro = '';
        ouroCaixa.append(valor, 'g');
        relogio.append(data, estacao, hora);
        caixa.append(relogio, ouroCaixa);
        hud.append(caixa);

        const atualizar = () => {
            const agora = new Date();
            const daEstacao = estacaoDe(agora);
            // Como no jogo, o relógio anda de 10 em 10 minutos.
            const minutos = Math.floor(agora.getMinutes() / 10) * 10;
            data.textContent = `${DIAS[agora.getDay()]} ${agora.getDate()}`;
            estacao.textContent = daEstacao.nome;
            estacao.dataset.estacao = daEstacao.id;
            hora.textContent = `${String(agora.getHours()).padStart(2, '0')}:${String(minutos).padStart(2, '0')}`;
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

    // ---------- Canteiro das telas de acesso ----------

    function soltarGotas(origem) {
        if (reduzMovimento) {
            return;
        }
        const area = origem.getBoundingClientRect();
        for (let i = 0; i < 6; i += 1) {
            const gota = document.createElement('span');
            gota.className = 'sv-gota';
            gota.style.left = `${area.left + window.scrollX + area.width / 2 - 2 + (i - 2.5) * 6}px`;
            gota.style.top = `${area.top + window.scrollY - 6}px`;
            gota.style.setProperty('--dx', `${(Math.random() - 0.5) * 30}px`);
            gota.style.animationDelay = `${i * 40}ms`;
            gota.addEventListener('animationend', () => gota.remove());
            document.body.append(gota);
        }
    }

    function soltarColheita(origem) {
        if (reduzMovimento) {
            return;
        }
        const area = origem.getBoundingClientRect();
        const pastinaca = document.createElement('span');
        pastinaca.className = 'sv-colheita';
        pastinaca.style.left = `${area.left + window.scrollX}px`;
        pastinaca.style.top = `${area.top + window.scrollY}px`;
        pastinaca.addEventListener('animationend', () => pastinaca.remove());
        document.body.append(pastinaca);
    }

    function criarCanteiro() {
        const canteiro = document.createElement('button');
        let estagio = 0;
        canteiro.type = 'button';
        canteiro.className = 'sv-canteiro';
        const descrever = () => {
            const acao = estagio < ESTAGIO_PRONTO ? 'clique para regar' : 'clique para colher';
            canteiro.title = `Canteiro de pastinaca: ${acao}`;
            canteiro.setAttribute('aria-label', `Canteiro de pastinaca: ${acao} e ganhar ouro`);
        };
        canteiro.addEventListener('click', () => {
            if (canteiro.classList.contains('colhido')) {
                return;
            }
            if (estagio < ESTAGIO_PRONTO) {
                estagio += 1;
                canteiro.style.setProperty('--estagio', String(estagio));
                canteiro.classList.remove('regando');
                void canteiro.offsetWidth; // reinicia a animação a cada rega
                canteiro.classList.add('regando');
                soltarGotas(canteiro);
                SONS.regar();
                descrever();
                return;
            }
            soltarColheita(canteiro);
            SONS.colher();
            window.setTimeout(SONS.moedas, 250);
            ganharOuro(OURO_POR_COLHEITA, canteiro);
            canteiro.classList.add('colhido');
            // Uma semente nova é plantada para colher de novo.
            window.setTimeout(() => {
                estagio = 0;
                canteiro.style.setProperty('--estagio', '0');
                canteiro.classList.remove('colhido');
                descrever();
            }, 1500);
        });
        descrever();
        document.body.append(canteiro);
    }

    /** Login, cadastro e erro (telas sem a barra de navegação) viram a tela de título do jogo. */
    function montarTelaDeAcesso() {
        const cartao = document.querySelector('.cartao-acesso');
        if (!cartao || document.querySelector('.barra-app')) {
            return;
        }
        if (cartao.classList.contains('text-center')) {
            const desmaio = criarEnfeite('sv-desmaio', 'p');
            const valor = document.createElement('span');
            valor.textContent = `${formatoOuro.format(ouro())}g`;
            desmaio.append('Você desmaiou de cansaço... mas o seu ouro está a salvo: ', valor);
            cartao.querySelector('p')?.after(desmaio);
        } else {
            const placa = criarEnfeite('sv-placa');
            placa.textContent = DICAS[Math.floor(Math.random() * DICAS.length)];
            cartao.querySelector(':scope > .text-center.mb-4')?.append(placa);
        }
        criarCanteiro();
        if (!reduzMovimento) {
            document.body.append(criarEnfeite('sv-personagem galinha'));
        }
    }

    function colocarFazendeiro() {
        if (!reduzMovimento) {
            document.body.append(criarEnfeite('sv-personagem fazendeiro'));
        }
    }

    criarBotaoSom();
    colocarHud();
    montarTelaDeAcesso();
    colocarFazendeiro();
    atualizarOuro();
})();
