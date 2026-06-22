export function todayString() {
  return new Date().toISOString().slice(0, 10)
}

export function minutesText(minutes?: number) {
  const value = minutes ?? 0
  if (value < 60) {
    return `${value} 分钟`
  }
  const hours = Math.floor(value / 60)
  const rest = value % 60
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分钟`
}
