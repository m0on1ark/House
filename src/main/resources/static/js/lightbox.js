document.addEventListener('DOMContentLoaded', () => {
    const photos = document.querySelectorAll('.deal-photo, .chat-photo');
    if (!photos.length) return;

    const box = document.createElement('div');
    box.className = 'lightbox';
    const img = document.createElement('img');
    box.appendChild(img);
    document.body.appendChild(box);

    const close = () => { box.classList.remove('open'); img.src = ''; };
    photos.forEach(p => p.addEventListener('click', () => {
        img.src = p.currentSrc || p.src;
        box.classList.add('open');
    }));
    box.addEventListener('click', close);
    document.addEventListener('keydown', e => { if (e.key === 'Escape') close(); });
});
