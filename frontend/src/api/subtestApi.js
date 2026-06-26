// API client for the subtest flow (§9). Server enforces timing; client is UX only.

const json = (r) => {
  if (!r.ok) throw Object.assign(new Error('HTTP ' + r.status), { status: r.status });
  return r.status === 200 ? r.json().catch(() => ({})) : {};
};

export const getCurrent = () =>
  fetch('/api/subtest/current', { credentials: 'same-origin' }).then(json);

export const getTiempoRestante = () =>
  fetch('/api/subtest/tiempo-restante', { credentials: 'same-origin' }).then(json);

export const postRespuesta = (ejecucionSubtestId, reactivoId, opcionReactivoId) =>
  fetch('/api/respuesta', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ejecucionSubtestId, reactivoId, opcionReactivoId }),
  }).then(json);

export const postSync = (respuestas) =>
  fetch('/api/respuesta/sync', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ respuestas }),
  }).then(json);

export const postCerrar = () =>
  fetch('/api/subtest/cerrar', { method: 'POST', credentials: 'same-origin' }).then(json);

// --- offline answer buffer (sessionStorage), keyed per execution (RN-BFA-09) ---

const bufKey = (ejecId) => `bfa.buffer.${ejecId}`;

export const bufferAnswer = (ejecId, reactivoId, opcionReactivoId) => {
  const buf = JSON.parse(sessionStorage.getItem(bufKey(ejecId)) || '{}');
  buf[reactivoId] = opcionReactivoId; // last write wins per item
  sessionStorage.setItem(bufKey(ejecId), JSON.stringify(buf));
};

export const flushBuffer = async (ejecId) => {
  const buf = JSON.parse(sessionStorage.getItem(bufKey(ejecId)) || '{}');
  const respuestas = Object.entries(buf).map(([reactivoId, opcionReactivoId]) => ({
    reactivoId: Number(reactivoId),
    opcionReactivoId,
  }));
  if (respuestas.length === 0) return { sincronizadas: 0, rechazadas: 0 };
  const res = await postSync(respuestas);
  sessionStorage.removeItem(bufKey(ejecId));
  return res;
};
