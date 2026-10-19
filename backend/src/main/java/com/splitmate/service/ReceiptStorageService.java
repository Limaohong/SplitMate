package com.splitmate.service;

import com.splitmate.config.ReceiptStorageProperties;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 收據檔案的實際存取（練習項目）。只負責「檔案放哪裡、怎麼存」，不處理權限與業務規則。
 */
@Service
public class ReceiptStorageService {

    private final Path storageDirectory;

    public ReceiptStorageService(ReceiptStorageProperties receiptStorageProperties) {
        this.storageDirectory = Paths.get(receiptStorageProperties.storageDirectory()).toAbsolutePath().normalize();
    }

    /**
     * 儲存收據並回傳「相對於 storageDirectory 的路徑」（存進 expenses.receipt_path）。
     *
     * <p>TODO 實作提示：
     * <ul>
     *   <li>檔名不要直接使用使用者上傳的原始檔名（想想 "../../etc/passwd" 這種檔名會發生什麼事）</li>
     *   <li>目錄不存在時要先建立</li>
     *   <li>為什麼回傳相對路徑而不是絕對路徑？（提示：搬家、換主機、改掛載點）</li>
     * </ul>
     */
    public String storeReceipt(Long expenseId, MultipartFile receiptFile) {
        // TODO 練習項目：實作完成後移除下面這行
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
