document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('addr-form');
    if (!form) return;

    const toggle = document.getElementById('addr-toggle');
    const input = document.getElementById('addr-input');
    const list = document.getElementById('addr-list');
    const save = document.getElementById('addr-save');

    toggle.addEventListener('click', () => {
        form.hidden = !form.hidden;
        if (!form.hidden) input.focus();
    });

    let timer = null;
    let controller = null;
    let items = [];
    let active = -1;

    function close() {
        list.hidden = true;
        list.innerHTML = '';
        items = [];
        active = -1;
    }

    function clearSelection() {
        ['osmId', 'label', 'city', 'street', 'number', 'postalCode', 'lat', 'lon']
            .forEach(name => { form.elements[name].value = ''; });
        save.disabled = true;
    }

    function select(item) {
        if (!item.number) {
            input.value = item.street ? `${item.city}, ${item.street} ` : `${item.city}, `;
            close();
            input.focus();
            input.dispatchEvent(new Event('input'));
            return;
        }
        input.value = item.label;
        form.elements['osmId'].value = item.osmId;
        form.elements['label'].value = item.label;
        form.elements['city'].value = item.city;
        form.elements['street'].value = item.street ?? '';
        form.elements['number'].value = item.number;
        form.elements['postalCode'].value = item.postalCode ?? '';
        form.elements['lat'].value = item.lat;
        form.elements['lon'].value = item.lon;
        save.disabled = false;
        close();
    }

    function render() {
        list.innerHTML = '';
        items.forEach((item, i) => {
            const li = document.createElement('li');
            li.textContent = item.number ? item.label : `${item.label} …`;
            if (!item.number) li.classList.add('partial');
            if (i === active) li.classList.add('active');
            li.addEventListener('mousedown', (e) => {
                e.preventDefault();
                select(item);
            });
            list.appendChild(li);
        });
        list.hidden = items.length === 0;
    }

    input.addEventListener('input', () => {
        clearSelection();
        clearTimeout(timer);
        const q = input.value.trim();
        if (q.length < 3) {
            close();
            return;
        }
        timer = setTimeout(async () => {
            if (controller) controller.abort();
            controller = new AbortController();
            try {
                const res = await fetch(`/api/buildings/search?q=${encodeURIComponent(q)}`,
                    { signal: controller.signal });
                if (!res.ok) return;
                items = await res.json();
                active = -1;
                render();
            } catch (e) {
            }
        }, 350);
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && list.hidden) {
            if (save.disabled) e.preventDefault();
            return;
        }
        if (list.hidden) return;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            active = (active + 1) % items.length;
            render();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            active = (active - 1 + items.length) % items.length;
            render();
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (active >= 0) select(items[active]);
        } else if (e.key === 'Escape') {
            close();
        }
    });

    document.addEventListener('click', (e) => {
        if (!e.target.closest('.addr-box')) close();
    });
});
