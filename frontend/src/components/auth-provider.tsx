"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createContext,
  useContext,
  useEffect,
  type ReactNode,
} from "react";
import { apiRequest } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import type { AuthResponse, User } from "@/lib/types";

interface AuthContextValue {
  currentUser: User | null;
  users: User[];
  isLoading: boolean;
  error: Error | null;
  isSwitching: boolean;
  switchUser: (userId: string) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  retry: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const authQuery = useQuery({
    queryKey: queryKeys.auth,
    queryFn: ({ signal }) =>
      apiRequest<AuthResponse>("/api/auth/me", { signal }),
    retry: 1,
  });

  const switchMutation = useMutation({
    mutationFn: (userId: string) =>
      apiRequest<AuthResponse>("/api/auth/demo", {
        method: "POST",
        body: JSON.stringify({ userId }),
      }),
    onSuccess: async (response) => {
      const previousUserId = authQuery.data?.currentUser?.id;
      await queryClient.cancelQueries({
        predicate: (query) => query.queryKey[0] !== "auth",
      });
      queryClient.removeQueries({
        predicate: (query) => query.queryKey[0] !== "auth",
      });
      queryClient.setQueryData(queryKeys.auth, response);
      if (previousUserId !== response.currentUser?.id) {
        queryClient.removeQueries({
          predicate: (query) =>
            query.queryKey[0] !== "auth" &&
            query.queryKey[1] !== response.currentUser?.id,
        });
      }
    },
  });

  const logoutMutation = useMutation({
    mutationFn: () =>
      apiRequest<void>("/api/auth/logout", { method: "POST" }),
    onSuccess: async () => {
      await queryClient.cancelQueries({
        predicate: (query) => query.queryKey[0] !== "auth",
      });
      queryClient.removeQueries({
        predicate: (query) => query.queryKey[0] !== "auth",
      });
      queryClient.setQueryData<AuthResponse>(queryKeys.auth, (current) => ({
        currentUser: null,
        users: current?.users ?? [],
      }));
    },
  });

  const currentUser = authQuery.data?.currentUser ?? null;
  useEffect(() => {
    if (!currentUser) {
      return;
    }
    void queryClient.cancelQueries({
      predicate: (query) =>
        query.queryKey[0] !== "auth" &&
        query.queryKey[1] !== currentUser.id,
    });
    queryClient.removeQueries({
      predicate: (query) =>
        query.queryKey[0] !== "auth" &&
        query.queryKey[1] !== currentUser.id,
    });
  }, [currentUser, queryClient]);

  const value: AuthContextValue = {
    currentUser,
    users: authQuery.data?.users ?? [],
    isLoading: authQuery.isPending,
    error: authQuery.error,
    isSwitching: switchMutation.isPending,
    switchUser: switchMutation.mutateAsync,
    logout: async () => {
      await logoutMutation.mutateAsync();
    },
    retry: () => {
      void authQuery.refetch();
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
