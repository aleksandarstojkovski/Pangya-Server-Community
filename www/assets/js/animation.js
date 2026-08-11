document.addEventListener("DOMContentLoaded", function () {
    const loader = document.getElementById('page-loader');

    // Função para ocultar o loader suavemente
    function hideLoader() {
        if (loader && !loader.classList.contains('is-hidden')) {
            loader.classList.add('is-hidden');
            setTimeout(() => loader.remove(), 300);
        }
    }

    // Esconde o loader assim que a janela (com imagens e estilos) carregar totalmente
    if (document.readyState === 'complete') {
        hideLoader();
    } else {
        window.addEventListener('load', hideLoader);
    }

    // Trava de segurança: esconde o loader após no máximo 3 segundos,
    // mesmo que alguma imagem da página demore a carregar.
    setTimeout(hideLoader, 3000);

    // --- SLIDER DE IMAGENS DE FUNDO ---
    const bgImages = [
        'assets/img/bg/1.jpg',
        'assets/img/bg/2.jpg',
        'assets/img/bg/3.jpg',
        'assets/img/bg/4.jpg',
        'assets/img/bg/5.png',
        'assets/img/bg/6.jpg'
    ];

    let currentIndex = 0;
    const body = document.getElementById('bg-slider');

    if (body) {
        body.style.transition = 'background-image 1.5s ease-in-out';

        setInterval(() => {
            currentIndex = (currentIndex + 1) % bgImages.length;
            body.style.backgroundImage = `linear-gradient(180deg, rgba(10, 13, 20, .55), rgba(10, 13, 20, .8)), url('${bgImages[currentIndex]}')`;
        }, 10000); // Troca a cada 10 segundos
    }
});