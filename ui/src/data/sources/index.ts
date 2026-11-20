import type { Article, ArticleBody } from '../types'
import { listLocalArticles, getLocalArticleBody } from './local'

function newestFirst(a: Article, b: Article): number {
  if (a.pinned !== b.pinned) return a.pinned ? -1 : 1
  return b.publishedAt.localeCompare(a.publishedAt)
}

/** Local markdown files are the only article source for now. */
export function getArticles(): Article[] {
  return listLocalArticles().sort(newestFirst)
}

export function getArticleBody(article: Article): ArticleBody | null {
  return getLocalArticleBody(article.slug)
}

export type { Article, ArticleBody } from '../types'
