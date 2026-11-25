import type { Article, ArticleBody } from '../types'
import { listLocalArticles, getLocalArticleBody } from './local'

function newestFirst(a: Article, b: Article): number {
  if (a.pinned !== b.pinned) return a.pinned ? -1 : 1
  return b.publishedAt.localeCompare(a.publishedAt)
}

/**
 * Swappable article data-source facade.
 * Exposed as async Promises so switching to the backend `/api/posts` later
 * requires replacing only this module, without changing store or view contracts.
 */
export async function getArticles(): Promise<Article[]> {
  return listLocalArticles().sort(newestFirst)
}

export async function getArticleBySlug(slug: string): Promise<Article | null> {
  return listLocalArticles().find((article) => article.slug === slug) ?? null
}

export async function getArticleBody(slug: string): Promise<ArticleBody | null> {
  return getLocalArticleBody(slug)
}

export type { Article, ArticleBody } from '../types'
