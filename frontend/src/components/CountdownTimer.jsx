import React, { useEffect, useState } from 'react';

// Display-only countdown. Server enforces closure; this just shows time and
// fires onExpire once when it reaches zero (auto-submit trigger).
export default function CountdownTimer({ seconds, onExpire }) {
  const [restante, setRestante] = useState(seconds);

  useEffect(() => setRestante(seconds), [seconds]);

  useEffect(() => {
    if (restante <= 0) {
      onExpire?.();
      return;
    }
    const t = setTimeout(() => setRestante((s) => s - 1), 1000);
    return () => clearTimeout(t);
  }, [restante, onExpire]);

  const mm = String(Math.floor(Math.max(0, restante) / 60)).padStart(2, '0');
  const ss = String(Math.max(0, restante) % 60).padStart(2, '0');
  return <div className="bfa-timer" role="timer" aria-live="polite">{mm}:{ss}</div>;
}
