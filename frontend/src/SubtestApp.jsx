import React, { useCallback, useEffect, useState } from 'react';
import CountdownTimer from './components/CountdownTimer';
import ReactivoCard from './components/ReactivoCard';
import ProgressBar from './components/ProgressBar';
import ConsignaScreen from './components/ConsignaScreen';
import {
  getCurrent, getTiempoRestante, postIniciar, postRespuesta, postCerrar,
  bufferAnswer, flushBuffer,
} from './api/subtestApi';

// view: loading | consigna | test | agotado | completado
export default function SubtestApp() {
  const [view, setView] = useState('loading');
  const [data, setData] = useState(null);
  const [answers, setAnswers] = useState({});

  const cargar = useCallback(async () => {
    try {
      const d = await getCurrent();
      setData(d);
      setAnswers({});
      setView(d.estado === 'EN_CURSO' ? 'test' : 'consigna'); // PENDIENTE -> consigna
    } catch (e) {
      if (e.status === 404) setView('completado'); // no active subtest left
    }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  // Flush buffered answers when connectivity returns (RN-BFA-09).
  useEffect(() => {
    const onOnline = () => { if (data) flushBuffer(data.ejecucionSubtestId).catch(() => {}); };
    window.addEventListener('online', onOnline);
    return () => window.removeEventListener('online', onOnline);
  }, [data]);

  // Poll for server-side closure while taking the test (§11). SSE replaces this in P2-A.
  useEffect(() => {
    if (view !== 'test') return;
    const id = setInterval(async () => {
      try {
        const t = await getTiempoRestante();
        if (t.estado !== 'EN_CURSO') setView('agotado'); // server closed it
      } catch { setView('agotado'); }
    }, 30000);
    return () => clearInterval(id);
  }, [view]);

  const comenzar = async () => {
    await postIniciar();      // server sets fecha_inicio = NOW
    await cargar();           // reload -> EN_CURSO -> test view
  };

  const seleccionar = async (reactivoId, opcionId) => {
    setAnswers((a) => ({ ...a, [reactivoId]: opcionId }));
    try {
      await postRespuesta(data.ejecucionSubtestId, reactivoId, opcionId);
    } catch (e) {
      if (e.status === 409 || e.status === 423) setView('agotado');
      else bufferAnswer(data.ejecucionSubtestId, reactivoId, opcionId); // offline
    }
  };

  const expirar = useCallback(async () => {
    setView('agotado');
    try { await postCerrar(); } catch { /* server may have closed already */ }
  }, []);

  const finalizar = useCallback(async () => {
    try {
      const r = await postCerrar();
      if (r.next === 'COMPLETADO') { window.location.href = '/evaluacion/completado'; return; }
    } catch { /* ignore */ }
    await cargar();
  }, [cargar]);

  // Auto-submit when all items answered.
  useEffect(() => {
    if (view === 'test' && data && data.items.length > 0
        && Object.keys(answers).length === data.items.length) {
      finalizar();
    }
  }, [answers, data, view, finalizar]);

  if (view === 'loading') return <p>Cargando…</p>;
  if (view === 'completado') return <div className="bfa-completado"><h2>Evaluación completada</h2></div>;
  if (view === 'consigna') return <ConsignaScreen subtestType={data.subtestType} onComenzar={comenzar} />;
  if (view === 'agotado') {
    return (
      <div className="bfa-agotado">
        <h2>Tiempo agotado</h2>
        <p>Este subtest se ha cerrado.</p>
        <button type="button" onClick={cargar}>Continuar</button>
      </div>
    );
  }

  // view === 'test'
  const answered = Object.keys(answers).length;
  return (
    <div className="bfa-subtest">
      <header>
        <h1>Subtest {data.subtestType}</h1>
        <CountdownTimer seconds={data.tiempoRestanteSeg} onExpire={expirar} />
        <ProgressBar answered={Object.keys(answers).length} total={data.items.length} />
      </header>
      {data.items.map((item) => (
        <ReactivoCard key={item.id} item={item} selected={answers[item.id]} onSelect={seleccionar} />
      ))}
    </div>
  );
}
