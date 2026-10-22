-- Estrutura do banco. O Flyway roda este arquivo sozinho na primeira vez que o sistema sobe.

CREATE TABLE usuario (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome               VARCHAR(100) NOT NULL,
    email              VARCHAR(150) NOT NULL,
    login              VARCHAR(50)  NOT NULL,
    -- Guarda apenas o hash da senha (BCrypt, gerado pelo Spring Security), nunca a senha em texto puro.
    senha_hash         VARCHAR(255) NOT NULL,
    perfil             VARCHAR(20)  NOT NULL DEFAULT 'USUARIO',
    -- Proteção contra força bruta: após várias senhas erradas seguidas o login fica bloqueado por um tempo.
    tentativas_falhas  INTEGER      NOT NULL DEFAULT 0,
    bloqueado_ate      TIMESTAMPTZ,
    data_criacao       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_nome_nao_vazio CHECK (btrim(nome) <> ''),
    CONSTRAINT ck_usuario_login_formato  CHECK (login ~ '^[A-Za-z0-9._-]{3,50}$'),
    CONSTRAINT ck_usuario_email_formato  CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_usuario_perfil         CHECK (perfil IN ('ADMIN', 'USUARIO')),
    CONSTRAINT ck_usuario_tentativas     CHECK (tentativas_falhas >= 0)
);

-- Unicidade sem diferenciar maiúsculas/minúsculas ("Joao" e "joao" são o mesmo login).
CREATE UNIQUE INDEX uq_usuario_login ON usuario (lower(login));
CREATE UNIQUE INDEX uq_usuario_email ON usuario (lower(email));

CREATE TABLE categoria (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        VARCHAR(50)  NOT NULL,
    descricao   VARCHAR(255),
    usuario_id  BIGINT       NOT NULL,
    CONSTRAINT ck_categoria_nome_nao_vazio CHECK (btrim(nome) <> ''),
    -- Alvo da chave estrangeira composta de tarefa (garante categoria do mesmo dono da tarefa).
    CONSTRAINT uq_categoria_id_usuario UNIQUE (id, usuario_id),
    CONSTRAINT fk_categoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
);

-- Cada usuário não repete nome de categoria; usuários diferentes podem ter categorias com o mesmo nome.
CREATE UNIQUE INDEX uq_categoria_usuario_nome ON categoria (usuario_id, lower(nome));

CREATE TABLE status (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome      VARCHAR(30) NOT NULL,
    finaliza  BOOLEAN     NOT NULL DEFAULT false,
    CONSTRAINT ck_status_nome_nao_vazio CHECK (btrim(nome) <> '')
);

CREATE UNIQUE INDEX uq_status_nome ON status (lower(nome));
-- No máximo um status finaliza a tarefa (o sistema impede que fique nenhum).
CREATE UNIQUE INDEX uq_status_finaliza ON status (finaliza) WHERE finaliza;

CREATE TABLE prioridade (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome   VARCHAR(30) NOT NULL,
    nivel  INTEGER     NOT NULL,
    cor    VARCHAR(7)  NOT NULL,
    CONSTRAINT ck_prioridade_nome_nao_vazio CHECK (btrim(nome) <> ''),
    CONSTRAINT ck_prioridade_nivel CHECK (nivel BETWEEN 1 AND 99),
    -- A cor vai para o atributo style da página: só aceita o formato #RRGGBB.
    CONSTRAINT ck_prioridade_cor CHECK (cor ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT uq_prioridade_nivel UNIQUE (nivel)
);

CREATE UNIQUE INDEX uq_prioridade_nome ON prioridade (lower(nome));

CREATE TABLE tarefa (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo           VARCHAR(150) NOT NULL,
    descricao        TEXT,
    data_criacao     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_vencimento  DATE,
    usuario_id       BIGINT       NOT NULL,
    categoria_id     BIGINT,
    status_id        BIGINT       NOT NULL,
    prioridade_id    BIGINT       NOT NULL,
    CONSTRAINT ck_tarefa_titulo_nao_vazio CHECK (btrim(titulo) <> ''),
    -- Excluir o usuário apaga as tarefas dele.
    CONSTRAINT fk_tarefa_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE,
    -- A categoria precisa ser do mesmo usuário da tarefa. Excluir a categoria deixa a tarefa
    -- "sem categoria" (zera só categoria_id, não o dono).
    CONSTRAINT fk_tarefa_categoria FOREIGN KEY (categoria_id, usuario_id)
        REFERENCES categoria (id, usuario_id) ON DELETE SET NULL (categoria_id),
    -- Status/prioridade em uso não podem ser excluídos.
    CONSTRAINT fk_tarefa_status FOREIGN KEY (status_id)
        REFERENCES status (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tarefa_prioridade FOREIGN KEY (prioridade_id)
        REFERENCES prioridade (id) ON DELETE RESTRICT
);

-- A lista de tarefas sempre filtra pelo usuário logado + status/prioridade/vencimento.
CREATE INDEX ix_tarefa_usuario_status     ON tarefa (usuario_id, status_id);
CREATE INDEX ix_tarefa_usuario_prioridade ON tarefa (usuario_id, prioridade_id);
CREATE INDEX ix_tarefa_usuario_vencimento ON tarefa (usuario_id, data_vencimento);
CREATE INDEX ix_tarefa_categoria          ON tarefa (categoria_id);
