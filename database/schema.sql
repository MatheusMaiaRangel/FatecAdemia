-- Criação do banco da FatecAdemia (MySQL 8).
-- Espelha as entidades JPA. A aplicação também cria as tabelas sozinha (ddl-auto=update);
-- este script serve para criar o banco do zero, sem depender da primeira subida.
--
-- Uso: mysql -u root -p < database/schema.sql

CREATE DATABASE IF NOT EXISTS fatecademia
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fatecademia;

CREATE TABLE IF NOT EXISTS usuarios (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    nome   VARCHAR(100) NOT NULL,
    email  VARCHAR(150) NOT NULL,
    senha  VARCHAR(60)  NOT NULL COMMENT 'hash BCrypt',
    papel  VARCHAR(20)  NOT NULL COMMENT 'ADMIN ou ALUNO',
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('ADMIN', 'ALUNO'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS modalidades (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    nome      VARCHAR(100) NOT NULL,
    descricao VARCHAR(500) NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS instrutores (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nome          VARCHAR(100) NOT NULL,
    especialidade VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- Aula é o lado dono das relações com modalidade e instrutor (guarda as chaves estrangeiras).
CREATE TABLE IF NOT EXISTS aulas (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    horario       DATETIME(6) NOT NULL,
    capacidade    INT         NOT NULL,
    modalidade_id BIGINT      NOT NULL,
    instrutor_id  BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_aula_instrutor_horario UNIQUE (instrutor_id, horario),
    CONSTRAINT fk_aula_modalidade FOREIGN KEY (modalidade_id) REFERENCES modalidades (id),
    CONSTRAINT fk_aula_instrutor FOREIGN KEY (instrutor_id) REFERENCES instrutores (id)
) ENGINE = InnoDB;

-- Inscrição liga o aluno à aula; um aluno não se inscreve duas vezes na mesma aula.
CREATE TABLE IF NOT EXISTS inscricoes (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    aluno_id  BIGINT      NOT NULL,
    aula_id   BIGINT      NOT NULL,
    criada_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inscricao_aluno_aula UNIQUE (aluno_id, aula_id),
    CONSTRAINT fk_inscricao_aluno FOREIGN KEY (aluno_id) REFERENCES usuarios (id),
    CONSTRAINT fk_inscricao_aula FOREIGN KEY (aula_id) REFERENCES aulas (id)
) ENGINE = InnoDB;
