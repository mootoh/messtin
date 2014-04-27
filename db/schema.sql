CREATE TABLE book (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    pages INTEGER NOT NULL,
    gd_id TEXT,
    cover_img_id TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_opened_at DATETIME
);
