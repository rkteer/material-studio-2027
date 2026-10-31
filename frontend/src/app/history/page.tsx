"use client";

import { ArrowUpRight, CalendarDays, Sparkles } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/components/auth-provider";
import { TaskHistoryPanel } from "@/components/task-history-panel";

export default function HistoryPage() {
  const { currentUser } = useAuth();
  if (!currentUser) {
    return null;
  }

  return (
    <div className="page-wrap history-page">
      <section className="history-hero">
        <div>
          <p className="eyebrow">
            <CalendarDays size={14} />
            YOUR CREATIVE TIMELINE
          </p>
          <h1>每一次生成，<span>都留在这里。</span></h1>
          <p>
            未完成的任务会自动继续刷新；查询暂时中断时，已有状态仍会保留。
          </p>
        </div>
        <Link href="/" className="primary-button">
          <Sparkles size={17} />
          创建新作品
          <ArrowUpRight size={16} />
        </Link>
      </section>
      <TaskHistoryPanel userId={currentUser.id} />
    </div>
  );
}
