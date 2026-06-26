import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { axe } from 'jest-axe';
import React from 'react';
import ConsignaScreen from './components/ConsignaScreen';
import CountdownTimer from './components/CountdownTimer';
import ProgressBar from './components/ProgressBar';
import ReactivoCard from './components/ReactivoCard';

const items = [
  { id: 1, orden: 1, imagenUrl: '/img/1.png', enunciadoTexto: null,
    opciones: [{ id: 11, etiqueta: 'A' }, { id: 12, etiqueta: 'B' }] },
  { id: 2, orden: 2, imagenUrl: '/img/2.png', enunciadoTexto: null,
    opciones: [{ id: 21, etiqueta: 'A' }, { id: 22, etiqueta: 'B' }] },
];

const noCritical = (results) =>
  results.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');

describe('Accessibility (7-E) — zero critical/serious axe violations', () => {
  it('consigna screen', async () => {
    const { container } = render(
      <main><ConsignaScreen subtestType="S1A" onComenzar={() => {}} /></main>,
    );
    expect(noCritical(await axe(container))).toEqual([]);
  });

  it('subtest test view (timer + progress + items)', async () => {
    const { container } = render(
      <main>
        <header>
          <h1>Subtest S1A</h1>
          <CountdownTimer seconds={180} onExpire={() => {}} />
          <ProgressBar answers={{}} items={items} />
        </header>
        {items.map((it) => (
          <ReactivoCard key={it.id} item={it} selected={null} onSelect={() => {}} />
        ))}
      </main>,
    );
    expect(noCritical(await axe(container))).toEqual([]);
  });
});
