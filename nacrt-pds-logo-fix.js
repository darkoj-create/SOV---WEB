/**
 * Corrected PDS Velebit right-header logo.
 * Vector recreation based on the supplied official mark.
 */
(() => {
  'use strict';
  if (typeof NacrtRenderer === 'undefined' || typeof NacrtRenderer.render !== 'function') {
    throw new Error('PDS logo fix traži učitan NacrtRenderer.');
  }

  const renderPrevious = NacrtRenderer.render.bind(NacrtRenderer);

  function correctedLogo() {
    return `<svg class="sov-official-pds-logo" x="1036" y="23" width="146" height="146" viewBox="0 0 158 158" aria-label="PDS Velebit">
      <polygon points="10,148 4,48 92,4 154,148" fill="#fff" stroke="#cf241f" stroke-width="5" stroke-linejoin="miter"/>
      <polygon points="18,139 14,54 89,17 145,139" fill="#fff" stroke="#222" stroke-width="2.2"/>
      <polygon points="15,54 89,17 123,73 94,61 70,69 47,58 25,69" fill="#4d82ba"/>
      <path d="M25 69 L47 58 L70 69 L94 61 L123 73" fill="none" stroke="#fff" stroke-width="2.2"/>
      <path d="M25 70 L45 66 L47 82 L60 75 L72 82 L68 111 L61 131" fill="none" stroke="#222" stroke-width="5.4" stroke-linejoin="miter"/>
      <path d="M69 69 L86 67 L99 79 L94 107 L78 126 L68 111" fill="#fff" stroke="#222" stroke-width="4.8" stroke-linejoin="miter"/>
      <g transform="translate(112 112)" fill="none" stroke="#3f78b9" stroke-width="1.6">
        <circle r="6"/><circle r="2" fill="#3f78b9"/>
        <path d="M0-25 C4-15 4-11 0-6 C-4-11-4-15 0-25Z M0 25 C4 15 4 11 0 6 C-4 11-4 15 0 25Z M-25 0 C-15-4-11-4-6 0 C-11 4-15 4-25 0Z M25 0 C15-4 11-4 6 0 C11 4 15 4 25 0Z"/>
        <path d="M-18-18 C-9-14-6-10-5-5 C-10-6-14-9-18-18Z M18-18 C9-14 6-10 5-5 C10-6 14-9 18-18Z M-18 18 C-9 14-6 10-5 5 C-10 6-14 9-18 18Z M18 18 C9 14 6 10 5 5 C10 6 14 9 18 18Z"/>
      </g>
      <text x="20" y="119" font-family="Arial,Helvetica,sans-serif" font-size="17" font-weight="700" fill="#3f78b9">PDS</text>
      <text x="18" y="140" font-family="Arial,Helvetica,sans-serif" font-size="18" font-weight="700" letter-spacing="1.4" fill="#3f78b9">VELEBIT</text>
    </svg>`;
  }

  NacrtRenderer.render = function renderWithCorrectPdsLogo(survey, options = {}) {
    let svg = renderPrevious(survey, options);
    const logo = correctedLogo();
    svg = svg.replace(/<image\s+href="[^"]*"\s+x="1042"\s+y="22"\s+width="132"\s+height="166"\s+preserveAspectRatio="xMidYMid meet"\s*\/>/, logo);
    svg = svg.replace(/<image\s+href="[^"]*"\s+x="1050"\s+y="23"\s+width="118"\s+height="160"\s+preserveAspectRatio="xMidYMid meet"\s*\/>/, logo);
    return svg;
  };

  if (typeof window !== 'undefined') window.SOV_NACRT_PDS_LOGO_FIX = { version: '1.0' };
})();
