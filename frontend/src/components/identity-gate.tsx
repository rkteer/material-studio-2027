"use client";

import { ArrowRight, Sparkles } from "lucide-react";
import { useAuth } from "@/components/auth-provider";

export function IdentityGate() {
  const { users, switchUser, isSwitching } = useAuth();

  return (
    <main className="identity-page">
      <div className="identity-orb identity-orb-one" />
      <div className="identity-orb identity-orb-two" />
      <section className="identity-card" aria-labelledby="identity-title">
        <div className="brand-mark brand-mark-large" aria-hidden="true">
          <Sparkles size={27} />
        </div>
        <p className="eyebrow">DEMO WORKSPACE</p>
        <h1 id="identity-title">今天以谁的身份创作？</h1>
        <p className="identity-copy">
          这是用于展示个人记录隔离的演示账号。选择后可随时从顶栏切换。
        </p>
        <div className="identity-options">
          {users.map((user, index) => (
            <button
              className="identity-option"
              key={user.id}
              onClick={() => void switchUser(user.id)}
              disabled={isSwitching}
              type="button"
            >
              <span
                className="avatar avatar-large"
                style={{ backgroundColor: user.avatarColor }}
              >
                {user.displayName.slice(0, 1)}
              </span>
              <span className="identity-option-copy">
                <strong>{user.displayName}</strong>
                <small>{user.email}</small>
              </span>
              <ArrowRight size={19} aria-hidden="true" />
              <span className="identity-number">0{index + 1}</span>
            </button>
          ))}
        </div>
        {isSwitching ? <p className="inline-status">正在进入工作台…</p> : null}
      </section>
    </main>
  );
}
