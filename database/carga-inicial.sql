-- Carga inicial do catálogo da FatecAdemia: modalidades, instrutores e aulas.
-- Rode depois do schema.sql, em um banco vazio:
--   mysql -u root -p < database/carga-inicial.sql
--
-- Usuários não entram aqui: o ADMIN é criado pela aplicação a partir das variáveis ADMIN_*,
-- e os alunos são cadastrados pelo ADMIN em POST /usuarios (a senha precisa do BCrypt).
-- As aulas são marcadas a partir de amanhã, para aparecerem em /aulas/proximas.

USE fatecademia;

INSERT INTO modalidades (nome, descricao) VALUES
    ('Pilates',    'Fortalecimento do core, postura e flexibilidade'),
    ('Yoga',       'Respiração, equilíbrio e alongamento'),
    ('Spinning',   'Treino aeróbico em bicicleta ergométrica'),
    ('Funcional',  'Circuito com movimentos do dia a dia');

INSERT INTO instrutores (nome, especialidade) VALUES
    ('Carla Souza',   'Pilates'),
    ('Diego Martins', 'Yoga'),
    ('Ana Lima',      'Spinning'),
    ('Rafael Costa',  'Funcional');

-- Os IDs são buscados pelo nome, então a carga funciona mesmo se o AUTO_INCREMENT não começar em 1.
INSERT INTO aulas (horario, capacidade, modalidade_id, instrutor_id)
SELECT TIMESTAMP(CURRENT_DATE + INTERVAL a.dias DAY, a.hora), a.capacidade, m.id, i.id
FROM (
    SELECT 1 AS dias, '07:00:00' AS hora, 15 AS capacidade, 'Pilates'   AS modalidade, 'Carla Souza'   AS instrutor
    UNION ALL SELECT 1, '18:00:00', 20, 'Yoga',      'Diego Martins'
    UNION ALL SELECT 2, '19:00:00', 25, 'Spinning',  'Ana Lima'
    UNION ALL SELECT 3, '07:00:00', 15, 'Pilates',   'Carla Souza'
    UNION ALL SELECT 3, '18:30:00', 30, 'Funcional', 'Rafael Costa'
    UNION ALL SELECT 4, '08:00:00',  2, 'Yoga',      'Diego Martins'
) AS a
JOIN modalidades m ON m.nome = a.modalidade
JOIN instrutores i ON i.nome = a.instrutor;
