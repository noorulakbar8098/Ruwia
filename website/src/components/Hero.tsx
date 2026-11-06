"use client";

import { useState } from "react";
import Image from "next/image";
import {
  Download,
  ShieldCheck,
  Smartphone,
  Calendar,
  HardDrive,
  Cpu,
  ArrowRight,
  CheckCircle2,
  Sparkles,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

export default function Hero() {
  const [downloading, setDownloading] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);

  const handleDownloadClick = () => {
    setDownloading(true);
    window.location.href = APP_CONFIG.release.downloadUrl;
    setTimeout(() => {
      setDownloading(false);
      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 5000);
    }, 1200);
  };

  return (
    <section className="relative pt-8 pb-16 sm:pt-14 sm:pb-24 overflow-hidden">
      {/* Background Radial Glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-7xl h-[620px] pointer-events-none radial-glow-hero -z-10" />

      {/* Subtle Dot Grid */}
      <div className="absolute inset-0 bg-grid-pattern opacity-60 -z-20" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          
          {/* Left Column: Headlines & CTA */}
          <div className="lg:col-span-7 flex flex-col items-start text-left">
            
            {/* Version & Security Pill Badge */}
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 dark:bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 text-xs sm:text-sm font-semibold mb-6 shadow-sm backdrop-blur-md">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              <span>Official Release v{APP_CONFIG.release.version}</span>
              <span className="text-slate-300 dark:text-slate-600">|</span>
              <span className="flex items-center gap-1 text-slate-600 dark:text-slate-300 font-normal">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                Signed APK
              </span>
            </div>

            {/* Main Headline */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-900 dark:text-white leading-[1.12]">
              Your App. <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-500 via-teal-400 to-emerald-400">Your Device.</span>
              <br />
              Your Experience.
            </h1>

            {/* Sub-headline description */}
            <p className="mt-5 text-base sm:text-lg lg:text-xl text-slate-600 dark:text-slate-300 max-w-2xl leading-relaxed">
              {APP_CONFIG.app.heroDescription}
            </p>

            {/* Quick Specs Ribbon */}
            <div className="mt-8 grid grid-cols-2 sm:grid-cols-4 gap-3 w-full max-w-xl">
              <div className="p-3 rounded-2xl border border-slate-200 dark:border-slate-800/80 bg-white/70 dark:bg-slate-900/60 backdrop-blur-sm">
                <div className="flex items-center gap-1.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <HardDrive className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                  <span>Size</span>
                </div>
                <div className="mt-1 text-sm font-bold text-slate-800 dark:text-slate-100 truncate">{APP_CONFIG.release.apkSize}</div>
              </div>

              <div className="p-3 rounded-2xl border border-slate-200 dark:border-slate-800/80 bg-white/70 dark:bg-slate-900/60 backdrop-blur-sm">
                <div className="flex items-center gap-1.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <Calendar className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                  <span>Updated</span>
                </div>
                <div className="mt-1 text-sm font-bold text-slate-800 dark:text-slate-100 truncate">{APP_CONFIG.release.releaseDate}</div>
              </div>

              <div className="p-3 rounded-2xl border border-slate-200 dark:border-slate-800/80 bg-white/70 dark:bg-slate-900/60 backdrop-blur-sm">
                <div className="flex items-center gap-1.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <Smartphone className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                  <span>Android</span>
                </div>
                <div className="mt-1 text-sm font-bold text-slate-800 dark:text-slate-100 truncate">7.0 to 16</div>
              </div>

              <div className="p-3 rounded-2xl border border-slate-200 dark:border-slate-800/80 bg-white/70 dark:bg-slate-900/60 backdrop-blur-sm">
                <div className="flex items-center gap-1.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <Cpu className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                  <span>Arch</span>
                </div>
                <div className="mt-1 text-sm font-bold text-slate-800 dark:text-slate-100 truncate">Universal</div>
              </div>
            </div>

            {/* CTA Buttons Row */}
            <div className="mt-8 flex flex-col sm:flex-row items-stretch sm:items-center gap-3.5 w-full sm:w-auto">
              <button
                onClick={handleDownloadClick}
                disabled={downloading}
                type="button"
                className="group relative inline-flex items-center justify-center gap-3 px-8 py-4 rounded-2xl text-base font-bold text-slate-950 bg-emerald-400 hover:bg-emerald-300 active:bg-emerald-500 shadow-xl shadow-emerald-500/25 transition-all duration-300 hover:shadow-emerald-500/40 hover:-translate-y-0.5 focus:outline-none"
              >
                {downloading ? (
                  <>
                    <div className="w-5 h-5 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
                    <span>Initiating Download...</span>
                  </>
                ) : downloadSuccess ? (
                  <>
                    <CheckCircle2 className="w-5 h-5 text-emerald-950" />
                    <span>Downloading APK!</span>
                  </>
                ) : (
                  <>
                    <Download className="w-5 h-5 transition-transform duration-200 group-hover:scale-110" />
                    <span>Download for Android</span>
                    <span className="px-2 py-0.5 rounded-md text-xs font-mono font-bold bg-slate-950/15">
                      {APP_CONFIG.release.apkSize}
                    </span>
                  </>
                )}
              </button>

              <a
                href="#installation"
                className="inline-flex items-center justify-center gap-2 px-6 py-4 rounded-2xl text-base font-semibold text-slate-700 dark:text-slate-200 bg-slate-100 dark:bg-slate-900/80 hover:bg-slate-200 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-800 transition-colors"
              >
                <span>How to Install</span>
                <ArrowRight className="w-4 h-4 text-emerald-500" />
              </a>
            </div>

            {/* Download Security Subtext */}
            <div className="mt-4 flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
              <span>Official GitHub Releases link · Zero telemetry · 100% ad-free</span>
            </div>

          </div>

          {/* Right Column: Premium Smartphone Frame */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end">
            <div className="relative mx-auto w-[290px] sm:w-[325px] aspect-[390/844] rounded-[50px] p-3 bg-gradient-to-b from-slate-700 via-slate-800 to-slate-900 shadow-[0_30px_70px_-15px_rgba(0,0,0,0.8)] border border-slate-700/80 ring-1 ring-white/10 transition-transform duration-500 hover:scale-[1.02]">
              
              {/* Screen Bezel */}
              <div className="relative w-full h-full rounded-[40px] overflow-hidden bg-[#090D16]">
                <Image
                  src="/images/screenshots/dashboard.svg"
                  alt="Ruwia Dashboard Preview"
                  fill
                  sizes="(max-width: 640px) 290px, 325px"
                  className="object-contain"
                  priority
                />
              </div>

              {/* Glass Reflection Highlight */}
              <div className="pointer-events-none absolute inset-0 rounded-[50px] bg-gradient-to-tr from-transparent via-white/[0.04] to-transparent" />
            </div>
          </div>

        </div>
      </div>
    </section>
  );
}
