import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Article } from '../data/types'
import { getArticles, getArticleBody } from '../data/sources'

export const useArticlesStore = defineStore('articles', () => {
  // Local files are read synchronously at module load — no network, no loading state.
  const articles = ref<Article[]>(getArticles())

  // ---- Detail view state ----
  const selectedArticle = ref<Article | null>(null)
  const articleBody = ref('')

  function openArticle(article: Article): void {
    selectedArticle.value = article
    articleBody.value = getArticleBody(article)?.content ?? '_暂无正文。_'
  }

  function closeArticle(): void {
    selectedArticle.value = null
    articleBody.value = ''
  }

  return { articles, selectedArticle, articleBody, openArticle, closeArticle }
})
