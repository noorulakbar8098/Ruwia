"use client";

import { useState } from "react";
import {
  Download,
  Clock,
  FolderOpen,
  ShieldAlert,
  CheckCircle,
  Play,
  HelpCircle,
  Smartphone,
  ChevronDown,
  Info,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

export default function InstallationGuide() {
  const [activeTab, setActiveTab] = useState<"modern" | "mid" | "legacy">("modern");
  const [showTroubleshoot, setShowTroubleshoot] = useState(false);

  const stepIcons = [
    Download,
    Clock,
    FolderOpen,
    ShieldAlert,
    CheckCircle,
    Play,
  ];

  return (
    <section id="installation" className="py-20 sm:py-28 relative bg-slate-50/50 dark:bg-slate-900/30 border-y border-slate-200/60 dark:border-slate-800/60">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-4">
            <Smartphone className="w-3.5 h-3.5" />
            <span>Fast & Simple Setup</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            How to Install on Android
          </h2>
          <p className="mt-4 text-base sm:text-lg text-slate-600 dark:text-slate-400">
            Installing an APK directly takes less than 30 seconds. Follow these 6 straightforward steps.
          </p>
        </div>

        {/* 6 Steps Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
          {APP_CONFIG.installationSteps.map((step, idx) => {
            const Icon = stepIcons[idx] || CheckCircle;
            return (
              <div
                key={step.step}
                className="relative p-7 rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/80 shadow-sm hover:shadow-lg transition-all duration-300 flex flex-col justify-between"
              >
                <div>
                  {/* Step Number & Icon */}
                  <div className="flex items-center justify-between mb-5">
                    <span className="flex items-center justify-center w-9 h-9 rounded-xl bg-emerald-500 text-slate-950 font-extrabold text-base shadow-md shadow-emerald-500/20">
                      {step.step}
                    </span>
                    <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                      <Icon className="w-5 h-5 text-emerald-500" />
                    </div>
                  </div>

                  {/* Title & Description */}
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-2">
                    {step.title}
                  </h3>
                  <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
                    {step.description}
                  </p>
                </div>

                {/* Helpful Tip */}
                <div className="mt-5 pt-4 border-t border-slate-100 dark:border-slate-800/80 flex items-start gap-2 text-xs text-slate-500 dark:text-slate-400">
                  <Info className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                  <span>{step.tip}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Android Version Specific Instructions */}
        <div className="mt-14 max-w-4xl mx-auto rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 sm:p-8 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 dark:border-slate-800 pb-5 mb-5">
            <div>
              <h4 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white">
                Version-Specific Permissions
              </h4>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
                Depending on your Android OS version, the unknown apps permission screen looks slightly different:
              </p>
            </div>

            {/* Version Tabs */}
            <div className="flex items-center p-1 rounded-xl bg-slate-100 dark:bg-slate-800 shrink-0">
              <button
                onClick={() => setActiveTab("modern")}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                  activeTab === "modern"
                    ? "bg-white dark:bg-slate-900 text-emerald-600 dark:text-emerald-400 shadow-sm"
                    : "text-slate-600 dark:text-slate-400"
                }`}
              >
                Android 14 – 16
              </button>
              <button
                onClick={() => setActiveTab("mid")}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                  activeTab === "mid"
                    ? "bg-white dark:bg-slate-900 text-emerald-600 dark:text-emerald-400 shadow-sm"
                    : "text-slate-600 dark:text-slate-400"
                }`}
              >
                Android 8 – 13
              </button>
              <button
                onClick={() => setActiveTab("legacy")}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                  activeTab === "legacy"
                    ? "bg-white dark:bg-slate-900 text-emerald-600 dark:text-emerald-400 shadow-sm"
                    : "text-slate-600 dark:text-slate-400"
                }`}
              >
                Android 7
              </button>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200/80 dark:border-slate-800/80 text-sm leading-relaxed text-slate-700 dark:text-slate-300">
            {activeTab === "modern" && (
              <div className="space-y-2">
                <div className="font-semibold text-emerald-600 dark:text-emerald-400 flex items-center gap-2">
                  <Smartphone className="w-4 h-4" />
                  <span>Modern Android (14, 15, and 16)</span>
                </div>
                <p>{APP_CONFIG.androidVersionNotes.android14Plus}</p>
                <p className="text-xs text-slate-500">
                  Tip: Once granted for Chrome or your File Manager, future APK updates will install immediately in one tap without re-prompting.
                </p>
              </div>
            )}

            {activeTab === "mid" && (
              <div className="space-y-2">
                <div className="font-semibold text-emerald-600 dark:text-emerald-400 flex items-center gap-2">
                  <Smartphone className="w-4 h-4" />
                  <span>Android 8.0 through Android 13</span>
                </div>
                <p>{APP_CONFIG.androidVersionNotes.android8to13}</p>
              </div>
            )}

            {activeTab === "legacy" && (
              <div className="space-y-2">
                <div className="font-semibold text-emerald-600 dark:text-emerald-400 flex items-center gap-2">
                  <Smartphone className="w-4 h-4" />
                  <span>Android 7.0 & 7.1 (Nougat)</span>
                </div>
                <p>{APP_CONFIG.androidVersionNotes.android7}</p>
              </div>
            )}
          </div>
        </div>

        {/* Play Protect Troubleshooting Accordion */}
        <div className="mt-6 max-w-4xl mx-auto">
          <button
            onClick={() => setShowTroubleshoot(!showTroubleshoot)}
            type="button"
            className="w-full flex items-center justify-between p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-left hover:border-emerald-500/40 transition-colors"
          >
            <div className="flex items-center gap-2.5">
              <HelpCircle className="w-4 h-4 text-emerald-500 shrink-0" />
              <span className="text-sm font-bold text-slate-800 dark:text-slate-200">
                Seeing a Google Play Protect warning? Here is what to do
              </span>
            </div>
            <ChevronDown
              className={`w-4 h-4 text-slate-400 transition-transform duration-200 ${
                showTroubleshoot ? "rotate-180" : ""
              }`}
            />
          </button>

          {showTroubleshoot && (
            <div className="mt-2 p-5 rounded-2xl bg-slate-100/70 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800 text-xs sm:text-sm text-slate-600 dark:text-slate-400 space-y-2 animate-in fade-in">
              <p>
                Because this APK is distributed directly outside the Google Play Store, Google Play Protect may show a prompt: <strong>&ldquo;Unrecognized app&rdquo;</strong>.
              </p>
              <div className="p-3 rounded-xl bg-white dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-200 font-medium">
                Solution: Tap <strong>&ldquo;More details&rdquo;</strong>, then tap <strong>&ldquo;Install anyway&rdquo;</strong>.
              </div>
              <p>
                Ruwia is 100% clean, contains no ads or trackers, and is signed with our verified developer certificate.
              </p>
            </div>
          )}
        </div>

      </div>
    </section>
  );
}
