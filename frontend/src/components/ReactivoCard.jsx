import React, { useState } from 'react';

// One item: image (with graceful fallback) + options. No correct-answer data exists client-side.
export default function ReactivoCard({ item, selected, onSelect }) {
  const [imgError, setImgError] = useState(false);

  return (
    <fieldset className="bfa-reactivo">
      <legend>Ítem {item.orden}</legend>

      {!imgError && item.imagenUrl ? (
        <img
          src={item.imagenUrl}
          alt={`Ítem ${item.orden}`}
          loading="lazy"
          onError={() => setImgError(true)}
        />
      ) : (
        <div className="bfa-img-fallback" role="img"
             aria-label={`Ítem ${item.orden}: imagen no disponible`}>
          [ Imagen no disponible — Ítem {item.orden} ]
        </div>
      )}

      {item.enunciadoTexto && <p className="bfa-enunciado">{item.enunciadoTexto}</p>}

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
