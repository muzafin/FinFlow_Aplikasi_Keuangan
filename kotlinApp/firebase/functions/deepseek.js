const { HttpsError } = require('firebase-functions/v2/https');
const { z } = require('zod');

// Adapter DeepSeek (API kompatibel OpenAI) dengan bentuk subset klien Anthropic yang dipakai ai.js:
// messages.parse({ model, system, messages, output_config.format, max_tokens }) -> { parsed_output, stop_reason }
// beta.messages.create({ model, system, messages, max_tokens }) -> { content: [{ type: 'text', text }], stop_reason }
// Kunci API hanya dibaca dari Secret Manager oleh index.js; tidak pernah dikirim ke klien.

const BASE_URL = 'https://api.deepseek.com';

class DeepSeekError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

function systemText(system) {
  if (!system) return '';
  return Array.isArray(system) ? system.map(block => block.text || '').filter(Boolean).join('\n\n') : String(system);
}

function toChatMessages(system, messages) {
  const out = [];
  const sys = systemText(system);
  if (sys) out.push({ role: 'system', content: sys });
  for (const message of messages) {
    if (typeof message.content === 'string') { out.push({ role: message.role, content: message.content }); continue; }
    if (message.content.some(part => part.type === 'image')) {
      // Model DeepSeek tidak membaca gambar; ai.js mengembalikan kuota bila panggilan gagal.
      throw new HttpsError('failed-precondition', 'AI Scan belum didukung penyedia AI saat ini. Gunakan AI Chat atau catat manual.');
    }
    out.push({ role: message.role, content: message.content.filter(part => part.type === 'text').map(part => part.text).join('\n') });
  }
  return out;
}

function stopReason(choice) {
  if (choice?.finish_reason === 'length') return 'max_tokens';
  if (choice?.finish_reason === 'content_filter') return 'refusal';
  return 'end_turn';
}

function createDeepSeekClient({ apiKey, fetchImpl = fetch, timeoutMs = 90_000, baseUrl = BASE_URL }) {
  async function complete(body) {
    if (!apiKey) throw new DeepSeekError(401, 'missing key');
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetchImpl(`${baseUrl}/chat/completions`, {
        method: 'POST', signal: controller.signal,
        headers: { 'content-type': 'application/json', authorization: `Bearer ${apiKey}` },
        body: JSON.stringify(body),
      });
      if (!response.ok) throw new DeepSeekError(response.status, `DeepSeek HTTP ${response.status}`);
      return await response.json();
    } catch (error) {
      if (error instanceof DeepSeekError || error instanceof HttpsError) throw error;
      throw new DeepSeekError(503, error?.name === 'AbortError' ? 'timeout' : 'network');
    } finally { clearTimeout(timer); }
  }

  async function parse({ model, system, messages, max_tokens, output_config }) {
    const schema = output_config?.format?.schema;
    const zodSchema = output_config?.format?.zod;
    const instruction = `Balas HANYA dengan satu objek JSON valid (tanpa teks lain) yang sesuai JSON Schema berikut:\n${JSON.stringify(schema)}`;
    const json = await complete({
      model, max_tokens: Math.min(max_tokens || 4000, 8000), temperature: 0,
      response_format: { type: 'json_object' },
      messages: toChatMessages([...(Array.isArray(system) ? system : [{ type: 'text', text: systemText(system) }]), { type: 'text', text: instruction }], messages),
    });
    const choice = json.choices?.[0];
    let parsed = null;
    try { parsed = zodSchema.parse(JSON.parse(choice?.message?.content || '')); } catch { parsed = null; }
    return { parsed_output: parsed, stop_reason: stopReason(choice) };
  }

  async function create({ model, system, messages, max_tokens }) {
    const json = await complete({ model, max_tokens: Math.min(max_tokens || 4000, 8000), temperature: 0.7, messages: toChatMessages(system, messages) });
    const choice = json.choices?.[0];
    return { content: [{ type: 'text', text: choice?.message?.content || '' }], stop_reason: stopReason(choice) };
  }

  return { messages: { parse, create }, beta: { messages: { create } } };
}

// Pengganti zodOutputFormat Anthropic: membawa JSON Schema (untuk prompt) dan skema zod (untuk validasi).
function jsonOutputFormat(zodSchema) {
  return { type: 'json_schema', schema: z.toJSONSchema(zodSchema), zod: zodSchema };
}

// Pemetaan galat penyedia ke HttpsError yang ramah pengguna.
function mapDeepSeekError(error) {
  if (error instanceof HttpsError) return error;
  if (error instanceof DeepSeekError) {
    if (error.status === 401 || error.status === 402 || error.status === 403) return new HttpsError('internal', 'Layanan AI belum dikonfigurasi di server.');
    if (error.status === 429) return new HttpsError('unavailable', 'Layanan AI sedang sibuk. Coba lagi sebentar lagi.');
    if (error.status === 400 || error.status === 422) return new HttpsError('invalid-argument', 'Masukan tidak dapat diproses AI.');
    return new HttpsError('unavailable', 'Layanan AI sedang bermasalah. Coba lagi nanti.');
  }
  return null;
}

module.exports = { createDeepSeekClient, jsonOutputFormat, mapDeepSeekError, DeepSeekError };
