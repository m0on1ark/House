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

document.addEventListener('DOMContentLoaded', () => {
    const cells = Array.from(document.querySelectorAll('.skyline .bletter i.b'));
    if (!cells.length || reduceMotion) return;
    cells.forEach(c => { if (Math.random() < 0.25) c.classList.add('win-lit'); });
    setInterval(() => {
        const n = 1 + Math.floor(Math.random() * 3);
        for (let k = 0; k < n; k++) {
            cells[Math.floor(Math.random() * cells.length)].classList.toggle('win-lit');
        }
    }, 1400);
});

document.addEventListener('DOMContentLoaded', () => {
    const cta = document.querySelector('.hero a.btn-solid');
    if (!cta || reduceMotion) return;
    const groceries = ['--g-apple', '--g-bread', '--g-cheese', '--g-milk', '--g-bottle', '--g-can'];
    const colors = ['#ff5a5f', '#ffd23f', '#3fb984', '#4d9be6', '#b06fd6', '#ff924d'];

    cta.addEventListener('click', (e) => {
        e.preventDefault();
        const href = cta.getAttribute('href');
        const r = cta.getBoundingClientRect();
        burst(r.left + r.width / 2, r.top + r.height / 2);
        setTimeout(() => { window.location.href = href; }, 480);
    });

    function burst(cx, cy) {
        const layer = document.createElement('div');
        layer.style.cssText = 'position:fixed;left:0;top:0;z-index:9999;pointer-events:none';
        document.body.appendChild(layer);
        const N = 18;
        for (let i = 0; i < N; i++) {
            const p = document.createElement('span');
            const ang = (Math.PI * 2 * i) / N + Math.random() * 0.5;
            const dist = 70 + Math.random() * 95;
            const dx = Math.cos(ang) * dist;
            const dy = Math.sin(ang) * dist - 15;
            if (i % 2 === 0) {
                const g = groceries[Math.floor(Math.random() * groceries.length)];
                p.style.cssText = `position:absolute;left:${cx}px;top:${cy}px;width:26px;height:30px;` +
                    `margin:-15px 0 0 -13px;background:var(${g}) center/contain no-repeat`;
            } else {
                const c = colors[Math.floor(Math.random() * colors.length)];
                p.style.cssText = `position:absolute;left:${cx}px;top:${cy}px;width:9px;height:9px;` +
                    `margin:-4px 0 0 -4px;border-radius:50%;background:${c}`;
            }
            layer.appendChild(p);
            p.animate([
                { transform: 'translate(0,0) scale(.4) rotate(0deg)', opacity: 1 },
                { transform: `translate(${dx}px,${dy}px) scale(1) rotate(${Math.random() * 220 - 110}deg)`, opacity: 1, offset: .7 },
                { transform: `translate(${dx * 1.15}px,${dy * 1.15 + 45}px) scale(.6) rotate(${Math.random() * 260 - 130}deg)`, opacity: 0 }
            ], { duration: 640, easing: 'cubic-bezier(.2,.8,.3,1)' });
        }
        setTimeout(() => layer.remove(), 720);
    }
});
