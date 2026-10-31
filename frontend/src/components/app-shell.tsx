"use client";

import {
  ChevronDown,
  FlaskConical,
  History,
  LayoutGrid,
  LoaderCircle,
  LogOut,
  Sparkles,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import { useAuth } from "@/components/auth-provider";
import { DevPanel } from "@/components/dev-panel";
import { IdentityGate } from "@/components/identity-gate";

export function AppShell({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const [devOpen, setDevOpen] = useState(false);

  if (auth.isLoading) {
    return (
      <main className="boot-screen">
        <div className="brand-mark" aria-hidden="true">
          <Sparkles size={21} />
        </div>
        <LoaderCircle className="spin" size={21} />
        <p>正在打开素材工作台…</p>
      </main>
    );
  }

  if (auth.error) {
    return (
      <main className="boot-screen">
        <div className="boot-error-mark">!</div>
        <h1>工作台暂时无法连接</h1>
        <p>{auth.error.message}</p>
        <button className="primary-button" type="button" onClick={auth.retry}>
          重新连接
        </button>
      </main>
    );
  }

  if (!auth.currentUser) {
    return <IdentityGate />;
  }

  return (
    <div className="site-shell">
      <Header onOpenDev={() => setDevOpen(true)} />
      <main className="app-main">{children}</main>
      <footer className="site-footer">
        <span>MATERIAL STUDIO / 2027</span>
        <span>把想法排成看得见的作品。</span>
      </footer>
      <DevPanel
        userId={auth.currentUser.id}
        open={devOpen}
        onClose={() => setDevOpen(false)}
      />
    </div>
  );
}

function Header({ onOpenDev }: { onOpenDev: () => void }) {
  const auth = useAuth();
  const pathname = usePathname();
  const [accountOpen, setAccountOpen] = useState(false);
  if (!auth.currentUser) {
    return null;
  }

  const primaryUser = auth.currentUser;

  return (
    <header className="site-header">
      <Link className="brand" href="/" aria-label="Material Studio 首页">
        <span className="brand-mark" aria-hidden="true">
          <Sparkles size={20} />
        </span>
        <span className="brand-copy">
          <strong>MATERIAL</strong>
          <span>STUDIO</span>
        </span>
      </Link>

      <nav className="primary-nav" aria-label="主要导航">
        <Link
          href="/"
          className={pathname === "/" ? "nav-link active" : "nav-link"}
        >
          <LayoutGrid size={17} />
          模板库
        </Link>
        <Link
          href="/history"
          className={pathname === "/history" ? "nav-link active" : "nav-link"}
        >
          <History size={17} />
          我的制作
        </Link>
      </nav>

      <div className="header-actions">
        <button
          type="button"
          className="icon-button dev-button"
          aria-label="打开后台模拟设置"
          title="后台模拟设置"
          onClick={onOpenDev}
        >
          <FlaskConical size={18} />
        </button>
        <div className="account-wrap">
          <button
            type="button"
            className="account-trigger"
            aria-haspopup="menu"
            aria-expanded={accountOpen}
            onClick={() => setAccountOpen((open) => !open)}
          >
            <span
              className="avatar"
              style={{ backgroundColor: primaryUser.avatarColor }}
            >
              {primaryUser.displayName.slice(0, 1)}
            </span>
            <span className="account-name">{primaryUser.displayName}</span>
            <ChevronDown
              size={15}
              className={accountOpen ? "chevron-up" : ""}
            />
          </button>
          {accountOpen ? (
            <div className="account-menu" role="menu">
              <p className="menu-label">切换演示身份</p>
              {auth.users.map((user) => (
                <button
                  key={user.id}
                  type="button"
                  className={
                    user.id === primaryUser.id
                      ? "menu-user active"
                      : "menu-user"
                  }
                  role="menuitem"
                  disabled={auth.isSwitching}
                  onClick={async () => {
                    setAccountOpen(false);
                    if (user.id !== primaryUser.id) {
                      await auth.switchUser(user.id);
                    }
                  }}
                >
                  <span
                    className="avatar avatar-small"
                    style={{ backgroundColor: user.avatarColor }}
                  >
                    {user.displayName.slice(0, 1)}
                  </span>
                  <span>
                    <strong>{user.displayName}</strong>
                    <small>{user.email}</small>
                  </span>
                </button>
              ))}
              <div className="menu-divider" />
              <button
                type="button"
                className="menu-logout"
                role="menuitem"
                onClick={() => {
                  setAccountOpen(false);
                  void auth.logout();
                }}
              >
                <LogOut size={16} />
                退出演示身份
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  );
}
