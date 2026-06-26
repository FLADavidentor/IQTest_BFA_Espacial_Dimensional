import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ConsignaScreen, { CONSIGNAS } from './ConsignaScreen';

describe('ConsignaScreen (P1-A)', () => {
  it('shows the subtest consigna and starts only on Comenzar', () => {
    const onComenzar = vi.fn();
    render(<ConsignaScreen subtestType="S1A" onComenzar={onComenzar} />);

    expect(screen.getByText(CONSIGNAS.S1A)).toBeTruthy();
    expect(onComenzar).not.toHaveBeenCalled();   // timer not started yet

    fireEvent.click(screen.getByText('Comenzar'));
    expect(onComenzar).toHaveBeenCalledOnce();
  });
});
