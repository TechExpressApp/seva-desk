// Arrow buttons for sideways-scrolling rows (e.g. Upcoming Festivals on the home page).
// <button data-carousel-prev="ID"> / <button data-carousel-next="ID"> scroll the element with that id by one
// "page" of cards and are disabled at either end. Swiping, trackpads and the keyboard work without this script.
(function () {
  document.querySelectorAll('[data-carousel-next]').forEach(function (next) {
    var id = next.getAttribute('data-carousel-next');
    var track = document.getElementById(id);
    var prev = document.querySelector('[data-carousel-prev="' + id + '"]');
    if (!track) return;

    function step() {
      var card = track.firstElementChild;
      if (!card) return track.clientWidth;
      var gap = parseFloat(getComputedStyle(track).columnGap) || 0;
      var perView = Math.max(1, Math.round((track.clientWidth + gap) / (card.offsetWidth + gap)));
      return perView * (card.offsetWidth + gap);
    }
    function update() {
      var max = track.scrollWidth - track.clientWidth - 2;
      if (prev) prev.disabled = track.scrollLeft <= 2;
      next.disabled = track.scrollLeft >= max;
    }
    next.addEventListener('click', function () { track.scrollBy({ left: step() }); });
    if (prev) prev.addEventListener('click', function () { track.scrollBy({ left: -step() }); });
    track.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowRight') { e.preventDefault(); track.scrollBy({ left: step() }); }
      if (e.key === 'ArrowLeft') { e.preventDefault(); track.scrollBy({ left: -step() }); }
    });
    track.addEventListener('scroll', function () { window.requestAnimationFrame(update); }, { passive: true });
    window.addEventListener('resize', update);
    update();
  });
})();
