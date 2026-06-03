document.addEventListener("DOMContentLoaded", () => {
    const body = document.body;
    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebarOpen = document.getElementById('sidebarOpen');

    // 1. REACTIVAR TRANSICIONES
    // Solo necesitamos esto para que el toggle manual sea fluido
    setTimeout(() => {
        body.style.transition = '';
    }, 100);

    // 2. FUNCIÓN TOGGLE OPTIMIZADA
    // Ya no necesitas 'body.classList.add' al inicio porque el Fragmento ya lo hizo
    const toggleSidebar = () => {
        const isClosed = body.classList.contains('sidebar-closed');

        if (isClosed) {
            body.classList.replace('sidebar-closed', 'sidebar-open');
            localStorage.setItem('sidebarStatus', 'sidebar-open');
        } else {
            body.classList.replace('sidebar-open', 'sidebar-closed');
            localStorage.setItem('sidebarStatus', 'sidebar-closed');
        }
    };

    if (sidebarToggle) sidebarToggle.addEventListener('click', toggleSidebar);
    if (sidebarOpen) sidebarOpen.addEventListener('click', toggleSidebar);

    // 3. LINK ACTIVO (Sin cambios)
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-menu a').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        }
    });

    // 4. DROPDOWN (Sin cambios)
    const userDropdown = document.getElementById('userDropdown');
    if (userDropdown) {
        userDropdown.setAttribute('data-bs-display', 'static');
    }
});