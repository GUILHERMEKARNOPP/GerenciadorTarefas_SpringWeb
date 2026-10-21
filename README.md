# Gerenciador de Tarefas Web

Sistema web para organizar tarefas, feito com **Spring MVC**, **Thymeleaf** e **Spring Data JPA** sobre um
banco **PostgreSQL**. É a versão web do [Gerenciador de Tarefas desktop em Java Swing](https://github.com/MarcosJr62/GerenciadorTarefas_JavaSwing),
desenvolvido antes pelo grupo, e reaproveita o modelo de dados e as regras de negócio daquele projeto.

> **Etapa atual: 1ª etapa.** Tema, protótipos de tela, diagrama de classes e MER.
>
> 🔗 **[Abrir os protótipos de tela](https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/)**

---

## Tema

**Gestão de tarefas pessoais.** Cada pessoa cria uma conta e cadastra as próprias tarefas, com título, descrição,
prazo, categoria, prioridade e status. Na tela principal ela pesquisa, filtra e ordena as tarefas, conclui uma
tarefa com um clique e vê quais estão atrasadas.

O sistema tem dois perfis de acesso:

- **Usuário:** mantém as próprias tarefas e categorias e o próprio cadastro. Nunca vê os dados de outra pessoa.
- **Administrador:** faz tudo o que o usuário faz e também mantém os cadastros que valem para todos
  (status e prioridades) e as contas de usuário.

## Funcionalidades

**Usuário**

- Criar conta, entrar e sair.
- Cadastrar, editar, excluir e listar tarefas.
- Pesquisar tarefas por texto, filtrar por status, prioridade e categoria, ordenar e paginar a lista.
- Concluir uma tarefa com um clique e ver em destaque as tarefas atrasadas.
- Cadastrar, editar, excluir e listar as próprias categorias.
- Editar os próprios dados, trocar a senha e excluir a conta.

**Administrador**

- Cadastrar, editar, excluir e listar usuários, inclusive desbloquear um login travado por senhas erradas.
- Cadastrar, editar, excluir e listar status.
- Cadastrar, editar, excluir e listar prioridades.

## Tabelas e CRUDs

O sistema manipula **5 tabelas**, e todas têm telas de cadastro, edição, exclusão e listagem:

| Tabela       | O que guarda                                        | Quem mantém   |
|--------------|-----------------------------------------------------|---------------|
| `usuario`    | Contas de acesso, perfil e controle de bloqueio do login | Administrador (e cada usuário, o próprio cadastro) |
| `tarefa`     | As tarefas de cada usuário                          | Usuário (só as dele) |
| `categoria`  | Categorias criadas por cada usuário                 | Usuário (só as dele) |
| `status`     | Situações da tarefa: Pendente, Em andamento, Concluída | Administrador |
| `prioridade` | Níveis de urgência: Alta, Média, Baixa              | Administrador |

## Tecnologias

- Java 21 ou superior e Maven
- Spring Boot com **Spring MVC**, **Thymeleaf** e **Spring Data JPA**
- Spring Security para login, perfis de acesso e proteção CSRF dos formulários
- Bean Validation para validar os formulários no servidor
- PostgreSQL
- Bootstrap 5 e Bootstrap Icons nas telas

---

## Protótipos de tela

🔗 **[Abrir os protótipos navegáveis](https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/)**
(código em [`docs/`](docs/))

Os protótipos são páginas HTML com Bootstrap e dados fictícios, publicadas pelo GitHub Pages. Dá para navegar entre
elas pelos menus e botões, e cada tela tem no rodapé uma nota com a rota prevista no Spring MVC e as regras que
ela segue. Para abrir sem internet, basta abrir `docs/index.html` no navegador.

Clique em uma imagem para abrir a tela correspondente.

### Acesso

<table>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/login.html"><img src="docs/img/login.png" alt="Tela de login"></a><br><b>Login</b></td>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/criar-conta.html"><img src="docs/img/criar-conta.png" alt="Tela de criação de conta"></a><br><b>Criar conta</b> (com exemplo de erro de validação)</td>
</tr>
</table>

### Tarefas

<a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/tarefas.html"><img src="docs/img/tarefas.png" alt="Listagem de tarefas"></a>

**Listagem de tarefas:** tela principal, com resumo, pesquisa, filtros, ordenação e paginação.

<table>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/tarefa-form.html"><img src="docs/img/tarefa-form.png" alt="Cadastro e edição de tarefa"></a><br><b>Cadastro / edição de tarefa</b></td>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/tarefa-excluir.html"><img src="docs/img/tarefa-excluir.png" alt="Exclusão de tarefa"></a><br><b>Exclusão de tarefa</b></td>
</tr>
</table>

### Categorias

<table>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/categorias.html"><img src="docs/img/categorias.png" alt="Listagem de categorias"></a><br><b>Listagem de categorias</b></td>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/categoria-form.html"><img src="docs/img/categoria-form.png" alt="Cadastro de categoria"></a><br><b>Cadastro / edição de categoria</b> (com exemplo de nome repetido)</td>
</tr>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/categoria-excluir.html"><img src="docs/img/categoria-excluir.png" alt="Exclusão de categoria"></a><br><b>Exclusão de categoria</b></td>
<td width="50%"></td>
</tr>
</table>

### Administração: usuários

<table>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/usuarios.html"><img src="docs/img/usuarios.png" alt="Listagem de usuários"></a><br><b>Listagem de usuários</b></td>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/usuario-form.html"><img src="docs/img/usuario-form.png" alt="Cadastro e edição de usuário"></a><br><b>Cadastro / edição de usuário</b></td>
</tr>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/usuario-excluir.html"><img src="docs/img/usuario-excluir.png" alt="Exclusão de usuário"></a><br><b>Exclusão de usuário</b></td>
<td width="50%"></td>
</tr>
</table>

### Administração: status

<table>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/status.html"><img src="docs/img/status.png" alt="Listagem de status"></a><br><b>Listagem de status</b> (com exemplo de exclusão recusada)</td>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/status-form.html"><img src="docs/img/status-form.png" alt="Cadastro e edição de status"></a><br><b>Cadastro / edição de status</b></td>
</tr>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/status-excluir.html"><img src="docs/img/status-excluir.png" alt="Exclusão de status"></a><br><b>Exclusão de status</b></td>
<td width="50%"></td>
</tr>
</table>

### Administração: prioridades

<table>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/prioridades.html"><img src="docs/img/prioridades.png" alt="Listagem de prioridades"></a><br><b>Listagem de prioridades</b></td>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/prioridade-form.html"><img src="docs/img/prioridade-form.png" alt="Cadastro e edição de prioridade"></a><br><b>Cadastro / edição de prioridade</b></td>
</tr>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/prioridade-excluir.html"><img src="docs/img/prioridade-excluir.png" alt="Exclusão de prioridade"></a><br><b>Exclusão de prioridade</b></td>
<td width="50%"></td>
</tr>
</table>

### Conta

<table>
<tr>
<td width="50%"><a href="https://guilhermekarnopp.github.io/GerenciadorTarefas_SpringWeb/perfil.html"><img src="docs/img/perfil.png" alt="Meu perfil"></a><br><b>Meu perfil:</b> editar dados, trocar senha e excluir a conta</td>
<td width="50%"></td>
</tr>
</table>

---

## Diagrama de classes

Entidades do sistema, que viram classes `@Entity` do JPA. Getters, setters e construtores foram omitidos.

```mermaid
classDiagram
    class Usuario {
        -Long id
        -String nome
        -String email
        -String login
        -String senhaHash
        -Perfil perfil
        -int tentativasFalhas
        -OffsetDateTime bloqueadoAte
        -OffsetDateTime dataCriacao
        +isBloqueado() boolean
        +registrarFalhaLogin()
        +registrarLoginComSucesso()
    }

    class Perfil {
        <<enumeration>>
        ADMIN
        USUARIO
    }

    class Tarefa {
        -Long id
        -String titulo
        -String descricao
        -OffsetDateTime dataCriacao
        -LocalDate dataVencimento
        -Usuario usuario
        -Categoria categoria
        -Status status
        -Prioridade prioridade
        +concluir(Status statusFinal)
        +isAtrasada() boolean
    }

    class Categoria {
        -Long id
        -String nome
        -String descricao
        -Usuario usuario
    }

    class Status {
        -Long id
        -String nome
        -boolean finaliza
    }

    class Prioridade {
        -Long id
        -String nome
        -int nivel
        -String cor
    }

    Usuario "1" *-- "0..*" Tarefa : possui
    Usuario "1" *-- "0..*" Categoria : cria
    Tarefa "0..*" --> "0..1" Categoria : classificada em
    Tarefa "0..*" --> "1" Status : está em
    Tarefa "0..*" --> "1" Prioridade : tem
    Usuario --> Perfil
```

- **Usuario** guarda apenas o hash da senha (BCrypt, pelo `PasswordEncoder` do Spring Security), nunca a senha.
  `tentativasFalhas` e `bloqueadoAte` bloqueiam o login por 5 minutos depois de 5 senhas erradas seguidas.
- **Tarefa** e **Categoria** são compostas pelo **Usuario**: pertencem a um único usuário e são apagadas junto com ele.
- **Tarefa** aponta para **Categoria**, **Status** e **Prioridade** (`@ManyToOne`). A categoria é opcional.
- `Tarefa.concluir(...)` troca o status pelo status que tem `finaliza = true`.
  `Tarefa.isAtrasada()` é verdadeiro quando o vencimento já passou e o status ainda não finaliza a tarefa.
- **Prioridade.nivel** define a ordem de urgência (1 = mais urgente), e **Prioridade.cor** é a cor do selo na tela.

## Modelo Entidade-Relacionamento (MER)

```mermaid
erDiagram
    USUARIO ||--o{ TAREFA : possui
    USUARIO ||--o{ CATEGORIA : cria
    CATEGORIA |o--o{ TAREFA : classifica
    STATUS ||--o{ TAREFA : "situação de"
    PRIORIDADE ||--o{ TAREFA : "urgência de"

    USUARIO {
        bigint id PK
        varchar(100) nome "NOT NULL"
        varchar(150) email UK "NOT NULL"
        varchar(50) login UK "NOT NULL"
        varchar(255) senha_hash "NOT NULL, BCrypt"
        varchar(20) perfil "NOT NULL, ADMIN ou USUARIO"
        integer tentativas_falhas "NOT NULL, padrão 0"
        timestamptz bloqueado_ate "nulo = não bloqueado"
        timestamptz data_criacao "NOT NULL"
    }

    CATEGORIA {
        bigint id PK
        varchar(50) nome "NOT NULL, único por usuário"
        varchar(255) descricao
        bigint usuario_id FK "NOT NULL"
    }

    STATUS {
        bigint id PK
        varchar(30) nome UK "NOT NULL"
        boolean finaliza "NOT NULL, só um com true"
    }

    PRIORIDADE {
        bigint id PK
        varchar(30) nome UK "NOT NULL"
        integer nivel UK "NOT NULL, 1 = mais urgente"
        char(7) cor "NOT NULL, #RRGGBB"
    }

    TAREFA {
        bigint id PK
        varchar(150) titulo "NOT NULL"
        text descricao
        timestamptz data_criacao "NOT NULL"
        date data_vencimento
        bigint usuario_id FK "NOT NULL"
        bigint categoria_id FK "nulo = sem categoria"
        bigint status_id FK "NOT NULL"
        bigint prioridade_id FK "NOT NULL"
    }
```

### Relacionamentos

| Relacionamento         | Cardinalidade | Ao excluir o registro "pai"                                 |
|------------------------|---------------|-------------------------------------------------------------|
| USUARIO → TAREFA       | 1 : N         | As tarefas do usuário são apagadas (`ON DELETE CASCADE`)    |
| USUARIO → CATEGORIA    | 1 : N         | As categorias do usuário são apagadas (`ON DELETE CASCADE`) |
| CATEGORIA → TAREFA     | 0..1 : N      | As tarefas ficam sem categoria (`ON DELETE SET NULL`)       |
| STATUS → TAREFA        | 1 : N         | Não permite excluir status em uso (`ON DELETE RESTRICT`)    |
| PRIORIDADE → TAREFA    | 1 : N         | Não permite excluir prioridade em uso (`ON DELETE RESTRICT`) |

### Regras garantidas pelo banco

- `login` e `email` são únicos sem diferenciar maiúsculas de minúsculas (índice único em `lower(...)`).
- O nome da categoria é único **por usuário**: índice único em `(usuario_id, lower(nome))`.
- Só um status pode ter `finaliza = true` (índice único parcial em `finaliza WHERE finaliza`).
- `perfil` aceita apenas `ADMIN` ou `USUARIO`, e `cor` apenas o formato `#RRGGBB` (`CHECK`).
- Título, nome e login não podem ser só espaços (`CHECK (btrim(...) <> '')`).

---

## O que muda em relação à versão desktop

O modelo de dados é o mesmo do [projeto em Java Swing](https://github.com/MarcosJr62/GerenciadorTarefas_JavaSwing),
com os ajustes necessários para um sistema web com vários usuários ao mesmo tempo:

- **Categorias por usuário.** No desktop as categorias eram compartilhadas por todos. Na web isso deixaria uma
  pessoa editar ou apagar a categoria de outra, então cada categoria passa a ter um dono (`categoria.usuario_id`).
- **Perfil de acesso** (`usuario.perfil`). Status, prioridades e usuários ganham telas de manutenção, que só o
  administrador acessa.
- **`status.finaliza`.** O desktop encontrava o status de conclusão pelo nome "Concluída". Como agora o nome pode ser
  editado, um campo indica qual status encerra a tarefa.
- **`prioridade.nivel` e `prioridade.cor`.** Ordenam as prioridades por urgência (e não por ordem alfabética) e
  definem a cor do selo na lista de tarefas.
- **Senha com o Spring Security** (BCrypt) no lugar do hash PBKDF2 feito à mão.

## Etapas

- [x] **1ª etapa:** tema, protótipos de tela, diagrama de classes e MER
- [ ] Implementação com Spring MVC, Thymeleaf e Spring Data JPA
