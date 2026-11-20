import MarkdownIt from 'markdown-it'

// html:false drops raw HTML in the source; content renders as markdown only.
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

/** Render a markdown body to HTML for v-html. */
export function renderMarkdown(source: string): string {
  return md.render(source ?? '')
}
