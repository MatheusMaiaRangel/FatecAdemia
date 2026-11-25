# Aulas

## Escopo da branch `Aula`

- `Aula`: entidade com horário, capacidade, modalidade, instrutor e inscrições.
- `AulaRepository`: os três query methods derivados do nome previstos no plano.
- `AulaService`: CRUD, consulta e regras de integridade.
- `AulaController`: seis operações documentadas com `@Operation`.
- DTOs com `@NotNull`, `@Future` e `@Positive`; paginação e ordenação validadas.
- Authorize com JWT: aproveita `SwaggerConfig` e as permissões de `SecurityConfig` existentes.
- Testes de integração com H2, JPA, autenticação JWT e requisições HTTP via MockMvc.

## Integração com o grupo

`Aula` se relaciona com `Modalidade`, `Instrutor` e as inscrições assim:

| Entidade | Campos | Relação inversa |
| --- | --- | --- |
| `Modalidade` | `id`, `nome`, `descricao` | `@OneToMany(mappedBy = "modalidade")` |
| `Instrutor` | `id`, `nome`, `especialidade` | `@OneToMany(mappedBy = "instrutor")` |
| `Aula` | `id`, `horario`, `capacidade`, `modalidade`, `instrutor` | `@OneToMany(mappedBy = "aula")` para inscrições |

`Aula` é o lado dono das relações com modalidade e instrutor, usando `@ManyToOne`
e as chaves `modalidade_id` e `instrutor_id`. As relações são obrigatórias.
As respostas usam DTOs, evitando expor coleções e referências recursivas.

`getHorario()` e `getCapacidade()` continuam disponíveis para `InscricaoService`.
O construtor de `Aula` passou a exigir também modalidade e instrutor; os dados de
teste das inscrições foram atualizados para respeitar essas relações obrigatórias.
Não foi alterado o comportamento dos endpoints de inscrição.

## Preparar dados para experimentar

Siga o README para configurar MySQL, variáveis de ambiente, administrador e login.
Em um banco de desenvolvimento novo, inicie a aplicação para criar as tabelas.
Se preferir cadastrar o catálogo direto pelo MySQL:

```sql
INSERT INTO modalidades (nome, descricao)
VALUES ('Pilates', 'Fortalecimento e postura');

INSERT INTO instrutores (nome, especialidade)
VALUES ('Carla', 'Pilates');

SELECT id, nome FROM modalidades;
SELECT id, nome FROM instrutores;
```

Use os IDs realmente retornados no corpo da requisição de aula. Esses dados são
exemplos locais; não há carga automática de dados ou credenciais nesta branch.

### Banco existente com aulas anteriores

As aulas antigas da base não tinham modalidade nem instrutor. Em um banco já
preenchido, associe cada aula aos registros corretos antes de exigir as novas
chaves obrigatórias. Faça isso com a aplicação parada e uma cópia do banco:

1. Cadastre as modalidades e os instrutores verdadeiros, usando a estrutura acima.
2. Se as colunas ainda não existirem, adicione `modalidade_id BIGINT NULL` e
   `instrutor_id BIGINT NULL` à tabela `aulas`.
3. Preencha os dois IDs para cada aula existente, conforme seus dados reais.
4. Confirme que não restam IDs nulos ou inexistentes e que não há duplicatas do
   par `(instrutor_id, horario)`.
5. Torne as duas colunas `NOT NULL` e inicie a aplicação para atualizar o esquema.
   Verifique as FKs e a restrição `uk_aula_instrutor_horario` no MySQL.

Não é necessário apagar aulas ou inscrições para fazer essa adaptação.

## Endpoints

| Método | Caminho | Acesso | Resultado |
| --- | --- | --- | --- |
| POST | `/aulas` | ADMIN | 201, corpo da aula e cabeçalho `Location` |
| GET | `/aulas` | ADMIN ou ALUNO | 200, página de aulas |
| GET | `/aulas/proximas` | ADMIN ou ALUNO | 200, página de aulas futuras |
| GET | `/aulas/{id}` | ADMIN ou ALUNO | 200 ou 404 |
| PUT | `/aulas/{id}` | ADMIN | 200, substituição dos dados |
| DELETE | `/aulas/{id}` | ADMIN | 204, somente se não houver inscrições |

Corpo de criação e atualização (escolha uma data futura e IDs existentes):

```json
{
  "horario": "2027-06-15T18:00:00",
  "capacidade": 20,
  "modalidadeId": 1,
  "instrutorId": 1
}
```

O horário é local à academia, como no restante do projeto. Sem token, a API
responde 401; ALUNO tentando criar, editar ou excluir recebe 403.

Exemplos de listagem:

```text
GET /aulas?page=0&size=10&sort=horario,asc
GET /aulas?modalidade=pila&page=0&size=5&sort=capacidade,desc
GET /aulas?sort=modalidade.nome,asc
GET /aulas/proximas?page=0&size=5
```

`page` começa em zero e `size` aceita 1 a 100. `sort` aceita um par
`campo,direcao`, com direção `asc` ou `desc` e campo `id`, `horario`,
`capacidade`, `modalidade.nome` ou `instrutor.nome`. O ID desempata os demais
campos para manter a paginação consistente. A listagem de próximas aulas sempre
ordena por horário crescente e desempata por ID.

O filtro de modalidade ignora maiúsculas e espaços nas extremidades. Um filtro
sem resultados devolve uma página vazia. Filtro em branco, paginação inválida e
ordenação desconhecida devolvem 400. O formato da página é:

```json
{
  "conteudo": [],
  "pagina": 0,
  "tamanho": 10,
  "totalElementos": 0,
  "totalPaginas": 0
}
```

## Consultas para explicar na apresentação

O plano chama o campo de `dataHora`; a base já utilizava `horario`. Os nomes dos
métodos acompanham o atributo Java real para que o Spring Data derive as consultas.

| Consulta | Pergunta do domínio | Uso |
| --- | --- | --- |
| `findByHorarioAfterOrderByHorarioAsc` | Quais são as próximas aulas? | `/aulas/proximas`; exclui aulas que já começaram e ordena por início |
| `findByModalidadeNomeContainingIgnoreCase` | Quais aulas existem de determinada modalidade? | Filtro parcial de modalidade em `/aulas`, com paginação e ordenação |
| `findByInstrutorIdAndHorarioBetween` | Quais aulas esse instrutor tem no intervalo? | Consulta a agenda do dia durante a criação e edição para verificar o conflito |

A quarta consulta é `findByAlunoIdOrderByCriadaEmDesc`, das inscrições: retorna
somente as inscrições do aluno autenticado e é usada em `GET /inscricoes`.

## Regras e erros

- Mesmo instrutor no mesmo início: 409, na criação e na edição. A própria aula é
  desconsiderada ao editar. Outra aula do mesmo instrutor com início diferente e
  outro instrutor com o mesmo início são permitidos.
- O modelo do plano registra somente o início. Não é presumida uma duração ou
  um intervalo de descanso. Detectar sobreposição de durações exigiria acrescentar
  um horário de término ou uma duração ao modelo e ajustar a regra com o grupo.
- A restrição única do banco protege o par instrutor/horário também diante de
  duas gravações concorrentes; uma violação vira 409 pelo tratador existente.
- Reduzir capacidade abaixo do total de inscritos: 409.
- Excluir aula que possui inscrições: 409. Não há exclusão em cascata de inscrições.
- Aula, modalidade ou instrutor inexistente: 404.
- Campo ausente, horário passado ou capacidade/ID não positivo: 400.

Toda regra de negócio está no Service. O Controller recebe os DTOs, valida os
parâmetros de listagem e monta as respostas HTTP.

## Validação

Com Java 21, execute na raiz do projeto:

```powershell
.\mvnw.cmd verify
```

Os testes usam o perfil `test` com H2 em memória, sem depender do MySQL local.
A suíte verifica CRUD, Location, relacionamentos, as três consultas, paginação,
ordenação, entradas inválidas, conflito na criação/edição/troca de instrutor,
proteção de capacidade e inscrições, 401/403/404/409 e documentação OpenAPI/JWT.
Os testes existentes de inscrição também continuam no `verify`.
O JaCoCo deve respeitar os limites de 80% de linhas e ramificações já configurados.

Roteiro rápido no Swagger: login ADMIN, Authorize com o token, criar uma aula,
tentar outra com o mesmo instrutor e início (409), editar a primeira sem mudar
o início (200), fazer login ALUNO, consultar aulas (200) e tentar criar (403).
