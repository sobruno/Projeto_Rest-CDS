(function () {
  "use strict";

  /*
   * =========================================
   * CADASTRO
   * =========================================
   *
   * Função: capturar os campos do formulário e enviar para o backend.
   * O backend deve criar o usuário e, se necessário, hash da senha.
   *
   * Payload esperado no backend:
   * {
   *   username: "nome do usuário",
   *   endereco: "identificador do endereço/conta",
   *   senha: "senha"
   * }
   *
   * Se o seu amigo usar outro endpoint, edite em js/api.js.
   */

  const form = document.getElementById("form-cadastro");

  if (!form) {
    return;
  }

  function showError(message) {
    const errorBox = document.getElementById("erro-cadastro");
    if (!errorBox) {
      return;
    }

    errorBox.textContent = message;
    errorBox.classList.remove("hidden");
  }

  function clearError() {
    const errorBox = document.getElementById("erro-cadastro");
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
    const endereco = document.getElementById("endereco").value.trim();
    const enderecoConfirm = document.getElementById("endereco_confirm").value.trim();
    const senha = document.getElementById("senha").value;
    const senhaConfirm = document.getElementById("senha_confirm").value;

    if (!username || !endereco || !senha) {
      showError("Preencha todos os campos obrigatórios.");
      return;
    }

    if (endereco !== enderecoConfirm) {
      showError("Os endereços informados não conferem.");
      return;
    }

    if (senha !== senhaConfirm) {
      showError("As senhas informadas não conferem.");
      return;
    }

    const submitButton = form.querySelector("button[type='submit']");

    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = "Cadastrando...";
    }

    try {
      await window.CDSApi.registerUser({
        username,
        endereco,
        senha,
      });

      window.location.href = "login.html";
    } catch (error) {
      console.error("Erro no cadastro:", error);
      showError(error.message || "Não foi possível concluir o cadastro.");
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = "Cadastrar";
      }
    }
  });
})();
