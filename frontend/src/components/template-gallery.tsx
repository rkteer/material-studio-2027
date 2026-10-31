"use client";

import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  ArrowUpRight,
  ImagePlus,
  LoaderCircle,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { apiRequest } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import type { TemplateListResponse } from "@/lib/types";

export function TemplateGallery() {
  const { currentUser } = useAuth();
  const [category, setCategory] = useState("");
  if (!currentUser) {
    return null;
  }

  return (
    <TemplateGalleryContent
      userId={currentUser.id}
      category={category}
      onCategoryChange={setCategory}
    />
  );
}

function TemplateGalleryContent({
  userId,
  category,
  onCategoryChange,
}: {
  userId: string;
  category: string;
  onCategoryChange: (category: string) => void;
}) {
  const templatesQuery = useQuery({
    queryKey: queryKeys.templates(userId, category),
    queryFn: ({ signal }) => {
      const query = category
        ? `?category=${encodeURIComponent(category)}`
        : "";
      return apiRequest<TemplateListResponse>(`/api/templates${query}`, {
        signal,
      });
    },
    placeholderData: (previous) => previous,
  });

  const templates = templatesQuery.data?.templates ?? [];
  const categories = templatesQuery.data?.categories ?? [];

  return (
    <div className="page-wrap">
      <section className="gallery-hero">
        <div className="hero-copy">
          <p className="eyebrow">
            <Sparkles size={14} />
            CREATE SOMETHING WORTH KEEPING
          </p>
          <h1>
            把照片，
            <br />
            做成<span>作品。</span>
          </h1>
          <p className="hero-description">
            挑一个版式，上传你的瞬间。后台会按步骤完成检查、生成与整理，
            你可以继续浏览，也可以一次开始多个灵感。
          </p>
        </div>
        <div className="hero-stat">
          <span className="hero-stat-number">03</span>
          <span className="hero-stat-label">个后台处理阶段</span>
          <span className="hero-stat-line" />
          <span className="hero-stat-foot">CHECK · MAKE · FINISH</span>
        </div>
      </section>

      <section className="gallery-section" aria-labelledby="template-heading">
        <div className="section-heading">
          <div>
            <p className="eyebrow">CURATED TEMPLATES</p>
            <h2 id="template-heading">选一个开始的理由</h2>
          </div>
          <span className="result-count">
            {templatesQuery.isFetching && !templatesQuery.isPending ? (
              <LoaderCircle size={14} className="spin" />
            ) : null}
            {templates.length.toString().padStart(2, "0")} 个模板
          </span>
        </div>

        <div className="category-tabs" role="tablist" aria-label="模板分类">
          <button
            type="button"
            role="tab"
            aria-selected={category === ""}
            className={category === "" ? "category-tab active" : "category-tab"}
            onClick={() => onCategoryChange("")}
          >
            全部
          </button>
          {categories.map((item) => (
            <button
              key={item}
              type="button"
              role="tab"
              aria-selected={category === item}
              className={category === item ? "category-tab active" : "category-tab"}
              onClick={() => onCategoryChange(item)}
            >
              {item}
            </button>
          ))}
        </div>

        {templatesQuery.isError ? (
          <div className="large-state" role="alert">
            <AlertCircle size={26} />
            <h3>模板暂时没有加载出来</h3>
            <p>{templatesQuery.error.message}</p>
            <button
              className="secondary-button"
              type="button"
              onClick={() => void templatesQuery.refetch()}
            >
              <RefreshCw size={16} />
              重新加载
            </button>
          </div>
        ) : templatesQuery.isPending ? (
          <div className="template-grid">
            {Array.from({ length: 6 }).map((_, index) => (
              <div className="template-card skeleton-template" key={index}>
                <span className="skeleton-block skeleton-cover" />
                <span className="skeleton-line wide" />
                <span className="skeleton-line medium" />
              </div>
            ))}
          </div>
        ) : templates.length === 0 ? (
          <div className="large-state">
            <ImagePlus size={28} />
            <h3>这个分类还没有模板</h3>
            <p>换个分类看看，新的创作方向正在准备中。</p>
            <button
              className="secondary-button"
              type="button"
              onClick={() => onCategoryChange("")}
            >
              查看全部模板
            </button>
          </div>
        ) : (
          <div className="template-grid">
            {templates.map((template, index) => (
              <Link
                href={`/templates/${template.id}`}
                className="template-card"
                key={template.id}
                style={{ animationDelay: `${index * 55}ms` }}
              >
                <div className="template-cover-wrap">
                  {/* Template covers are local assets returned by the Java API. */}
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    className="template-cover"
                    src={template.coverUrl}
                    alt=""
                  />
                  <span className="template-category">{template.category}</span>
                  <span className="template-open" aria-hidden="true">
                    <ArrowUpRight size={18} />
                  </span>
                </div>
                <div className="template-card-copy">
                  <div>
                    <h3>{template.title}</h3>
                    <p>{template.description}</p>
                  </div>
                  <span className="template-assets">
                    <ImagePlus size={14} />
                    {template.requiredImageCount} 张素材
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
