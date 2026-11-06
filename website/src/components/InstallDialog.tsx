"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import {
  ArrowRight,
  Check,
  CheckCircle2,
  Copy,
  Download,
  ShieldCheck,
  Smartphone,
  X,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";
import NotificationShade from "@/components/NotificationShade";
import { useCopyToClipboard } from "@/hooks/useCopyToClipboard";

interface InstallDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

type InstallStage = "prompt" | "guide";

const GUIDE_STEPS = [
  {
    title: "Tap the download notification",
    detail:
      "Swipe down from the top of the screen and tap the finished file. The Ruwia installer opens straight away — no file manager needed.",
  },
  {
    title: "Allow this source — first install only",
    detail:
      "If Android blocks it, tap Settings in that prompt, switch on Allow from this source, then press Back.",
  },
  {
    title: "Tap Install, then Open",
    detail:
      "Confirm the signed package and Android installs it. Tap Open to launch Ruwia. Future updates skip straight to this step.",
  },
];

export default function InstallDialog({ isOpen, onClose }: InstallDialogProps) {
  // Mount fresh on every open so each session starts at the prompt stage.
  if (!isOpen) return null;
  return <InstallDialogContent onClose={onClose} />;
}

function InstallDialogContent({ onClose }: { onClose: () => void }) {
  const [installStage, setInstallStage] = useState<InstallStage>("prompt");
  const [isAndroid] = useState(
    () => typeof navigator !== "undefined" && /android/i.test(navigator.userAgent),
  );
  const dialogRef = useRef<HTMLDivElement | null>(null);
  const previouslyFocusedRef = useRef<HTMLElement | null>(null);
  const { copied, copy } = useCopyToClipboard();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [onClose]);

  useEffect(() => {
    previouslyFocusedRef.current = document.activeElement as HTMLElement | null;
    return () => previouslyFocusedRef.current?.focus();
  }, []);

  useEffect(() => {
    const frame = requestAnimationFrame(() => dialogRef.current?.focus());
    return () => cancelAnimationFrame(frame);
  }, [installStage]);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="install-dialog-title"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in"
    >
      <div className="absolute inset-0" onClick={onClose} />

      <div
        ref={dialogRef}
        tabIndex={-1}
        className="relative z-10 max-h-[calc(100dvh-2rem)] w-full max-w-sm overflow-y-auto rounded-3xl border border-[#E2E5DD] bg-white p-6 shadow-2xl outline-none transition-all dark:border-[#2C3F34] dark:bg-[#1A2520]"
      >
        <button
          onClick={onClose}
          type="button"
          className="absolute top-4 right-4 rounded-full p-1.5 text-[#777D76] transition-colors hover:bg-[#F7F7F2] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#173D32] dark:text-[#8E9B93] dark:hover:bg-[#202E28] dark:focus-visible:outline-[#C8D8B9]"
          aria-label="Close install instructions"
        >
          <X className="h-4 w-4" />
        </button>

        {installStage === "prompt" && (
          <div>
            <div className="mb-5 flex items-center gap-3.5 pr-8">
              <div className="relative flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-[#E2E5DD] bg-white shadow-sm dark:border-[#2C3F34] dark:bg-[#141C18]">
                <Image
                  src={APP_CONFIG.app.logoPath}
                  alt={APP_CONFIG.app.name}
                  width={56}
                  height={56}
                  className="object-cover"
                />
              </div>
              <div className="min-w-0">
                <h2
                  id="install-dialog-title"
                  className="text-lg font-semibold tracking-tight text-[#202521] dark:text-[#F3F6F1]"
                >
                  Download complete
                </h2>
                <p className="font-mono text-xs text-[#777D76] dark:text-[#8E9B93]">
                  {APP_CONFIG.release.apkFileName}
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-[#E2E5DD] bg-[#F7F7F2] p-3.5 dark:border-[#2C3F34] dark:bg-[#151E1A]">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#173D32] dark:bg-[#C8D8B9]">
                  <CheckCircle2 className="h-5 w-5 text-[#F7F7F2] dark:text-[#111714]" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">
                    Saved to your Downloads folder
                  </p>
                  <p className="text-xs text-[#777D76] dark:text-[#8E9B93]">
                    {APP_CONFIG.release.apkSize} · v{APP_CONFIG.release.version} signed build
                  </p>
                </div>
              </div>
            </div>

            <p className="mt-4 text-xs leading-relaxed text-[#777D76] dark:text-[#8E9B93]">
              One system confirmation is left. Android does not let a website
              start an install on its own, so the final tap has to happen on
              your phone.
            </p>

            <div className="mt-4 flex items-center gap-2 text-[11px] text-[#777D76] dark:text-[#8E9B93]">
              <ShieldCheck className="h-4 w-4 shrink-0 text-[#173D32] dark:text-[#C8D8B9]" />
              <span>Verified against the Ruwia production certificate</span>
            </div>

            <div className="mt-6 flex items-center justify-end gap-2">
              <button
                onClick={onClose}
                type="button"
                className="rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-wider text-[#777D76] transition-colors hover:bg-[#F7F7F2] dark:text-[#8E9B93] dark:hover:bg-[#202E28]"
              >
                Not now
              </button>
              <button
                onClick={() => setInstallStage("guide")}
                type="button"
                className="inline-flex items-center gap-2 rounded-full bg-[#173D32] px-6 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#F7F7F2] shadow-sm transition-colors hover:bg-[#315E4C] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#173D32] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1] dark:focus-visible:outline-[#C8D8B9]"
              >
                <span>Show me how</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        )}

        {installStage === "guide" && (
          <div>
            <div className="mb-4 flex items-center gap-3.5 pr-8">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-[#C8D8B9] bg-[#EBF0E6] text-[#173D32] dark:border-[#2C3F34] dark:bg-[#202E28] dark:text-[#C8D8B9]">
                <CheckCircle2 className="h-7 w-7" />
              </div>
              <div className="min-w-0">
                <h3
                  id="install-dialog-title"
                  className="text-base font-semibold text-[#202521] dark:text-[#F3F6F1]"
                >
                  Three taps left
                </h3>
                <p className="text-xs text-[#777D76] dark:text-[#8E9B93]">
                  Android handles the rest for you.
                </p>
              </div>
            </div>

            <NotificationShade className="mb-4" />

            <ol className="space-y-3">
              {GUIDE_STEPS.map((step, index) => (
                <li key={step.title} className="flex gap-3">
                  <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[#173D32] font-mono text-[10px] font-bold text-[#F7F7F2] dark:bg-[#C8D8B9] dark:text-[#111714]">
                    {index + 1}
                  </span>
                  <div>
                    <p className="text-xs font-semibold text-[#202521] dark:text-[#F3F6F1]">
                      {step.title}
                    </p>
                    <p className="mt-0.5 text-[11px] leading-relaxed text-[#777D76] dark:text-[#8E9B93]">
                      {step.detail}
                    </p>
                  </div>
                </li>
              ))}
            </ol>

            <div className="mt-4 rounded-xl border border-dashed border-[#C8D8B9] bg-[#F7F7F2] p-3 dark:border-[#2C3F34] dark:bg-[#151E1A]">
              <p className="text-[11px] font-semibold text-[#202521] dark:text-[#F3F6F1]">
                No notification showing?
              </p>
              <p className="mt-0.5 text-[11px] leading-relaxed text-[#777D76] dark:text-[#8E9B93]">
                Open Chrome and tap the download icon in the toolbar, or search
                this name inside Files → Downloads.
              </p>
              <div className="mt-2 flex items-center gap-2">
                <code className="min-w-0 flex-1 truncate rounded-md border border-[#E2E5DD] bg-white px-2 py-1.5 font-mono text-[11px] text-[#202521] dark:border-[#2C3F34] dark:bg-[#111714] dark:text-[#F3F6F1]">
                  {APP_CONFIG.release.apkFileName}
                </code>
                <button
                  onClick={() => copy(APP_CONFIG.release.apkFileName)}
                  type="button"
                  aria-live="polite"
                  className="inline-flex shrink-0 items-center gap-1.5 rounded-md border border-[#E2E5DD] bg-white px-2.5 py-1.5 text-[11px] font-mono text-[#202521] transition-colors hover:bg-[#EBF0E6] dark:border-[#2C3F34] dark:bg-[#18221D] dark:text-[#F3F6F1] dark:hover:bg-[#202E28]"
                >
                  {copied ? (
                    <>
                      <Check className="h-3.5 w-3.5 text-[#173D32] dark:text-[#C8D8B9]" />
                      <span>Copied</span>
                    </>
                  ) : (
                    <>
                      <Copy className="h-3.5 w-3.5 text-[#777D76] dark:text-[#8E9B93]" />
                      <span>Copy</span>
                    </>
                  )}
                </button>
              </div>
            </div>

            {!isAndroid && (
              <div className="mt-3 flex items-start gap-2 rounded-lg border border-[#C8D8B9] bg-[#EBF0E6] p-3 text-[11px] leading-relaxed text-[#173D32] dark:border-[#2C3F34] dark:bg-[#202E28] dark:text-[#C8D8B9]">
                <Smartphone className="mt-0.5 h-4 w-4 shrink-0" />
                <span>
                  This file downloaded to your computer. Open this page on your
                  Android phone, or transfer the APK to the device before
                  installing.
                </span>
              </div>
            )}

            <div className="flex items-center justify-end gap-2 pt-5">
              <button
                onClick={onClose}
                type="button"
                className="rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-wider text-[#777D76] transition-colors hover:bg-[#F7F7F2] dark:text-[#8E9B93] dark:hover:bg-[#202E28]"
              >
                Done
              </button>
              <a
                href="#installation"
                onClick={onClose}
                className="inline-flex items-center gap-1.5 rounded-full bg-[#173D32] px-5 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#F7F7F2] shadow-sm transition-colors hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1]"
              >
                <Download className="h-3.5 w-3.5" />
                <span>Full guide</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </a>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
