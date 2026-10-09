(function () {
  "use strict";

  /*
   * CAMADA DE INTEGRAÇÃO COM O BACKEND SPRING BOOT (API GATEWAY)
   * Base URL do Gateway: http://localhost:8080/api
   */

  const API_BASE_URL = "http://localhost:8080/api";
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

  function buildHeaders(extraHeaders = {}) {
    const headers = { ...DEFAULT_HEADERS, ...extraHeaders };
    const token = getAuthToken();

    if (token) {
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

    const requestConfig = {
      method,
      headers: buildHeaders(options.headers || {}),
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

    const response = await fetch(
      `${API_BASE_URL}${finalEndpoint}`,
      requestConfig,
    );
    const responseText = await response.text();
    const payload = normalizeResponse(responseText);

    if (!response.ok) {
      const message =
        (payload &&
          (payload.erro ||
            payload.error ||
            payload.message ||
            payload.msg ||
            payload.details)) ||
        "Erro ao comunicar com o backend.";

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
        nome: payload.nome || payload.username,
        senha: payload.senha || payload.password,
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
        nome: payload.nome || payload.username,
        endereco: payload.endereco || payload.address,
        senha: payload.senha,
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
  };

  window.CDSApi = api;
})();
