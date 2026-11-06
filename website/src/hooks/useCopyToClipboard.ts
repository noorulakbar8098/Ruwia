"use client";

import { useCallback, useEffect, useRef, useState } from "react";

const FEEDBACK_DURATION_MS = 2200;

/**
 * Copies text to the clipboard and exposes short-lived confirmation state.
 *
 * The async clipboard API is unavailable on insecure origins, so `copy`
 * reports failure instead of throwing and callers keep the raw value visible
 * as a manual fallback.
 */
export function useCopyToClipboard() {
  const [copied, setCopied] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const copy = useCallback(async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
    } catch {
      return false;
    }

    setCopied(true);
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => setCopied(false), FEEDBACK_DURATION_MS);
    return true;
  }, []);

  return { copied, copy };
}
