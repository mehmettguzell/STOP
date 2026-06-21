import { AxiosError } from "axios";
import { ApiError } from "../types/api.types";

/**
 * Backend'den gelen hata mesajini cikarir.
 * Backend ProblemDetail (RFC 7807) formatinda doner:
 * { type, title, status, detail, instance, code }
 */
export function getApiError(err: unknown): ApiError {
  // 1. Dogrudan ApiError objesi (api layer'dan throw edilen)
  if (isApiError(err)) {
    return err;
  }

  // 2. AxiosError — response body'de ProblemDetail var
  if (isAxiosError(err)) {
    const responseData = err.response?.data;

    // ProblemDetail format: { detail, code, status, ... }
    if (responseData && typeof responseData === "object") {
      const pd = responseData as { code?: string; detail?: string };
      if (pd.detail || pd.code) {
        return {
          code: pd.code ?? "HTTP_ERROR",
          message: pd.detail ?? "Islem basarisiz oldu.",
        };
      }
    }

    // Network hatasi (sunucuya ulasilamadi)
    if (!err.response) {
      return {
        code: "NETWORK_ERROR",
        message: "Sunucuya baglanilamadi. Internet baglantinizi kontrol edin.",
      };
    }

    // HTTP status'a gore anlamli Turkce mesajlar
    const status = err.response.status;
    if (status === 400) return { code: "BAD_REQUEST",   message: "Girilen bilgiler gecersiz. Lutfen kontrol edin." };
    if (status === 401) return { code: "UNAUTHORIZED",  message: "Oturum suresi doldu. Lutfen tekrar giris yapin." };
    if (status === 403) return { code: "FORBIDDEN",     message: "Bu islemi yapmaya yetkiniz yok." };
    if (status === 404) return { code: "NOT_FOUND",     message: "Aranan icerik bulunamadi." };
    if (status === 409) return { code: "CONFLICT",      message: "Bu islem zaten gerceklestirilmis veya kayit zaten mevcut." };
    if (status === 422) return { code: "UNPROCESSABLE", message: "Islem gerceklestirilemedi. Girilen degerler uygun degil." };
    if (status >= 500)  return { code: "SERVER_ERROR",  message: "Sunucu hatasi olustu. Lutfen daha sonra tekrar deneyin." };

    return {
      code: "HTTP_ERROR",
      message: `Islem basarisiz oldu. (Hata kodu: ${status})`,
    };
  }

  // 3. Plain Error objesi (ornegin new Error("..."))
  if (err instanceof Error && err.message) {
    if (err.message.toLowerCase().includes("request failed with status code")) {
      return { code: "REQUEST_FAILED", message: "Istek basarisiz oldu. Lutfen tekrar deneyin." };
    }
    if (err.message.toLowerCase().includes("network error")) {
      return { code: "NETWORK_ERROR", message: "Sunucuya baglanilamadi. Internet baglantinizi kontrol edin." };
    }
    if (err.message.toLowerCase().includes("timeout")) {
      return { code: "TIMEOUT", message: "Istek zaman asimina ugradi. Lutfen tekrar deneyin." };
    }
    return { code: "ERROR", message: err.message };
  }

  // 4. Bilinmeyen hata
  return { code: "UNKNOWN", message: "Beklenmeyen bir hata olustu. Lutfen tekrar deneyin." };
}

/** Hata mesajini dogrudan string olarak dondurur. */
export function getErrorMessage(err: unknown): string {
  return getApiError(err).message;
}

/** Hata kodunu dondurur (orn. "EMAIL_ALREADY_EXISTS"). */
export function getErrorCode(err: unknown): string {
  return getApiError(err).code;
}

// --- Type guards ---

function isApiError(err: unknown): err is ApiError {
  return (
    typeof err === "object" &&
    err !== null &&
    "code" in err &&
    "message" in err &&
    typeof (err as ApiError).code === "string" &&
    typeof (err as ApiError).message === "string" &&
    !("isAxiosError" in err)
  );
}

function isAxiosError(err: unknown): err is AxiosError {
  return (
    typeof err === "object" &&
    err !== null &&
    (
      ("isAxiosError" in err && (err as AxiosError).isAxiosError === true) ||
      ("response" in err && "config" in err && "request" in err)
    )
  );
}
