import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ReactivoCard from './ReactivoCard';

const item = {
  id: 1, orden: 7, imagenUrl: '/img/missing.png', enunciadoTexto: 'Texto de respaldo',
  opciones: [{ id: 10, etiqueta: 'A' }, { id: 11, etiqueta: 'B' }],
};

describe('ReactivoCard image fallback (P0-A)', () => {
  it('shows fallback text (no broken image) when the image fails, item stays interactable', () => {
    render(<ReactivoCard item={item} selected={null} onSelect={() => {}} />);

    // image present initially
    const img = screen.getByAltText('Ítem 7');
    fireEvent.error(img); // simulate 404

    // broken <img> is gone, fallback shown
    expect(screen.queryByAltText('Ítem 7')).toBeNull();
    expect(screen.getByText(/Imagen no disponible — Ítem 7/)).toBeTruthy();
    // text enunciado fallback shown
    expect(screen.getByText('Texto de respaldo')).toBeTruthy();
    // still interactable
    expect(screen.getAllByRole('radio')).toHaveLength(2);
  });
});
