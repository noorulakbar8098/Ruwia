"use client";

import { useState } from "react";
import { Download, ChevronDown } from "lucide-react";
import { APP_CONFIG, ReleaseItem } from "@/config/app.config";

export default function ReleaseHistory() {
  const [expandedVersions, setExpandedVersions] = useState<Record<string, boolean>>({
    [APP_CONFIG.releases[0]?.version || "2.4"]: true,
  });

  const toggleExpand = (version: string) => {
    setExpandedVersions((prev) => ({
      ...prev,
      [version]: !prev[version],
    }));
  };

  return (
    <section id="releases" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Eyebrow & Header (No slashes) */}
        <div className="flex items-center gap-2 mb-6">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            Releases &amp; Changelogs
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] gap-6">
          <div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              Historical release <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">ledger &amp; notes.</span>
            </h2>
          </div>
          <p className="text-sm sm:text-base text-[#777D76] dark:text-[#8E9B93] max-w-sm leading-relaxed">
            Every published production build is cryptographically archived with full changelog accountability.
          </p>
        </div>

        {/* Releases Ledger Stack */}
        <div className="space-y-4">
          {APP_CONFIG.releases.map((rel: ReleaseItem) => {
            const isExpanded = !!expandedVersions[rel.version];
            return (
              <div
                key={rel.version}
                className={`border rounded transition-colors overflow-hidden ${
                  rel.isLatest
                    ? "border-[#173D32] dark:border-[#C8D8B9] bg-white dark:bg-[#18221D]"
                    : "border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D]"
                }`}
              >
                {/* Header row */}
                <div
                  onClick={() => toggleExpand(rel.version)}
                  className="cursor-pointer p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 select-none hover:bg-[#F7F7F2]/60 dark:hover:bg-[#151E1A]/60 transition-colors"
                >
                  <div className="flex items-center gap-4">
                    <div className="font-mono text-xl font-bold text-[#202521] dark:text-[#F3F6F1]">
                      v{rel.version}
                    </div>

                    {rel.isLatest && (
                      <span className="px-2.5 py-0.5 rounded text-[10px] font-mono uppercase tracking-wider bg-[#173D32] text-[#F7F7F2] dark:bg-[#C8D8B9] dark:text-[#111714]">
                        Latest Stable
                      </span>
                    )}

                    <span className="text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
                      Build #{rel.versionCode} · {rel.releaseDate}
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    {rel.isLatest && rel.downloadUrl ? (
                      <a
                        href={rel.downloadUrl}
                        download={`ruwia-v${rel.version}-release.apk`}
                        onClick={(e) => e.stopPropagation()}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono uppercase tracking-wider text-[#173D32] bg-[#EBF0E6] hover:bg-[#C8D8B9] dark:bg-[#202E28] dark:text-[#C8D8B9] dark:hover:bg-[#2F443B] transition-colors"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>APK ({rel.apkSize})</span>
                      </a>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93] bg-[#F7F7F2] dark:bg-[#151E1A] border border-[#E2E5DD] dark:border-[#26372F]">
                        Archived
                      </span>
                    )}

                    <button
                      type="button"
                      aria-label={isExpanded ? "Collapse notes" : "Expand notes"}
                      className="p-1.5 rounded text-[#777D76] dark:text-[#8E9B93] hover:text-[#202521] dark:hover:text-[#F3F6F1] transition-colors"
                    >
                      <ChevronDown
                        className={`w-4 h-4 transition-transform duration-150 ${
                          isExpanded ? "rotate-180" : ""
                        }`}
                      />
                    </button>
                  </div>
                </div>

                {/* Subtitle summary */}
                <div className="px-6 pb-4 -mt-2 text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93]">
                  {rel.highlights}
                </div>

                {/* Expanded Details Breakdown (No slashes in headings) */}
                {isExpanded && (
                  <div className="px-6 py-6 border-t border-[#E2E5DD] dark:border-[#26372F] bg-[#F7F7F2]/40 dark:bg-[#151E1A]/40 space-y-6">
                    
                    {/* New Features */}
                    {rel.features.length > 0 && (
                      <div>
                        <div className="text-[11px] font-mono uppercase tracking-widest text-[#173D32] dark:text-[#C8D8B9] font-semibold mb-2">
                          New Capabilities
                        </div>
                        <ul className="space-y-1.5 text-sm text-[#202521] dark:text-[#F3F6F1]">
                          {rel.features.map((feat, i) => (
                            <li key={i} className="flex items-start gap-2">
                              <span className="text-[#173D32] dark:text-[#C8D8B9] font-bold">—</span>
                              <span>{feat}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Improvements */}
                    {rel.improvements.length > 0 && (
                      <div>
                        <div className="text-[11px] font-mono uppercase tracking-widest text-[#315E4C] dark:text-[#C8D8B9] font-semibold mb-2">
                          Improvements &amp; Performance
                        </div>
                        <ul className="space-y-1.5 text-sm text-[#202521] dark:text-[#F3F6F1]">
                          {rel.improvements.map((imp, i) => (
                            <li key={i} className="flex items-start gap-2">
                              <span className="text-[#315E4C] dark:text-[#C8D8B9] font-bold">—</span>
                              <span>{imp}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Bug Fixes */}
                    {rel.fixes.length > 0 && (
                      <div>
                        <div className="text-[11px] font-mono uppercase tracking-widest text-[#777D76] dark:text-[#8E9B93] font-semibold mb-2">
                          Bug Fixes
                        </div>
                        <ul className="space-y-1.5 text-sm text-[#202521] dark:text-[#F3F6F1]">
                          {rel.fixes.map((fix, i) => (
                            <li key={i} className="flex items-start gap-2">
                              <span className="text-[#777D76] dark:text-[#8E9B93] font-bold">—</span>
                              <span>{fix}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Checksum & release status footer */}
                    <div className="pt-4 border-t border-[#E2E5DD] dark:border-[#26372F] flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
                      <span>SHA-256: {rel.sha256.substring(0, 24)}...</span>
                      <span className="inline-flex items-center gap-1 text-[#173D32] dark:text-[#C8D8B9]">
                        {rel.isLatest ? "Signed production release" : "Superseded"}
                      </span>
                    </div>

                  </div>
                )}
              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
}
