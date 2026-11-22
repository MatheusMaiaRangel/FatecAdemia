# FatecAdemia

API REST de uma academia de aulas coletivas: a academia cadastra modalidades, instrutores e aulas com horário e vagas, e o aluno se inscreve sem estourar a lotação e sem conflito de horário.

Feita com Spring Boot 3, Java 21, MySQL, Spring Data JPA, Spring Security com JWT e Swagger.

## Como rodar

### 1. Pré-requisitos

- Java 21
- MySQL 8 rodando (ou Docker, veja abaixo)
- Não precisa instalar o Maven: o projeto usa o Maven Wrapper (`./mvnw`)

Se não tiver MySQL instalado, dá pra subir um com Docker:

```bash
docker run -d --name fatecademia-mysql -e MYSQL_ROOT_PASSWORD=troque-esta-senha -p 3306:3306 mysql:8.4
```

### 2. Variáveis de ambiente

Copie o modelo e preencha os valores:

```bash
cp .env.example .env
```

O arquivo `.env` é lido automaticamente pela aplicação e está no `.gitignore`. Se preferir, as mesmas variáveis podem ser exportadas no terminal; as do sistema têm prioridade sobre o arquivo.

| Variável        | Para que serve                                         | Exemplo                                                                  |
|-----------------|--------------------------------------------------------|--------------------------------------------------------------------------|
| `DB_URL`        | Endereço JDBC do MySQL                                 | `jdbc:mysql://localhost:3306/fatecademia?createDatabaseIfNotExist=true`  |
| `DB_USUARIO`    | Usuário do banco                                       | `root`                                                                   |
| `DB_SENHA`      | Senha do banco                                         |                                                                          |
| `JWT_SEGREDO`   | Chave que assina os tokens (mínimo de 32 caracteres)   | gere com `openssl rand -base64 48`                                       |
| `JWT_EXPIRACAO` | Validade do token, no formato de duração do Spring     | `2h`, `30m`, `1d`                                                        |
| `ADMIN_NOME`    | Nome do administrador criado na primeira subida        | `Administrador`                                                          |
| `ADMIN_EMAIL`   | E-mail desse administrador                             | `admin@fatecademia.local`                                                |
| `ADMIN_SENHA`   | Senha desse administrador (8 a 72 caracteres)          |                                                                          |

A aplicação não sobe se alguma delas faltar ou se o segredo do JWT for curto demais.

### 3. Subir a API

```bash
./mvnw spring-boot:run
```

Na primeira subida as tabelas são criadas e o administrador é cadastrado com os dados de `ADMIN_*`. Nas próximas, ele só confirma que o administrador já existe.

### 4. Usar

- Swagger: http://localhost:8080/swagger-ui.html
- Crie um aluno em `POST /auth/registro`, faça login em `POST /auth/login` e use o token recebido no cabeçalho `Authorization: Bearer <token>`.

### 5. Testes e cobertura

```bash
./mvnw verify
```

Os testes usam H2 em memória (perfil `test`), então não precisam do MySQL. O `verify` falha se a cobertura ficar abaixo de 80%. O relatório fica em `target/site/jacoco/index.html`.

## Segurança

### Papéis

- `ADMIN`: cria, altera e exclui modalidades, instrutores e aulas.
- `ALUNO`: consulta o catálogo e cuida das próprias inscrições.

O cadastro público (`/auth/registro`) sempre cria `ALUNO`. O único `ADMIN` nasce das variáveis de ambiente.

### Quem acessa o quê

| Rota                                          | Método              | Quem pode                 |
|-----------------------------------------------|---------------------|---------------------------|
| `/auth/**`                                    | todos               | qualquer um               |
| `/swagger-ui/**`, `/v3/api-docs/**`           | todos               | qualquer um               |
| `/modalidades/**`, `/instrutores/**`, `/aulas/**` | `GET`           | qualquer usuário logado   |
| `/modalidades/**`, `/instrutores/**`, `/aulas/**` | `POST`, `PUT`, `DELETE` | só `ADMIN`        |
| `/inscricoes/**`                              | todos               | só `ALUNO`                |
| qualquer outra                                | todos               | qualquer usuário logado   |

A senha é gravada com BCrypt e nunca sai nas respostas.

### Formato dos erros

Todo erro, inclusive 401 e 403, volta no mesmo formato:

```json
{
  "instante": "2026-11-22T16:04:48Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos",
  "caminho": "/auth/registro",
  "campos": [
    { "campo": "email", "mensagem": "deve ser um endereço de e-mail bem formado" }
  ]
}
```

| Situação                                               | Status |
|--------------------------------------------------------|--------|
| Bean Validation, JSON mal formado, parâmetro inválido  | 400    |
| Sem token, token inválido ou login errado              | 401    |
| Papel sem permissão para a rota                        | 403    |
| `RecursoNaoEncontradoException` ou rota inexistente    | 404    |
| `RegraNegocioException` ou dado duplicado              | 409    |
| Corpo que não é JSON                                   | 415    |

## Para quem for implementar as outras partes

- **Regra de negócio violada:** lance `RegraNegocioException("mensagem")` no service. Vira 409.
- **Registro não encontrado:** lance `RecursoNaoEncontradoException("mensagem")`. Vira 404.
- **Usuário logado:** injete `UsuarioLogado` e chame `obter()` (ou `obterId()`). O usuário vem do token, fora de transação, então use para comparar ids ou associar a outra entidade, e não para navegar coleções lazy dele.
- As regras de rota por papel já estão prontas em `SecurityConfig`; basta criar os controllers nos caminhos da tabela acima.

## Estrutura

```
com.academia
├── config      relógio do sistema (injetável para facilitar testes com horário)
├── erro        tratador global, resposta de erro padrão e exceções
├── seguranca   SecurityConfig, filtro JWT, TokenService, UsuarioLogado e respostas 401/403
└── usuario     entidade Usuario, papéis, registro, login e administrador inicial
```

## Infraestrutura reaproveitada do projeto da aula

Como permitido no enunciado, estas partes de infraestrutura seguem a estrutura do projeto desenvolvido em aula, adaptadas para este domínio:

- **Configuração de segurança** (`seguranca/SecurityConfig`): API stateless, BCrypt e filtro JWT antes do filtro padrão do Spring Security.
- **Serviço de token** (`seguranca/TokenService`): geração e validação do JWT.
- **Filtro** (`seguranca/FiltroJwt`): lê o `Authorization: Bearer` e autentica o usuário da requisição.
- **Tratador de erros** (`erro/TratadorDeErros`): `@RestControllerAdvice` que converte exceções em respostas HTTP.

O modelo de dados, as regras de negócio e as consultas são do grupo.
