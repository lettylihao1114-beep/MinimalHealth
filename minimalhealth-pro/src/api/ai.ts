import client from './client'

export interface AiSuggestionsData {
  greeting: string
  suggestions: Array<{
    title: string
    description: string
    iconKey: string
  }>
}

export function getSuggestions(date?: string) {
  return client.get<{ data: AiSuggestionsData }>('/ai/suggestions', { params: { date } })
}
