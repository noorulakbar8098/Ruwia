"use client";

import { useState } from "react";
import Image from "next/image";
import {
  Download,
  Copy,
  Check,
  ShieldCheck,
  AlertTriangle,
  ExternalLink,
  Info,
  Sparkles,
  Smartphone,
  HardDrive,
  Calendar,
  Layers,
  Cpu,
  FileCode2,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

export default function DownloadCard() {
  const [copiedSha, setCopiedSha] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [downloadTriggered, setDownloadTriggered] = useState(false);

  const handleCopySha = () => {
    navigator.clipboard.writeText(APP_CONFIG.release.sha256);
    setCopiedSha(true);
    setTimeout(() => setCopiedSha(false), 2500);
  };

  const handleDownload = () => {
    setDownloading(true);
    window.location.href = APP_CONFIG.release.downloadUrl;
    setTimeout(() => {
      setDownloading(false);
      setDownloadTriggered(true);
    }, 1200);
  };

  return (
    <section id="download" className="py-20 sm:py-28 relative overflow-hidden">
      {/* Background Radial Glow */}
      <div className="absolute inset-0 radial-glow pointer-events-none -z-10" />

      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-12 sm:mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-4">
            <Download className="w-3.5 h-3.5" />
            <span>Official Distribution Channel</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Download Ruwia APK
          </h2>
          <p className="mt-4 text-base sm:text-lg text-slate-600 dark:text-slate-400">
            Get the authentic, signed Android package directly from our GitHub Releases deployment. No app store lock-in or forced updates.
          </p>
        </div>

        {/* Master Download Card */}
        <div className="relative rounded-3xl sm:rounded-[32px] border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/90 shadow-2xl shadow-emerald-950/10 p-6 sm:p-10 lg:p-12 overflow-hidden backdrop-blur-xl">
          
          {/* Subtle Top Accent Bar */}
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-emerald-500 via-teal-400 to-emerald-600" />

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-10 items-center">
            
            {/* Left: App Identity & Specifications */}
            <div className="lg:col-span-7 space-y-6">
              
              {/* Brand Header inside card */}
              <div className="flex items-center gap-4">
                <div className="relative w-16 h-16 rounded-2xl overflow-hidden shadow-lg border border-emerald-500/30 bg-slate-900 shrink-0">
                  <Image
                    src={APP_CONFIG.app.logoPath}
                    alt={APP_CONFIG.app.name}
                    width={64}
                    height={64}
                    className="object-cover"
                  />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-2xl font-extrabold text-slate-900 dark:text-white">
                      {APP_CONFIG.app.name}
                    </h3>
                    <span className="px-2 py-0.5 rounded-md text-xs font-bold bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30">
                      v{APP_CONFIG.release.version}
                    </span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
                    Package: <code className="font-mono text-emerald-600 dark:text-emerald-400">{APP_CONFIG.app.packageName}</code>
                  </p>
                </div>
              </div>

              {/* Technical Specifications Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-2">
                <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200/80 dark:border-slate-800/80">
                  <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 uppercase">
                    <HardDrive className="w-3.5 h-3.5 text-emerald-500" />
                    <span>APK Size</span>
                  </div>
                  <div className="mt-1 text-sm font-bold text-slate-900 dark:text-white">
                    {APP_CONFIG.release.apkSize}
                  </div>
                </div>

                <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200/80 dark:border-slate-800/80">
                  <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 uppercase">
                    <Calendar className="w-3.5 h-3.5 text-emerald-500" />
                    <span>Released</span>
                  </div>
                  <div className="mt-1 text-sm font-bold text-slate-900 dark:text-white">
                    {APP_CONFIG.release.releaseDate}
                  </div>
                </div>

                <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200/80 dark:border-slate-800/80">
                  <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 uppercase">
                    <Smartphone className="w-3.5 h-3.5 text-emerald-500" />
                    <span>Min Android</span>
                  </div>
                  <div className="mt-1 text-sm font-bold text-slate-900 dark:text-white">
                    7.0+ (API 24)
                  </div>
                </div>

                <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200/80 dark:border-slate-800/80">
                  <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 uppercase">
                    <Layers className="w-3.5 h-3.5 text-emerald-500" />
                    <span>Target SDK</span>
                  </div>
                  <div className="mt-1 text-sm font-bold text-slate-900 dark:text-white">
                    API 36 (Android 16)
                  </div>
                </div>

                <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200/80 dark:border-slate-800/80">
                  <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 uppercase">
                    <Cpu className="w-3.5 h-3.5 text-emerald-500" />
                    <span>Architecture</span>
                  </div>
                  <div className="mt-1 text-sm font-bold text-slate-900 dark:text-white">
                    Universal (All)
                  </div>
                </div>

                <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200/80 dark:border-slate-800/80">
                  <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 uppercase">
                    <FileCode2 className="w-3.5 h-3.5 text-emerald-500" />
                    <span>Build Type</span>
                  </div>
                  <div className="mt-1 text-sm font-bold text-emerald-600 dark:text-emerald-400">
                    Signed Production
                  </div>
                </div>
              </div>

              {/* SHA-256 Checksum Verification Box */}
              <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-950/80 border border-slate-200 dark:border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <ShieldCheck className="w-4 h-4 text-emerald-500" />
                    <span>SHA-256 Checksum</span>
                  </div>
                  <button
                    onClick={handleCopySha}
                    type="button"
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10 transition-colors"
                  >
                    {copiedSha ? (
                      <>
                        <Check className="w-3.5 h-3.5" />
                        <span>Copied!</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>Copy Checksum</span>
                      </>
                    )}
                  </button>
                </div>
                <div className="font-mono text-[11px] sm:text-xs text-slate-600 dark:text-slate-400 break-all bg-white dark:bg-slate-900 p-2.5 rounded-xl border border-slate-200 dark:border-slate-800/80 select-all">
                  {APP_CONFIG.release.sha256}
                </div>
                <p className="mt-2 text-[11px] text-slate-500 flex items-center gap-1">
                  <Info className="w-3 h-3 text-slate-400" />
                  <span>Verify with: <code>sha256sum ruwia-v2.3-release.apk</code></span>
                </p>
              </div>

            </div>

            {/* Right: Primary Download Action & Instructions */}
            <div className="lg:col-span-5 flex flex-col justify-center space-y-6 lg:border-l lg:border-slate-200 lg:dark:border-slate-800 lg:pl-10">
              
              {/* Big CTA Button */}
              <div>
                <button
                  onClick={handleDownload}
                  disabled={downloading}
                  type="button"
                  className="w-full group relative flex items-center justify-center gap-3 px-8 py-5 rounded-2xl text-lg font-bold text-slate-950 bg-emerald-400 hover:bg-emerald-300 active:bg-emerald-500 shadow-xl shadow-emerald-500/25 transition-all duration-300 hover:shadow-emerald-500/40 hover:-translate-y-1 focus:outline-none"
                >
                  {downloading ? (
                    <>
                      <div className="w-6 h-6 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
                      <span>Connecting CDN...</span>
                    </>
                  ) : (
                    <>
                      <Download className="w-6 h-6 transition-transform group-hover:scale-110" />
                      <span>Download APK</span>
                      <span className="text-xs px-2 py-0.5 rounded-md bg-slate-950/15 font-mono">
                        v{APP_CONFIG.release.version}
                      </span>
                    </>
                  )}
                </button>

                {downloadTriggered && (
                  <div className="mt-3 p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-700 dark:text-emerald-300 flex items-center gap-2 animate-in fade-in">
                    <ShieldCheck className="w-4 h-4 shrink-0 text-emerald-500" />
                    <span>Your download has started! Check your browser's download tray.</span>
                  </div>
                )}
              </div>

              {/* GitHub Releases and Mirrors */}
              <div className="space-y-2">
                <a
                  href={APP_CONFIG.release.githubReleaseUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-semibold text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-950/60 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-800 transition-colors"
                >
                  <span className="flex items-center gap-2">
                    <ExternalLink className="w-4 h-4 text-emerald-500" />
                    View Release on GitHub
                  </span>
                  <span className="text-xs text-slate-400">Tagged v{APP_CONFIG.release.version}</span>
                </a>

                <a
                  href="#releases"
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs sm:text-sm font-semibold text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-950/60 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-800 transition-colors"
                >
                  <span>Previous Releases & Changelogs</span>
                  <span className="text-xs text-emerald-600 dark:text-emerald-400">v2.2, v2.0 →</span>
                </a>
              </div>

              {/* Security Advisory / Notice */}
              <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-slate-800 dark:text-slate-200 text-xs leading-relaxed">
                <div className="flex items-center gap-2 font-bold text-amber-700 dark:text-amber-400 mb-1.5">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>Android Security Notice</span>
                </div>
                <p className="text-slate-600 dark:text-slate-400">
                  When downloading directly, Android or Chrome may show a standard prompt: <em>&ldquo;File might be harmful&rdquo;</em> or request <em>&ldquo;Allow from this source&rdquo;</em>. This is normal for all direct APK downloads outside Google Play. Our releases are 100% verified, clean, and malware-free.
                </p>
              </div>

            </div>

          </div>
        </div>

      </div>
    </section>
  );
}
