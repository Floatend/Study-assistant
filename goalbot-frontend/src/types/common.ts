export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface OptionItem {
  label: string
  value: number
}
