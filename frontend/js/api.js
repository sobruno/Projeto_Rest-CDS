(function () {
  "use strict";

  /*
   * CAMADA DE INTEGRAÇÃO COM O BACKEND SPRING BOOT (API GATEWAY)
   * Base URL do Gateway: http://localhost:8080/api
   */

  // Detecta dinamicamente o IP/host de onde o frontend foi acessado (ex: localhost, 192.168.x.x)
  const API_HOST = window.location.hostname || "localhost";
  const API_PROTOCOL = window.location.protocol === "https:" ? "https:" : "http:";
  const API_BASE_URL = `${API_PROTOCOL}//${API_HOST}:8080/api`;
  const DEFAULT_HEADERS = { "Content-Type": "application/json" };

  function getAuthToken() {
    return (
      localStorage.getItem("cds_token") || sessionStorage.getItem("cds_token")
    );
  }

  function setAuthToken(token) {
    if (!token) {
      clearAuthToken();
      return;
    }
    localStorage.setItem("cds_token", token);
  }

  function clearAuthToken() {
    localStorage.removeItem("cds_token");
    sessionStorage.removeItem("cds_token");
  }

  function buildHeaders(extraHeaders = {}, isPublic = false) {
    const headers = { ...DEFAULT_HEADERS, ...extraHeaders };
    const token = getAuthToken();

    if (token && !isPublic) {
      headers.Authorization = `Bearer ${token}`;
    }

    return headers;
  }

  function normalizeResponse(responseText) {
    if (!responseText) {
      return null;
    }

    try {
      return JSON.parse(responseText);
    } catch (error) {
      return responseText;
    }
  }

  async function request(endpoint, options = {}) {
    const finalEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
    const method = (options.method || "GET").toUpperCase();
    const isPublic = finalEndpoint.startsWith("/auth");

    const requestConfig = {
      method,
      headers: buildHeaders(options.headers || {}, isPublic),
      ...options,
    };

    if (
      method !== "GET" &&
      method !== "HEAD" &&
      requestConfig.body !== undefined &&
      typeof requestConfig.body !== "string"
    ) {
      requestConfig.body = JSON.stringify(requestConfig.body);
    }

    let response;
    try {
      response = await fetch(
        `${API_BASE_URL}${finalEndpoint}`,
        requestConfig,
      );
    } catch (fetchError) {
      console.error("Falha ao conectar com o backend:", fetchError);
      throw new Error(
        "Não foi possível conectar ao servidor. Verifique se o backend está em execução.",
      );
    }

    const responseText = await response.text();
    const payload = normalizeResponse(responseText);

    if (!response.ok) {
      let message = "";
      if (payload) {
        if (typeof payload === "string" && payload.trim().length > 0) {
          message = payload.trim();
        } else if (typeof payload === "object") {
          message =
            payload.erro ||
            payload.error ||
            payload.message ||
            payload.msg ||
            payload.details ||
            "";
        }
      }

      if (!message) {
        if (response.status === 401) {
          message = "Credenciais inválidas. Verifique seu nome de usuário e senha.";
        } else if (response.status === 400) {
          message = "Dados inválidos. Verifique os campos informados.";
        } else if (response.status === 403) {
          message = "Acesso não autorizado.";
        } else if (response.status === 404) {
          message = "Recurso não encontrado no servidor.";
        } else if (response.status >= 500) {
          message = "Erro interno no servidor. Tente novamente mais tarde.";
        } else {
          message = "Erro ao comunicar com o backend.";
        }
      }

      throw new Error(message);
    }

    return payload;
  }

  const api = {
    setToken(token) {
      setAuthToken(token);
      return token;
    },

    clearToken() {
      clearAuthToken();
    },

    login(payload) {
      const body = {
        nome: (payload.nome || payload.username || "").trim(),
        senha: payload.senha || payload.password || "",
      };

      return request("/auth/login", {
        method: "POST",
        body,
      }).then((data) => {
        const token =
          data?.token || data?.accessToken || data?.jwt || data?.data?.token;

        if (token) {
          setAuthToken(token);
        }

        return data;
      });
    },

    registerUser(payload) {
      const body = {
        nome: (payload.nome || payload.username || "").trim(),
        endereco: (payload.endereco || payload.address || "").trim(),
        senha: payload.senha || payload.password || "",
      };

      return request("/auth/cadastrar", {
        method: "POST",
        body,
      });
    },

    // ALINHADO COM O ContaController.java (/contas/minha-conta)
    getProfile() {
      return request("/contas/minha-conta");
    },

    getAddresses() {
      return request("/enderecos");
    },

    addAddress(payload) {
      return request("/enderecos", {
        method: "POST",
        body: payload,
      });
    },

    // ALINHADO COM O ContaController.java (/contas/depositar)
    deposit(valor) {
      return request("/contas/depositar", {
        method: "POST",
        body: { valor },
      });
    },

    // ALINHADO COM O ContaController.java (/contas/transferir)
    transfer(payload) {
      return request("/contas/transferir", {
        method: "POST",
        body: payload,
      });
    },

    // Busca o extrato utilizando a conta do próprio usuário logado
    async getTransactions(limit = 5) {
      const conta = await this.getProfile();
      const contaId = conta.id || (conta.content && conta.content.id);
      if (!contaId) return [];

      const extrato = await request(`/contas/${contaId}/extrato`);
      return Array.isArray(extrato) ? extrato.slice(0, limit) : [];
    },

    logout() {
      clearAuthToken();
      return true;
    },

    /**
     * Estabelece conexão WebSocket com o API Gateway para atualizações em tempo real
     */
    connectWebSocket(onNotification, onStatusChange) {
      const token = getAuthToken();
      if (!token) return null;

      const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";
      const wsUrl = `${wsProtocol}//${API_HOST}:8080/ws/notificacoes?token=${encodeURIComponent(token)}`;

      let socket = null;
      let reconnectTimer = null;

      function connect() {
        if (typeof onStatusChange === "function") {
          onStatusChange("connecting");
        }
        try {
          socket = new WebSocket(wsUrl);

          socket.onopen = function () {
            console.log("[WS] Conectado ao canal WebSocket de notificações.");
            if (typeof onStatusChange === "function") {
              onStatusChange("connected");
            }
            try {
              socket.send(JSON.stringify({ tipo: "AUTH", token }));
            } catch (e) {}
          };

          socket.onmessage = function (event) {
            try {
              const data = JSON.parse(event.data);
              if (typeof onNotification === "function") {
                onNotification(data);
              }
            } catch (err) {
              console.warn("[WS] Mensagem não parseável:", event.data);
            }
          };

          socket.onclose = function () {
            console.log("[WS] Conexão encerrada. Reconectando em 4 segundos...");
            if (typeof onStatusChange === "function") {
              onStatusChange("disconnected");
            }
            clearTimeout(reconnectTimer);
            reconnectTimer = setTimeout(connect, 4000);
          };

          socket.onerror = function (err) {
            console.error("[WS] Erro no WebSocket:", err);
            if (typeof onStatusChange === "function") {
              onStatusChange("error");
            }
          };
        } catch (e) {
          console.error("[WS] Exceção ao abrir WebSocket:", e);
          if (typeof onStatusChange === "function") {
            onStatusChange("disconnected");
          }
        }
      }

      connect();
      return {
        close: () => {
          clearTimeout(reconnectTimer);
          if (socket) socket.close();
        }
      };
    }
  };

  window.CDSApi = api;
})();
