(function () {
  "use strict";

  /*
   * =========================================
   * CAMADA DE INTEGRAÇÃO COM O BACKEND
   * =========================================
   *
   * Editar aqui somente quando o amigo do Spring Boot estiver pronto:
   * 1) API_BASE_URL
   * 2) os endpoints abaixo, se ele usar nomes diferentes
   * 3) o formato da resposta JWT/token, caso venha em outra chave
   *
   * Convenção usada neste projeto:
   * - base: http://localhost:8080/api
   * - autenticação via Bearer token no header Authorization
   * - respostas em JSON
   *
   * Exemplo real de backend em Spring Boot:
   * POST /api/auth/login
   * POST /api/usuarios
   * GET  /api/usuarios/me
   * GET  /api/enderecos
   * POST /api/enderecos
   * GET  /api/transacoes?limit=5
   * POST /api/depositos
   * POST /api/transferencias
   */

  const API_BASE_URL = "http://localhost:8080/api";
  const DEFAULT_HEADERS = { "Content-Type": "application/json" };

  function getAuthToken() {
    return localStorage.getItem("cds_token") || sessionStorage.getItem("cds_token");
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

    if (method !== "GET" && method !== "HEAD" && requestConfig.body !== undefined && typeof requestConfig.body !== "string") {
      requestConfig.body = JSON.stringify(requestConfig.body);
    }

    const response = await fetch(`${API_BASE_URL}${finalEndpoint}`, requestConfig);
    const responseText = await response.text();
    const payload = normalizeResponse(responseText);

    if (!response.ok) {
      const message =
        (payload && (payload.message || payload.error || payload.msg || payload.details)) ||
        "Erro ao comunicar com o backend.";

      throw new Error(message);
    }

    return payload;
  }

  const api = {
    /*
     * Login e autenticação.
     * Se o backend retornar a chave "token", "accessToken" ou "jwt",
     * o método abaixo já salva no localStorage.
     */
    setToken(token) {
      setAuthToken(token);
      return token;
    },

    clearToken() {
      clearAuthToken();
    },

    login(payload) {
      // EDITAR AQUI: endpoint de autenticação, se o seu amigo usar outro nome.
      return request("/auth/login", {
        method: "POST",
        body: payload,
      }).then((data) => {
        const token = data?.token || data?.accessToken || data?.jwt || data?.data?.token;

        if (token) {
          setAuthToken(token);
        }

        return data;
      });
    },

    registerUser(payload) {
      // EDITAR AQUI: se o endpoint for /auth/register ou /usuarios/create.
      return request("/usuarios", {
        method: "POST",
        body: payload,
      });
    },

    getProfile() {
      // EDITAR AQUI: endpoint do perfil do usuário autenticado.
      return request("/usuarios/me");
    },

    getAddresses() {
      // EDITAR AQUI: endpoint para listar endereços salvos.
      return request("/enderecos");
    },

    addAddress(payload) {
      // EDITAR AQUI: endpoint para criar endereço favorito.
      return request("/enderecos", {
        method: "POST",
        body: payload,
      });
    },

    getTransactions(limit = 5) {
      // EDITAR AQUI: endpoint do extrato.
      return request(`/transacoes?limit=${limit}`);
    },

    deposit(valor) {
      // EDITAR AQUI: endpoint para depósito.
      return request("/depositos", {
        method: "POST",
        body: { valor },
      });
    },

    transfer(payload) {
      // EDITAR AQUI: endpoint para transferência.
      return request("/transferencias", {
        method: "POST",
        body: payload,
      });
    },

    logout() {
      clearAuthToken();
      return true;
    },
  };

  window.CDSApi = api;
})();
