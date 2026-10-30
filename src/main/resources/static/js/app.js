// Image preview on the complaint form
document.addEventListener("DOMContentLoaded", function () {
  var input = document.getElementById("image");
  var preview = document.getElementById("preview");
  if (input && preview) {
    input.addEventListener("change", function () {
      var file = input.files && input.files[0];
      if (!file) { preview.classList.add("d-none"); return; }
      if (file.size > 5 * 1024 * 1024) {
        alert("Image must be 5 MB or smaller.");
        input.value = "";
        preview.classList.add("d-none");
        return;
      }
      preview.src = URL.createObjectURL(file);
      preview.classList.remove("d-none");
    });
  }
});
