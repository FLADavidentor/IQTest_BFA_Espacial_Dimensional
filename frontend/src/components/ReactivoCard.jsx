import React from 'react';

// One item: image + options (A/B or A–E). No correct-answer data exists client-side.
export default function ReactivoCard({ item, selected, onSelect }) {
  return (
    <fieldset className="bfa-reactivo">
      <legend>Ítem {item.orden}</legend>
      <img src={item.imagenUrl} alt={`Ítem ${item.orden}`} loading="lazy" />
      <div className="bfa-opciones">
        {item.opciones.map((o) => (
          <label key={o.id}>
            <input
              type="radio"
              name={`item-${item.id}`}
              value={o.id}
              checked={selected === o.id}
              onChange={() => onSelect(item.id, o.id)}
            />
            {o.etiqueta}
          </label>
        ))}
      </div>
    </fieldset>
  );
}
