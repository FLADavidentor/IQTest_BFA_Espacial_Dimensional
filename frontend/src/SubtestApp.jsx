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
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [syncMessage, setSyncMessage] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const d = await getCurrent();
      setData(d);
      setAnswers({});
      setCurrentIndex(0);
      setView(d.estado === 'EN_CURSO' ? 'test' : 'consigna'); // PENDIENTE -> consigna
    } catch (e) {
      if (e.status === 404) setView('completado'); // no active subtest left
    }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  // Flush buffered answers and handle connectivity changes (RN-BFA-09).
  useEffect(() => {
    const handleOnline = async () => {
      setIsOnline(true);
      if (data) {
        try {
          const res = await flushBuffer(data.ejecucionSubtestId);
          if (res && res.sincronizadas > 0) {
            setSyncMessage(`[ ✓ Conexión restablecida: ${res.sincronizadas} respuesta(s) sincronizada(s) con éxito ]`);
            setTimeout(() => setSyncMessage(null), 4000);
          }
        } catch (e) {
          // ignore
        }
      }
    };
    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Initial check on load
    if (navigator.onLine && data) {
      flushBuffer(data.ejecucionSubtestId).catch(() => {});
    }

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [data]);

  // Primary closure signal (P2-A): SSE push the moment the server closes the subtest.
  useEffect(() => {
    if (view !== 'test') return undefined;
    const es = new EventSource('/api/subtest/timer-events');
    es.addEventListener('cerrado', () => setView('agotado'));
    return () => es.close();
  }, [view]);

  // Fallback poll (in case SSE drops). Server enforces; client never decides closure alone.
  useEffect(() => {
    if (view !== 'test') return undefined;
    const id = setInterval(async () => {
      try {
        const t = await getTiempoRestante();
        if (t.estado !== 'EN_CURSO') setView('agotado');
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
      setIsOnline(true);
    } catch (e) {
      if (e.status === 409 || e.status === 423) {
        setView('agotado');
      } else {
        bufferAnswer(data.ejecucionSubtestId, reactivoId, opcionId); // offline
        setIsOnline(false);
      }
    }

    // Auto-advance after a small delay (350ms) to allow OMR lead visual fill feedback
    if (data && data.items && currentIndex < data.items.length - 1) {
      setTimeout(() => {
        setCurrentIndex((prev) => prev + 1);
      }, 350);
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

  if (view === 'completado') {
    return (
      <div className="bfa-completado">
        <h2>Evaluación Completada</h2>
        <p>Has finalizado con éxito los 3 subtests de la evaluación BFA Espacial.</p>
        <hr style={{ border: 'none', borderTop: '2px dashed #202020', margin: '25px 0' }} />
        <p style={{ fontWeight: 'bold', textTransform: 'uppercase', color: '#202020' }}>[ Acceso Bloqueado ]</p>
        <p>Ya has realizado la prueba correspondiente a este período académico. No es posible iniciar un nuevo intento o modificar las respuestas.</p>
      </div>
    );
  }

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
  const currentItem = data && data.items && data.items.length > 0 ? data.items[currentIndex] : null;

  return (
    <div className="bfa-subtest">
      {!isOnline && (
        <div className="bfa-offline-banner">
          [ AVISO: Modo sin conexión. Las respuestas se están guardando localmente en la computadora y se sincronizarán al recuperar la señal. ]
        </div>
      )}
      {syncMessage && (
        <div className="bfa-sync-banner">
          {syncMessage}
        </div>
      )}

      <header>
        <h1>Subtest {data.subtestType}</h1>
        <CountdownTimer seconds={data.tiempoRestanteSeg} onExpire={expirar} />
        <ProgressBar answers={answers} items={data.items} />
      </header>

      {currentItem && (
        <ReactivoCard
          item={currentItem}
          selected={answers[currentItem.id]}
          onSelect={seleccionar}
        />
      )}

      {data && data.items && data.items.length > 0 && (
        <div className="bfa-navegacion">
          <button
            type="button"
            onClick={() => setCurrentIndex((i) => Math.max(0, i - 1))}
            disabled={currentIndex === 0}
          >
            ← Anterior
          </button>
          <span className="bfa-progreso-texto">
            Ítem {currentIndex + 1} de {data.items.length}
          </span>
          <button
            type="button"
            onClick={() => setCurrentIndex((i) => Math.min(data.items.length - 1, i + 1))}
            disabled={currentIndex === data.items.length - 1}
          >
            Siguiente →
          </button>
        </div>
      )}
    </div>
  );
}
