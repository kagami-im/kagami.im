import type { Article, ArticleBody } from '../types'
import { parseMarkdown } from '../frontmatter'

// Vite built-in: raw text, eager so parsing happens once at module load.
const files = import.meta.glob<string>('../../content/posts/*.md', {
  query: '?raw',
  import: 'default',
  eager: true,
})

function slugFromPath(path: string): string {
  return path.split('/').pop()!.replace(/\.md$/, '')
}

function firstLine(body: string): string {
  return body.split(/\r?\n/).map((l) => l.trim()).find(Boolean) ?? ''
}

// Parse every file once; both list and detail read from these caches.
const articleBySlug = new Map<string, Article>()
const bodyBySlug = new Map<string, string>()

for (const [path, raw] of Object.entries(files)) {
  const { frontmatter, body } = parseMarkdown(raw)
  const slug = frontmatter.slug ?? slugFromPath(path)
  articleBySlug.set(slug, {
    slug,
    title: frontmatter.title ?? slugFromPath(path),
    summary: frontmatter.summary ?? firstLine(body),
    publishedAt: frontmatter.date ?? '1970-01-01',
    tags: Array.isArray(frontmatter.tags) ? frontmatter.tags : [],
    coverUrl: frontmatter.cover,
    pinned: frontmatter.pinned === true,
    readingMinutes: typeof frontmatter.readingMinutes === 'number' ? frontmatter.readingMinutes : 1,
  })
  bodyBySlug.set(slug, body)
}

export function listLocalArticles(): Article[] {
  return [...articleBySlug.values()]
}

export function getLocalArticleBody(slug: string): ArticleBody | null {
  const body = bodyBySlug.get(slug)
  return body === undefined ? null : { slug, content: body }
}
