(function() {
    // Aplicar tema inmediatamente para evitar parpadeo blanco
    const temaGuardado = localStorage.getItem("tema-seleccionado") || "dark-mode";
    document.documentElement.classList.add(temaGuardado);

    // Sincronizar radios cuando cargue el DOM
    document.addEventListener("DOMContentLoaded", function() {
        const darkInput = document.getElementById("darkMode");
        const lightInput = document.getElementById("lightMode");
        if(darkInput) darkInput.checked = (temaGuardado === "dark-mode");
        if(lightInput) lightInput.checked = (temaGuardado === "light-mode");
    });

    // Seguridad: Bloqueo de historial hacia atrás
    window.history.forward();
    window.addEventListener('pageshow', function(event) {
        if (event.persisted || (typeof window.performance != "undefined" && window.performance.navigation.type === 2)) {
            window.location.href = "/login";
        }
    });
})();