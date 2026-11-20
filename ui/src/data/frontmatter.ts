import type { Frontmatter, ParsedMarkdown } from './types'

function stripQuotes(s: string): string {
  if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) {
    return s.slice(1, -1)
  }
  return s
}

function parseValue(raw: string): Frontmatter[keyof Frontmatter] {
  const v = raw.trim()
  if (v.startsWith('[') && v.endsWith(']')) {
    return v.slice(1, -1).split(',').map((s) => stripQuotes(s.trim())).filter(Boolean)
  }
  if (v === 'true') return true
  if (v === 'false') return false
  if (/^-?\d+$/.test(v)) return Number(v)
  return stripQuotes(v)
}

/** Parse a minimal `---` frontmatter block. Body is everything after the closing delimiter. */
export function parseMarkdown(raw: string): ParsedMarkdown {
  const lines = raw.replace(/^\uFEFF/, '').split(/\r?\n/)
  if (lines[0]?.trim() !== '---') {
    return { frontmatter: {}, body: raw.trim() }
  }
  const frontmatter: Record<string, unknown> = {}
  let i = 1
  for (; i < lines.length; i++) {
    const line = lines[i]
    if (line.trim() === '---') break
    const sep = line.indexOf(':')
    if (sep === -1 || line.trimStart().startsWith('#')) continue
    frontmatter[line.slice(0, sep).trim()] = parseValue(line.slice(sep + 1))
  }
  return { frontmatter: frontmatter as Frontmatter, body: lines.slice(i + 1).join('\n').trim() }
}
