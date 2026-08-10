document.addEventListener("DOMContentLoaded", function () {
    // Lista com o caminho das imagens de fundo
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

    // Transição suave via CSS inline
    body.style.transition = 'background-image 1.5s ease-in-out';

    setInterval(() => {
        currentIndex = (currentIndex + 1) % bgImages.length;
        body.style.backgroundImage = `linear-gradient(180deg, rgba(10, 13, 20, .55), rgba(10, 13, 20, .8)), url('${bgImages[currentIndex]}')`;
    }, 10000); // 10000 ms = 10 segundos
});