"use client";

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { APP_CONFIG } from "@/config/app.config";

type DownloadState = "idle" | "downloading" | "downloaded";

interface DownloadContextType {
  downloadState: DownloadState;
  progress: number;
  downloadedMB: string;
  totalMB: string;
  isInstallDialogOpen: boolean;
  startDownload: () => void;
  openInstallDialog: () => void;
  closeInstallDialog: () => void;
  resetDownload: () => void;
}

const DownloadContext = createContext<DownloadContextType | undefined>(undefined);

const APK_MIME_TYPE = "application/vnd.android.package-archive";
const APK_FILE_NAME = `ruwia-v${APP_CONFIG.release.version}-release.apk`;

export function DownloadProvider({ children }: { children: React.ReactNode }) {
  const [downloadState, setDownloadState] = useState<DownloadState>("idle");
  const [progress, setProgress] = useState(0);
  const [isInstallDialogOpen, setIsInstallDialogOpen] = useState(false);
  const requestRef = useRef<XMLHttpRequest | null>(null);
  const downloadStartedRef = useRef(false);
  const dialogTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const blobUrlRef = useRef<string | null>(null);

  const totalMB = APP_CONFIG.release.apkSize;
  const totalMBNumber = parseFloat(totalMB) || 26.8;
  const downloadedMB = `${((totalMBNumber * progress) / 100).toFixed(1)} MB`;

  const releaseBlobUrl = useCallback(() => {
    if (blobUrlRef.current) {
      URL.revokeObjectURL(blobUrlRef.current);
      blobUrlRef.current = null;
    }
  }, []);

  useEffect(() => {
    return () => {
      requestRef.current?.abort();
      if (dialogTimerRef.current) clearTimeout(dialogTimerRef.current);
      releaseBlobUrl();
    };
  }, [releaseBlobUrl]);

  const saveBlobAsApk = useCallback((payload: Blob) => {
    releaseBlobUrl();
    const apkBlob = new Blob([payload], { type: APK_MIME_TYPE });
    const url = URL.createObjectURL(apkBlob);
    blobUrlRef.current = url;

    const link = document.createElement("a");
    link.href = url;
    link.download = APK_FILE_NAME;
    link.rel = "noopener";
    document.body.appendChild(link);
    link.click();
    link.remove();
  }, [releaseBlobUrl]);

  const triggerDirectDownload = useCallback(() => {
    const link = document.createElement("a");
    link.href = APP_CONFIG.release.downloadUrl;
    link.download = APK_FILE_NAME;
    document.body.appendChild(link);
    link.click();
    link.remove();
  }, []);

  const openInstallPrompt = useCallback(() => {
    if (dialogTimerRef.current) clearTimeout(dialogTimerRef.current);
    // Give the browser a moment to register the saved file before asking
    // the user to open the download notification.
    dialogTimerRef.current = setTimeout(() => {
      setIsInstallDialogOpen(true);
    }, 700);
  }, []);

  const completeDownload = useCallback(() => {
    setProgress(100);
    setDownloadState("downloaded");
    downloadStartedRef.current = false;
    openInstallPrompt();
  }, [openInstallPrompt]);

  const startDownload = useCallback(() => {
    if (downloadState === "downloaded") {
      setIsInstallDialogOpen(true);
      return;
    }
    if (downloadStartedRef.current) return;

    downloadStartedRef.current = true;
    setDownloadState("downloading");
    setProgress(0);
    setIsInstallDialogOpen(false);

    let settled = false;
    const settle = () => {
      settled = true;
      requestRef.current = null;
    };

    try {
      const request = new XMLHttpRequest();
      requestRef.current = request;
      request.open("GET", APP_CONFIG.release.downloadUrl, true);
      request.responseType = "blob";

      request.onprogress = (event) => {
        if (!event.lengthComputable || event.total <= 0) return;
        // Hold at 99% until the browser has the complete file.
        setProgress(Math.min(99, Math.round((event.loaded / event.total) * 100)));
      };

      request.onload = () => {
        if (settled) return;
        settle();

        if (request.status >= 200 && request.status < 300 && request.response instanceof Blob) {
          saveBlobAsApk(request.response);
          completeDownload();
          return;
        }

        // Static hosts can reject XHR (for example on file://). Fall back to
        // the native browser download so the primary action never dead-ends.
        triggerDirectDownload();
        completeDownload();
      };

      request.onerror = () => {
        if (settled) return;
        settle();
        triggerDirectDownload();
        completeDownload();
      };

      request.onabort = () => {
        if (settled) return;
        settle();
        downloadStartedRef.current = false;
        setDownloadState("idle");
        setProgress(0);
      };

      request.send();
    } catch {
      triggerDirectDownload();
      completeDownload();
    }
  }, [completeDownload, downloadState, saveBlobAsApk, triggerDirectDownload]);

  const openInstallDialog = useCallback(() => {
    setIsInstallDialogOpen(true);
  }, []);

  const closeInstallDialog = useCallback(() => {
    setIsInstallDialogOpen(false);
  }, []);

  const resetDownload = useCallback(() => {
    requestRef.current?.abort();
    requestRef.current = null;
    downloadStartedRef.current = false;
    if (dialogTimerRef.current) clearTimeout(dialogTimerRef.current);
    releaseBlobUrl();
    setProgress(0);
    setDownloadState("idle");
    setIsInstallDialogOpen(false);
  }, [releaseBlobUrl]);

  return (
    <DownloadContext.Provider
      value={{
        downloadState,
        progress,
        downloadedMB,
        totalMB,
        isInstallDialogOpen,
        startDownload,
        openInstallDialog,
        closeInstallDialog,
        resetDownload,
      }}
    >
      {children}
    </DownloadContext.Provider>
  );
}

export function useDownload() {
  const context = useContext(DownloadContext);
  if (!context) {
    throw new Error("useDownload must be used within a DownloadProvider");
  }
  return context;
}
