"use client";

import Image from "next/image";
import { Download, ArrowRight, ShieldCheck, CheckCircle2, Play } from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";
import { useDownload } from "@/context/DownloadContext";

export default function Hero() {
  const {
    downloadState,
    progress,
    downloadedMB,
    totalMB,
    startDownload,
    openInstallDialog,
    resetDownload,
  } = useDownload();

  const handlePrimaryClick = () => {
    if (downloadState === "downloaded") {
      openInstallDialog();
    } else if (downloadState === "idle") {
      startDownload();
    }
  };

  return (
    <section className="relative pt-12 pb-20 sm:pt-20 sm:pb-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Asymmetric Editorial Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center">

          {/* Left Column: Editorial Content */}
          <div className="lg:col-span-7 flex flex-col items-start">

            {/* Category Eyebrow (No versions in top section) */}
            <div className="flex items-center gap-3 mb-6">
              <span className="inline-block w-2 h-2 rounded-full bg-[#173D32] dark:bg-[#C8D8B9]" />
              <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
                Direct Android Distribution
              </span>
            </div>

            {/* Main Headline */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1] leading-[1.08]">
              Technology that works <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">around you.</span>
            </h1>

            {/* Description */}
            <p className="mt-6 text-base sm:text-lg text-[#777D76] dark:text-[#8E9B93] max-w-xl leading-relaxed">
              A thoughtfully built digital experience, designed to make everyday tasks simpler, faster, and more connected. Download the verified production Android application directly to your device.
            </p>

            {/* CTA & Explore Row */}
            <div className="mt-8 flex flex-col sm:flex-row items-stretch sm:items-center gap-4 w-full sm:w-auto">
              {/* Primary Action Button: Transforms into Open when downloaded */}
              <button
                onClick={handlePrimaryClick}
                disabled={downloadState === "downloading"}
                type="button"
                className={`inline-flex items-center justify-center gap-2.5 px-7 py-3.5 rounded text-sm font-semibold uppercase tracking-wider transition-all focus:outline-none ${
                  downloadState === "downloaded"
                    ? "text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1] shadow-md ring-2 ring-[#C8D8B9]/60 dark:ring-[#173D32]/60 transform hover:scale-[1.02] active:scale-[0.98]"
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
                    <span className="ml-1 text-[11px] font-mono opacity-90">
                      (Downloaded)
                    </span>
                  </>
                ) : (
                  <>
                    <Download className="w-4 h-4 text-[#C8D8B9] dark:text-[#111714]" />
                    <span>Download for Android</span>
                    <span className="ml-1 text-[11px] font-mono opacity-80">
                      ({APP_CONFIG.release.apkSize})
                    </span>
                  </>
                )}
              </button>

              <a
                href="#overview"
                className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded text-sm font-semibold uppercase tracking-wider text-[#202521] dark:text-[#F3F6F1] hover:text-[#173D32] dark:hover:text-[#C8D8B9] border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] hover:bg-[#EBF0E6]/50 dark:hover:bg-[#202E28] transition-colors"
              >
                <span>Explore Product</span>
                <ArrowRight className="w-4 h-4 text-[#777D76] dark:text-[#8E9B93]" />
              </a>
            </div>

            {/* Horizontal Line Download Status Indicator (Top First Screen) */}
            {downloadState === "downloading" && (
              <div className="w-full max-w-xl mt-5 p-4 rounded-lg bg-[#EBF0E6] dark:bg-[#18221D] border border-[#C8D8B9] dark:border-[#26372F] transition-all">
                <div className="flex items-center justify-between text-xs font-mono mb-2">
                  <span className="text-[#173D32] dark:text-[#C8D8B9] font-medium flex items-center gap-2">
                    <span className="relative flex h-2 w-2">
                      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#173D32] dark:bg-[#C8D8B9] opacity-75"></span>
                      <span className="relative inline-flex rounded-full h-2 w-2 bg-[#173D32] dark:bg-[#C8D8B9]"></span>
                    </span>
                    Downloading ruwia-v{APP_CONFIG.release.version}-release.apk
                  </span>
                  <span className="text-[#202521] dark:text-[#F3F6F1] font-semibold">
                    {downloadedMB} / {totalMB} ({progress}%)
                  </span>
                </div>

                {/* Horizontal Progress Bar Line */}
                <div className="w-full bg-[#D2D8CA] dark:bg-[#26372F] h-2.5 rounded-full overflow-hidden">
                  <div
                    className="bg-[#173D32] dark:bg-[#C8D8B9] h-full rounded-full transition-all duration-100 ease-out"
                    style={{ width: `${progress}%` }}
                  />
                </div>

                <div className="mt-2 flex items-center justify-between text-[11px] text-[#777D76] dark:text-[#8E9B93] font-mono">
                  <span>Source: Official uniwattelektrik.com download</span>
                  <span>Status: In Progress ({progress}%)</span>
                </div>
              </div>
            )}

            {downloadState === "downloaded" && (
              <div className="w-full max-w-xl mt-5 p-4 rounded-lg bg-[#EBF0E6] dark:bg-[#18221D] border border-[#173D32]/30 dark:border-[#C8D8B9]/30 transition-all">
                <div className="flex items-center justify-between text-xs font-mono mb-2">
                  <span className="text-[#173D32] dark:text-[#C8D8B9] font-semibold flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-[#173D32] dark:text-[#C8D8B9]" />
                    <span>Download completed · {totalMB}</span>
                  </span>
                  <button
                    onClick={resetDownload}
                    type="button"
                    className="text-[11px] text-[#777D76] dark:text-[#8E9B93] hover:text-[#173D32] dark:hover:text-[#C8D8B9] underline cursor-pointer"
                  >
                    Download again
                  </button>
                </div>

                {/* Horizontal Completed Line */}
                <div className="w-full bg-[#173D32] dark:bg-[#C8D8B9] h-2 rounded-full" />

                <div className="mt-2 flex items-center justify-between text-[11px] text-[#777D76] dark:text-[#8E9B93] font-mono">
                  <span>APK saved to your Downloads folder</span>
                  <button
                    onClick={openInstallDialog}
                    className="text-[#173D32] dark:text-[#C8D8B9] font-semibold hover:underline"
                  >
                    Tap the download notification to install →
                  </button>
                </div>
              </div>
            )}

            {/* Security Subtext */}
            <div className="mt-4 flex items-center gap-2 text-xs text-[#777D76] dark:text-[#8E9B93]">
              <ShieldCheck className="w-4 h-4 text-[#315E4C] dark:text-[#C8D8B9] shrink-0" />
              <span>Official signed build · SHA-256 verified · No Google Play account required</span>
            </div>

            {/* Architectural Specs Ribbon: Initial detail screen showing version name, app size, and updated on */}
            <div className="mt-10 pt-6 border-t border-[#E2E5DD] dark:border-[#26372F] grid grid-cols-2 sm:grid-cols-4 gap-6 w-full max-w-xl">
              <div>
                <div className="text-[10px] font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">Version</div>
                <div className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1] font-mono">v{APP_CONFIG.release.version}</div>
              </div>
              <div>
                <div className="text-[10px] font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">Size</div>
                <div className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1] font-mono">{APP_CONFIG.release.apkSize}</div>
              </div>
              <div>
                <div className="text-[10px] font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">Updated On</div>
                <div className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">{APP_CONFIG.release.releaseDate}</div>
              </div>
              <div>
                <div className="text-[10px] font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">Compatibility</div>
                <div className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">Android 7.0+</div>
              </div>
            </div>

          </div>

          {/* Right Column: Architectural Device Showcase */}
          <div className="lg:col-span-5 flex flex-col items-center lg:items-end">
            <div className="relative w-full max-w-[340px]">

              {/* Top Editorial Annotation Badge (No version in top section badge) */}
              <div className="absolute -top-4 -left-4 z-20 px-3 py-1 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded text-[10px] font-mono uppercase tracking-wider text-[#173D32] dark:text-[#C8D8B9] shadow-sm">
                Built for everyday use
              </div>

              {/* Primary Realistic Device Frame */}
              <div className="relative w-full drop-shadow-[0_20px_40px_rgba(0,0,0,0.18)] dark:drop-shadow-[0_25px_50px_rgba(0,0,0,0.7)] transition-transform duration-300 hover:scale-[1.01]">
                <Image
                  src={APP_CONFIG.app.heroScreenshotPath}
                  alt={`${APP_CONFIG.app.name} Owner Dashboard`}
                  width={1469}
                  height={3104}
                  className="w-full h-auto object-contain"
                  priority
                />
              </div>

              {/* Bottom Caption (No version in top section caption) */}
              <div className="mt-4 flex items-center justify-between text-[11px] font-mono text-[#777D76] dark:text-[#8E9B93] px-2">
                <span>Figure 1.0 — Real-Time Owner Dashboard</span>
                <span>Verified Production Build</span>
              </div>

            </div>
          </div>

        </div>

      </div>
    </section>
  );
}
