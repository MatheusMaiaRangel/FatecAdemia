# FatecAdemia

API REST de uma academia de aulas coletivas: a academia cadastra modalidades, instrutores e aulas com horário e vagas, e o aluno se inscreve sem estourar a lotação e sem conflito de horário.

Feita com Spring Boot 3, Java 21, MySQL, Spring Data JPA, Spring Security com JWT e Swagger.

## 👥 Integrantes

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/MatheusMaiaRangel">
        <img src="https://avatars.githubusercontent.com/u/179478474?v=4" width="100px" alt="Foto do Matheus Maia"/><br>
        <sub><b>Matheus Maia</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/alvesxr">
        <img src="https://avatars.githubusercontent.com/u/175729323?v=4" width="100px" alt="Foto do João Rafael"/><br>
        <sub><b>João Rafael</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/otaviocuriel">
        <img src="https://github.com/user-attachments/assets/f6f9b505-e0d8-48c2-a2c1-9f6ac73c6b51" width="100px" alt="Foto do Otávio"/><br>
        <sub><b>Otávio</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/brunor18">
        <img src="https://github.com/user-attachments/assets/7a8c8cd4-3efb-4c74-ba7f-6360f293dcda" width="100px" alt="Foto do Bruno"/><br>
        <sub><b>Bruno</b></sub>
      </a>
    </td>
  </tr>
</table>

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
| `ADMIN_SENHA`   | Senha desse administrador (8 caracteres a 72 bytes)    |                                                                          |

A aplicação não sobe se alguma delas faltar ou se o segredo do JWT for curto demais.

### 3. Criar o banco (opcional) e carregar o catálogo

A aplicação cria as tabelas sozinha na primeira subida. Para criar o banco pelo script e já ter modalidades, instrutores e aulas de exemplo:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/carga-inicial.sql
```

A carga marca as aulas a partir do dia seguinte. A aula de Yoga com 2 vagas serve para mostrar o 409 de aula lotada. Rode a carga uma vez só, em um banco vazio.

### 4. Subir a API

```bash
./mvnw spring-boot:run
```

Na primeira subida as tabelas são criadas e o administrador é cadastrado com os dados de `ADMIN_*`. Nas próximas, ele só confirma que o administrador já existe.

### 5. Usar

- Swagger: http://localhost:8080/swagger-ui.html
- No Swagger, faça login como administrador em `POST /auth/login` (dados de `ADMIN_EMAIL` e `ADMIN_SENHA`) e copie o `token` da resposta.
- Clique em **Authorize**, cole só o token (sem escrever `Bearer`) e confirme. A partir daí as rotas protegidas funcionam pela própria interface, como o cadastro de alunos em `POST /usuarios`.
- Fora do Swagger, mande o token no cabeçalho `Authorization: Bearer <token>`.

### 6. Testes e cobertura

```bash
./mvnw verify
```

Os testes usam H2 em memória (perfil `test`), então não precisam do MySQL. O `verify` falha se a cobertura ficar abaixo de 80%. O relatório fica em `target/site/jacoco/index.html`.

No Windows, use `mvnw.cmd` e confirme que o terminal está usando Java 21:

```bat
java -version
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

### Regras de inscrição

- A aula deve começar no futuro; aulas já iniciadas não aceitam inscrições.
- O aluno não pode ter duas aulas com o mesmo horário de início. O modelo não possui duração das aulas.
- Inscrições concorrentes protegem a agenda do aluno e a capacidade da aula com bloqueios no banco, dentro da transação.
- Alterações de horário também verificam conflitos dos alunos inscritos. Se novas inscrições entrarem durante a atualização, a API pode responder 409 e pedir uma nova tentativa.
- Aula iniciada, conflito de horário, inscrição duplicada e lotação retornam 409.

## Segurança

### Papéis

- `ADMIN`: cria, altera e exclui modalidades, instrutores e aulas.
- `ALUNO`: consulta o catálogo e cuida das próprias inscrições.

Não existe cadastro público: quem cadastra alunos é o `ADMIN`, em `POST /usuarios`, como a recepção da academia. Todo usuário cadastrado ali é `ALUNO`. O único `ADMIN` nasce das variáveis de ambiente.

### Quem acessa o quê

| Rota                                          | Método              | Quem pode                 |
|-----------------------------------------------|---------------------|---------------------------|
| `/auth/login`                                 | `POST`              | qualquer um               |
| `/usuarios/**`                                | todos               | só `ADMIN`                |
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
  "caminho": "/usuarios",
  "campos": [
    { "campo": "email", "mensagem": "deve ser um endereço de e-mail bem formado" }
  ]
}
```

| Situação                                               | Status |
|--------------------------------------------------------|--------|
| Bean Validation, JSON mal formado, parâmetro inválido  | 400    |
| Campo com tipo errado (ex.: texto em `capacidade`, data fora do formato) — `campos` diz qual e o formato esperado | 400 |
| Sem token, token inválido ou login errado              | 401    |
| Papel sem permissão para a rota                        | 403    |
| `RecursoNaoEncontradoException` ou rota inexistente    | 404    |
| `RegraNegocioException` ou dado duplicado (a mensagem diz qual regra) | 409 |
| Registro travado por outra requisição ao mesmo tempo (pede para tentar de novo) | 409 |
| Corpo que não é JSON                                   | 415    |

## Para quem for implementar as outras partes

- **Regra de negócio violada:** lance `RegraNegocioException("mensagem")` no service. Vira 409.
- **Registro não encontrado:** lance `RecursoNaoEncontradoException("mensagem")`. Vira 404.
- **Usuário logado:** injete `UsuarioLogado` e chame `obter()` (ou `obterId()`). O usuário vem do token, fora de transação, então use para comparar ids ou associar a outra entidade, e não para navegar coleções lazy dele.
- As regras de rota por papel já estão prontas em `SecurityConfig`; basta criar os controllers nos caminhos da tabela acima.

## Aulas

O módulo de aulas tem CRUD em `/aulas`, paginação, ordenação, filtro por modalidade e
listagem de próximas aulas em `/aulas/proximas`. A criação e a edição impedem duas
aulas do mesmo instrutor no mesmo horário de início, com resposta 409.

O campo de data/hora se chama **`horario`**, preservando a integração com inscrições.
O Swagger já tem o Authorize configurado e todos os endpoints de aulas têm `@Operation`.

Veja [o guia de aulas](docs/aulas.md) para exemplos de requisições, consultas e testes.

## Modalidades e instrutores

CRUD completo em `/modalidades` e `/instrutores`. Qualquer usuário logado consulta; criar, editar e excluir é só do `ADMIN`.

| Método   | Rota                                   | Resposta                                        |
|----------|----------------------------------------|-------------------------------------------------|
| `GET`    | `/modalidades`, `/instrutores`         | 200, lista em ordem alfabética                  |
| `GET`    | `/modalidades/{id}`, `/instrutores/{id}` | 200 ou 404                                    |
| `POST`   | `/modalidades`, `/instrutores`         | 201 com `Location`, ou 400                      |
| `PUT`    | `/modalidades/{id}`, `/instrutores/{id}` | 200, 400 ou 404                               |
| `DELETE` | `/modalidades/{id}`, `/instrutores/{id}` | 204, 404 ou 409                               |

Validação nos DTOs: `@NotBlank` e `@Size` (nome até 100 caracteres, descrição até 500, especialidade até 100).

**Regra 4 — não pode excluir modalidade com aulas futuras.** Quando o ADMIN tenta apagar uma modalidade que ainda tem aula marcada, o `ModalidadeService` devolve 409. Usa `AulaRepository.existsByModalidadeIdAndHorarioAfter`.

Para não quebrar o histórico das inscrições, também devolvem 409:
- excluir uma modalidade que só tem aulas já realizadas;
- excluir um instrutor que tem qualquer aula cadastrada.

**Lado dono:** a `Aula` guarda `modalidade_id` e `instrutor_id` com `@ManyToOne`. Do outro lado, `Modalidade` e `Instrutor` têm `@OneToMany(mappedBy = ...)`, que só lê a relação.

**Script SQL:** `database/schema.sql` cria o banco e as tabelas, e `database/carga-inicial.sql` cadastra o catálogo de exemplo.

## Consultas

### Query methods (derivados do nome do método)

O Spring Data lê o nome do método e monta a consulta sozinho. Estes quatro são os principais, cada um responde a uma pergunta diferente do domínio:

| Método | Pergunta que responde | Onde é usado |
|--------|-----------------------|--------------|
| `AulaRepository.findByHorarioAfterOrderByHorarioAsc` | Quais são as próximas aulas, da mais próxima para a mais distante? | `AulaService.listarProximas`, em `GET /aulas/proximas` |
| `AulaRepository.findByModalidadeNomeContainingIgnoreCase` | Quais aulas são de uma modalidade (buscando parte do nome)? | `AulaService.listar`, no filtro `GET /aulas?modalidade=` |
| `AulaRepository.findByInstrutorIdAndHorarioBetween` | Quais aulas esse instrutor já tem nesse dia? | `AulaService.validarHorario`, em `POST` e `PUT /aulas`, para barrar o instrutor em duas aulas no mesmo horário |
| `InscricaoRepository.findByAlunoIdOrderByCriadaEmDesc` | Quais são as inscrições do aluno logado? | `InscricaoService.listarMinhasInscricoes`, em `GET /inscricoes` |

Os outros query methods do projeto sustentam as regras de negócio:

| Método | Onde é usado | Para quê |
|--------|--------------|----------|
| `InscricaoRepository.findByIdAndAlunoId` | `InscricaoService.cancelar` (`DELETE /inscricoes/{id}`) | Só acha a inscrição se for do aluno logado (separação por dono) |
| `InscricaoRepository.existsByAlunoIdAndAulaId` | `InscricaoService.inscrever` (`POST /inscricoes`) | Impede inscrição repetida na mesma aula |
| `InscricaoRepository.existsByAlunoIdAndAulaHorarioAndAulaIdNot` | `InscricaoService.inscrever` e `AulaService.atualizar` | Impede o aluno de ter duas aulas no mesmo horário |
| `InscricaoRepository.countByAulaId` | `InscricaoService.inscrever`, `AulaService.atualizar` e `AulaService.excluir` | Conta inscritos para checar lotação, capacidade e exclusão |
| `AulaRepository.existsByModalidadeIdAndHorarioAfter` | `ModalidadeService.excluir` (`DELETE /modalidades/{id}`) | Regra 4: não exclui modalidade com aulas futuras |
| `AulaRepository.existsByModalidadeId` | `ModalidadeService.excluir` | Não exclui modalidade com aulas já realizadas |
| `AulaRepository.existsByInstrutorId` | `InstrutorService.excluir` (`DELETE /instrutores/{id}`) | Não exclui instrutor com aulas |
| `UsuarioRepository.findByEmail` | `UsuarioService.autenticar` (`POST /auth/login`) | Acha o usuário que está fazendo login |
| `UsuarioRepository.existsByEmail` | `UsuarioService.registrarAluno` (`POST /usuarios`) e criação do admin inicial | Impede e-mail repetido |

### Consultas com `@Query` (JPQL escrito à mão)

| Método | Onde é usado | Por que o nome do método não daria conta |
|--------|--------------|------------------------------------------|
| `InscricaoRepository.buscarAlunoIdsPorAula` | `AulaService.atualizar` (`PUT /aulas/{id}`) | Devolve só os ids dos alunos inscritos (`select i.aluno.id ...`), e não as inscrições inteiras. Com esses ids a remarcação confere a agenda de cada aluno. Um método derivado sempre devolve a entidade. |
| `AulaRepository.buscarPorIdParaAtualizar` | `InscricaoService.inscrever`, `AulaService.atualizar` e `AulaService.excluir` | Busca a aula já travando a linha no banco (`@Lock`), para duas inscrições ao mesmo tempo não ocuparem a última vaga. O `@Query` deixa explícito qual registro é travado. |
| `UsuarioRepository.buscarPorIdParaAtualizar` | `InscricaoService.inscrever` e `AulaService.atualizar` | Trava o aluno durante a inscrição, para ele não se inscrever em duas aulas do mesmo horário em requisições simultâneas. |

## Estrutura

```
com.academia
├── aula        entidade e relacionamentos, DTOs, CRUD, consultas e regra de horário
├── config      Swagger com botão Authorize, redirecionamento da raiz e relógio do sistema
├── erro        tratador global, resposta de erro padrão e exceções
├── inscricao   inscrições e cancelamento do aluno logado
├── instrutor   entidade, DTOs, CRUD e bloqueio de exclusão com aulas
├── modalidade  entidade, DTOs, CRUD e regra 4 (exclusão com aulas futuras)
├── seguranca   SecurityConfig, filtro JWT, TokenService, UsuarioLogado e respostas 401/403
└── usuario     entidade Usuario, papéis, cadastro de alunos, login e administrador inicial
```

## Infraestrutura reaproveitada do projeto da aula

Como permitido no enunciado, estas partes de infraestrutura seguem a estrutura do projeto desenvolvido em aula, adaptadas para este domínio:

- **Configuração de segurança** (`seguranca/SecurityConfig`): API stateless, BCrypt e filtro JWT antes do filtro padrão do Spring Security.
- **Serviço de token** (`seguranca/TokenService`): geração e validação do JWT.
- **Filtro** (`seguranca/FiltroJwt`): lê o `Authorization: Bearer` e autentica o usuário da requisição.
- **Tratador de erros** (`erro/TratadorDeErros`): `@RestControllerAdvice` que converte exceções em respostas HTTP.

O modelo de dados, as regras de negócio e as consultas são do grupo.
