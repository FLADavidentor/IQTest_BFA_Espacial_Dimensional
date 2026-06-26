import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ProgressBar from './ProgressBar';

describe('ProgressBar (P1-C)', () => {
  it('shows last position reached, not the answer count', () => {
    const items = Array.from({ length: 27 }, (_, i) => ({ id: 100 + i + 1, orden: i + 1 }));
    // answered items at positions 1, 3, 5 (ids 101, 103, 105)
    const answers = { 101: 9, 103: 9, 105: 9 };

    render(<ProgressBar answers={answers} items={items} />);
    expect(screen.getByText('Ítem 5 de 27')).toBeTruthy();
  });
});
