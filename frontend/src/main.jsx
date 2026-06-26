import React from 'react';
import { createRoot } from 'react-dom/client';
import SubtestApp from './SubtestApp';

const el = document.getElementById('bfa-subtest-app');
if (el) createRoot(el).render(<SubtestApp />);
