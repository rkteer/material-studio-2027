export const queryKeys = {
  auth: ["auth"] as const,
  templates: (userId: string, category: string) =>
    ["templates", userId, category] as const,
  template: (userId: string, templateId: string) =>
    ["template", userId, templateId] as const,
  tasks: (userId: string) => ["tasks", userId] as const,
  simulation: (userId: string) => ["simulation", userId] as const,
};
