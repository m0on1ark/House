const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

document.addEventListener('DOMContentLoaded', () => {
    const reveals = Array.from(document.querySelectorAll(
        '.hero-kicker, .hero-tagline, .hero-sub, .hero .btn, ' +
        '.sec-label, section h2, .cell-card, .empty, .cta'
    ));
    if (reveals.length && 'IntersectionObserver' in window && !reduceMotion) {
        reveals.forEach(el => el.classList.add('reveal'));
        const io = new IntersectionObserver((entries, obs) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('in');
                    obs.unobserve(entry.target);
                }
            });
        }, { threshold: 0.15 });
        reveals.forEach(el => io.observe(el));
    }
});

document.addEventListener('DOMContentLoaded', () => {
    const layer = document.querySelector('.grocery-pops');
    if (!layer || reduceMotion) return;

    const icons = ['--g-apple', '--g-bread', '--g-cheese', '--g-milk', '--g-bottle', '--g-can'];

    const spawn = () => {
        if (document.hidden || layer.childElementCount > 7) return;
        const el = document.createElement('span');
        el.className = 'pop';
        el.style.setProperty('--img', `var(${icons[Math.floor(Math.random() * icons.length)]})`);
        el.style.setProperty('--rot', (Math.random() * 28 - 14) + 'deg');
        const onLeft = Math.random() < 0.5;
        el.style.left = (onLeft ? 2 + Math.random() * 22 : 76 + Math.random() * 22) + '%';
        el.style.top = (8 + Math.random() * 78) + '%';
        el.addEventListener('animationend', () => el.remove());
        layer.appendChild(el);
    };

    spawn();
    spawn();
    setInterval(spawn, 750);
});

document.addEventListener('DOMContentLoaded', () => {
    const html = document.documentElement;
    const intro = document.getElementById('intro');
    if (!intro) return;
    try { sessionStorage.setItem('introSeen', '1'); } catch (e) {}
    if (!html.classList.contains('intro-playing') || reduceMotion) {
        html.classList.remove('intro-playing');
        intro.remove();
        return;
    }
    intro.addEventListener('animationend', (e) => {
        if (e.animationName === 'intro-fade') {
            html.classList.remove('intro-playing');
            intro.remove();
        }
    });
});

document.addEventListener('DOMContentLoaded', () => {
    const residents = Array.from(document.querySelectorAll('.skyline .res'));
    if (!residents.length) return;

    residents.forEach((r, i) => {
        if (i % 2 === 0) r.classList.add('hidden');
    });

    if (reduceMotion) return;

    setInterval(() => {
        const hidden = residents.filter(r => r.classList.contains('hidden'));
        const shown = residents.filter(r => !r.classList.contains('hidden'));

        if (hidden.length && (shown.length < 2 || Math.random() < 0.6)) {
            const next = hidden[Math.floor(Math.random() * hidden.length)];
            next.classList.remove('hidden');
        }
        if (shown.length > 2 || (shown.length && Math.random() < 0.35)) {
            const away = shown[Math.floor(Math.random() * shown.length)];
            away.classList.add('hidden');
        }
    }, 2400);
});
