"use client";

import Image from "next/image";
import {
  Download,
  Copy,
  Check,
  ShieldCheck,
  ArrowRight,
  Info,
  CheckCircle2,
  Play,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";
import { useDownload } from "@/context/DownloadContext";
import { useCopyToClipboard } from "@/hooks/useCopyToClipboard";

export default function DownloadCard() {
  const { copied: copiedSha, copy } = useCopyToClipboard();
  const { downloadState, progress, startDownload, openInstallDialog } = useDownload();

  const handleCopySha = () => {
    void copy(APP_CONFIG.release.sha256);
  };

  const handlePrimaryClick = () => {
    if (downloadState === "downloaded") {
      openInstallDialog();
    } else {
      startDownload();
    }
  };

  const specs = [
    { label: "Application Name", value: APP_CONFIG.app.name },
    { label: "Package Identifier", value: APP_CONFIG.app.packageName, isMono: true },
    { label: "Release Version", value: `v${APP_CONFIG.release.version} (Code ${APP_CONFIG.release.versionCode})`, isMono: true },
    { label: "APK Package Size", value: APP_CONFIG.release.apkSize, isMono: true },
    { label: "Publication Date", value: APP_CONFIG.release.releaseDate },
    { label: "Minimum Compatibility", value: APP_CONFIG.release.minAndroid },
    { label: "Target Compatibility", value: APP_CONFIG.release.targetAndroid },
    { label: "CPU Architecture", value: APP_CONFIG.release.architecture },
    { label: "Distribution Method", value: "Signed Production APK (Direct Download)" },
  ];

  return (
    <section id="download" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Eyebrow & Header (No slashes) */}
        <div className="flex items-center gap-2 mb-6">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            Technical Specifications &amp; Direct Download
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] gap-6">
          <div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              Verified Production <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">Android Package.</span>
            </h2>
          </div>
          <p className="text-sm sm:text-base text-[#777D76] dark:text-[#8E9B93] max-w-sm leading-relaxed">
            Download the official signed release binary directly. No telemetry, no intermediate stores, no delayed approval queues.
          </p>
        </div>

        {/* Technical Specification Ledger Container */}
        <div className="border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] rounded overflow-hidden shadow-sm transition-colors">
          
          {/* Header Bar */}
          <div className="px-6 py-5 bg-[#F7F7F2] dark:bg-[#151E1A] border-b border-[#E2E5DD] dark:border-[#26372F] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="relative w-8 h-8 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] overflow-hidden flex items-center justify-center shrink-0">
                <Image
                  src={APP_CONFIG.app.logoPath}
                  alt={APP_CONFIG.app.name}
                  width={32}
                  height={32}
                  className="object-cover"
                />
              </div>
              <div>
                <span className="text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">
                  {APP_CONFIG.app.name} Distribution Ledger
                </span>
                <span className="ml-2 text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
                  STATUS: VERIFIED STABLE
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span className="inline-block w-2 h-2 rounded-full bg-[#315E4C] dark:bg-[#C8D8B9]" />
              <span className="text-xs font-mono text-[#173D32] dark:text-[#C8D8B9] uppercase">
                Production Signed APK
              </span>
            </div>
          </div>

          {/* Specifications Table */}
          <div className="divide-y divide-[#E2E5DD] dark:divide-[#26372F]">
            {specs.map((item) => (
              <div
                key={item.label}
                className="grid grid-cols-1 sm:grid-cols-12 px-6 py-3.5 hover:bg-[#F7F7F2]/60 dark:hover:bg-[#151E1A]/60 transition-colors text-xs sm:text-sm"
              >
                <div className="sm:col-span-4 font-mono text-xs uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93]">
                  {item.label}
                </div>
                <div className={`sm:col-span-8 font-medium text-[#202521] dark:text-[#F3F6F1] ${item.isMono ? "font-mono" : ""}`}>
                  {item.value}
                </div>
              </div>
            ))}
          </div>

          {/* Cryptographic SHA-256 Checksum Row */}
          <div className="p-6 bg-[#F7F7F2] dark:bg-[#151E1A] border-t border-[#E2E5DD] dark:border-[#26372F] space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-[#173D32] dark:text-[#C8D8B9]" />
                <span className="text-xs font-mono uppercase tracking-wider text-[#202521] dark:text-[#F3F6F1] font-semibold">
                  SHA-256 Cryptographic Checksum
                </span>
              </div>
              <button
                onClick={handleCopySha}
                type="button"
                className="inline-flex items-center gap-1.5 px-3 py-1 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] text-xs font-mono text-[#202521] dark:text-[#F3F6F1] hover:bg-[#EBF0E6] dark:hover:bg-[#202E28] transition-colors"
              >
                {copiedSha ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-[#173D32] dark:text-[#C8D8B9]" />
                    <span>Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5 text-[#777D76] dark:text-[#8E9B93]" />
                    <span>Copy Hash</span>
                  </>
                )}
              </button>
            </div>

            <div className="font-mono text-xs text-[#202521] dark:text-[#F3F6F1] bg-white dark:bg-[#111714] p-3 rounded border border-[#E2E5DD] dark:border-[#26372F] select-all break-all">
              {APP_CONFIG.release.sha256}
            </div>

            <div className="flex items-center gap-1.5 text-xs text-[#777D76] dark:text-[#8E9B93]">
              <Info className="w-3.5 h-3.5 shrink-0" />
              <span>Verify terminal integrity: <code className="font-mono text-[#202521] dark:text-[#F3F6F1]">sha256sum ruwia-v{APP_CONFIG.release.version}-release.apk</code></span>
            </div>
          </div>

          {/* Primary Action Button & Mirrors */}
          <div className="p-6 sm:p-8 border-t border-[#E2E5DD] dark:border-[#26372F] flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
            <button
              onClick={handlePrimaryClick}
              disabled={downloadState === "downloading"}
              type="button"
              className={`inline-flex items-center justify-center gap-3 px-8 py-4 rounded text-sm font-semibold uppercase tracking-wider transition-all focus:outline-none ${
                downloadState === "downloaded"
                  ? "text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1] shadow-md ring-2 ring-[#C8D8B9]/60"
                  : downloadState === "downloading"
                  ? "text-[#F7F7F2] bg-[#315E4C] dark:bg-[#C8D8B9]/80 dark:text-[#111714] cursor-wait"
                  : "text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1]"
              }`}
            >
              {downloadState === "downloading" ? (
                <>
                  <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
                  <span>Downloading... {progress}%</span>
                </>
              ) : downloadState === "downloaded" ? (
                <>
                  <Play className="w-4 h-4 fill-current text-[#C8D8B9] dark:text-[#111714]" />
                  <span>Install APK</span>
                </>
              ) : (
                <>
                  <Download className="w-4 h-4 text-[#C8D8B9] dark:text-[#111714]" />
                  <span>Download APK (v{APP_CONFIG.release.version})</span>
                </>
              )}
            </button>

            <a
              href="#installation"
              className="inline-flex items-center justify-center gap-2 px-6 py-4 rounded text-sm font-semibold uppercase tracking-wider text-[#202521] dark:text-[#F3F6F1] hover:text-[#173D32] dark:hover:text-[#C8D8B9] border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] hover:bg-[#F7F7F2] dark:hover:bg-[#202E28] transition-colors"
            >
              <span>How to install</span>
              <ArrowRight className="w-4 h-4 text-[#777D76] dark:text-[#8E9B93]" />
            </a>
          </div>

          {downloadState === "downloading" && (
            <div className="px-6 py-3 bg-[#EBF0E6] dark:bg-[#202E28] border-t border-[#E2E5DD] dark:border-[#26372F] text-xs font-mono text-[#173D32] dark:text-[#C8D8B9] flex items-center justify-between">
              <span className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-[#173D32] dark:bg-[#C8D8B9] animate-ping" />
                <span>Downloading APK...</span>
              </span>
              <span>{progress}%</span>
            </div>
          )}

          {downloadState === "downloaded" && (
            <div className="px-6 py-3 bg-[#EBF0E6] dark:bg-[#202E28] border-t border-[#E2E5DD] dark:border-[#26372F] text-xs font-mono text-[#173D32] dark:text-[#C8D8B9] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <span className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 shrink-0" />
                <span>Download completed ({APP_CONFIG.release.apkSize}). Tap the download notification to install.</span>
              </span>
              <button
                onClick={openInstallDialog}
                className="underline hover:opacity-80 text-left"
              >
                Show install steps
              </button>
            </div>
          )}

        </div>

        {/* Reassuring Android Permission Notice */}
        <div className="mt-8 p-6 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded transition-colors">
          <div className="text-xs font-mono uppercase tracking-wider text-[#173D32] dark:text-[#C8D8B9] font-semibold mb-2">
            Installation Notice for Android
          </div>
          <p className="text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
            When installing directly from a browser for the first time, Android displays a standard prompt: <em>&ldquo;For your security, your phone is not allowed to install unknown apps from this source.&rdquo;</em> This is standard behavior for all direct installations outside Google Play. Tap the completed download notification to open the APK, then tap <strong>Settings</strong>, toggle <strong>Allow from this source</strong>, and tap <strong>Install</strong>.
          </p>
          <p className="mt-3 text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
            Upgrading from a build signed with the old certificate (v2.3 or
            earlier)? Android may ask you to uninstall it once before installing
            v{APP_CONFIG.release.version}.
          </p>
        </div>

      </div>
    </section>
  );
}
