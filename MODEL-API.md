# Cadebot Model API

OpenAI-compatible. Point any OpenAI client's `base_url` at these.

| Service | Base URL |
|---|---|
| Language model | `https://llm.cadebot.ba-ailab.com` |
| Speech → text | `https://stt.cadebot.ba-ailab.com` |
| Ordering & payment | `https://cadebot.ba-ailab.com` |

---

## Authentication

Both model endpoints, every request:

```
Authorization: Bearer <api-key>
```

Key issued separately. `cadebot.ba-ailab.com` requires no key.

| | No / bad key |
|---|---|
| Language model | `401` |
| Speech | `403` `{"detail":"Not authenticated"}` |

---

## Language model

### `GET /v1/models`

```json
{"data":[{"id":"cadebot-3b"},{"id":"cadebot"}]}
```

| `model` | |
|---|---|
| `cadebot` | fine-tuned — use this |
| `cadebot-3b` | untuned base |

### `POST /v1/chat/completions`

```bash
curl https://llm.cadebot.ba-ailab.com/v1/chat/completions \
  -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "cadebot",
    "messages": [{"role":"user","content":"Cho tôi hai ly cà phê sữa đá size lớn"}],
    "temperature": 0.3,
    "max_tokens": 300
  }'
```

```json
{"id":"chatcmpl-…","model":"cadebot",
 "choices":[{"finish_reason":"stop",
             "message":{"role":"assistant","content":"<JSON string>"}}],
 "usage":{"prompt_tokens":39,"completion_tokens":169,"total_tokens":208}}
```

`message.content` is a **string containing JSON**. Parse it again:

```json
{"intent": "ADD_TO_CART_DRAFT",
 "confidence": 0.97,
 "answerText": "Cadebot đã thêm vào giỏ hàng:\n• 2x Cà Phê Sữa Đá — Size L",
 "spokenText": "Đã thêm 2 ly Cà Phê Sữa Đá size L vào giỏ.",
 "recommendedItems": [],
 "draftCartItems": [{"menuItemId":"ML_LATTE_M","quantity":2,
                     "options":{"size":"L","sugar":"70%","ice":"normal"}}],
 "requiresHumanSupport": false,
 "sourceIds": ["menu:ML_LATTE_M"]}
```

| Field | Use |
|---|---|
| `intent` | `MENU_QA` · `RECOMMENDATION` · `ADD_TO_CART_DRAFT` · `PROMOTION_QA` · `CALL_STAFF` · `FALLBACK` |
| `answerText` | display on screen |
| `spokenText` | pass to TTS |
| `draftCartItems` | add to cart |
| `recommendedItems` | suggest to customer |
| `requiresHumanSupport` | `true` → escalate to staff |
| `sourceIds` | knowledge entries used |

Parsing: take the span from the first `{` to the last `}` — output is sometimes
wrapped in prose.

---

## Speech → text

### `POST /v1/audio/transcriptions`

```bash
curl https://stt.cadebot.ba-ailab.com/v1/audio/transcriptions \
  -H "Authorization: Bearer $KEY" \
  -F "file=@recording.m4a" \
  -F "model=vinai/PhoWhisper-large-ct2" \
  -F "language=vi"
```

```json
{"text": "cho tôi hai ly cà phê sữa đá"}
```

| Field | Required | |
|---|---|---|
| `file` | yes | `m4a`, `wav`, `mp3` |
| `model` | yes | send explicitly |
| `language` | no | `vi` |
| `response_format` | no | `json` |

| `model` | |
|---|---|
| `vinai/PhoWhisper-large-ct2` | Vietnamese — use this |
| `Systran/faster-whisper-large-v3` | other languages |

- Output is lowercase, unpunctuated.
- First request after idle: ~35 s. Subsequent: ~0.7 s. Set client timeout ≥ 60 s.

---

## Ordering API — `POST /chat`

Same response schema, no key required.

```bash
curl https://cadebot.ba-ailab.com/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"Cho tôi hai ly cà phê sữa đá size lớn","history":[]}'
```

```json
{"response": "<JSON string, schema as above>"}
```

Adds to the raw model: menu and FAQ context, `menuItemId` validation against the
live menu, and `FALLBACK` when an answer has no source. Use this for the robot.

Other endpoints: `GET /health`, `GET /api/pricing`,
`POST /api/orders/create`, `GET /api/orders/{code}/status`,
`GET /api/orders/{code}`, `POST /stt`.

---

## Text → speech

No service. Use Android `TextToSpeech` on-device with `Locale("vi","VN")`.
Pass `spokenText`.

---

## Errors

| Code | Meaning |
|---|---|
| `401` / `403` | key missing, wrong or malformed |
| `404` | unknown `model` |
| `422` | malformed body or missing field |
| `500` | generation or transcription failed — retry once |

```json
{"error":{"message":"The model `nope` does not exist.","type":"NotFoundError","param":"model"}}
```

---

## Android 5.1.1

The device trust store cannot validate these certificates; TLS fails at
handshake. Bundle ISRG Root X1 + X2 and install a composite trust manager — see
`TlsCompat.kt` in the `app` repo. `network_security_config.xml` has no effect on
API 22.
