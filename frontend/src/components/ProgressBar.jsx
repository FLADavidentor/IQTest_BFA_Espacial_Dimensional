import React from 'react';

// "Ítem X de N": X = highest item position (orden) the student has reached/answered,
// not the count of answers (answering item 27 first must show 27, not 1).
export default function ProgressBar({ answers, items }) {
  const total = items.length;
  const ordenes = items.filter((i) => answers[i.id] != null).map((i) => i.orden);
  const pos = ordenes.length ? Math.max(...ordenes) : 0;
  return (
    <div className="bfa-progress">
      <progress value={pos} max={total} />
      <span>Ítem {pos} de {total}</span>
    </div>
  );
}
