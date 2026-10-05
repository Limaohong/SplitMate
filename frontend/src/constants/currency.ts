/** 與後端 com.splitmate.entity.CurrencyCode 保持一致 */
export const CURRENCY_CODES = ['TWD', 'JPY', 'USD', 'EUR', 'KRW', 'HKD'] as const

export type CurrencyCode = (typeof CURRENCY_CODES)[number]

export const CURRENCY_LABELS: Record<CurrencyCode, string> = {
  TWD: '新台幣',
  JPY: '日圓',
  USD: '美元',
  EUR: '歐元',
  KRW: '韓元',
  HKD: '港幣',
}
