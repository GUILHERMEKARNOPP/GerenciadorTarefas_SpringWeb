-- Status e prioridades iniciais. O administrador pode mudá-los depois pelas telas de administração.
-- Usuários são criados pela tela "Criar conta" (a senha precisa virar hash no Java) e as categorias
-- são de cada usuário.

INSERT INTO status (nome, finaliza) VALUES
    ('Pendente', false),
    ('Em andamento', false),
    ('Concluída', true);

INSERT INTO prioridade (nome, nivel, cor) VALUES
    ('Alta', 1, '#dc3545'),
    ('Média', 2, '#fd7e14'),
    ('Baixa', 3, '#198754');
