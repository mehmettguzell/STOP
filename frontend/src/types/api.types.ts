export interface ApiError {
  type?: string;
  title?: string;
  status: number;
  detail: string;
  instance?: string;
  code?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface SliceResponse<T> {
  content: T[];
  page: number;
  size: number;
  hasNext: boolean;
}

export const extractErrorMessage = (
  error: unknown,
  fallback = "Bir hata oluştu",
): string => {
  if (error && typeof error === "object" && "response" in error) {
    const data = (error as { response?: { data?: ApiError } }).response?.data;
    if (data?.detail) return data.detail;
  }
  return fallback;
};
