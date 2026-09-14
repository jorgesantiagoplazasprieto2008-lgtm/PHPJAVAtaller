/**
 * SVIS - JavaScript Frontend Interactivity
 */

document.addEventListener('DOMContentLoaded', () => {

    // 1. Manejo dinámico de opciones en la creación de encuestas
    const addOptionBtn = document.getElementById('btn-add-option');
    const optionsContainer = document.getElementById('options-container');

    if (addOptionBtn && optionsContainer) {
        addOptionBtn.addEventListener('click', () => {
            const currentCount = optionsContainer.querySelectorAll('.option-input-row').length + 1;
            const row = document.createElement('div');
            row.className = 'form-group option-input-row';
            row.style.display = 'flex';
            row.style.gap = '0.5rem';
            row.innerHTML = `
                <input type="text" name="opciones[]" class="form-control" placeholder="Opción ${currentCount}" required>
                <button type="button" class="btn btn-danger btn-sm btn-remove-option" title="Eliminar opción">✕</button>
            `;
            optionsContainer.appendChild(row);

            row.querySelector('.btn-remove-option').addEventListener('click', () => {
                if (optionsContainer.querySelectorAll('.option-input-row').length > 2) {
                    row.remove();
                } else {
                    alert('Una encuesta debe contar con al menos dos opciones de sufragio.');
                }
            });
        });

        // Event listener delegado para los botones de remover iniciales
        optionsContainer.addEventListener('click', (e) => {
            if (e.target.classList.contains('btn-remove-option')) {
                if (optionsContainer.querySelectorAll('.option-input-row').length > 2) {
                    e.target.closest('.option-input-row').remove();
                } else {
                    alert('Una encuesta debe contar con al menos dos opciones de sufragio.');
                }
            }
        });
    }

    // 2. Selección visual de tarjetas de votación
    const optionCards = document.querySelectorAll('.option-card');
    const hiddenRadioInputs = document.querySelectorAll('input[name="opcion_id"]');

    optionCards.forEach(card => {
        card.addEventListener('click', () => {
            optionCards.forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');
            const radio = card.querySelector('input[type="radio"]');
            if (radio) radio.checked = true;
        });
    });

    // 3. Copiar comprobante o token al portapapeles
    const copyButtons = document.querySelectorAll('.btn-copy');
    copyButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetText = btn.getAttribute('data-clipboard');
            if (targetText) {
                navigator.clipboard.writeText(targetText).then(() => {
                    const originalText = btn.innerHTML;
                    btn.innerHTML = '✓ ¡Copiado!';
                    btn.classList.add('btn-success');
                    setTimeout(() => {
                        btn.innerHTML = originalText;
                        btn.classList.remove('btn-success');
                    }, 2000);
                });
            }
        });
    });
});
