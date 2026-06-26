import React, { useCallback, useEffect, useState } from 'react';
import CountdownTimer from './components/CountdownTimer';
import ReactivoCard from './components/ReactivoCard';
import ProgressBar from './components/ProgressBar';
import {
  getCurrent, getTiempoRestante, postRespuesta, postCerrar,
  bufferAnswer, flushBuffer,
} from './api/subtestApi';

export default function SubtestApp() {
  const [data, setData] = useState(null);     // SubtestActual
  const [answers, setAnswers] = useState({}); // reactivoId -> opcionId
  const [cerrado, setCerrado] = useState(false);

  const cargar = useCallback(async () => {
    const d = await getCurrent();
    setData(d);
    setAnswers({});
    setCerrado(d.estado !== 'EN_CURSO');
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  // Flush buffered answers when connectivity returns (RN-BFA-09).
  useEffect(() => {
    const onOnline = () => { if (data) flushBuffer(data.ejecucionSubtestId).catch(() => {}); };
    window.addEventListener('online', onOnline);
    return () => window.removeEventListener('online', onOnline);
  }, [data]);

  // Poll server for closure (server enforces timer). 30s per §11.
  useEffect(() => {
    if (!data || cerrado) return;
    const id = setInterval(async () => {
      try {
        const t = await getTiempoRestante();
        if (t.estado !== 'EN_CURSO') setCerrado(true);
      } catch { /* ignore transient */ }
    }, 30000);
    return () => clearInterval(id);
  }, [data, cerrado]);

  const seleccionar = async (reactivoId, opcionId) => {
    setAnswers((a) => ({ ...a, [reactivoId]: opcionId }));
    try {
      await postRespuesta(data.ejecucionSubtestId, reactivoId, opcionId);
    } catch (e) {
      if (e.status === 409 || e.status === 423) setCerrado(true);
      else bufferAnswer(data.ejecucionSubtestId, reactivoId, opcionId); // offline
    }
  };

  const cerrar = useCallback(async () => {
    if (cerrado) return;
    setCerrado(true);
    try {
      const r = await postCerrar();
      if (r.next === 'COMPLETADO') window.location.href = '/evaluacion/completado';
      else await cargar(); // advance to next subtest
    } catch { /* server may have closed already */ }
  }, [cerrado, cargar]);

  // Auto-submit when all items answered.
  useEffect(() => {
    if (data && !cerrado && data.items.length > 0
        && Object.keys(answers).length === data.items.length) {
      cerrar();
    }
  }, [answers, data, cerrado, cerrar]);

  if (!data) return <p>Cargando…</p>;
  if (cerrado) return <div className="bfa-cerrado"><h2>Tiempo agotado</h2><p>Este subtest está cerrado.</p></div>;

  const answered = Object.keys(answers).length;
  return (
    <div className="bfa-subtest">
      <header>
        <h1>Subtest {data.subtestType}</h1>
        <CountdownTimer seconds={data.tiempoRestanteSeg} onExpire={cerrar} />
        <ProgressBar answered={answered} total={data.items.length} />
      </header>
      {data.items.map((item) => (
        <ReactivoCard key={item.id} item={item} selected={answers[item.id]} onSelect={seleccionar} />
      ))}
    </div>
  );
}
