import React from 'react';

// Mandatory instructions per subtest (Manual BFA consigna). Timer starts only on "Comenzar".
export const CONSIGNAS = {
  S1A: 'A continuación verá 27 pares de figuras. Seleccione cuál de las opciones (A o B) es idéntica a la figura modelo. Tiene 3 minutos. Al terminar el tiempo, la prueba se cerrará automáticamente.',
  S2: 'A continuación verá 34 pares de figuras espaciales desplazadas. Determine si son idénticas seleccionando A o B. Tiene 5 minutos.',
  S1B: 'A continuación verá 49 estructuras tridimensionales de cubos. Identifique la vista correcta entre las opciones A, B, C, D o E. Tiene 3 minutos 30 segundos.',
};

export default function ConsignaScreen({ subtestType, onComenzar }) {
  return (
    <div className="bfa-consigna">
      <h1>Subtest {subtestType}</h1>
      <p>{CONSIGNAS[subtestType] || 'Lea las instrucciones y presione Comenzar.'}</p>
      <button type="button" onClick={onComenzar}>Comenzar</button>
    </div>
  );
}
