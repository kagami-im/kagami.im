import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Article } from '../data/types'
import { getArticleBody, getArticleBySlug, getArticles } from '../data/sources'

export type LoadStatus = 'idle' | 'loading' | 'ready' | 'error'
export type DetailStatus = LoadStatus | 'not-found'

export const useArticlesStore = defineStore('articles', () => {
  const articles = ref<Article[]>([])
  const listStatus = ref<LoadStatus>('idle')

  const selectedArticle = ref<Article | null>(null)
  const articleBody = ref('')
  const detailStatus = ref<DetailStatus>('idle')

  // Guard against out-of-order async responses when navigating quickly between posts.
  let detailRequestId = 0

  async function loadArticles(force = false): Promise<void> {
    if (!force && (listStatus.value === 'ready' || listStatus.value === 'loading')) {
      return
    }
    listStatus.value = 'loading'
    try {
      articles.value = await getArticles()
      listStatus.value = 'ready'
    } catch (e) {
      console.error('Articles load failed', e)
      listStatus.value = 'error'
    }
  }

  async function loadArticleBySlug(slug: string): Promise<void> {
    const requestId = ++detailRequestId
    detailStatus.value = 'loading'
    try {
      const article =
        articles.value.find((item) => item.slug === slug) ?? (await getArticleBySlug(slug))
      if (requestId !== detailRequestId) return

      if (!article) {
        selectedArticle.value = null
        articleBody.value = ''
        detailStatus.value = 'not-found'
        return
      }

      const body = await getArticleBody(article.slug)
      if (requestId !== detailRequestId) return

      selectedArticle.value = article
      articleBody.value = body?.content ?? '_暂无正文。_'
      detailStatus.value = 'ready'
    } catch (e) {
      if (requestId !== detailRequestId) return
      console.error('Article detail load failed', e)
      selectedArticle.value = null
      articleBody.value = ''
      detailStatus.value = 'error'
    }
  }

  function clearArticle(): void {
    detailRequestId++
    selectedArticle.value = null
    articleBody.value = ''
    detailStatus.value = 'idle'
  }

  return {
    articles,
    listStatus,
    selectedArticle,
    articleBody,
    detailStatus,
    loadArticles,
    loadArticleBySlug,
    clearArticle,
  }
})
