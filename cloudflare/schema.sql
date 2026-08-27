CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  phone TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  about TEXT DEFAULT 'Hai, saya menggunakan AnhChat.',
  avatar_url TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sender_id INTEGER NOT NULL,
  receiver_id INTEGER NOT NULL,
  type TEXT NOT NULL DEFAULT 'text',
  body TEXT,
  media_url TEXT,
  delivered_at TEXT,
  read_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(sender_id) REFERENCES users(id),
  FOREIGN KEY(receiver_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_users_phone
ON users(phone);

CREATE INDEX IF NOT EXISTS idx_messages_sender_receiver
ON messages(sender_id, receiver_id);

CREATE INDEX IF NOT EXISTS idx_messages_receiver_sender
ON messages(receiver_id, sender_id);
