import React from 'react';

export default function ProgressBar({ answered, total }) {
  return (
    <div className="bfa-progress">
      <progress value={answered} max={total} />
      <span>{answered} / {total}</span>
    </div>
  );
}
