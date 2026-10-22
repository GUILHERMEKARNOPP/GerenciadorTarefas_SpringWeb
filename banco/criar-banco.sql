-- Cria o usuário gerenciador_web e o banco gerenciador_web (ou só troca a senha, se já existirem).
-- Rodado pelo setup-banco.ps1, conectado como superusuário (postgres). As TABELAS não são criadas
-- aqui: o próprio sistema cria pelo Flyway (src/main/resources/db/migration) na primeira vez que sobe.
-- A senha vem do ambiente para não ficar em arquivo versionado nem na linha de comando.

\getenv app_password APP_DB_PASSWORD
\if :{?app_password}
\else
  \echo 'ERRO: defina a variavel de ambiente APP_DB_PASSWORD antes de rodar.'
  \quit
\endif

SELECT 'CREATE ROLE gerenciador_web'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'gerenciador_web')\gexec

ALTER ROLE gerenciador_web WITH LOGIN PASSWORD :'app_password';

SELECT 'CREATE DATABASE gerenciador_web OWNER gerenciador_web ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'gerenciador_web')\gexec

-- Impede que outros usuários do servidor se conectem a este banco.
REVOKE CONNECT ON DATABASE gerenciador_web FROM PUBLIC;
GRANT CONNECT ON DATABASE gerenciador_web TO gerenciador_web;
