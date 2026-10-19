import type { CurrencyCode } from '@/constants/currency'

const DISPLAY_LOCALE = 'zh-TW'

// Intl.NumberFormat 建立成本不低，依幣別快取重複使用
const moneyFormatterByCurrency = new Map<CurrencyCode, Intl.NumberFormat>()

const dateTimeFormatter = new Intl.DateTimeFormat(DISPLAY_LOCALE, {
  dateStyle: 'medium',
  timeStyle: 'short',
})

/**
 * 依幣別格式化金額，小數位數由瀏覽器依 ISO 4217 決定（例如 JPY 不顯示小數、TWD 顯示兩位）。
 * 注意：JPY 金額若含小數會被四捨五入顯示，但後端仍保存原值。
 */
export function formatMoney(amount: number, currencyCode: CurrencyCode): string {
  let moneyFormatter = moneyFormatterByCurrency.get(currencyCode)
  if (!moneyFormatter) {
    moneyFormatter = new Intl.NumberFormat(DISPLAY_LOCALE, { style: 'currency', currency: currencyCode })
    moneyFormatterByCurrency.set(currencyCode, moneyFormatter)
  }
  return moneyFormatter.format(amount)
}

export function formatDateTime(isoDateTime: string): string {
  return dateTimeFormatter.format(new Date(isoDateTime))
}
