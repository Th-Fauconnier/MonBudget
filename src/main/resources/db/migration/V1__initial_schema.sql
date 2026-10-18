-- 1. Livres thématiques
CREATE TABLE book (
                      id              INTEGER PRIMARY KEY AUTOINCREMENT,
                      name            TEXT NOT NULL,
                      description     TEXT,
                      currency        TEXT NOT NULL DEFAULT 'EUR',
                      initial_balance INTEGER NOT NULL DEFAULT 0,
                      color_hex       TEXT DEFAULT '#3498db',
                      icon_name       TEXT DEFAULT 'wallet',
                      is_archived     INTEGER NOT NULL DEFAULT 0 CHECK (is_archived IN (0, 1)),
                      created_at      TEXT NOT NULL DEFAULT (DATETIME('now')),
                      updated_at      TEXT NOT NULL DEFAULT (DATETIME('now'))
);

-- 2. Catégories
CREATE TABLE category (
                          id          INTEGER PRIMARY KEY AUTOINCREMENT,
                          parent_id   INTEGER,
                          name        TEXT NOT NULL,
                          type        TEXT NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
                          color_hex   TEXT,
                          icon_name   TEXT,
                          is_system   INTEGER NOT NULL DEFAULT 0 CHECK (is_system IN (0, 1)),
                          FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
);

-- 3. Modes de règlement
CREATE TABLE payment_method (
                                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                                name    TEXT NOT NULL UNIQUE
);

-- 4. Règles de récurrence
CREATE TABLE recurrence_rule (
                                 id           INTEGER PRIMARY KEY AUTOINCREMENT,
                                 frequency    TEXT NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
                                 interval     INTEGER NOT NULL DEFAULT 1 CHECK (interval > 0),
                                 start_date   TEXT NOT NULL,
                                 end_date     TEXT,
                                 day_of_month INTEGER CHECK (day_of_month BETWEEN 1 AND 31)
);

-- 5. Opérations
CREATE TABLE operation (
                           id                    INTEGER PRIMARY KEY AUTOINCREMENT,
                           book_id               INTEGER NOT NULL,
                           category_id           INTEGER,
                           payment_method_id     INTEGER,
                           recurrence_rule_id    INTEGER,
                           label                 TEXT NOT NULL,
                           amount                INTEGER NOT NULL,
                           operation_type        TEXT NOT NULL CHECK (operation_type IN ('EXPENSE', 'INCOME', 'TRANSFER')),
                           status                TEXT NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'SETTLED')),
                           operation_date        TEXT NOT NULL,
                           settled_at            TEXT,
                           notes                 TEXT,
                           transfer_operation_id INTEGER,
                           created_at            TEXT NOT NULL DEFAULT (DATETIME('now')),
                           updated_at            TEXT NOT NULL DEFAULT (DATETIME('now')),
                           FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
                           FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
                           FOREIGN KEY (payment_method_id) REFERENCES payment_method(id) ON DELETE SET NULL,
                           FOREIGN KEY (recurrence_rule_id) REFERENCES recurrence_rule(id) ON DELETE SET NULL,
                           FOREIGN KEY (transfer_operation_id) REFERENCES operation(id) ON DELETE SET NULL
);

-- Index
CREATE INDEX idx_operation_book_date ON operation(book_id, operation_date);
CREATE INDEX idx_operation_status ON operation(status);