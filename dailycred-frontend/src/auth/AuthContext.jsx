import { createContext, useContext, useMemo, useState } from "react";

const AuthContext = createContext(null);

function readInitialAuth() {
  const token = localStorage.getItem("dailycred_token");
  const role = localStorage.getItem("dailycred_role");
  const userId = localStorage.getItem("dailycred_user_id");
  const displayName = localStorage.getItem("dailycred_display_name");
  const identifier = localStorage.getItem("dailycred_identifier");

  if (!token || !role || !userId) {
    return {
      token: null,
      role: null,
      userId: null,
      displayName: null,
      identifier: null,
    };
  }

  return {
    token,
    role,
    userId,
    displayName,
    identifier,
  };
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readInitialAuth());

  const login = (payload) => {
    localStorage.setItem("dailycred_token", payload.token);
    localStorage.setItem("dailycred_role", payload.role);
    localStorage.setItem("dailycred_user_id", String(payload.userId));
    localStorage.setItem("dailycred_display_name", payload.displayName || "");
    localStorage.setItem("dailycred_identifier", payload.identifier || "");

    setAuth({
      token: payload.token,
      role: payload.role,
      userId: String(payload.userId),
      displayName: payload.displayName || "",
      identifier: payload.identifier || "",
    });
  };

  const logout = () => {
    localStorage.removeItem("dailycred_token");
    localStorage.removeItem("dailycred_role");
    localStorage.removeItem("dailycred_user_id");
    localStorage.removeItem("dailycred_display_name");
    localStorage.removeItem("dailycred_identifier");

    setAuth({
      token: null,
      role: null,
      userId: null,
      displayName: null,
      identifier: null,
    });
  };

  const value = useMemo(
    () => ({
      ...auth,
      isAuthenticated: Boolean(auth.token),
      login,
      logout,
    }),
    [auth]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);

  if (!value) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return value;
}