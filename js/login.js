(function () {
  "use strict";

  /*
   * =========================================
   * LOGIN
   * =========================================
   *
   * Função: enviar username + password para o backend.
   * O backend deve validar o usuário e devolver um token JWT ou sessão.
   *
   * Se o seu amigo usar outro nome de endpoint, edite em js/api.js.
   * Esta página só precisa enviar o payload certo e redirecionar.
   */

  const form = document.getElementById("form-login");

  if (!form) {
    return;
  }

  function showError(message) {
    const errorBox = document.getElementById("erro-login");
    if (!errorBox) {
      return;
    }

    errorBox.textContent = message;
    errorBox.classList.remove("hidden");
  }

  function clearError() {
    const errorBox = document.getElementById("erro-login");
    if (!errorBox) {
      return;
    }

    errorBox.textContent = "";
    errorBox.classList.add("hidden");
  }

  form.addEventListener("submit", async function (event) {
    event.preventDefault();
    clearError();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const submitButton = form.querySelector("button[type='submit']");

    if (!username || !password) {
      showError("Preencha usuário e senha antes de entrar.");
      return;
    }

    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = "Entrando...";
    }

    try {
      const data = await window.CDSApi.login({ username, password });

      // O backend pode responder com token, accessToken, jwt ou objeto aninhado.
      // Se o token não vier, a página continua, mas o fluxo de autenticação
      // precisa ser ajustado no backend para devolver um token válido.
      const token = data?.token || data?.accessToken || data?.jwt || data?.data?.token;

      if (token) {
        window.CDSApi.setToken(token);
      }

      window.location.href = "index.html";
    } catch (error) {
      console.error("Erro no login:", error);
      showError(error.message || "Não foi possível entrar. Verifique usuário e senha.");
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = "Entrar";
      }
    }
  });
})();
