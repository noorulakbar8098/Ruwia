"use client";

import { useState } from "react";
import {
  History,
  Download,
  Tag,
  CheckCircle2,
  Wrench,
  Sparkles,
  ChevronDown,
  ExternalLink,
  ShieldCheck,
} from "lucide-react";
import { APP_CONFIG, ReleaseItem } from "@/config/app.config";

export default function ReleaseHistory() {
  const [expandedVersions, setExpandedVersions] = useState<Record<string, boolean>>({
    [APP_CONFIG.releases[0]?.version || "2.3"]: true,
  });

  const toggleExpand = (version: string) => {
    setExpandedVersions((prev) => ({
      ...prev,
      [version]: !prev[version],
    }));
  };

  return (
    <section id="releases" className="py-20 sm:py-28 relative">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-4">
            <History className="w-3.5 h-3.5" />
            <span>Changelog & Releases</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Release History
          </h2>
          <p className="mt-4 text-base sm:text-lg text-slate-600 dark:text-slate-400">
            Transparent release notes for every version of Ruwia. All builds are archived and available for download.
          </p>
        </div>

        {/* Releases Timeline Stack */}
        <div className="space-y-6">
          {APP_CONFIG.releases.map((rel: ReleaseItem) => {
            const isExpanded = !!expandedVersions[rel.version];
            return (
              <div
                key={rel.version}
                className={`rounded-3xl border transition-all duration-300 ${
                  rel.isLatest
                    ? "border-emerald-500/40 bg-white dark:bg-slate-900/90 shadow-xl shadow-emerald-950/5 ring-1 ring-emerald-500/20"
                    : "border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 shadow-sm"
                }`}
              >
                {/* Release Card Header */}
                <div
                  onClick={() => toggleExpand(rel.version)}
                  className="cursor-pointer p-6 sm:p-7 flex flex-col sm:flex-row sm:items-center justify-between gap-4 select-none"
                >
                  <div className="flex items-center gap-3.5">
                    <div className="flex items-center justify-center w-11 h-11 rounded-2xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 shrink-0">
                      <Tag className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2.5 flex-wrap">
                        <h3 className="text-xl font-bold text-slate-900 dark:text-white">
                          v{rel.version}
                        </h3>
                        {rel.isLatest && (
                          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500 text-slate-950 shadow-sm">
                            Latest Stable
                          </span>
                        )}
                        <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                          Build #{rel.versionCode} · {rel.releaseDate}
                        </span>
                      </div>
                      <p className="mt-1 text-xs sm:text-sm text-slate-600 dark:text-slate-400">
                        {rel.highlights}
                      </p>
                    </div>
                  </div>

                  {/* Actions & Chevron */}
                  <div className="flex items-center gap-3 self-end sm:self-auto shrink-0">
                    <a
                      href={rel.downloadUrl}
                      onClick={(e) => e.stopPropagation()}
                      className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-500 hover:text-slate-950 border border-emerald-500/30 transition-colors"
                    >
                      <Download className="w-3.5 h-3.5" />
                      <span>APK ({rel.apkSize})</span>
                    </a>

                    <button
                      type="button"
                      aria-label={isExpanded ? "Collapse changelog" : "Expand changelog"}
                      className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors"
                    >
                      <ChevronDown
                        className={`w-5 h-5 transition-transform duration-200 ${
                          isExpanded ? "rotate-180" : ""
                        }`}
                      />
                    </button>
                  </div>
                </div>

                {/* Expanded Changelog Breakdown */}
                {isExpanded && (
                  <div className="px-6 pb-7 sm:px-7 pt-2 border-t border-slate-100 dark:border-slate-800/80 space-y-5 animate-in fade-in duration-200">
                    
                    {/* New Features */}
                    {rel.features.length > 0 && (
                      <div>
                        <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400 mb-2">
                          <Sparkles className="w-3.5 h-3.5" />
                          <span>What's New</span>
                        </div>
                        <ul className="space-y-1.5 text-sm text-slate-700 dark:text-slate-300">
                          {rel.features.map((feat, i) => (
                            <li key={i} className="flex items-start gap-2">
                              <span className="text-emerald-500 font-bold">•</span>
                              <span>{feat}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Improvements */}
                    {rel.improvements.length > 0 && (
                      <div>
                        <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400 mb-2">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>Improvements & Performance</span>
                        </div>
                        <ul className="space-y-1.5 text-sm text-slate-700 dark:text-slate-300">
                          {rel.improvements.map((imp, i) => (
                            <li key={i} className="flex items-start gap-2">
                              <span className="text-blue-500 font-bold">•</span>
                              <span>{imp}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Bug Fixes */}
                    {rel.fixes.length > 0 && (
                      <div>
                        <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-amber-600 dark:text-amber-400 mb-2">
                          <Wrench className="w-3.5 h-3.5" />
                          <span>Bug Fixes</span>
                        </div>
                        <ul className="space-y-1.5 text-sm text-slate-700 dark:text-slate-300">
                          {rel.fixes.map((fix, i) => (
                            <li key={i} className="flex items-start gap-2">
                              <span className="text-amber-500 font-bold">•</span>
                              <span>{fix}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Footer Hash & Verification Details */}
                    <div className="pt-3 border-t border-slate-100 dark:border-slate-800/60 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-slate-500 dark:text-slate-400">
                      <div className="flex items-center gap-1.5">
                        <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                        <span>SHA-256: <code className="font-mono text-[11px]">{rel.sha256.substring(0, 16)}...{rel.sha256.substring(rel.sha256.length - 8)}</code></span>
                      </div>
                      <a
                        href={rel.githubReleaseUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400 hover:underline"
                      >
                        <span>GitHub Release Assets</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>

                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* View All on GitHub Banner */}
        <div className="mt-10 text-center">
          <a
            href={APP_CONFIG.release.allReleasesUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold text-slate-700 dark:text-slate-300 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
          >
            <span>Browse Complete Tag Archive on GitHub</span>
            <ExternalLink className="w-4 h-4 text-emerald-500" />
          </a>
        </div>

      </div>
    </section>
  );
}
