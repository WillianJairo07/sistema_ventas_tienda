// --- ELEMENTOS DEL DOM ---
const body = document.body;
const hamburgerWrapper = document.getElementById('hamburgerWrapper');
const mainContent = document.getElementById('mainContent');

// --- SIDEBAR ---
if (hamburgerWrapper) {
    hamburgerWrapper.addEventListener('click', (e) => {
        e.stopPropagation();
        body.classList.toggle('sidebar-open');
    });
}

if (mainContent) {
    mainContent.addEventListener('click', () => {
        if (window.innerWidth <= 768) {
            body.classList.remove('sidebar-open');
        }
    });
}

// --- TEMA ---
document.addEventListener("DOMContentLoaded", () => {
    const darkInput = document.getElementById("darkMode");
    const lightInput = document.getElementById("lightMode");

    function aplicarTema(tema) {
        document.documentElement.classList.remove("dark-mode", "light-mode");
        document.documentElement.classList.add(tema);
        localStorage.setItem("tema-seleccionado", tema);
    }

    darkInput?.addEventListener("change", () => aplicarTema("dark-mode"));
    lightInput?.addEventListener("change", () => aplicarTema("light-mode"));
});