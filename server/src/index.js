import "dotenv/config";
import crypto from "crypto";
import http from "http";
import express from "express";
import cors from "cors";
import jwt from "jsonwebtoken";
import pg from "pg";
import rateLimit from "express-rate-limit";
import { Server } from "socket.io";

const { Pool } = pg;

const PORT = Number(process.env.PORT || 4000);
const DATABASE_URL = process.env.DATABASE_URL;
const JWT_SECRET = process.env.JWT_SECRET || "CHANGE_ME";
const OTP_MODE = process.env.OTP_MODE || "dev";

if (!DATABASE_URL) {
  console.error("DATABASE_URL belum diatur");
  process.exit(1);
}

const pool = new Pool({
  connectionString: DATABASE_URL
});

function normalizePhone(value = "") {
  let phone = String(value)
    .replace(/[^\d+]/g, "")
    .trim();

  if (phone.startsWith("+62")) {
    // sudah benar
  } else if (phone.startsWith("62")) {
    phone = `+${phone}`;
  } else if (phone.startsWith("0")) {
    phone = `+62${phone.slice(1)}`;
  } else {
    return null;
  }

  if (!/^\+62\d{8,13}$/.test(phone)) {
    return null;
  }

  return phone;
}

function otpHash(phone, code) {
  return crypto
    .createHash("sha256")
    .update(`${phone}:${code}:${JWT_SECRET}`)
    .digest("hex");
}

function makeToken(user) {
  return jwt.sign(
    {
      userId: String(user.id),
      phone: user.phone
    },
    JWT_SECRET,
    { expiresIn: "30d" }
  );
}

async function initDb() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id BIGSERIAL PRIMARY KEY,
      phone VARCHAR(20) UNIQUE NOT NULL,
      name VARCHAR(100) NOT NULL,
      avatar_url TEXT,
      about VARCHAR(255) DEFAULT 'Hai, saya menggunakan AnhChat.',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS otps (
      phone VARCHAR(20) PRIMARY KEY,
      code_hash TEXT NOT NULL,
      expires_at TIMESTAMPTZ NOT NULL,
      attempts INTEGER NOT NULL DEFAULT 0,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS messages (
      id BIGSERIAL PRIMARY KEY,
      sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      receiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      type VARCHAR(30) NOT NULL DEFAULT 'text',
      body TEXT,
      media_url TEXT,
      reply_to BIGINT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      delivered_at TIMESTAMPTZ,
      read_at TIMESTAMPTZ
    );

    CREATE INDEX IF NOT EXISTS idx_messages_sender
      ON messages(sender_id);

    CREATE INDEX IF NOT EXISTS idx_messages_receiver
      ON messages(receiver_id);

    CREATE INDEX IF NOT EXISTS idx_messages_conversation
      ON messages(sender_id, receiver_id, created_at);
  `);

  console.log("Database AnhChat siap");
}

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
  cors: {
    origin: "*"
  }
});

app.use(cors());
app.use(express.json({ limit: "10mb" }));

const otpLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: 5,
  standardHeaders: true,
  legacyHeaders: false
});

function auth(req, res, next) {
  const header = req.headers.authorization || "";

  if (!header.startsWith("Bearer ")) {
    return res.status(401).json({
      ok: false,
      message: "Token diperlukan"
    });
  }

  try {
    req.user = jwt.verify(
      header.slice(7),
      JWT_SECRET
    );

    next();
  } catch {
    return res.status(401).json({
      ok: false,
      message: "Token tidak valid"
    });
  }
}

app.get("/", (req, res) => {
  res.json({
    ok: true,
    app: "AnhChat Server",
    version: "1.0.0"
  });
});

app.get("/health", async (req, res) => {
  await pool.query("SELECT 1");

  res.json({
    ok: true,
    database: "connected"
  });
});

app.post(
  "/auth/request-otp",
  otpLimiter,
  async (req, res) => {
    try {
      const phone = normalizePhone(req.body.phone);

      if (!phone) {
        return res.status(400).json({
          ok: false,
          message: "Nomor HP tidak valid"
        });
      }

      const code = String(
        crypto.randomInt(100000, 1000000)
      );

      const hash = otpHash(phone, code);

      await pool.query(
        `
        INSERT INTO otps (
          phone,
          code_hash,
          expires_at,
          attempts
        )
        VALUES (
          $1,
          $2,
          NOW() + INTERVAL '5 minutes',
          0
        )
        ON CONFLICT (phone)
        DO UPDATE SET
          code_hash = EXCLUDED.code_hash,
          expires_at = EXCLUDED.expires_at,
          attempts = 0,
          created_at = NOW()
        `,
        [phone, hash]
      );

      console.log(
        `[OTP] ${phone} -> ${code}`
      );

      const response = {
        ok: true,
        phone,
        message: "OTP dibuat",
        expiresInSeconds: 300
      };

      if (OTP_MODE === "dev") {
        response.debugOtp = code;
      }

      res.json(response);

    } catch (error) {
      console.error(error);

      res.status(500).json({
        ok: false,
        message: "Gagal membuat OTP"
      });
    }
  }
);

app.post(
  "/auth/verify-otp",
  async (req, res) => {
    const client = await pool.connect();

    try {
      const phone = normalizePhone(req.body.phone);
      const code = String(req.body.code || "");
      const name = String(
        req.body.name || ""
      ).trim();

      if (!phone || !/^\d{6}$/.test(code)) {
        return res.status(400).json({
          ok: false,
          message: "Nomor atau OTP tidak valid"
        });
      }

      const otpResult = await client.query(
        `
        SELECT *
        FROM otps
        WHERE phone = $1
        `,
        [phone]
      );

      const otp = otpResult.rows[0];

      if (!otp) {
        return res.status(400).json({
          ok: false,
          message: "OTP tidak ditemukan"
        });
      }

      if (new Date(otp.expires_at) < new Date()) {
        return res.status(400).json({
          ok: false,
          message: "OTP sudah kedaluwarsa"
        });
      }

      if (otp.attempts >= 5) {
        return res.status(429).json({
          ok: false,
          message: "Terlalu banyak percobaan OTP"
        });
      }

      const valid =
        otp.code_hash === otpHash(phone, code);

      if (!valid) {
        await client.query(
          `
          UPDATE otps
          SET attempts = attempts + 1
          WHERE phone = $1
          `,
          [phone]
        );

        return res.status(400).json({
          ok: false,
          message: "OTP salah"
        });
      }

      await client.query("BEGIN");

      const defaultName =
        name || `AnhChat ${phone.slice(-4)}`;

      const userResult = await client.query(
        `
        INSERT INTO users (
          phone,
          name
        )
        VALUES ($1, $2)
        ON CONFLICT (phone)
        DO UPDATE SET
          name = CASE
            WHEN EXCLUDED.name <> ''
            THEN EXCLUDED.name
            ELSE users.name
          END
        RETURNING *
        `,
        [phone, defaultName]
      );

      await client.query(
        "DELETE FROM otps WHERE phone = $1",
        [phone]
      );

      await client.query("COMMIT");

      const user = userResult.rows[0];

      res.json({
        ok: true,
        token: makeToken(user),
        user
      });

    } catch (error) {
      await client.query("ROLLBACK");

      console.error(error);

      res.status(500).json({
        ok: false,
        message: "Verifikasi OTP gagal"
      });

    } finally {
      client.release();
    }
  }
);

app.get("/me", auth, async (req, res) => {
  const result = await pool.query(
    `
    SELECT
      id,
      phone,
      name,
      avatar_url,
      about,
      created_at
    FROM users
    WHERE id = $1
    `,
    [req.user.userId]
  );

  res.json({
    ok: true,
    user: result.rows[0] || null
  });
});

app.get(
  "/users/by-phone/:phone",
  auth,
  async (req, res) => {
    const phone = normalizePhone(
      req.params.phone
    );

    if (!phone) {
      return res.status(400).json({
        ok: false,
        message: "Nomor tidak valid"
      });
    }

    const result = await pool.query(
      `
      SELECT
        id,
        phone,
        name,
        avatar_url,
        about
      FROM users
      WHERE phone = $1
      `,
      [phone]
    );

    res.json({
      ok: true,
      registered: result.rowCount > 0,
      user: result.rows[0] || null
    });
  }
);

app.get(
  "/messages/:phone",
  auth,
  async (req, res) => {
    const phone = normalizePhone(
      req.params.phone
    );

    if (!phone) {
      return res.status(400).json({
        ok: false,
        message: "Nomor tidak valid"
      });
    }

    const target = await pool.query(
      "SELECT id FROM users WHERE phone = $1",
      [phone]
    );

    if (!target.rowCount) {
      return res.status(404).json({
        ok: false,
        message: "Pengguna belum terdaftar"
      });
    }

    const targetId = target.rows[0].id;

    const result = await pool.query(
      `
      SELECT
        m.id,
        m.sender_id,
        m.receiver_id,
        m.type,
        m.body,
        m.media_url,
        m.reply_to,
        m.created_at,
        m.delivered_at,
        m.read_at
      FROM messages m
      WHERE
        (
          m.sender_id = $1
          AND m.receiver_id = $2
        )
        OR
        (
          m.sender_id = $2
          AND m.receiver_id = $1
        )
      ORDER BY m.created_at ASC
      LIMIT 500
      `,
      [req.user.userId, targetId]
    );

    res.json({
      ok: true,
      messages: result.rows
    });
  }
);

io.use((socket, next) => {
  try {
    const token =
      socket.handshake.auth?.token;

    if (!token) {
      return next(
        new Error("Token diperlukan")
      );
    }

    socket.user = jwt.verify(
      token,
      JWT_SECRET
    );

    next();

  } catch {
    next(
      new Error("Token tidak valid")
    );
  }
});

const onlineUsers = new Map();

io.on("connection", (socket) => {
  const userId = String(
    socket.user.userId
  );

  socket.join(`user:${userId}`);

  onlineUsers.set(userId, socket.id);

  socket.broadcast.emit(
    "presence:update",
    {
      userId,
      online: true
    }
  );

  socket.on(
    "typing",
    async (payload = {}) => {
      const phone = normalizePhone(
        payload.toPhone
      );

      if (!phone) return;

      const target = await pool.query(
        "SELECT id FROM users WHERE phone = $1",
        [phone]
      );

      if (!target.rowCount) return;

      io.to(
        `user:${target.rows[0].id}`
      ).emit(
        "typing",
        {
          fromUserId: userId,
          typing: Boolean(payload.typing)
        }
      );
    }
  );

  socket.on(
    "message:send",
    async (payload = {}, ack) => {
      try {
        const phone = normalizePhone(
          payload.toPhone
        );

        if (!phone) {
          throw new Error(
            "Nomor tujuan tidak valid"
          );
        }

        const targetResult =
          await pool.query(
            `
            SELECT id, phone, name
            FROM users
            WHERE phone = $1
            `,
            [phone]
          );

        if (!targetResult.rowCount) {
          throw new Error(
            "Nomor belum terdaftar di AnhChat"
          );
        }

        const target =
          targetResult.rows[0];

        const type =
          String(payload.type || "text");

        const body =
          payload.body == null
            ? null
            : String(payload.body);

        const mediaUrl =
          payload.mediaUrl == null
            ? null
            : String(payload.mediaUrl);

        if (
          type === "text" &&
          (!body || !body.trim())
        ) {
          throw new Error(
            "Pesan tidak boleh kosong"
          );
        }

        const result = await pool.query(
          `
          INSERT INTO messages (
            sender_id,
            receiver_id,
            type,
            body,
            media_url
          )
          VALUES (
            $1,
            $2,
            $3,
            $4,
            $5
          )
          RETURNING *
          `,
          [
            userId,
            target.id,
            type,
            body,
            mediaUrl
          ]
        );

        const message =
          result.rows[0];

        io.to(
          `user:${target.id}`
        ).emit(
          "message:new",
          message
        );

        socket.emit(
          "message:new",
          message
        );

        if (typeof ack === "function") {
          ack({
            ok: true,
            message
          });
        }

      } catch (error) {
        if (typeof ack === "function") {
          ack({
            ok: false,
            message: error.message
          });
        }
      }
    }
  );

  socket.on(
    "message:delivered",
    async ({ messageId } = {}) => {
      if (!messageId) return;

      const result = await pool.query(
        `
        UPDATE messages
        SET delivered_at =
          COALESCE(delivered_at, NOW())
        WHERE
          id = $1
          AND receiver_id = $2
        RETURNING sender_id, delivered_at
        `,
        [messageId, userId]
      );

      if (!result.rowCount) return;

      const row = result.rows[0];

      io.to(
        `user:${row.sender_id}`
      ).emit(
        "message:receipt",
        {
          messageId,
          deliveredAt:
            row.delivered_at,
          readAt: null
        }
      );
    }
  );

  socket.on(
    "message:read",
    async ({ messageId } = {}) => {
      if (!messageId) return;

      const result = await pool.query(
        `
        UPDATE messages
        SET
          delivered_at =
            COALESCE(delivered_at, NOW()),
          read_at =
            COALESCE(read_at, NOW())
        WHERE
          id = $1
          AND receiver_id = $2
        RETURNING
          sender_id,
          delivered_at,
          read_at
        `,
        [messageId, userId]
      );

      if (!result.rowCount) return;

      const row = result.rows[0];

      io.to(
        `user:${row.sender_id}`
      ).emit(
        "message:receipt",
        {
          messageId,
          deliveredAt:
            row.delivered_at,
          readAt:
            row.read_at
        }
      );
    }
  );

  socket.on("disconnect", () => {
    onlineUsers.delete(userId);

    socket.broadcast.emit(
      "presence:update",
      {
        userId,
        online: false
      }
    );
  });
});

initDb()
  .then(() => {
    server.listen(
      PORT,
      "0.0.0.0",
      () => {
        console.log(
          `AnhChat Server hidup di port ${PORT}`
        );
      }
    );
  })
  .catch((error) => {
    console.error(
      "Gagal memulai server:",
      error
    );

    process.exit(1);
  });
