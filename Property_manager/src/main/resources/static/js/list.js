function applyFilter(){
    const q = (document.getElementById('q').value || '').trim().toLowerCase();
    const rows = document.querySelectorAll('#tbody tr.row');

    rows.forEach(r => {
        if (!q) { r.style.display = ''; return; }
        const text = r.innerText.toLowerCase();
        r.style.display = text.includes(q) ? '' : 'none';
    });
}

function resetFilter(){
    document.getElementById('q').value = '';
    applyFilter();
}