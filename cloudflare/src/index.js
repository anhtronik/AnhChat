function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "access-control-allow-origin": "*",
      "access-control-allow-headers": "content-type, authorization",
      "access-control-allow-methods": "GET,POST,PUT,DELETE,OPTIONS"
    }
  });
}

function normalizePhone(value = "") {
  let phone = String(value)
    .replace(/[^\d+]/g, "")
    .trim();

  if (phone.startsWith("+62")) {
  } else if (phone.startsWith("62")) {
    phone = "+" + phone;
  } else if (phone.startsWith("0")) {
    phone = "+62" + phone.substring(1);
  } else {
    return null;
  }

  if (!/^\+62\d{8,13}$/.test(phone)) {
    return null;
  }

  return phone;
}

async function findUserByPhone(env, phone) {
  return await env.DB
    .prepare(`
      SELECT
        id,
        phone,
        name,
        about,
        avatar_url,
        created_at
      FROM users
      WHERE phone = ?
    `)
    .bind(phone)
    .first();
}

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);

      if (request.method === "OPTIONS") {
        return json({ ok: true });
      }

      if (
        request.method === "GET" &&
        url.pathname === "/"
      ) {
        return json({
          ok: true,
          app: "AnhChat",
          backend: "Cloudflare Workers",
          version: "1.0.0"
        });
      }

      if (
        request.method === "GET" &&
        url.pathname === "/health"
      ) {
        const result = await env.DB
          .prepare("SELECT 1 AS alive")
          .first();

        return json({
          ok: true,
          database: result?.alive === 1
            ? "connected"
            : "error"
        });
      }

      if (
        request.method === "POST" &&
        url.pathname === "/users/register"
      ) {
        const body = await request.json();

        const phone = normalizePhone(body.phone);
        const name = String(body.name || "").trim();

        if (!phone) {
          return json({
            ok: false,
            message: "Nomor HP tidak valid"
          }, 400);
        }

        if (name.length < 2) {
          return json({
            ok: false,
            message: "Nama minimal 2 karakter"
          }, 400);
        }

        await env.DB.prepare(`
          INSERT INTO users (
            phone,
            name
          )
          VALUES (?, ?)
          ON CONFLICT(phone)
          DO UPDATE SET
            name = excluded.name
        `)
          .bind(phone, name)
          .run();

        const user =
          await findUserByPhone(env, phone);

        return json({
          ok: true,
          user
        });
      }

      if (
        request.method === "GET" &&
        url.pathname === "/users/find"
      ) {
        const phone =
          normalizePhone(
            url.searchParams.get("phone") || ""
          );

        if (!phone) {
          return json({
            ok: false,
            message: "Nomor HP tidak valid"
          }, 400);
        }

        const user =
          await findUserByPhone(env, phone);

        return json({
          ok: true,
          registered: Boolean(user),
          user: user || null
        });
      }

      if (
        request.method === "POST" &&
        url.pathname === "/messages/send"
      ) {
        const body = await request.json();

        const fromPhone =
          normalizePhone(body.fromPhone);

        const toPhone =
          normalizePhone(body.toPhone);

        const message =
          String(body.message || "").trim();

        if (!fromPhone || !toPhone) {
          return json({
            ok: false,
            message: "Nomor pengirim/tujuan tidak valid"
          }, 400);
        }

        if (!message) {
          return json({
            ok: false,
            message: "Pesan kosong"
          }, 400);
        }

        const sender =
          await findUserByPhone(env, fromPhone);

        const receiver =
          await findUserByPhone(env, toPhone);

        if (!sender) {
          return json({
            ok: false,
            message: "Nomor pengirim belum terdaftar"
          }, 404);
        }

        if (!receiver) {
          return json({
            ok: false,
            message: "Nomor tujuan belum menggunakan AnhChat"
          }, 404);
        }

        const result =
          await env.DB.prepare(`
            INSERT INTO messages (
              sender_id,
              receiver_id,
              type,
              body
            )
            VALUES (?, ?, 'text', ?)
          `)
            .bind(
              sender.id,
              receiver.id,
              message
            )
            .run();

        return json({
          ok: true,
          messageId: result.meta.last_row_id
        });
      }

      if (
        request.method === "GET" &&
        url.pathname === "/messages"
      ) {
        const me =
          normalizePhone(
            url.searchParams.get("me") || ""
          );

        const withPhone =
          normalizePhone(
            url.searchParams.get("with") || ""
          );

        if (!me || !withPhone) {
          return json({
            ok: false,
            message: "Nomor tidak valid"
          }, 400);
        }

        const userA =
          await findUserByPhone(env, me);

        const userB =
          await findUserByPhone(env, withPhone);

        if (!userA || !userB) {
          return json({
            ok: false,
            message: "Pengguna tidak ditemukan"
          }, 404);
        }

        const data =
          await env.DB.prepare(`
            SELECT
              m.id,
              su.phone AS sender_phone,
              ru.phone AS receiver_phone,
              m.type,
              m.body,
              m.media_url,
              m.delivered_at,
              m.read_at,
              m.created_at
            FROM messages m
            JOIN users su
              ON su.id = m.sender_id
            JOIN users ru
              ON ru.id = m.receiver_id
            WHERE
              (
                m.sender_id = ?
                AND m.receiver_id = ?
              )
              OR
              (
                m.sender_id = ?
                AND m.receiver_id = ?
              )
            ORDER BY m.id ASC
            LIMIT 500
          `)
            .bind(
              userA.id,
              userB.id,
              userB.id,
              userA.id
            )
            .all();

        return json({
          ok: true,
          messages: data.results || []
        });
      }

      return json({
        ok: false,
        message: "Endpoint tidak ditemukan"
      }, 404);

    } catch (error) {
      return json({
        ok: false,
        message: String(
          error?.message || error
        )
      }, 500);
    }
  }
};
