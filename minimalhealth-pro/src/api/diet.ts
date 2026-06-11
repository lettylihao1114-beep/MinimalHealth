import client from './client'

export interface DietData {
  totalCalories: number
  calorieGoal: number
  meals: Array<{
    id: number
    mealType: string
    mealTypeLabel: string
    calories: number
    foods: string | null
    recorded: boolean
  }>
}

export function getDiet(date?: string) {
  return client.get<{ data: DietData }>('/diet', { params: { date } })
}

export function addMeal(mealType: string, calories: number, foods?: string) {
  return client.post<{ data: any }>('/diet', { mealType, foods: foods || '快速记录', calories })
}

export function updateMeal(id: number, data: { foods?: string; calories?: number }) {
  return client.put<{ data: any }>(`/diet/${id}`, data)
}

export function deleteMeal(id: number) {
  return client.delete<{ data: any }>(`/diet/${id}`)
}

export function getDietGoal(date?: string) {
  return client.get<{ data: { date: string; targetKcal: number } }>('/diet/goal', { params: { date } })
}
