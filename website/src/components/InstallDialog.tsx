"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import {
  ArrowRight,
  CheckCircle2,
  Download,
  ShieldCheck,
  Smartphone,
  X,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

interface InstallDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

type InstallStage = "prompt" | "guide";

export default function InstallDialog({ isOpen, onClose }: InstallDialogProps) {
  const [installStage, setInstallStage] = useState<InstallStage>("prompt");
  const [isAndroid, setIsAndroid] = useState(true);

  useEffect(() => {
    if (!isOpen) return;
    setInstallStage("prompt");
    setIsAndroid(/android/i.test(navigator.userAgent));
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    if (!isOpen) return;

    window.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "unset";
    };
  }, [isOpen, onClose]);

  const handleInstallClick = () => {
    setInstallStage("guide");
  };

  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="install-dialog-title"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in"
    >
      <div className="absolute inset-0" onClick={onClose} />

      <div className="relative w-full max-w-sm rounded-3xl bg-white dark:bg-[#1A2520] border border-[#E2E5DD] dark:border-[#2C3F34] shadow-2xl p-6 transition-all transform scale-100 z-10">
        {installStage === "prompt" && (
          <div>
            <button
              onClick={onClose}
              type="button"
              className="absolute top-4 right-4 p-1.5 rounded-full text-[#777D76] dark:text-[#8E9B93] hover:bg-[#F7F7F2] dark:hover:bg-[#202E28] transition-colors"
              aria-label="Close dialog"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="flex items-center gap-3.5 mb-5">
              <div className="relative w-14 h-14 rounded-2xl overflow-hidden border border-[#E2E5DD] dark:border-[#2C3F34] bg-white dark:bg-[#141C18] flex items-center justify-center shadow-sm shrink-0">
                <Image
                  src={APP_CONFIG.app.logoPath}
                  alt={APP_CONFIG.app.name}
                  width={56}
                  height={56}
                  className="object-cover"
                />
              </div>
              <div>
                <h2
                  id="install-dialog-title"
                  className="text-lg font-semibold tracking-tight text-[#202521] dark:text-[#F3F6F1]"
                >
                  {APP_CONFIG.app.name}
                </h2>
                <p className="text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
                  v{APP_CONFIG.release.version} · {APP_CONFIG.release.apkSize}
                </p>
              </div>
            </div>

            <div className="py-2 border-t border-b border-[#E2E5DD] dark:border-[#2C3F34] my-4">
              <p className="text-base font-medium text-[#202521] dark:text-[#F3F6F1]">
                Do you want to install this application?
              </p>
              <p className="mt-1 text-xs text-[#777D76] dark:text-[#8E9B93]">
                The signed APK has finished downloading to this device.
              </p>
            </div>

            <div className="flex items-center gap-2 mb-6 text-[11px] text-[#777D76] dark:text-[#8E9B93]">
              <ShieldCheck className="w-4 h-4 text-[#173D32] dark:text-[#C8D8B9] shrink-0" />
              <span>Official signed production APK package</span>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={onClose}
                type="button"
                className="px-4 py-2 rounded-full text-xs font-semibold uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93] hover:bg-[#F7F7F2] dark:hover:bg-[#202E28] transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleInstallClick}
                type="button"
                className="px-6 py-2.5 rounded-full text-xs font-semibold uppercase tracking-wider text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1] shadow-sm transition-colors"
              >
                Install
              </button>
            </div>
          </div>
        )}

        {installStage === "guide" && (
          <div>
            <div className="flex items-center gap-3.5 mb-4">
              <div className="w-12 h-12 rounded-2xl bg-[#EBF0E6] dark:bg-[#202E28] flex items-center justify-center text-[#173D32] dark:text-[#C8D8B9] shrink-0 border border-[#C8D8B9] dark:border-[#2C3F34]">
                <CheckCircle2 className="w-7 h-7" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                  Download complete.
                </h3>
                <p className="text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
                  ruwia-v{APP_CONFIG.release.version}-release.apk · {APP_CONFIG.release.apkSize}
                </p>
              </div>
            </div>

            <p className="text-xs text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
              Android asks you to confirm the install. Open the downloaded file to
              continue:
            </p>

            <ol className="my-5 space-y-3">
              {[
                "Open your browser's download notification or the Downloads app.",
                `Tap ruwia-v${APP_CONFIG.release.version}-release.apk.`,
                "If prompted, allow installs from this browser, then tap Install.",
              ].map((step, index) => (
                <li key={step} className="flex gap-3 text-xs text-[#202521] dark:text-[#F3F6F1]">
                  <span className="w-5 h-5 rounded-full bg-[#173D32] dark:bg-[#C8D8B9] text-[#F7F7F2] dark:text-[#111714] text-[10px] font-mono font-bold flex items-center justify-center shrink-0">
                    {index + 1}
                  </span>
                  <span className="leading-relaxed">{step}</span>
                </li>
              ))}
            </ol>

            <div className="p-3 rounded-lg bg-[#F7F7F2] dark:bg-[#151E1A] border border-[#E2E5DD] dark:border-[#2C3F34] text-[11px] text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
              <strong className="text-[#202521] dark:text-[#F3F6F1]">
                Why doesn&apos;t it open automatically?
              </strong>{" "}
              Android blocks websites from launching the package installer directly.
              The final tap on the downloaded APK is the system security confirmation.
            </div>

            {!isAndroid && (
              <div className="mt-3 flex items-start gap-2 p-3 rounded-lg bg-[#EBF0E6] dark:bg-[#202E28] border border-[#C8D8B9] dark:border-[#2C3F34] text-[11px] text-[#173D32] dark:text-[#C8D8B9] leading-relaxed">
                <Smartphone className="w-4 h-4 shrink-0 mt-0.5" />
                <span>
                  You&apos;re on a desktop browser. Open this page on your Android
                  phone or transfer the downloaded APK to your device.
                </span>
              </div>
            )}

            <div className="flex items-center justify-end gap-3 pt-5">
              <button
                onClick={onClose}
                type="button"
                className="px-5 py-2 rounded-full text-xs font-semibold uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93] hover:bg-[#F7F7F2] dark:hover:bg-[#202E28] transition-colors"
              >
                Done
              </button>
              <a
                href="#installation"
                onClick={onClose}
                className="inline-flex items-center gap-1.5 px-6 py-2.5 rounded-full text-xs font-semibold uppercase tracking-wider text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1] shadow-sm transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Full guide</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </a>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
