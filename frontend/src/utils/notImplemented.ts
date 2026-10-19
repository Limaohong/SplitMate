/**
 * 練習項目骨架使用：呼叫時拋出錯誤，提醒此功能尚未實作。
 * pendingArguments 只是讓骨架函式的參數「有被使用」，以通過 TypeScript 的 noUnusedParameters 檢查；實作後請移除此呼叫。
 */
export function throwNotImplemented(featureName: string, ...pendingArguments: unknown[]): never {
  throw new Error(`${featureName} 尚未實作（練習項目，見 PRACTICE.md），收到參數：${pendingArguments.length} 個`)
}
