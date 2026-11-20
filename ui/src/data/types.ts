/** Article shape parsed from a local markdown file; the list and detail view only see this. */
export interface Article {
  slug: string
  title: string
  summary: string
  publishedAt: string // 'YYYY-MM-DD HH:mm:ss' from frontmatter
  tags: string[]
  coverUrl?: string
  pinned: boolean
  readingMinutes: number
}

/** Full markdown body, loaded on demand by the detail view. */
export interface ArticleBody {
  slug: string
  content: string // raw markdown
}

/** Raw frontmatter keys we understand; everything else is ignored. */
export interface Frontmatter {
  title?: string
  slug?: string
  date?: string
  summary?: string
  tags?: string[]
  cover?: string
  pinned?: boolean
  readingMinutes?: number
}

export interface ParsedMarkdown {
  frontmatter: Frontmatter
  body: string
}
