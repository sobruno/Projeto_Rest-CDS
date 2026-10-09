(function () {
  "use strict";

  /*
   * DASHBOARD / CARTEIRA
   */

  const pageBody = document.body;

  function formatCurrency(value) {
    const numericValue = Number(value || 0);
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(numericValue);
  }

  function formatDate(dateValue) {
    if (!dateValue) {
      return "Data indisponível";
    }

    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
      return dateValue;
    }

    return new Intl.DateTimeFormat("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(date);
  }

  function getInitials(name) {
    if (!name) {
      return "U";
    }

    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }

    return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  }

  function renderProfile(profile) {
    const nomeUsuario = document.getElementById("nome-usuario");
    const saldo = document.getElementById("saldo");
    const saldoModal = document.getElementById("saldo-modal");
    const avatar = document.getElementById("avatar");

    // ALINHADO COM O ContaDto.java (procura titular ou nome)
    const nomeExibicao = profile?.titular || profile?.nome || "Usuário";

    if (nomeUsuario) {
      nomeUsuario.textContent = nomeExibicao;
    }

    if (saldo) {
      saldo.textContent = formatCurrency(profile?.saldo || 0);
    }

    if (saldoModal) {
      saldoModal.textContent = formatCurrency(profile?.saldo || 0);
    }

    if (avatar) {
      avatar.textContent = getInitials(nomeExibicao);
    }
  }

  function renderAddresses(addresses) {
    const list = document.getElementById("lista-enderecos");
    if (!list) {
      return;
    }

    if (!addresses || addresses.length === 0) {
      list.innerHTML =
        '<li class="rounded-lg border border-dashed border-line p-3 text-sm text-mute">Nenhum endereço salvo.</li>';
      return;
    }

    list.innerHTML = addresses
      .map((item) => {
        const nome = item.nome || "Usuário";
        const endereco = item.endereco || item.address || "";
        return `
          <li>
            <button type="button" data-endereco="${endereco}" class="w-full rounded-lg border border-line bg-white px-4 py-3 text-left hover:border-brand focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand">
              <span class="block text-xs text-mute">Nome de usuário</span>
              <span class="block truncate text-sm font-semibold">${nome}</span>
              <span class="mt-1.5 block text-xs text-mute">Endereço / Conta ID</span>
              <span class="block truncate text-sm">${endereco}</span>
            </button>
          </li>
        `;
      })
      .join("");

    document.querySelectorAll("[data-endereco]").forEach((button) => {
      button.addEventListener("click", function () {
        const target = document.getElementById("tr-dest");
        if (target) {
          target.value = button.getAttribute("data-endereco");
        }

        const dialog = document.getElementById("dlg-transferir");
        if (dialog && typeof dialog.showModal === "function") {
          dialog.showModal();
        }
      });
    });
  }

  function renderTransactions(transactions, myContaId) {
    const list = document.getElementById("lista-transacoes");
    if (!list) {
      return;
    }

    if (!transactions || transactions.length === 0) {
      list.innerHTML =
        '<li class="rounded-lg border border-dashed border-line p-3 text-sm text-mute">Nenhuma transação registrada.</li>';
      return;
    }

    list.innerHTML = transactions
      .map((item) => {
        // ALINHADO COM O Transacao.java (trata dataHora, DEPOSITO e TRANSFERENCIA)
        const isDeposito = item.tipo === "DEPOSITO";
        const isEntrada =
          isDeposito ||
          (item.contaDestinoId &&
            String(item.contaDestinoId) === String(myContaId));
        const positivo = isEntrada;
        const sinal = positivo ? "+" : "−";

        let descricao = "Transação";
        if (isDeposito) {
          descricao = "Depósito Realizado";
        } else if (positivo) {
          descricao = `Transferência Recebida (Conta #${item.contaOrigemId})`;
        } else {
          descricao = `Transferência Enviada (Conta #${item.contaDestinoId})`;
        }

        const dataOriginal = item.dataHora || item.data;
        const dataTexto = dataOriginal
          ? formatDate(dataOriginal)
          : "Data indisponível";
        const textoSentido = positivo ? "Recebido" : "Enviado";
        const valor = Number(item.valor || 0);

        return `
          <li class="flex items-center gap-3 rounded-lg px-4 py-3 ${positivo ? "bg-entrada/10 text-entrada" : "bg-saida/10 text-saida"}">
            <svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="${positivo ? "M19 12H5M11 6l-6 6 6 6" : "M5 12h14M13 6l6 6-6 6"}"/>
            </svg>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-semibold">${descricao}</p>
              <p class="text-xs opacity-80">${textoSentido} em ${dataTexto}</p>
            </div>
            <p class="text-base font-bold tabular-nums">${sinal} ${formatCurrency(valor)}</p>
          </li>
        `;
      })
      .join("");
  }

  function bindDialogActions() {
    document.querySelectorAll("[data-open]").forEach((button) => {
      button.addEventListener("click", function () {
        const targetId = button.getAttribute("data-open");
        const dialog = document.getElementById(targetId);
        if (dialog && typeof dialog.showModal === "function") {
          dialog.showModal();
        }
      });
    });

    document.querySelectorAll("[data-close]").forEach((button) => {
      button.addEventListener("click", function () {
        const dialog = button.closest("dialog");
        if (dialog && typeof dialog.close === "function") {
          dialog.close();
        }
      });
    });

    document.querySelectorAll("dialog").forEach((dialog) => {
      dialog.addEventListener("click", function (event) {
        if (event.target === dialog) {
          dialog.close();
        }
      });
    });
  }

  function bindFormSubmitters() {
    const depositForm = document.getElementById("form-depositar");
    if (depositForm) {
      depositForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const valueInput = document.getElementById("dep-valor");
        const valor = Number((valueInput.value || "").replace(",", "."));

        if (!valor || valor <= 0) {
          alert("Informe um valor válido para depósito.");
          return;
        }

        try {
          await window.CDSApi.deposit(valor);
          document.getElementById("dlg-depositar").close();
          depositForm.reset();
          await loadDashboard();
        } catch (error) {
          console.error("Erro no depósito:", error);
          alert(error.message || "Não foi possível concluir o depósito.");
        }
      });
    }

    const transferForm = document.getElementById("form-transferir");
    if (transferForm) {
      transferForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const destinatario = document.getElementById("tr-dest").value.trim();
        const valor = Number(
          (document.getElementById("tr-valor").value || "").replace(",", "."),
        );

        if (!destinatario) {
          alert("Informe a conta de destino.");
          return;
        }

        if (!valor || valor <= 0) {
          alert("Informe um valor válido para transferência.");
          return;
        }

          // Envia destinatário (chave/endereço ou ID) e valor
          await window.CDSApi.transfer({
            destinatario,
            valor,
          });
          document.getElementById("dlg-transferir").close();
          transferForm.reset();
          await loadDashboard();
        } catch (error) {
          console.error("Erro na transferência:", error);
          alert(error.message || "Não foi possível concluir a transferência.");
        }
      });
    }

    const addressForm = document.getElementById("form-endereco");
    if (addressForm) {
      addressForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const nome = document.getElementById("end-nome").value.trim();
        const endereco = document.getElementById("end-endereco").value.trim();

        if (!nome || !endereco) {
          alert("Informe nome e endereço antes de salvar.");
          return;
        }

        try {
          await window.CDSApi.addAddress({ nome, endereco });
          document.getElementById("dlg-endereco").close();
          addressForm.reset();
          await loadDashboard();
        } catch (error) {
          console.error("Erro ao salvar endereço:", error);
          alert(error.message || "Não foi possível salvar o endereço.");
        }
      });
    }
  }

  async function loadDashboard() {
    if (!window.CDSApi) {
      return;
    }

    try {
      const [profile, addresses, transactions] = await Promise.all([
        window.CDSApi.getProfile(),
        window.CDSApi.getAddresses().catch(() => []),
        window.CDSApi.getTransactions(5).catch(() => []),
      ]);

      renderProfile(profile || { titular: "Usuário", saldo: 0 });
      renderAddresses(addresses || []);
      renderTransactions(transactions || [], profile?.id);
    } catch (error) {
      console.warn("Backend indisponível ou token expirado:", error.message);
    }
  }

  function bindLogoutAction() {
    const logoutButton = document.getElementById("btn-sair");
    if (!logoutButton) {
      return;
    }

    logoutButton.addEventListener("click", function () {
      window.CDSApi.logout();
      window.location.href = "login.html";
    });
  }

  function initDashboard() {
    if (!pageBody) {
      return;
    }

    bindDialogActions();
    bindFormSubmitters();
    bindLogoutAction();
    loadDashboard();
  }

  initDashboard();
})();
